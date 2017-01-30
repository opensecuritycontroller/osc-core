package org.osc.core.broker.service.tasks.conformance.openstack.securitygroup;

import java.util.Set;

import org.hibernate.Session;
import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.model.entities.virtualization.SecurityGroup;
import org.osc.core.broker.model.entities.virtualization.SecurityGroupInterface;
import org.osc.core.broker.service.persistence.EntityManager;
import org.osc.core.broker.service.tasks.TransactionalTask;

public class AddSecurityGroupTask extends TransactionalTask {
    //private static final Logger log = Logger.getLogger(AddSecurityGroupTask.class);

    private SecurityGroupInterface sgi;
    private String sgName;
    private String nsxSgId;

    public AddSecurityGroupTask(String sgName, String nsxSgId, SecurityGroupInterface sgi) {
        this.sgi = sgi;
        this.sgName = sgName;
        this.nsxSgId = nsxSgId;
        this.name = getName();
    }

    @Override
    public String getName() {
        return String.format("Creating Security Group '%s'" , this.sgName);
    }

    @Override
    public void executeTransaction(Session session) {
        this.sgi = (SecurityGroupInterface) session.get(SecurityGroupInterface.class, this.sgi.getId());
        SecurityGroup sg = new SecurityGroup(this.sgi.getVirtualSystem().getVirtualizationConnector(), this.nsxSgId);
        sg.setName(this.sgName);
        sg.addSecurityGroupInterface(this.sgi);
        EntityManager.create(session, sg);
        this.sgi.addSecurityGroup(sg);
        EntityManager.update(session, this.sgi);
    }

    @Override
    public Set<LockObjectReference> getObjects() {
        return LockObjectReference.getObjectReferences(this.sgi);
    }

}
