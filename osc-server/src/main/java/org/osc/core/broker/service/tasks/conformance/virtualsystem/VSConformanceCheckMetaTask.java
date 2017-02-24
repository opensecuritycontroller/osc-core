package org.osc.core.broker.service.tasks.conformance.virtualsystem;

import java.util.Set;

import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.osc.core.broker.job.TaskGraph;
import org.osc.core.broker.job.TaskGuard;
import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.job.lock.LockRequest.LockType;
import org.osc.core.broker.model.entities.appliance.DistributedAppliance;
import org.osc.core.broker.model.entities.appliance.VirtualSystem;
import org.osc.core.broker.model.entities.appliance.VirtualSystemPolicy;
import org.osc.core.broker.model.entities.management.Policy;
import org.osc.core.broker.model.entities.virtualization.VirtualizationConnector;
import org.osc.core.broker.model.entities.virtualization.openstack.DeploymentSpec;
import org.osc.core.broker.model.entities.virtualization.openstack.OsFlavorReference;
import org.osc.core.broker.model.entities.virtualization.openstack.OsImageReference;
import org.osc.core.broker.model.plugin.manager.ManagerApiFactory;
import org.osc.core.broker.model.plugin.sdncontroller.VMwareSdnApiFactory;
import org.osc.core.broker.rest.client.openstack.jcloud.Endpoint;
import org.osc.core.broker.rest.server.AgentAuthFilter;
import org.osc.core.broker.rest.server.NsxAuthFilter;
import org.osc.core.broker.service.LockUtil;
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
import org.osc.core.util.EncryptionUtil;
import org.osc.core.util.ServerUtil;
import org.osc.sdk.sdn.api.ServiceApi;
import org.osc.sdk.sdn.api.ServiceManagerApi;
import org.osc.sdk.sdn.element.ServiceElement;
import org.osc.sdk.sdn.element.ServiceManagerElement;

import com.google.common.base.Objects;

public class VSConformanceCheckMetaTask extends TransactionalMetaTask {
    private static final Logger LOG = Logger.getLogger(VSConformanceCheckMetaTask.class);

    private VirtualSystem vs;
    private TaskGraph tg;

    public VSConformanceCheckMetaTask(VirtualSystem vs) {
        this.vs = vs;
        this.name = getName();
    }

    @Override
    public void executeTransaction(Session session) throws Exception {

        this.vs = (VirtualSystem) session.get(VirtualSystem.class, this.vs.getId());

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

        if (vc.getVirtualizationType().isVmware()) {
            if (this.vs.getNsxServiceManagerId() != null) {
                tg.appendTask(new UnregisterServiceManagerCallbackTask(this.vs));
            }

            if (this.vs.getNsxServiceInstanceId() != null) {
                tg.appendTask(new DeleteServiceInstanceTask(this.vs));
            }

            if (this.vs.getNsxServiceId() != null) {
                tg.appendTask(new NsxSecurityGroupInterfacesCheckMetaTask(this.vs));
                tg.appendTask(new DeleteServiceTask(this.vs));
            }

            if (this.vs.getNsxServiceManagerId() != null) {
                tg.appendTask(new DeleteServiceManagerTask(this.vs));
            }

            tg.appendTask(new ValidateNsxAgentsTask(this.vs));

        } else if (vc.getVirtualizationType().isOpenstack()) {
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
        if (vc.getVirtualizationType().isVmware()) {

            // Sync service manager
            if (this.vs.getNsxServiceManagerId() == null) {
                tg.addTask(new CreateNsxServiceManagerTask(this.vs));
            } else {
                if (isNsxServiceManagerOutOfSync(this.vs)) {
                    tg.addTask(new UpdateNsxServiceManagerTask(this.vs));
                }
            }

            boolean updateNsxServiceAttributesScheduled = false;

            if (this.vs.getNsxServiceId() == null) {
                tg.appendTask(new CreateNsxServiceTask(this.vs));
            } else {
                // Ensure NSX service attributes has correct IP and password.
                if (isNsxServiceOutOfSync(this.vs)) {
                    updateNsxServiceAttributesScheduled = true;
                    tg.addTask(new UpdateNsxServiceAttributesTask(this.vs));
                }
            }

            DistributedAppliance distributedAppliance = this.vs.getDistributedAppliance();

            tg.appendTask(new NsxDeploymentSpecCheckMetaTask(this.vs, updateNsxServiceAttributesScheduled));

            if (this.vs.getNsxServiceInstanceId() == null) {
                tg.appendTask(new RegisterServiceInstanceTask(this.vs));
            } else {
                // Ensure NSX service instance attributes has correct attributes.
                tg.addTask(new UpdateNsxServiceInstanceAttributesTask(this.vs));
            }

            syncNsxPolicies(this.vs, tg, distributedAppliance);

            // Sync NSX security group interfaces
            if (this.vs.getNsxServiceId() != null) {
                tg.appendTask(new NsxSecurityGroupInterfacesCheckMetaTask(this.vs), TaskGuard.ALL_PREDECESSORS_COMPLETED);
                tg.appendTask(new NsxSecurityGroupsCheckMetaTask(this.vs), TaskGuard.ALL_PREDECESSORS_COMPLETED);
            }

            tg.appendTask(new ValidateNsxAgentsTask(this.vs), TaskGuard.ALL_PREDECESSORS_COMPLETED);

        } else if (vc.getVirtualizationType().isOpenstack()) {

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

        if (this.vs.getMgrId() != null
                && ManagerApiFactory.createApplianceManagerApi(this.vs).isSecurityGroupSyncSupport()) {
            // Sync Manager Security Groups
            tg.appendTask(new MgrSecurityGroupCheckMetaTask(this.vs));
        }

        tg.appendTask(vcUnlockTask, TaskGuard.ALL_PREDECESSORS_COMPLETED);
        return tg;
    }

    private boolean isNsxServiceManagerOutOfSync(VirtualSystem vs) throws Exception {
        ServiceManagerApi serviceManagerApi = VMwareSdnApiFactory.createServiceManagerApi(vs);
        ServiceManagerElement serviceManager = serviceManagerApi.getServiceManager(vs.getNsxServiceManagerId());

        // Check name
        if (!serviceManager.getName().equals(CreateNsxServiceManagerTask.generateServiceManagerName(vs))) {
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
        if (!Objects.equal(serviceManager.getOscPassword(), NsxAuthFilter.VMIDC_NSX_PASS)) {
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
        if (!service.getOscPassword().equals(EncryptionUtil.encryptAESCTR(AgentAuthFilter.VMIDC_AGENT_PASS))) {
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
                    tg.appendTask(new DeleteDefaultServiceProfileTask(vsp));
                    tg.appendTask(new RemoveVendorTemplateTask(vsp));
                }
            } else {
                // Check if vendor template already exist. If so no-op,
                // otherwise, create one.
                if (vsp == null || vsp.getNsxVendorTemplateId() == null) {
                    RegisterVendorTemplateTask regVndTemplateTask = null;
                    if (vsp == null) {
                        regVndTemplateTask = new RegisterVendorTemplateTask(vs, policy);
                    } else {
                        regVndTemplateTask = new RegisterVendorTemplateTask(vsp);
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
