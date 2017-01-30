package org.osc.core.broker.service.tasks.conformance.securitygroupinterface;

import java.util.Set;

import org.hibernate.Session;
import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.model.entities.virtualization.SecurityGroup;
import org.osc.core.broker.model.entities.virtualization.SecurityGroupInterface;
import org.osc.core.broker.service.persistence.EntityManager;
import org.osc.core.broker.service.tasks.TransactionalTask;

/**
 *
 * Deletes Security Group Interface and removes the SG->SGI mappings
 *
 */

public class DeleteSecurityGroupInterfaceTask extends TransactionalTask {
    // private static final Logger log = Logger.getLogger(DeleteSecurityGroupInterfaceTask.class);

    private SecurityGroupInterface securityGroupInterface;

    public DeleteSecurityGroupInterfaceTask(SecurityGroupInterface securityGroupInterface) {
        this.securityGroupInterface = securityGroupInterface;
        this.name = getName();
    }

    @Override
    public void executeTransaction(Session session) throws Exception {

        this.securityGroupInterface = (SecurityGroupInterface) session.get(SecurityGroupInterface.class,
                this.securityGroupInterface.getId());
        boolean isVmwareSGI = this.securityGroupInterface.getVirtualSystem().getVirtualizationConnector().isVmware();

        for (SecurityGroup sg : this.securityGroupInterface.getSecurityGroups()) {
            sg.removeSecurityInterface(this.securityGroupInterface);
            // If Security Group has no bindings left to any service profiles, delete the SG and its members.
            if (isVmwareSGI && sg.getSecurityGroupInterfaces().size() == 0) {
                EntityManager.delete(session, sg);
            }
        }

        EntityManager.delete(session, this.securityGroupInterface);
    }

    @Override
    public String getName() {
        return "Delete Security Group Interface '" + this.securityGroupInterface.getName() + "' ("
                + this.securityGroupInterface.getTag() + ")";
    }

    @Override
    public Set<LockObjectReference> getObjects() {
        return LockObjectReference.getObjectReferences(this.securityGroupInterface);
    }

}
