package org.osc.core.broker.service.tasks.conformance.openstack.deploymentspec;

import java.util.Set;

import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.model.entities.appliance.DistributedApplianceInstance;
import org.osc.core.broker.model.entities.virtualization.openstack.DeploymentSpec;
import org.osc.core.broker.model.entities.virtualization.openstack.VMPort;
import org.osc.core.broker.service.persistence.DeploymentSpecEntityMgr;
import org.osc.core.broker.service.persistence.EntityManager;
import org.osc.core.broker.service.tasks.TransactionalTask;

public class ForceDeleteDSTask extends TransactionalTask {
    private static final Logger log = Logger.getLogger(ForceDeleteDSTask.class);

    private DeploymentSpec ds;

    public ForceDeleteDSTask(DeploymentSpec ds) {
        this.ds = ds;
        this.name = getName();
    }

    @Override
    public String getName() {
        return String.format("Force Delete Deployment Specification '%s'", this.ds.getName());
    }

    @Override
    public void executeTransaction(Session session) {
        log.info("Force Deleting Deployment Specification: " + this.ds.getName());
        // load deployment spec from database to avoid lazy loading issues
        this.ds = DeploymentSpecEntityMgr.findById(session, this.ds.getId());

        // remove DAI(s) for this ds
        for (DistributedApplianceInstance dai : this.ds.getDistributedApplianceInstances()) {
            for (VMPort port : dai.getProtectedPorts()) {
                dai.removeProtectedPort(port);
            }
            EntityManager.delete(session, dai);
        }

        // remove the sg reference from database
        boolean osSgCanBeDeleted = DeploymentSpecEntityMgr.findDeploymentSpecsByVirtualSystemTenantAndRegion(session,
                this.ds.getVirtualSystem(), this.ds.getTenantId(), this.ds.getRegion()).size() <= 1;

        if (osSgCanBeDeleted) {
            EntityManager.delete(session, this.ds.getOsSecurityGroupReference());
        }

        // delete DS from the database
        EntityManager.delete(session, this.ds);
    }

    @Override
    public Set<LockObjectReference> getObjects() {
        return LockObjectReference.getObjectReferences(this.ds);
    }

}
