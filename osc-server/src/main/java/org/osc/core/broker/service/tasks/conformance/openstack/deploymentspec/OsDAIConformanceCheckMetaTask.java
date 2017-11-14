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
package org.osc.core.broker.service.tasks.conformance.openstack.deploymentspec;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Set;

import javax.persistence.EntityManager;

import org.apache.commons.lang.StringUtils;
import org.openstack4j.model.compute.Server;
import org.osc.core.broker.job.TaskGraph;
import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.model.entities.appliance.ApplianceSoftwareVersion;
import org.osc.core.broker.model.entities.appliance.DistributedApplianceInstance;
import org.osc.core.broker.model.entities.virtualization.VirtualizationConnector;
import org.osc.core.broker.model.entities.virtualization.openstack.DeploymentSpec;
import org.osc.core.broker.model.entities.virtualization.openstack.OsImageReference;
import org.osc.core.broker.model.plugin.ApiFactoryService;
import org.osc.core.broker.rest.client.openstack.openstack4j.Endpoint;
import org.osc.core.broker.rest.client.openstack.openstack4j.Openstack4JNova;
import org.osc.core.broker.service.persistence.OSCEntityManager;
import org.osc.core.broker.service.tasks.IgnoreCompare;
import org.osc.core.broker.service.tasks.TransactionalMetaTask;
import org.osc.core.broker.service.tasks.conformance.deleteda.DeleteDAIFromDbTask;
import org.osc.sdk.controller.DefaultInspectionPort;
import org.osc.sdk.controller.DefaultNetworkPort;
import org.osc.sdk.controller.api.SdnRedirectionApi;
import org.osc.sdk.controller.element.InspectionPortElement;
import org.osc.sdk.controller.element.NetworkElement;
import org.osc.sdk.controller.exception.NetworkPortNotFoundException;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Makes sure the DAI has a corresponding SVA on the specified end point. If the SVA does not exist
 * this task recreates the SVA. If the openstack host does not exist anymore, it deletes the DAI.
 */
@Component(service = OsDAIConformanceCheckMetaTask.class)
public class OsDAIConformanceCheckMetaTask extends TransactionalMetaTask {

    private static final Logger log = LoggerFactory.getLogger(OsDAIConformanceCheckMetaTask.class);

    @Reference
    DeleteSvaServerTask deleteSvaServerTask;

    @Reference
    DeleteInspectionPortTask deleteInspectionPortTask;

    @Reference
    OsSvaEnsureActiveTask osSvaEnsureActiveTask;

    @Reference
    OsSvaStateCheckTask osSvaStateCheckTask;

    @Reference
    OnboardDAITask onboardDAITask;

    @Reference
    OsSvaCheckFloatingIpTask osSvaCheckFloatingIpTask;

    @Reference
    private ApiFactoryService apiFactoryService;

    @Reference
    private OsSvaCreateMetaTask osSvaCreateMetaTask;

    @Reference
    private OsDAIUpgradeMetaTask osDAIUpgradeMetaTask;

    @Reference
    private DeleteDAIFromDbTask deleteDAIFromDbTask;

    private DistributedApplianceInstance dai;
    private boolean doesOSHostExist;
    private TaskGraph tg;
    @IgnoreCompare
    private OsDAIConformanceCheckMetaTask factory;

    public OsDAIConformanceCheckMetaTask create(DistributedApplianceInstance dai, boolean doesOSHostExist) {
        OsDAIConformanceCheckMetaTask task = new OsDAIConformanceCheckMetaTask();
        task.factory = this;
        task.dai = dai;
        task.doesOSHostExist = doesOSHostExist;
        task.deleteSvaServerTask = this.deleteSvaServerTask;
        task.osSvaEnsureActiveTask = this.osSvaEnsureActiveTask;
        task.osSvaStateCheckTask = this.osSvaStateCheckTask;
        task.onboardDAITask = this.onboardDAITask;
        task.osSvaCheckFloatingIpTask = this.osSvaCheckFloatingIpTask;
        task.dbConnectionManager = this.dbConnectionManager;
        task.txBroadcastUtil = this.txBroadcastUtil;
        task.deleteInspectionPortTask = this.deleteInspectionPortTask;

        task.osSvaCreateMetaTask = this.osSvaCreateMetaTask;
        task.osDAIUpgradeMetaTask = this.osDAIUpgradeMetaTask;
        task.deleteDAIFromDbTask = this.deleteDAIFromDbTask;
        task.apiFactoryService = this.apiFactoryService;

        return task;
    }

