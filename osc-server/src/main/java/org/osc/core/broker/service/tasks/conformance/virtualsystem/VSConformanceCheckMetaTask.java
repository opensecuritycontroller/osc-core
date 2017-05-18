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
import org.osc.core.broker.model.entities.appliance.VirtualSystemPolicy;
import org.osc.core.broker.model.entities.appliance.VirtualizationType;
import org.osc.core.broker.model.entities.management.Policy;
import org.osc.core.broker.model.entities.virtualization.VirtualizationConnector;
import org.osc.core.broker.model.entities.virtualization.openstack.DeploymentSpec;
import org.osc.core.broker.model.entities.virtualization.openstack.OsFlavorReference;
import org.osc.core.broker.model.entities.virtualization.openstack.OsImageReference;
import org.osc.core.broker.model.plugin.ApiFactoryService;
import org.osc.core.broker.model.plugin.manager.ManagerApiFactory;
import org.osc.core.broker.model.plugin.sdncontroller.VMwareSdnApiFactory;
import org.osc.core.broker.rest.client.openstack.jcloud.Endpoint;
import org.osc.core.broker.service.LockUtil;
import org.osc.core.broker.service.api.server.EncryptionApi;
import org.osc.core.broker.service.tasks.FailedWithObjectInfoTask;
import org.osc.core.broker.service.tasks.TransactionalMetaTask;
import org.osc.core.broker.service.tasks.conformance.GenerateVSSKeysTask;
import org.osc.core.broker.service.tasks.conformance.UnlockObjectMetaTask;
import org.osc.core.broker.service.tasks.conformance.UnlockObjectTask;
import org.osc.core.broker.service.tasks.conformance.deleteda.DeleteServiceInstanceTask;
import org.osc.core.broker.service.tasks.conformance.deleteda.DeleteServiceManagerTask;
import org.osc.core.broker.service.tasks.conformance.deleteda.DeleteServiceTask;
import org.osc.core.broker.service.tasks.conformance.deleteda.DeleteVsFromDbTask;
import org.osc.core.broker.service.tasks.conformance.deleteda.UnregisterServiceManagerCallbackTask;
import org.osc.core.broker.service.tasks.conformance.manager.MgrCheckDevicesMetaTask;
import org.osc.core.broker.service.tasks.conformance.manager.MgrDeleteVSSDeviceTask;
import org.osc.core.broker.service.tasks.conformance.openstack.DeleteFlavorTask;
import org.osc.core.broker.service.tasks.conformance.openstack.DeleteImageFromGlanceTask;
import org.osc.core.broker.service.tasks.conformance.openstack.deploymentspec.DSConformanceCheckMetaTask;
import org.osc.core.broker.service.tasks.conformance.securitygroup.MgrSecurityGroupCheckMetaTask;
import org.osc.core.broker.service.tasks.conformance.securitygroup.NsxSecurityGroupsCheckMetaTask;
import org.osc.core.broker.service.tasks.conformance.securitygroupinterface.NsxSecurityGroupInterfacesCheckMetaTask;
import org.osc.core.broker.service.tasks.conformance.securitygroupinterface.SecurityGroupCleanupCheckMetaTask;
import org.osc.core.broker.service.tasks.network.UpdateNsxServiceInstanceAttributesTask;
import org.osc.core.broker.service.tasks.network.UpdateNsxServiceManagerTask;
import org.osc.core.broker.service.tasks.passwordchange.UpdateNsxServiceAttributesTask;
import org.osc.core.broker.util.PasswordUtil;
import org.osc.core.util.ServerUtil;
import org.osc.sdk.sdn.api.ServiceApi;
import org.osc.sdk.sdn.api.ServiceManagerApi;
import org.osc.sdk.sdn.element.ServiceElement;
import org.osc.sdk.sdn.element.ServiceManagerElement;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.google.common.base.Objects;

