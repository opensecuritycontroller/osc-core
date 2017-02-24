package org.osc.core.broker.service.tasks.conformance.deleteda;

import java.util.Set;

import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.model.entities.appliance.DistributedApplianceInstance;
import org.osc.core.broker.service.exceptions.VmidcException;
import org.osc.core.broker.service.persistence.EntityManager;
import org.osc.core.broker.service.tasks.TransactionalTask;
import org.osc.core.broker.service.tasks.conformance.openstack.deploymentspec.OpenstackUtil;

public class DeleteDAIFromDbTask extends TransactionalTask {
    private static final Logger log = Logger.getLogger(DeleteDAIFromDbTask.class);

    private DistributedApplianceInstance dai;

    public DeleteDAIFromDbTask(DistributedApplianceInstance dai) {
        this.dai = dai;
        this.name = getName();
    }

    @Override
    public String getName() {
        return "Delete Distributed Appliance Instance '" + this.dai.getName() + "'";
    }

    @Override
    public void executeTransaction(Session session) throws VmidcException, InterruptedException {
        log.debug("Start Executing DeleteDAIFromDb Task : " + this.dai.getId());
        this.dai = (DistributedApplianceInstance) session.get(DistributedApplianceInstance.class, this.dai.getId());

        OpenstackUtil.scheduleSecurityGroupJobsRelatedToDai(session, this.dai, this);
        EntityManager.delete(session, this.dai);
    }

    @Override
    public Set<LockObjectReference> getObjects() {
        return LockObjectReference.getObjectReferences(this.dai);
    }

}
