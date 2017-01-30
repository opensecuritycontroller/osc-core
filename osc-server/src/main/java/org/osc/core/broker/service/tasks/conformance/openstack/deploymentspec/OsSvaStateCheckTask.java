package org.osc.core.broker.service.tasks.conformance.openstack.deploymentspec;

import java.util.Set;

import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.jclouds.openstack.nova.v2_0.domain.Server;
import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.model.entities.appliance.DistributedApplianceInstance;
import org.osc.core.broker.model.entities.virtualization.openstack.DeploymentSpec;
import org.osc.core.broker.rest.client.openstack.jcloud.Endpoint;
import org.osc.core.broker.rest.client.openstack.jcloud.JCloudNova;
import org.osc.core.broker.service.persistence.DistributedApplianceInstanceEntityMgr;
import org.osc.core.broker.service.tasks.TransactionalTask;

class OsSvaStateCheckTask extends TransactionalTask {

    private final Logger log = Logger.getLogger(OsSvaStateCheckTask.class);

    private DistributedApplianceInstance dai;

    public OsSvaStateCheckTask(DistributedApplianceInstance dai) {
        this.dai = dai;
    }

    @Override
    public void executeTransaction(Session session) throws Exception {
        this.dai = DistributedApplianceInstanceEntityMgr.findById(session, this.dai.getId());
        DeploymentSpec ds = this.dai.getDeploymentSpec();

        Endpoint endPoint = new Endpoint(ds);
        JCloudNova nova = new JCloudNova(endPoint);
        try {
            Server serverDAI = nova.getServer(ds.getRegion(), this.dai.getOsServerId());
            // Check is SVA is Shut off
            if (serverDAI.getStatus().equals(Server.Status.SHUTOFF)) {
                this.log.info("SVA found in SHUTOFF state we will try to start it ...");
                nova.startServer(ds.getRegion(), this.dai.getOsServerId());
            }

        } finally {
            nova.close();
        }
    }

    @Override
    public String getName() {
        return String.format("Checking State for Distributed Appliance Instance '%s'", this.dai.getName());
    }

    @Override
    public Set<LockObjectReference> getObjects() {
        return LockObjectReference.getObjectReferences(this.dai);
    }
}