@Component(service = VSConformanceCheckMetaTask.class)
public class VSConformanceCheckMetaTask extends TransactionalMetaTask {
    private static final Logger LOG = Logger.getLogger(VSConformanceCheckMetaTask.class);

    @Reference
    ApiFactoryService apiFactoryService;

    @Reference
    CreateNsxServiceManagerTask createNsxServiceManagerTask;

    @Reference
    UpdateNsxServiceManagerTask updateNsxServiceManagerTask;

    @Reference
    CreateNsxServiceTask createNsxServiceTask;

    @Reference
    UpdateNsxServiceAttributesTask updateNsxServiceAttributesTask;

    @Reference
    NsxDeploymentSpecCheckMetaTask nsxDeploymentSpecCheckMetaTask;

    @Reference
    UpdateNsxServiceInstanceAttributesTask updateNsxServiceInstanceAttributesTask;

    @Reference
    UnregisterServiceManagerCallbackTask unregisterServiceManagerCallbackTask;

    @Reference
    DeleteServiceInstanceTask deleteServiceInstanceTask;

    @Reference
    NsxSecurityGroupInterfacesCheckMetaTask nsxSecurityGroupInterfacesCheckMetaTask;

    @Reference
    DeleteServiceTask deleteServiceTask;

    @Reference
    DeleteServiceManagerTask deleteServiceManagerTask;

    @Reference
    DeleteImageFromGlanceTask deleteImageFromGlanceTask;

    @Reference
    DeleteFlavorTask deleteFlavorTask;

    @Reference
    SecurityGroupCleanupCheckMetaTask securityGroupCleanupCheckMetaTask;

    @Reference
    DeleteVsFromDbTask deleteVsFromDbTask;

    @Reference
    RegisterServiceInstanceTask registerServiceInstanceTask;

    @Reference
    NsxSecurityGroupsCheckMetaTask nsxSecurityGroupsCheckMetaTask;

    @Reference
    GenerateVSSKeysTask generateVSSKeysTask;

    @Reference
    MgrSecurityGroupCheckMetaTask mgrSecurityGroupCheckMetaTask;

    @Reference
    DeleteDefaultServiceProfileTask deleteDefaultServiceProfileTask;

    @Reference
    RemoveVendorTemplateTask removeVendorTemplateTask;

    @Reference
    RegisterVendorTemplateTask registerVendorTemplateTask;

    @Reference
    PasswordUtil passwordUtil;

    @Reference
    EncryptionApi encryption;

    @Reference
    MgrCheckDevicesMetaTask mgrCheckDevicesMetaTask;

    @Reference
    DSConformanceCheckMetaTask dsConformanceCheckMetaTask;

    @Reference
    ValidateNsxAgentsTask validateNsxAgentsTask;

    @Reference
    MgrDeleteVSSDeviceTask mgrDeleteVSSDeviceTask;


    private VirtualSystem vs;
    private TaskGraph tg;

