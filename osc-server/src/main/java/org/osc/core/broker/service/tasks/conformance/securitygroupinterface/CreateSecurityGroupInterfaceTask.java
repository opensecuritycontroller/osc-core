package org.osc.core.broker.service.tasks.conformance.securitygroupinterface;

import java.util.Set;

import org.hibernate.Session;
import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.model.entities.appliance.VirtualSystemPolicy;
import org.osc.core.broker.model.entities.virtualization.SecurityGroupInterface;
import org.osc.core.broker.service.persistence.EntityManager;
import org.osc.core.broker.service.tasks.TransactionalTask;
import org.osc.sdk.sdn.element.ServiceProfileElement;

public class CreateSecurityGroupInterfaceTask extends TransactionalTask {
    private VirtualSystemPolicy vsp;
    private ServiceProfileElement serviceProfile;

    public CreateSecurityGroupInterfaceTask(VirtualSystemPolicy vsp, ServiceProfileElement nsxServiceProfile) {
        this.vsp = vsp;
        this.serviceProfile = nsxServiceProfile;
        this.name = getName();
    }

    @Override
    public void executeTransaction(Session session) throws Exception {
        this.vsp = (VirtualSystemPolicy) session.get(VirtualSystemPolicy.class, this.vsp.getId());

        SecurityGroupInterface securityGroupInterface = new SecurityGroupInterface(this.vsp,
                this.serviceProfile.getId());

        securityGroupInterface.setName(this.serviceProfile.getName());
        securityGroupInterface.setTag(this.serviceProfile.getId());
        securityGroupInterface.setNsxVsmUuid(this.serviceProfile.getVsmId());

        EntityManager.create(session, securityGroupInterface);
    }

    @Override
    public String getName() {
        return "Creating Security Group Interface '" + this.serviceProfile.getName() + "' ("
                + this.serviceProfile.getId() + ") assigned to Policy '" + this.vsp.getPolicy().getName() + "'";
    }

    @Override
    public Set<LockObjectReference> getObjects() {
        return LockObjectReference.getObjectReferences(this.vsp.getVirtualSystem());
    }

}
