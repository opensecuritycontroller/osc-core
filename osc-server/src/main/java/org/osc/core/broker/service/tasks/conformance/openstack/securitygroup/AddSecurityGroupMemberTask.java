package org.osc.core.broker.service.tasks.conformance.openstack.securitygroup;

import java.util.Set;

import org.hibernate.Session;
import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.model.entities.virtualization.SecurityGroup;
import org.osc.core.broker.model.entities.virtualization.SecurityGroupMember;
import org.osc.core.broker.model.entities.virtualization.SecurityGroupMemberType;
import org.osc.core.broker.service.persistence.EntityManager;
import org.osc.core.broker.service.tasks.TransactionalTask;

public class AddSecurityGroupMemberTask extends TransactionalTask {
    //private static final Logger log = Logger.getLogger(AddSecurityGroupTask.class);

    private SecurityGroup securityGroup;
    private final SecurityGroupMemberType type;
    private final String address;

    public AddSecurityGroupMemberTask(SecurityGroup securityGroup, SecurityGroupMemberType type, String address) {
        this.securityGroup = securityGroup;
        this.type = type;
        this.address = address;
        this.name = getName();
    }

    @Override
    public String getName() {
        return String.format("Creating Security Group Member '%s' (%s) for Security Group '%s'", this.address,
                this.type, this.securityGroup.getName());
    }

    @Override
    public void executeTransaction(Session session) {
        this.securityGroup = (SecurityGroup) session.get(SecurityGroup.class, securityGroup.getId());
        SecurityGroupMember securityGroupMember = new SecurityGroupMember(securityGroup, type, address);
        EntityManager.create(session, securityGroupMember);
    }

    @Override
    public Set<LockObjectReference> getObjects() {
        return LockObjectReference.getObjectReferences(this.securityGroup);
    }

}
