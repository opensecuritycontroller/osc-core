/*******************************************************************************
 * Copyright (c) Intel Corporation
 * Copyright (c) 2017
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package org.osc.core.broker.service.tasks.conformance.virtualsystem;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;

import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.osc.core.broker.job.TaskGraph;
import org.osc.core.broker.model.entities.appliance.VirtualSystem;
import org.osc.core.broker.model.entities.appliance.VmwareSoftwareVersion;
import org.osc.core.broker.model.plugin.ApiFactoryService;
import org.osc.core.broker.rest.client.nsx.model.VersionedDeploymentSpec;
import org.osc.core.broker.service.tasks.IgnoreCompare;
import org.osc.core.broker.service.tasks.TransactionalMetaTask;
import org.osc.core.broker.service.tasks.conformance.manager.MCConformanceCheckMetaTask;
import org.osc.core.broker.service.tasks.network.UpdateNsxDeploymentSpecTask;
import org.osc.core.broker.service.tasks.network.UpdateNsxServiceInstanceAttributesTask;
import org.osc.core.broker.service.tasks.passwordchange.UpdateNsxServiceAttributesTask;
import org.osc.sdk.sdn.api.DeploymentSpecApi;
import org.osc.sdk.sdn.element.DeploymentSpecElement;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * This task handles the following
 * adds deployment spec(s) to NSX after a new Distributed Appliance is added
 * updates deployment spec's ovf url on NSX when applianceSoftwareVersion is changed
 * updates Distributed Appliance Instance's service attributes when applianceSoftwareVersion is changed
 * handles upgrade case where a new ESX version is supported
 * synchronize expired/deleted deploymentspecs on NSX
 *
 */
@Component(service = NsxDeploymentSpecCheckMetaTask.class)
public class NsxDeploymentSpecCheckMetaTask extends TransactionalMetaTask {
    private static final Logger log = Logger.getLogger(MCConformanceCheckMetaTask.class);

    @Reference
    UpdateNsxServiceAttributesTask updateNsxServiceAttributesTask;

    @Reference
    UpdateNsxServiceInstanceAttributesTask updateNsxServiceInstanceAttributesTask;

    @Reference
    RegisterDeploymentSpecTask registerDeploymentSpecTask;

    @Reference
    UpdateNsxDeploymentSpecTask updateNsxDeploymentSpecTask;

    @IgnoreCompare
    @Reference
    ApiFactoryService apiFactoryService;

    private TaskGraph tg;
    private VirtualSystem vs;
    private boolean updateNsxServiceAttributesScheduled;

    public NsxDeploymentSpecCheckMetaTask create(VirtualSystem vs, boolean updateNsxServiceAttributesScheduled) {
        NsxDeploymentSpecCheckMetaTask task = new NsxDeploymentSpecCheckMetaTask();
        task.vs = vs;
        task.name = task.getName();
        task.updateNsxServiceAttributesScheduled = updateNsxServiceAttributesScheduled;
        task.updateNsxServiceAttributesTask = this.updateNsxServiceAttributesTask;
        task.updateNsxServiceInstanceAttributesTask = this.updateNsxServiceInstanceAttributesTask;
        task.registerDeploymentSpecTask = this.registerDeploymentSpecTask;
        task.updateNsxDeploymentSpecTask = this.updateNsxDeploymentSpecTask;
        task.apiFactoryService = this.apiFactoryService;
        task.dbConnectionManager = this.dbConnectionManager;
        task.txBroadcastUtil = this.txBroadcastUtil;

        return task;
    }

