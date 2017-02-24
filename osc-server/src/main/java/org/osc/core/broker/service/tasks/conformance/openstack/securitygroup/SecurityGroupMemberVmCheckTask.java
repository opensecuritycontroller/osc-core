package org.osc.core.broker.service.tasks.conformance.openstack.securitygroup;

import org.hibernate.Session;
import org.osc.core.broker.job.TaskGraph;
import org.osc.core.broker.model.entities.virtualization.SecurityGroupMember;
import org.osc.core.broker.model.entities.virtualization.openstack.VM;
import org.osc.core.broker.rest.client.openstack.discovery.VmDiscoveryCache;
import org.osc.core.broker.rest.client.openstack.discovery.VmDiscoveryCache.VmInfo;
import org.osc.core.broker.service.tasks.TransactionalMetaTask;

class SecurityGroupMemberVmCheckTask extends TransactionalMetaTask {

    private TaskGraph tg;
    private SecurityGroupMember sgm;
    private VM vm;
    private final VmDiscoveryCache vdc;

    /**
     * Checks the security group member and updates the associated flows
     */
    public SecurityGroupMemberVmCheckTask(SecurityGroupMember sgm, VM vm, VmDiscoveryCache vdc) {
        this.sgm = sgm;
        this.vdc = vdc;
        this.vm = vm;
    }

    @Override
    public void executeTransaction(Session session) throws Exception {
        this.tg = new TaskGraph();
        this.sgm = (SecurityGroupMember) session.get(SecurityGroupMember.class, this.sgm.getId());
        this.vm = this.sgm.getVm();

        boolean isControllerDefined = this.sgm.getSecurityGroup().getVirtualizationConnector().isControllerDefined();

        VmInfo vmInfo = this.vdc.discover(this.vm.getRegion(), this.vm.getOpenstackId());
        if (vmInfo == null || this.sgm.getMarkedForDeletion()) {
            if (isControllerDefined) {
                this.tg.addTask(new SecurityGroupMemberAllHooksRemoveTask(this.sgm));
            }
            this.tg.appendTask(new SecurityGroupMemberDeleteTask(this.sgm));
        } else {
            this.tg.addTask(new SecurityGroupMemberVmUpdateTask(this.sgm, vmInfo));
            if (isControllerDefined) {
                this.tg.appendTask(new SecurityGroupMemberHookCheckTask(this.sgm, this.vdc));
            }
        }
    }

    @Override
    public String getName() {
        return String.format("Checking Security Group Member of type '%s' with Name '%s'", this.sgm.getType(),
                this.vm.getName());
    }

    @Override
    public TaskGraph getTaskGraph() {
        return this.tg;
    }

}