    @Override
    public void executeTransaction(EntityManager em) throws Exception {

        this.tg = new TaskGraph();
        OSCEntityManager<DistributedApplianceInstance> daiEntityMgr = new OSCEntityManager<DistributedApplianceInstance>(
                DistributedApplianceInstance.class, em, this.txBroadcastUtil);
        this.dai = daiEntityMgr.findByPrimaryKey(this.dai.getId());

        DeploymentSpec ds = this.dai.getDeploymentSpec();

        log.info("Checking DAI: " + this.dai.getName());

        VirtualizationConnector vc = ds.getVirtualSystem().getVirtualizationConnector();

        Endpoint endPoint = new Endpoint(vc, ds.getProjectName());

        try (Openstack4JNova nova = new Openstack4JNova(endPoint)) {

            Server sva = null;
            if (this.dai.getExternalId() != null) {
                sva = nova.getServer(ds.getRegion(), this.dai.getExternalId());
            }

            // Attempt to lookup SVA by name
            if (sva == null) {
                log.info("SVA missing for Dai: " + this.dai);
                Server namedSva = nova.getServerByName(ds.getRegion(), this.dai.getName());
            /*
             * If we found VM with this name and we have not previously with the ID we had in DB,
             * that means that all are OS attributes are staled and should be re-discovered
             */
                if (namedSva != null) {
                    if(vc.isControllerDefined()) {
                        this.tg.addTask(this.deleteInspectionPortTask.create(ds.getRegion(), this.dai));
                    }
                    log.info("Missing SVA found by name, deleting stale SVA to redeploy: " + this.dai.getName());
                    this.tg.appendTask(this.deleteSvaServerTask.create(ds.getRegion(), this.dai));
                }

                // Make sure host exists in openstack cluster
                if (this.doesOSHostExist) {
                    log.info("Re-Creating missing SVA for Dai: " + this.dai.getName());
                    this.tg.appendTask(this.osSvaCreateMetaTask.create(this.dai));
                } else {
                    log.info("Host removed from openstack: " + this.dai.getOsHostName() + "Removing Dai: "
                            + this.dai.getName());
                    if(vc.isControllerDefined()) {
                        this.tg.addTask(this.deleteInspectionPortTask.create(ds.getRegion(), this.dai));
                    }
                    this.tg.appendTask(this.deleteDAIFromDbTask.create(this.dai));
                }
            } else {
                ApplianceSoftwareVersion currentSoftwareVersion = ds.getVirtualSystem().getApplianceSoftwareVersion();
                boolean doesSvaVersionMatchVsVersion = false;

                for (OsImageReference imageRef : ds.getVirtualSystem().getOsImageReference()) {
                	if (imageRef.getImageRefId().equals(sva.getImageId())
                            && imageRef.getApplianceVersion().equals(currentSoftwareVersion)) {
                        doesSvaVersionMatchVsVersion = true;
                        break;
                    }
                }

                if (doesSvaVersionMatchVsVersion) {
                    this.tg.appendTask(this.osSvaEnsureActiveTask.create(this.dai));
                    if (!StringUtils.isEmpty(ds.getFloatingIpPoolName())) {
                        // Check if floating ip is assigned to SVA
                        this.tg.appendTask(this.osSvaCheckFloatingIpTask.create(this.dai));

                        // TODO: Future. Check if OS SG is assigned to DAI

                    }
                    this.tg.addTask(this.osSvaStateCheckTask.create(this.dai));
                } else {
                    this.tg.appendTask(this.osDAIUpgradeMetaTask.create(this.dai, currentSoftwareVersion));
                }

                if (vc.isControllerDefined()) {
                    if (!isPortRegistered()) {
                        this.tg.appendTask(this.onboardDAITask.create(this.dai));
                    }
                }

            }
        }

    }

    private boolean isPortRegistered() throws NetworkPortNotFoundException, Exception {
        SdnRedirectionApi controller = this.apiFactoryService.createNetworkRedirectionApi(this.dai);
        try {
            DefaultNetworkPort ingressPort = new DefaultNetworkPort(this.dai.getInspectionOsIngressPortId(),
                    this.dai.getInspectionIngressMacAddress());
            DefaultNetworkPort egressPort = new DefaultNetworkPort(this.dai.getInspectionOsEgressPortId(),
                    this.dai.getInspectionEgressMacAddress());

            InspectionPortElement inspectionPort = null;
            if (this.apiFactoryService.supportsPortGroup(this.dai.getVirtualSystem())) {
                DeploymentSpec ds = this.dai.getDeploymentSpec();

                String domainId = OpenstackUtil.extractDomainId(ds.getProjectId(), ds.getProjectName(),
                        ds.getVirtualSystem().getVirtualizationConnector(),
                        new ArrayList<NetworkElement>(Arrays.asList(ingressPort)));
                if (domainId != null) {
                    ingressPort.setParentId(domainId);
                    egressPort.setParentId(domainId);
                    //Element object in DefaultInspectionport is not used at this point, hence null
                    inspectionPort = controller.getInspectionPort(new DefaultInspectionPort(ingressPort, egressPort, null));
                } else {
                    log.warn("DomainId is missing, cannot be null");
                }

            } else {
                inspectionPort = controller.getInspectionPort(new DefaultInspectionPort(ingressPort, egressPort, null));
            }
            return inspectionPort != null;
        } finally {
            controller.close();
        }
    }

    @Override
    public String getName() {
        return String.format("Checking Distributed Appliance Instance '%s'", this.dai.getName());
    }

    @Override
    public TaskGraph getTaskGraph() {
        return this.tg;
    }

    @Override
    public Set<LockObjectReference> getObjects() {
        return LockObjectReference.getObjectReferences(this.dai);
    }

}