    @Override
    public void executeTransaction(EntityManager em) throws Exception {
        this.tg = new TaskGraph();
        this.vs = em.find(VirtualSystem.class, this.vs.getId());

        boolean deploymentSpecChanged = false;

        List<VmwareSoftwareVersion> vsSoftwareVersions = new ArrayList<>();
        Map<org.osc.core.broker.model.entities.appliance.VmwareSoftwareVersion, String> vsDeplSpecs = this.vs.getNsxDeploymentSpecIds();

        List<org.osc.core.broker.model.entities.appliance.VmwareSoftwareVersion> unorderedList = new ArrayList<>(vsDeplSpecs.keySet());
        Collections.sort(unorderedList);
        for(org.osc.core.broker.model.entities.appliance.VmwareSoftwareVersion vsv : unorderedList) {
            vsSoftwareVersions.add(VmwareSoftwareVersion.valueOf(vsv.name()));//now ordered by enum ordinal
        }

        DeploymentSpecApi deploymentSpecApi = this.apiFactoryService.createDeploymentSpecApi(this.vs);
        List<DeploymentSpecElement> deploymentSpecs = deploymentSpecApi.getDeploymentSpecs(this.vs.getNsxServiceId());
        List<VmwareSoftwareVersion> apiSoftwareVersions = new ArrayList<>();
        for (DeploymentSpecElement ds : deploymentSpecs){
            apiSoftwareVersions.add(VmwareSoftwareVersion.valueOf(
                    org.osc.core.broker.model.virtualization.VmwareSoftwareVersion.fromText(StringUtils.chomp(ds.getHostVersion(), RegisterDeploymentSpecTask.ALL_MINOR_VERSIONS))
                    .name()));
        }

        List<VmwareSoftwareVersion> nsxDepSpecsOutOfSync =
                getOutofSyncSpecs(vsSoftwareVersions, apiSoftwareVersions);

        if (this.vs.getNsxDeploymentSpecIds().isEmpty()){
            //new DA added(create DS's)
            List<VmwareSoftwareVersion> softVersions = Arrays.asList(VmwareSoftwareVersion.values());
            for (VmwareSoftwareVersion version : softVersions){
                this.tg.appendTask(this.registerDeploymentSpecTask.create(this.vs, version));
            }
        } else if (nsxDepSpecsOutOfSync.isEmpty() && vsSoftwareVersions.size() == VmwareSoftwareVersion.values().length){
            //specs are in sync on nsx and vs
            String imageName = this.vs.getApplianceSoftwareVersion().getImageUrl();
            String imageUrl = RegisterDeploymentSpecTask.generateOvfUrl(imageName);
            for (DeploymentSpecElement spec : ListUtils.emptyIfNull(deploymentSpecs)) {
                if (!StringUtils.isEmpty(spec.getImageUrl()) && !spec.getImageUrl().equals(imageUrl)) {
                    // DA/applianceSoftwareVersion changed
                    log.info("image url:" + imageUrl + " vds url:" + spec.getImageUrl());
                    this.tg.addTask(this.updateNsxDeploymentSpecTask.create(this.vs, new VersionedDeploymentSpec(spec)));
                    deploymentSpecChanged = true;
                }
            }
        } else  {
            //NSX DS not in sync with VS
            List<VmwareSoftwareVersion> softwareVersionsToSync =
                    new ArrayList<>(Arrays.asList(VmwareSoftwareVersion.values()));
            if (vsSoftwareVersions.size() < softwareVersionsToSync.size()) {
                //db version upgrade, add new deploymentspec for a new softwVer defined in VmwareSoftwareVersion
                List<VmwareSoftwareVersion> outOfSyncList = getOutofSyncSpecs(softwareVersionsToSync, apiSoftwareVersions);

                for (VmwareSoftwareVersion softVersion : outOfSyncList){
                    this.tg.appendTask(this.registerDeploymentSpecTask.create(this.vs, softVersion));
                }

            } else {
                //nsx deploymentspecs are less than vs.nsxdeploymentspecs
                //partial nsx deploymentspec expired(missing)
                //new deployspec is created on nsx, clear the expired/missing deployspec
                this.vs.getNsxDeploymentSpecIds().keySet().removeAll(nsxDepSpecsOutOfSync);
                for (VmwareSoftwareVersion softVersion : nsxDepSpecsOutOfSync) {
                    this.tg.appendTask(this.registerDeploymentSpecTask.create(this.vs, softVersion));
                }
            }

        }
        if (deploymentSpecChanged) {
            // Deployment spec change would mean service attributes like
            // version and model might need to updated
            // So add an update service attributes task if it not been
            // added already
            if (!this.updateNsxServiceAttributesScheduled) {
                this.tg.addTask(this.updateNsxServiceAttributesTask.create(this.vs));
            }

            this.tg.addTask(this.updateNsxServiceInstanceAttributesTask.create(this.vs));
        }
    }



    private List<VmwareSoftwareVersion> getOutofSyncSpecs(List<VmwareSoftwareVersion> superList,
            List<VmwareSoftwareVersion> subList) {
        List<VmwareSoftwareVersion> outOfSync = new ArrayList<>(superList);
        outOfSync.removeAll(subList);
        return outOfSync;
    }

    @Override
    public TaskGraph getTaskGraph() {
        return this.tg;
    }

    @Override
    public String getName() {
        return "Verify Deployment Specs are in Sync";
    }

}
