package org.osc.core.broker.service.tasks.conformance.securitygroup;

import java.util.List;
import java.util.Set;

import org.hibernate.Session;
import org.osc.core.broker.job.TaskGraph;
import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.model.entities.appliance.VirtualSystem;
import org.osc.core.broker.model.entities.virtualization.SecurityGroup;
import org.osc.core.broker.model.entities.virtualization.SecurityGroupInterface;
import org.osc.core.broker.model.entities.virtualization.SecurityGroupMember;
import org.osc.core.broker.model.plugin.sdncontroller.VMwareSdnApiFactory;
import org.osc.core.broker.rest.client.nsx.model.ContainerSet;
import org.osc.core.broker.service.persistence.SecurityGroupEntityMgr;
import org.osc.core.broker.service.tasks.TransactionalMetaTask;
import org.osc.core.broker.service.tasks.conformance.openstack.securitygroup.DeleteSecurityGroupFromDbTask;
import org.osc.core.broker.service.tasks.conformance.openstack.securitygroup.SecurityGroupMemberDeleteTask;
import org.osc.sdk.sdn.api.ServiceProfileApi;
import org.osc.sdk.sdn.element.SecurityGroupElement;

public class NsxSecurityGroupsCheckMetaTask extends TransactionalMetaTask {
    //private static final Logger log = Logger.getLogger(NsxSecurityGroupsCheckMetaTask.class);

    private VirtualSystem vs;
    private TaskGraph tg;

    public NsxSecurityGroupsCheckMetaTask(VirtualSystem vs) {
        this.vs = vs;
        this.name = getName();
    }

    @Override
    public void executeTransaction(Session session) throws Exception {
        this.tg = new TaskGraph();

        this.vs = (VirtualSystem) session.get(VirtualSystem.class, this.vs.getId());
        ServiceProfileApi serviceProfileApi = VMwareSdnApiFactory.createServiceProfileApi(this.vs);
        for (SecurityGroupInterface sgi : this.vs.getSecurityGroupInterfaces()) {
            List<SecurityGroupElement> securityGroups = serviceProfileApi.getSecurityGroups(sgi.getTag());
            this.tg.appendTask(new NsxServiceProfileContainerCheckMetaTask(sgi, new ContainerSet(securityGroups)));
        }

        List<SecurityGroup> unbindedSecurityGroups = SecurityGroupEntityMgr.listSecurityGroupsByVsAndNoBindings(
                session, this.vs);
        for (SecurityGroup sg : unbindedSecurityGroups) {
            for (SecurityGroupMember sgm : sg.getSecurityGroupMembers()) {
                this.tg.appendTask(new SecurityGroupMemberDeleteTask(sgm));
            }
            this.tg.appendTask(new DeleteSecurityGroupFromDbTask(sg));
        }
    }

    @Override
    public String getName() {
        return "Checking Security Groups on Virtual System '" + this.vs.getVirtualizationConnector().getName() + "'";
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
