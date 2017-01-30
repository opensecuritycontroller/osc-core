package org.osc.core.broker.service.tasks.conformance.securitygroupinterface;

import java.util.Set;

import org.hibernate.Session;
import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.model.entities.virtualization.SecurityGroupInterface;
import org.osc.core.broker.model.plugin.manager.ManagerApiFactory;
import org.osc.core.broker.service.tasks.TransactionalTask;
import org.osc.sdk.manager.api.ManagerSecurityGroupInterfaceApi;

public class UpdateMgrSecurityGroupInterfaceTask extends TransactionalTask {
    //private static final Logger log = Logger.getLogger(UpdateMgrSecurityGroupInterfaceTask.class);

    private SecurityGroupInterface securityGroupInterface;

    public UpdateMgrSecurityGroupInterfaceTask(SecurityGroupInterface securityGroupInterface) {
        this.securityGroupInterface = securityGroupInterface;
        this.name = getName();
    }

    @Override
    public void executeTransaction(Session session) throws Exception {
        this.securityGroupInterface = (SecurityGroupInterface) session.get(SecurityGroupInterface.class,
                this.securityGroupInterface.getId());

        ManagerSecurityGroupInterfaceApi mgrApi = ManagerApiFactory
                .createManagerSecurityGroupInterfaceApi(this.securityGroupInterface.getVirtualSystem());
        try {
            mgrApi.updateSecurityGroupInterface(this.securityGroupInterface.getMgrSecurityGroupIntefaceId(),
                    this.securityGroupInterface.getName(), this.securityGroupInterface.getMgrPolicyId(),
                    this.securityGroupInterface.getTag());
        } finally {
            mgrApi.close();
        }
    }

    @Override
    public String getName() {
        return "Update Manager Security Group Interface '" + this.securityGroupInterface.getName() + "' ("
                + this.securityGroupInterface.getId() + ") of Virtualization System '"
                + this.securityGroupInterface.getVirtualSystem().getVirtualizationConnector().getName() + "'";
    }

    @Override
    public Set<LockObjectReference> getObjects() {
        return LockObjectReference.getObjectReferences(this.securityGroupInterface);
    }

}
