package org.osc.core.broker.service.tasks.conformance.securitygroupinterface;

import java.util.Set;

import org.hibernate.Session;
import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.model.entities.appliance.VirtualSystemPolicy;
import org.osc.core.broker.model.entities.virtualization.SecurityGroupInterface;
import org.osc.core.broker.service.persistence.EntityManager;
import org.osc.core.broker.service.tasks.TransactionalTask;
import org.osc.sdk.sdn.element.ServiceProfileElement;

public class UpdateSecurityGroupInterfaceTask extends TransactionalTask {
    private SecurityGroupInterface securityGroupInterface;
    private VirtualSystemPolicy vsp;
    private ServiceProfileElement serviceProfile;

    public UpdateSecurityGroupInterfaceTask(SecurityGroupInterface securityGroupInterface, VirtualSystemPolicy vsp,
            ServiceProfileElement serviceProfile) {

        this.vsp = vsp;
        this.securityGroupInterface = securityGroupInterface;
        this.serviceProfile = serviceProfile;
        this.name = getName();
    }

    @Override
    public void executeTransaction(Session session) throws Exception {

        this.vsp = (VirtualSystemPolicy) session.get(VirtualSystemPolicy.class, this.vsp.getId());
        this.securityGroupInterface = (SecurityGroupInterface) session.get(SecurityGroupInterface.class,
                this.securityGroupInterface.getId());

        this.securityGroupInterface.setName(this.serviceProfile.getName());
        EntityManager.update(session, this.securityGroupInterface);
    }

    @Override
    public String getName() {
        return "Updating Security Group Interface '" + this.serviceProfile.getName() + "' (" + this.serviceProfile.getId()
        + ")";
    }

    @Override
    public Set<LockObjectReference> getObjects() {
        return LockObjectReference.getObjectReferences(this.securityGroupInterface);
    }
}
