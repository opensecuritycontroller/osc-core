package org.osc.core.broker.service.tasks.conformance.openstack.deploymentspec;

import java.util.Set;

import org.hibernate.Session;
import org.osc.core.broker.job.TaskGraph;
import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.model.entities.appliance.DistributedApplianceInstance;
import org.osc.core.broker.service.exceptions.VmidcBrokerValidationException;
import org.osc.core.broker.service.persistence.DistributedApplianceInstanceEntityMgr;
import org.osc.core.broker.service.tasks.TransactionalMetaTask;
import org.osc.core.broker.service.tasks.conformance.deleteda.DeleteDAIFromDbTask;

class DeleteSvaServerAndDAIMetaTask extends TransactionalMetaTask {

    private DistributedApplianceInstance dai;
    private final String region;
    private TaskGraph tg;

    /**
     * Deletes the SVA associated with the DAI from openstack and deletes the DAI from the DB
     *
     * @param region
     *            the region the sva belongs to
     * @param serverId
     *            the server id
     * @param daiId
     *            the dai id
     * @param osEndPoint
     */
    public DeleteSvaServerAndDAIMetaTask(String region, DistributedApplianceInstance dai) {
        this.region = region;
        this.dai = dai;
    }

    @Override
    public void executeTransaction(Session session) throws Exception {
        this.tg = new TaskGraph();
        this.dai = DistributedApplianceInstanceEntityMgr.findById(session, this.dai.getId());

        if (this.dai.getProtectedPorts() != null && !this.dai.getProtectedPorts().isEmpty()) {
            throw new VmidcBrokerValidationException("Server is being actively used to protect other servers");
        }

        this.tg.addTask(new DeleteSvaServerTask(this.region, this.dai));
        if (this.dai.getFloatingIpId() != null) {
            this.tg.appendTask(new OsSvaDeleteFloatingIpTask(this.dai));
        }
        this.tg.appendTask(new DeleteDAIFromDbTask(this.dai));
    }

    @Override
    public String getName() {
        return String.format("Deleting Distributed Appliance Instance and Server instance '%s' from region '%s'",
                this.dai.getName(), this.region);
    }

    @Override
    public TaskGraph getTaskGraph() {
        return this.tg;
    }

    @Override
    public Set<LockObjectReference> getObjects() {
        return LockObjectReference.getObjectReferences(this.dai);
    }

}
