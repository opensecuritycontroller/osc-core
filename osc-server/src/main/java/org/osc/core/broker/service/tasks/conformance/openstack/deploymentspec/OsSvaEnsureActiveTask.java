package org.osc.core.broker.service.tasks.conformance.openstack.deploymentspec;

import java.util.Set;

import org.hibernate.Session;
import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.model.entities.appliance.DistributedApplianceInstance;
import org.osc.core.broker.model.entities.virtualization.VirtualizationConnector;
import org.osc.core.broker.model.entities.virtualization.openstack.DeploymentSpec;
import org.osc.core.broker.service.persistence.DistributedApplianceInstanceEntityMgr;
import org.osc.core.broker.service.tasks.TransactionalTask;

/**
 * Ensures the SVA vm is active. Basically waits until the SVA is active for us to
 * follow up with other tasks which rely on the server being active and ready.
 * This is a transactional task but does not updates on the enities so should be safe from conflicts
 */
class OsSvaEnsureActiveTask extends TransactionalTask {

    private DistributedApplianceInstance dai;

    public OsSvaEnsureActiveTask(DistributedApplianceInstance dai) {
        this.dai = dai;
    }

    @Override
    public void executeTransaction(Session session) throws Exception {
        this.dai = DistributedApplianceInstanceEntityMgr.findById(session, this.dai.getId());

        String osServerId = this.dai.getOsServerId();
        DeploymentSpec ds = this.dai.getDeploymentSpec();
        VirtualizationConnector vc = ds.getVirtualSystem().getVirtualizationConnector();

        String tenantName = ds.getTenantName();
        String region = ds.getRegion();
        OpenstackUtil.ensureVmActive(vc, tenantName, region, osServerId);
    }

    @Override
    public String getName() {
        return String.format("Ensuring SVA '%s' is active", this.dai.getName());
    }

    @Override
    public Set<LockObjectReference> getObjects() {
        return LockObjectReference.getObjectReferences(this.dai);
    }

}
