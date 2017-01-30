package org.osc.core.broker.service.tasks.conformance.securitygroupinterface;

import java.util.Set;

import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.model.entities.virtualization.SecurityGroupInterface;
import org.osc.core.broker.model.plugin.manager.ManagerApiFactory;
import org.osc.core.broker.service.persistence.EntityManager;
import org.osc.core.broker.service.tasks.TransactionalTask;
import org.osc.sdk.manager.api.ManagerSecurityGroupInterfaceApi;

public class CreateMgrSecurityGroupInterfaceTask extends TransactionalTask {
    private static final Logger log = Logger.getLogger(CreateMgrSecurityGroupInterfaceTask.class);

    private SecurityGroupInterface securityGroupInterface;

    public CreateMgrSecurityGroupInterfaceTask(SecurityGroupInterface securityGroup) {

        this.securityGroupInterface = securityGroup;
        this.name = getName();
    }

    @Override
    public void executeTransaction(Session session) throws Exception {

        this.securityGroupInterface = (SecurityGroupInterface) session.get(SecurityGroupInterface.class,
                this.securityGroupInterface.getId());

        ManagerSecurityGroupInterfaceApi mgrApi = ManagerApiFactory
                .createManagerSecurityGroupInterfaceApi(this.securityGroupInterface.getVirtualSystem());
        try {
            String mgrSecurityGroupId = mgrApi.createSecurityGroupInterface(this.securityGroupInterface.getName(),
                    this.securityGroupInterface.getMgrPolicyId(), this.securityGroupInterface.getTag());
            log.info("Created Manager Security Group Interface '" + mgrSecurityGroupId + "'");

            this.securityGroupInterface.setMgrSecurityGroupIntefaceId(mgrSecurityGroupId);
            EntityManager.update(session, this.securityGroupInterface);

        } finally {
            mgrApi.close();
        }

    }

    @Override
    public String getName() {
        return "Create Manager Security Group Interface '" + this.securityGroupInterface.getName()
                + "' of Virtualization System '"
                + this.securityGroupInterface.getVirtualSystem().getVirtualizationConnector().getName() + "'";
    }

    @Override
    public Set<LockObjectReference> getObjects() {
        return LockObjectReference.getObjectReferences(this.securityGroupInterface);
    }

}