    public VSConformanceCheckMetaTask create(VirtualSystem vs) {
        VSConformanceCheckMetaTask task = new VSConformanceCheckMetaTask();
        task.vs = vs;
        task.apiFactoryService = this.apiFactoryService;
        task.createNsxServiceManagerTask = this.createNsxServiceManagerTask;
        task.createNsxServiceTask = this.createNsxServiceTask;
        task.updateNsxServiceManagerTask = this.updateNsxServiceManagerTask;
        task.updateNsxServiceAttributesTask = this.updateNsxServiceAttributesTask;
        task.nsxDeploymentSpecCheckMetaTask = this.nsxDeploymentSpecCheckMetaTask;
        task.updateNsxServiceInstanceAttributesTask = this.updateNsxServiceInstanceAttributesTask;
        task.validateNsxAgentsTask = this.validateNsxAgentsTask;
        task.mgrDeleteVSSDeviceTask = this.mgrDeleteVSSDeviceTask;
        task.mgrCheckDevicesMetaTask = this.mgrCheckDevicesMetaTask;
        task.dsConformanceCheckMetaTask = this.dsConformanceCheckMetaTask;
        task.unregisterServiceManagerCallbackTask = this.unregisterServiceManagerCallbackTask;
        task.deleteServiceInstanceTask = this.deleteServiceInstanceTask;
        task.nsxSecurityGroupInterfacesCheckMetaTask = this.nsxSecurityGroupInterfacesCheckMetaTask;
        task.deleteServiceTask = this.deleteServiceTask;
        task.deleteServiceManagerTask = this.deleteServiceManagerTask;
        task.deleteImageFromGlanceTask = this.deleteImageFromGlanceTask;
        task.securityGroupCleanupCheckMetaTask = this.securityGroupCleanupCheckMetaTask;
        task.deleteFlavorTask = this.deleteFlavorTask;
        task.deleteVsFromDbTask = this.deleteVsFromDbTask;
        task.registerServiceInstanceTask = this.registerServiceInstanceTask;
        task.nsxSecurityGroupsCheckMetaTask = this.nsxSecurityGroupsCheckMetaTask;
        task.generateVSSKeysTask = this.generateVSSKeysTask;
        task.mgrSecurityGroupCheckMetaTask = this.mgrSecurityGroupCheckMetaTask;
        task.deleteDefaultServiceProfileTask = this.deleteDefaultServiceProfileTask;
        task.removeVendorTemplateTask = this.removeVendorTemplateTask;
        task.registerVendorTemplateTask = this.registerVendorTemplateTask;
        task.passwordUtil = this.passwordUtil;
        task.encryption = this.encryption;
        task.name = task.getName();
        task.dbConnectionManager = this.dbConnectionManager;
        task.txBroadcastUtil = this.txBroadcastUtil;

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

        if (vc.getVirtualizationType() == VirtualizationType.VMWARE) {
            if (this.vs.getNsxServiceManagerId() != null) {
                tg.appendTask(this.unregisterServiceManagerCallbackTask.create(this.vs));
            }

            if (this.vs.getNsxServiceInstanceId() != null) {
                tg.appendTask(this.deleteServiceInstanceTask.create(this.vs));
            }

            if (this.vs.getNsxServiceId() != null) {
                tg.appendTask(this.nsxSecurityGroupInterfacesCheckMetaTask.create(this.vs));
                tg.appendTask(this.deleteServiceTask.create(this.vs));
            }

            if (this.vs.getNsxServiceManagerId() != null) {
                tg.appendTask(this.deleteServiceManagerTask.create(this.vs));
            }

            tg.appendTask(this.validateNsxAgentsTask.create(this.vs));

        } else if (vc.getVirtualizationType() == VirtualizationType.OPENSTACK) {
            for (DeploymentSpec ds : this.vs.getDeploymentSpecs()) {
                Endpoint endPoint = new Endpoint(vc, ds.getTenantName());
                tg.appendTask(this.dsConformanceCheckMetaTask.create(ds, endPoint));
            }
            for (OsImageReference image : this.vs.getOsImageReference()) {
                tg.appendTask(this.deleteImageFromGlanceTask.create(image.getRegion(), image, new Endpoint(vc)));
            }
            for (OsFlavorReference flavor : this.vs.getOsFlavorReference()) {
                tg.appendTask(this.deleteFlavorTask.create(flavor.getRegion(), flavor, new Endpoint(vc)));
            }
        }

        tg.appendTask(this.securityGroupCleanupCheckMetaTask.create(this.vs), TaskGuard.ALL_ANCESTORS_SUCCEEDED);
        tg.appendTask(this.mgrDeleteVSSDeviceTask.create(this.vs), TaskGuard.ALL_ANCESTORS_SUCCEEDED);
        tg.appendTask(this.deleteVsFromDbTask.create(this.vs), TaskGuard.ALL_ANCESTORS_SUCCEEDED);
        tg.appendTask(vcUnlockTask, TaskGuard.ALL_PREDECESSORS_COMPLETED);

        return tg;
    }

