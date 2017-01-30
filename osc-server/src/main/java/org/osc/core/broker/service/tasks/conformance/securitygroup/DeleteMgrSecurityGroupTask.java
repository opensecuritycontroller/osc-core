package org.osc.core.broker.service.tasks.conformance.securitygroup;

import java.util.Set;

import org.hibernate.Session;
import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.model.entities.appliance.VirtualSystem;
import org.osc.core.broker.model.plugin.manager.ManagerApiFactory;
import org.osc.core.broker.service.tasks.TransactionalTask;
import org.osc.sdk.manager.api.ManagerSecurityGroupApi;
import org.osc.sdk.manager.element.ManagerSecurityGroupElement;

public class DeleteMgrSecurityGroupTask extends TransactionalTask {

    private VirtualSystem vs;
    private ManagerSecurityGroupElement msge;

    public DeleteMgrSecurityGroupTask(VirtualSystem vs, ManagerSecurityGroupElement msge) {
        this.vs = vs;
        this.msge = msge;
        this.name = getName();
    }

    @Override
    public void executeTransaction(Session session) throws Exception {
        this.vs = (VirtualSystem) session.get(VirtualSystem.class, this.vs.getId());

        ManagerSecurityGroupApi mgrApi = ManagerApiFactory.createManagerSecurityGroupApi(this.vs);
        try {
            mgrApi.deleteSecurityGroup(this.msge.getSGId());
        } finally {
            mgrApi.close();
        }
    }

    @Override
    public String getName() {
        return "Delete Manager Security Group '" + this.msge.getName() + "' (" + this.msge.getSGId()
                + ") of Virtualization System '" + this.vs.getVirtualizationConnector().getName() + "'";
    }

    @Override
    public Set<LockObjectReference> getObjects() {
        return LockObjectReference.getObjectReferences(this.vs);
    }

}
