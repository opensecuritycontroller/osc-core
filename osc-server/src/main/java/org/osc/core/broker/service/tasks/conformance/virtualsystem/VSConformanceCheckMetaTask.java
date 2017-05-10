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

import java.util.Set;

import javax.persistence.EntityManager;

import org.apache.log4j.Logger;
import org.osc.core.broker.job.TaskGraph;
import org.osc.core.broker.job.TaskGuard;
import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.job.lock.LockRequest.LockType;
import org.osc.core.broker.model.entities.appliance.DistributedAppliance;
import org.osc.core.broker.model.entities.appliance.VirtualSystem;
import org.osc.core.broker.model.entities.appliance.VirtualizationType;
import org.osc.core.broker.model.entities.virtualization.VirtualizationConnector;
import org.osc.core.broker.model.entities.virtualization.openstack.DeploymentSpec;
import org.osc.core.broker.model.entities.virtualization.openstack.OsFlavorReference;
import org.osc.core.broker.model.entities.virtualization.openstack.OsImageReference;
import org.osc.core.broker.model.plugin.ApiFactoryService;
import org.osc.core.broker.model.plugin.manager.ManagerApiFactory;
import org.osc.core.broker.rest.client.openstack.jcloud.Endpoint;
import org.osc.core.broker.service.LockUtil;
import org.osc.core.broker.service.tasks.FailedWithObjectInfoTask;
import org.osc.core.broker.service.tasks.TransactionalMetaTask;
import org.osc.core.broker.service.tasks.conformance.GenerateVSSKeysTask;
import org.osc.core.broker.service.tasks.conformance.UnlockObjectMetaTask;
import org.osc.core.broker.service.tasks.conformance.UnlockObjectTask;
import org.osc.core.broker.service.tasks.conformance.deleteda.DeleteVsFromDbTask;
import org.osc.core.broker.service.tasks.conformance.manager.MgrCheckDevicesMetaTask;
import org.osc.core.broker.service.tasks.conformance.manager.MgrDeleteVSSDeviceTask;
import org.osc.core.broker.service.tasks.conformance.openstack.DeleteFlavorTask;
import org.osc.core.broker.service.tasks.conformance.openstack.DeleteImageFromGlanceTask;
import org.osc.core.broker.service.tasks.conformance.openstack.deploymentspec.DSConformanceCheckMetaTask;
import org.osc.core.broker.service.tasks.conformance.securitygroup.MgrSecurityGroupCheckMetaTask;
import org.osc.core.broker.service.tasks.conformance.securitygroupinterface.SecurityGroupCleanupCheckMetaTask;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(service = VSConformanceCheckMetaTask.class)
public class VSConformanceCheckMetaTask extends TransactionalMetaTask {
    private static final Logger LOG = Logger.getLogger(VSConformanceCheckMetaTask.class);

    @Reference
    private ApiFactoryService apiFactoryService;

    private VirtualSystem vs;
    private TaskGraph tg;

    public VSConformanceCheckMetaTask create(VirtualSystem vs) {
        VSConformanceCheckMetaTask task = new VSConformanceCheckMetaTask();
        task.vs = vs;
        task.name = task.getName();
        return task;
    }

    @Override
    public void executeTransaction(EntityManager em) throws Exception {

        this.vs = em.find(VirtualSystem.class, this.vs.getId());

        this.tg = new TaskGraph();
        if (this.vs.getMarkedForDeletion() || this.vs.getDistributedAppliance().getMarkedForDeletion()) {
            this.tg = buildVirtualSystemDeleteTaskGraph();
        } else {
            this.tg = buildVirtualSystemUpdateTaskGraph();
        }
    }

    @Override
    public String getName() {
        return "Checking Virtual System '" + this.vs.getName() + "'";
    }

    @Override
    public TaskGraph getTaskGraph() {
        return this.tg;
    }