    private TaskGraph buildVirtualSystemUpdateTaskGraph() throws Exception {
        TaskGraph tg = new TaskGraph();
        VirtualizationConnector vc = this.vs.getVirtualizationConnector();
        UnlockObjectTask vcUnlockTask = LockUtil.lockVC(vc, LockType.READ_LOCK);
        if (vc.getVirtualizationType() == VirtualizationType.VMWARE) {

            // Sync service manager
            if (this.vs.getNsxServiceManagerId() == null) {
                tg.addTask(this.createNsxServiceManagerTask.create(this.vs));
            } else {
                if (isNsxServiceManagerOutOfSync(this.vs)) {
                    tg.addTask(this.updateNsxServiceManagerTask.create(this.vs));
                }
            }

            boolean updateNsxServiceAttributesScheduled = false;

            if (this.vs.getNsxServiceId() == null) {
                tg.appendTask(this.createNsxServiceTask.create(this.vs));
            } else {
                // Ensure NSX service attributes has correct IP and password.
                if (isNsxServiceOutOfSync(this.vs)) {
                    updateNsxServiceAttributesScheduled = true;
                    tg.addTask(this.updateNsxServiceAttributesTask.create(this.vs));
                }
            }

            DistributedAppliance distributedAppliance = this.vs.getDistributedAppliance();

            tg.appendTask(this.nsxDeploymentSpecCheckMetaTask.create(this.vs, updateNsxServiceAttributesScheduled));

            if (this.vs.getNsxServiceInstanceId() == null) {
                tg.appendTask(this.registerServiceInstanceTask.create(this.vs));
            } else {
                // Ensure NSX service instance attributes has correct attributes.
                tg.addTask(this.updateNsxServiceInstanceAttributesTask.create(this.vs));
            }

            syncNsxPolicies(this.vs, tg, distributedAppliance);

            // Sync NSX security group interfaces
            if (this.vs.getNsxServiceId() != null) {
                tg.appendTask(this.nsxSecurityGroupInterfacesCheckMetaTask.create(this.vs), TaskGuard.ALL_PREDECESSORS_COMPLETED);
                tg.appendTask(this.nsxSecurityGroupsCheckMetaTask.create(this.vs), TaskGuard.ALL_PREDECESSORS_COMPLETED);
            }

            tg.appendTask(this.validateNsxAgentsTask.create(this.vs), TaskGuard.ALL_PREDECESSORS_COMPLETED);

        } else if (vc.getVirtualizationType() == VirtualizationType.OPENSTACK) {

            // Conformance of Deployment Specs
            for (DeploymentSpec ds : this.vs.getDeploymentSpecs()) {
                Endpoint endPoint = new Endpoint(vc, ds.getTenantName());
                UnlockObjectMetaTask dsUnlockTask = null;
                try {
                    DistributedAppliance da = ds.getVirtualSystem().getDistributedAppliance();
                    dsUnlockTask = LockUtil.tryLockDS(ds, da, da.getApplianceManagerConnector(),
                            this.vs.getVirtualizationConnector());
                    tg.appendTask(this.dsConformanceCheckMetaTask.create(ds, endPoint));
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
            tg.appendTask(this.generateVSSKeysTask.create(this.vs));
        }

        // Sync Manager Devices
        tg.appendTask(this.mgrCheckDevicesMetaTask.create(this.vs), TaskGuard.ALL_PREDECESSORS_COMPLETED);

        if (this.vs.getMgrId() != null && ManagerApiFactory.syncsSecurityGroup(this.vs)) {
            // Sync Manager Security Groups
            tg.appendTask(this.mgrSecurityGroupCheckMetaTask.create(this.vs));
        }

        tg.appendTask(vcUnlockTask, TaskGuard.ALL_PREDECESSORS_COMPLETED);
        return tg;
    }

    private boolean isNsxServiceManagerOutOfSync(VirtualSystem vs) throws Exception {
        ServiceManagerApi serviceManagerApi = VMwareSdnApiFactory.createServiceManagerApi(vs);
        ServiceManagerElement serviceManager = serviceManagerApi.getServiceManager(vs.getNsxServiceManagerId());

        // Check name
        if (!serviceManager.getName().equals(this.apiFactoryService.generateServiceManagerName(vs))) {
            LOG.info("Service Manager name is out of sync");
            return true;
        }

        // Check callback URL (Containing ISC IP)
        String restUrl = CreateNsxServiceManagerTask.buildRestCallbackUrl();
        String smurl = serviceManager.getCallbackUrl();
        if (!Objects.equal(smurl, restUrl)) {
            LOG.info("Nsx Service Manager image url (" + serviceManager.getCallbackUrl() + ") is out of sync (" + restUrl
                    + ")");
            return true;
        }

        // Check password
        if (!Objects.equal(serviceManager.getOscPassword(), this.passwordUtil.getVmidcNsxPass())) {
            LOG.info("Nsx Service Manager password is out of sync");
            return true;
        }

        return false;
    }

    private boolean isNsxServiceOutOfSync(VirtualSystem vs) throws Exception {
        ServiceApi serviceApi = VMwareSdnApiFactory.createServiceApi(vs);
        ServiceElement service = serviceApi.getService(vs.getNsxServiceId());

        // Check name
        if (!service.getName().equals(vs.getDistributedAppliance().getName())) {
            LOG.info("NSX service out of sync: service name.");
            return true;
        }

        // Check IP
        if (!service.getOscIpAddress().equals(ServerUtil.getServerIP())) {
            LOG.info("NSX service out of sync: OSC ip addres.");
            return true;
        }

        // Check password
        if (!service.getOscPassword().equals(this.encryption.encryptAESCTR(this.passwordUtil.getOscDefaultPass()))) {
            LOG.info("NSX service out of sync: OSC password.");
            return true;
        }

        return false;
    }

    private void syncNsxPolicies(VirtualSystem vs, TaskGraph tg, DistributedAppliance distributedAppliance)
            throws Exception {
        // Sync policies
        Set<Policy> policies = vs.getDomain().getPolicies();

        for (Policy policy : policies) {

            VirtualSystemPolicy vsp = findVirtualSystemPolicy(vs, policy);

            if (policy.getMarkedForDeletion()) {
                // If vendor template exist for this policy, generate remove task
                if (vsp != null) {
                    tg.appendTask(this.deleteDefaultServiceProfileTask.create(vsp));
                    tg.appendTask(this.removeVendorTemplateTask.create(vsp));
                }
            } else {
                // Check if vendor template already exist. If so no-op,
                // otherwise, create one.
                if (vsp == null || vsp.getNsxVendorTemplateId() == null) {
                    RegisterVendorTemplateTask regVndTemplateTask = null;
                    if (vsp == null) {
                        regVndTemplateTask = this.registerVendorTemplateTask.create(vs, policy);
                    } else {
                        regVndTemplateTask = this.registerVendorTemplateTask.create(vsp);
                    }
                    tg.appendTask(regVndTemplateTask);
                } else {
                    if (vs.getNsxServiceId() != null) {
                        // Update for vendor template name change
                        tg.appendTask(new UpdateVendorTemplateTask(vsp, vsp.getPolicy().getName()));
                    }
                }
            }
        }
    }

    private VirtualSystemPolicy findVirtualSystemPolicy(VirtualSystem vs, Policy policy) {
        for (VirtualSystemPolicy vsp : vs.getVirtualSystemPolicies()) {
            if (vsp.getPolicy().equals(policy)) {
                return vsp;
            }
        }
        return null;
    }

    @Override
    public Set<LockObjectReference> getObjects() {
        return LockObjectReference.getObjectReferences(this.vs);
    }

}
