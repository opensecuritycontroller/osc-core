package org.osc.core.broker.service.tasks.conformance.securitygroup;

import java.util.Set;

import org.hibernate.Session;
import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.model.entities.appliance.VirtualSystem;
import org.osc.core.broker.model.entities.virtualization.SecurityGroup;
import org.osc.core.broker.model.plugin.manager.ManagerApiFactory;
import org.osc.core.broker.service.tasks.TransactionalTask;
import org.osc.sdk.manager.api.ManagerSecurityGroupApi;

class UpdateMgrSecurityGroupTask extends TransactionalTask {
    //private static final Logger log = Logger.getLogger(UpdateMgrSecurityGroupInterfaceTask.class);

    private SecurityGroup sg;
    private VirtualSystem vs;

    public UpdateMgrSecurityGroupTask(VirtualSystem vs, SecurityGroup securityGroup) {
        this.vs = vs;
        this.sg = securityGroup;
        this.name = getName();
    }

    @Override
    public void executeTransaction(Session session) throws Exception {
        this.sg = (SecurityGroup) session.get(SecurityGroup.class, this.sg.getId());

        ManagerSecurityGroupApi mgrApi = ManagerApiFactory.createManagerSecurityGroupApi(this.vs);
        try {
            mgrApi.updateSecurityGroup(this.sg.getMgrId(), this.sg.getName(),
                    this.sg.getSecurityGroupMemberListElement());
        } finally {
            mgrApi.close();
        }
    }

    @Override
    public String getName() {
        return "Update Manager Security Group '" + this.sg.getName() + " of Virtualization System '"
                + this.vs.getVirtualizationConnector().getName() + "'";
    }

    @Override
    public Set<LockObjectReference> getObjects() {
        return LockObjectReference.getObjectReferences(this.vs);
    }

}
