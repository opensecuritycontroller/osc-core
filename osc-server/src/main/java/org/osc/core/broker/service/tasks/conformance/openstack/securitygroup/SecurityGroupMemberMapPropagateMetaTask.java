package org.osc.core.broker.service.tasks.conformance.openstack.securitygroup;

import java.util.Set;

import org.hibernate.Session;
import org.osc.core.broker.job.TaskGraph;
import org.osc.core.broker.job.TaskGuard;
import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.model.entities.appliance.VirtualSystem;
import org.osc.core.broker.model.entities.virtualization.SecurityGroup;
import org.osc.core.broker.model.entities.virtualization.SecurityGroupInterface;
import org.osc.core.broker.model.plugin.manager.ManagerApiFactory;
import org.osc.core.broker.service.tasks.TransactionalMetaTask;
import org.osc.core.broker.service.tasks.conformance.securitygroup.MgrSecurityGroupCheckMetaTask;
import org.osc.sdk.manager.api.ApplianceManagerApi;

public class SecurityGroupMemberMapPropagateMetaTask extends TransactionalMetaTask {

    private SecurityGroup sg;

    private TaskGraph tg;

    public SecurityGroupMemberMapPropagateMetaTask(SecurityGroup sg) {
        this.sg = sg;
    }

    @Override
    public void executeTransaction(Session session) throws Exception {
        this.sg = (SecurityGroup) session.get(SecurityGroup.class, this.sg.getId());

        this.tg = new TaskGraph();
        for (SecurityGroupInterface sgi : this.sg.getSecurityGroupInterfaces()) {
            VirtualSystem vs = sgi.getVirtualSystem();
            ApplianceManagerApi managerApi = ManagerApiFactory.createApplianceManagerApi(vs);
            if (vs.getMgrId() != null  && managerApi.isSecurityGroupSyncSupport()) {
                // Sync SG members mapping to the manager directly
                this.tg.addTask(new MgrSecurityGroupCheckMetaTask(vs, this.sg), TaskGuard.ALL_PREDECESSORS_COMPLETED);
            }
        }
    }

    @Override
    public String getName() {
        return String.format("Checking to propogate Security Group member list to appliances/managers '%s'", this.sg.getName());
    }

    @Override
    public TaskGraph getTaskGraph() {
        return this.tg;
    }

    @Override
    public Set<LockObjectReference> getObjects() {
        return LockObjectReference.getObjectReferences(this.sg);
    }

}
