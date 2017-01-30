package org.osc.core.broker.service.tasks.conformance.securitygroupinterface;

import java.util.List;
import java.util.Set;

import org.hibernate.Session;
import org.osc.core.broker.job.TaskGraph;
import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.model.entities.appliance.VirtualSystem;
import org.osc.core.broker.model.entities.appliance.VirtualSystemPolicy;
import org.osc.core.broker.model.entities.virtualization.SecurityGroupInterface;
import org.osc.core.broker.model.plugin.manager.ManagerApiFactory;
import org.osc.core.broker.model.plugin.sdncontroller.VMwareSdnApiFactory;
import org.osc.core.broker.rest.client.nsx.model.ContainerSet;
import org.osc.core.broker.rest.client.nsx.model.ServiceProfile;
import org.osc.core.broker.service.persistence.SecurityGroupInterfaceEntityMgr;
import org.osc.core.broker.service.persistence.VirtualSystemPolicyEntityMgr;
import org.osc.core.broker.service.tasks.TransactionalMetaTask;
import org.osc.core.broker.service.tasks.agent.AgentsInterfaceEndpointMapRemoveMetaTask;
import org.osc.core.broker.service.tasks.agent.AgentsInterfaceEndpointMapUpdateMetaTask;
import org.osc.core.broker.service.tasks.conformance.securitygroup.NsxServiceProfileContainerCheckMetaTask;
import org.osc.sdk.manager.api.ApplianceManagerApi;
import org.osc.sdk.sdn.api.ServiceProfileApi;
import org.osc.sdk.sdn.element.SecurityGroupElement;
import org.osc.sdk.sdn.element.ServiceProfileElement;

public class NsxSecurityGroupInterfacesCheckMetaTask extends TransactionalMetaTask {
    private VirtualSystem vs;
    private TaskGraph tg;

    public NsxSecurityGroupInterfacesCheckMetaTask(VirtualSystem vs) {
        this.vs = vs;
        this.name = getName();
    }

    @Override
    public void executeTransaction(Session session) throws Exception {

        this.tg = new TaskGraph();

        this.vs = (VirtualSystem) session.get(VirtualSystem.class, this.vs.getId());

        ServiceProfileApi serviceProfileApi = VMwareSdnApiFactory.createServiceProfileApi(this.vs);

        //List<ServiceProfile> serviceProfiles = spa.getServiceProfilesByServiceId();
        List<ServiceProfileElement> serviceProfiles = serviceProfileApi.getServiceProfiles(this.vs.getNsxServiceId());
        for (ServiceProfileElement serviceProfile : serviceProfiles) {
            processServiceProfile(session, serviceProfileApi, this.tg, this.vs, serviceProfile);
        }

        // Delete any dangling interfaces
        for (SecurityGroupInterface dbSecurityGroupInterface : this.vs.getSecurityGroupInterfaces()) {
            boolean found = false;
            for (ServiceProfileElement serviceProfile : serviceProfiles) {
                if (dbSecurityGroupInterface.getTag().equals(serviceProfile.getId())) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                ApplianceManagerApi managerApi = ManagerApiFactory.createApplianceManagerApi(this.vs);
                if (managerApi.isSecurityGroupSyncSupport() && managerApi.isAgentManaged()) {
                    this.tg.appendTask(new AgentsInterfaceEndpointMapRemoveMetaTask(dbSecurityGroupInterface));
                }
                this.tg.appendTask(new DeleteSecurityGroupInterfaceTask(dbSecurityGroupInterface));
            }
        }
    }

    public static void processServiceProfile(Session session, TaskGraph tg, VirtualSystem vs,
            ServiceProfile serviceProfile) throws Exception {

        ServiceProfileApi serviceProfileApi = VMwareSdnApiFactory.createServiceProfileApi(vs);

        processServiceProfile(session, serviceProfileApi, tg, vs, serviceProfile);
    }

    private static void processServiceProfile(Session session, ServiceProfileApi serviceProfileApi,
            TaskGraph tg, VirtualSystem vs, ServiceProfileElement serviceProfile) throws Exception {

        // Locate the relevant Security Group in our DB associate with this Service Profile
        SecurityGroupInterface sgi = SecurityGroupInterfaceEntityMgr.findSecurityGroupInterfaceByVsAndTag(session, vs,
                serviceProfile.getId());

        // Check if there are any NSX objects associated with this Service Profile.
        if (!isServiceProfileBound(serviceProfile)) {

            // No NSX bindings exist for this Service Profile. Remove any Security Group from our DB if exist
            // and propagate the removal of DPA mapping.
            // Later, we'll also remove it the interface binding from mgr.

            if (sgi != null) {
                ApplianceManagerApi managerApi = ManagerApiFactory.createApplianceManagerApi(vs);
                if (managerApi.isSecurityGroupSyncSupport() && managerApi.isAgentManaged()) {
                    tg.appendTask(new AgentsInterfaceEndpointMapRemoveMetaTask(sgi));
                }
                tg.appendTask(new DeleteSecurityGroupInterfaceTask(sgi));
            }

        } else {
            // NSX bindings exists. Ensure we have it in our db and update it if necessary

            VirtualSystemPolicy vsp = VirtualSystemPolicyEntityMgr.findVirtualSystemPolicyByNsxId(session, vs,
                    serviceProfile);

            if (sgi == null) {
                // Create SGI
                tg.appendTask(new CreateSecurityGroupInterfaceTask(vsp, serviceProfile));

            } else {

                // Existing binding. Check if binding attributes changed.
                if (isChanged(sgi, serviceProfile)) {
                    tg.appendTask(new UpdateSecurityGroupInterfaceTask(sgi, vsp, serviceProfile));
                }
            }

            // Sync SGs binded to SGI
            List<SecurityGroupElement> securityGroups = serviceProfileApi.getSecurityGroups(serviceProfile.getId());
            ContainerSet containerSet = new ContainerSet(securityGroups);
            tg.appendTask(new NsxServiceProfileContainerCheckMetaTask(vs, serviceProfile, containerSet));

            // Propagate SGs to DAI
            ApplianceManagerApi managerApi = ManagerApiFactory.createApplianceManagerApi(vs);
            if (vs.getMgrId() != null && managerApi.isSecurityGroupSyncSupport() && managerApi.isAgentManaged()) {
                tg.appendTask(new AgentsInterfaceEndpointMapUpdateMetaTask(vs, serviceProfile.getId(),
                        containerSet.toIscEndpointGroupSet()));
            }
        }
    }

    private static boolean isServiceProfileBound(ServiceProfileElement serviceProfile) {
        if (!serviceProfile.getDistributedVirtualPortGroups().isEmpty() ||
                !serviceProfile.getSecurityGroups().isEmpty() ||
                !serviceProfile.getVirtualServers().isEmpty() ||
                !serviceProfile.getVirtualWires().isEmpty()) {
            return true;
        }

        return false;
    }

    private static boolean isChanged(SecurityGroupInterface securityGroup, ServiceProfileElement serviceProfile) {
        return !securityGroup.getName().equals(serviceProfile.getName());
    }

    @Override
    public String getName() {
        return "Checking Traffic Policy Mappings on Virtual System '" + this.vs.getVirtualizationConnector().getName() + "'";
    }

    @Override
    public TaskGraph getTaskGraph() {
        return this.tg;
    }

    @Override
    public Set<LockObjectReference> getObjects() {
        return LockObjectReference.getObjectReferences(this.vs);
    }

}