    private TaskGraph buildVirtualSystemDeleteTaskGraph() throws Exception {
        TaskGraph tg = new TaskGraph();
        VirtualizationConnector vc = this.vs.getVirtualizationConnector();
        UnlockObjectTask vcUnlockTask = LockUtil.lockVC(vc, LockType.READ_LOCK);

        if (vc.getVirtualizationType() == VirtualizationType.OPENSTACK) {
            for (DeploymentSpec ds : this.vs.getDeploymentSpecs()) {
                Endpoint endPoint = new Endpoint(vc, ds.getTenantName());
                tg.appendTask(new DSConformanceCheckMetaTask(ds, endPoint));
            }
            for (OsImageReference image : this.vs.getOsImageReference()) {
                tg.appendTask(new DeleteImageFromGlanceTask(image.getRegion(), image, new Endpoint(vc)));
            }
            for (OsFlavorReference flavor : this.vs.getOsFlavorReference()) {
                tg.appendTask(new DeleteFlavorTask(flavor.getRegion(), flavor, new Endpoint(vc)));
            }
        }

        tg.appendTask(new SecurityGroupCleanupCheckMetaTask(this.vs), TaskGuard.ALL_ANCESTORS_SUCCEEDED);
        tg.appendTask(new MgrDeleteVSSDeviceTask(this.vs), TaskGuard.ALL_ANCESTORS_SUCCEEDED);
        tg.appendTask(new DeleteVsFromDbTask(this.vs), TaskGuard.ALL_ANCESTORS_SUCCEEDED);
        tg.appendTask(vcUnlockTask, TaskGuard.ALL_PREDECESSORS_COMPLETED);

        return tg;
    }

    private TaskGraph buildVirtualSystemUpdateTaskGraph() throws Exception {
        TaskGraph tg = new TaskGraph();
        VirtualizationConnector vc = this.vs.getVirtualizationConnector();
        UnlockObjectTask vcUnlockTask = LockUtil.lockVC(vc, LockType.READ_LOCK);
        if (vc.getVirtualizationType() == VirtualizationType.OPENSTACK) {

            // Conformance of Deployment Specs
            for (DeploymentSpec ds : this.vs.getDeploymentSpecs()) {
                Endpoint endPoint = new Endpoint(vc, ds.getTenantName());
                UnlockObjectMetaTask dsUnlockTask = null;
                try {
                    DistributedAppliance da = ds.getVirtualSystem().getDistributedAppliance();
                    dsUnlockTask = LockUtil.tryLockDS(ds, da, da.getApplianceManagerConnector(),
                            this.vs.getVirtualizationConnector());
                    tg.appendTask(new DSConformanceCheckMetaTask(ds, endPoint));
                    tg.appendTask(dsUnlockTask, TaskGuard.ALL_PREDECESSORS_COMPLETED);
                } catch (Exception e) {
                    LOG.info("Acquiring Write lock for DS: '" + ds.getName() + "' failed in VS Conformance.");
                    LockUtil.releaseLocks(dsUnlockTask);
                    tg.appendTask(new FailedWithObjectInfoTask("Acquiring Write lock for Deployment Specification", e,
                            LockObjectReference.getObjectReferences(ds)), TaskGuard.ALL_PREDECESSORS_COMPLETED);
                }
            }

        }

        if (this.vs.getKeyStore() == null) {
            tg.appendTask(new GenerateVSSKeysTask(this.vs));
        }

        // Sync Manager Devices
        tg.appendTask(new MgrCheckDevicesMetaTask(this.vs), TaskGuard.ALL_PREDECESSORS_COMPLETED);

        if (this.vs.getMgrId() != null && ManagerApiFactory.syncsSecurityGroup(this.vs)) {
            // Sync Manager Security Groups
            tg.appendTask(new MgrSecurityGroupCheckMetaTask(this.vs));
        }

        tg.appendTask(vcUnlockTask, TaskGuard.ALL_PREDECESSORS_COMPLETED);
        return tg;
    }

    @Override
    public Set<LockObjectReference> getObjects() {
        return LockObjectReference.getObjectReferences(this.vs);
    }

}
