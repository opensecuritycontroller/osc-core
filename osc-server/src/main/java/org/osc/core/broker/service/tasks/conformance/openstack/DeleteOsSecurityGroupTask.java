package org.osc.core.broker.service.tasks.conformance.openstack;

import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.jclouds.openstack.neutron.v2.domain.SecurityGroup;

import org.osc.core.broker.model.entities.virtualization.openstack.DeploymentSpec;
import org.osc.core.broker.model.entities.virtualization.openstack.OsSecurityGroupReference;
import org.osc.core.broker.rest.client.openstack.jcloud.Endpoint;
import org.osc.core.broker.rest.client.openstack.jcloud.JCloudNeutron;
import org.osc.core.broker.service.persistence.DeploymentSpecEntityMgr;
import org.osc.core.broker.service.persistence.EntityManager;
import org.osc.core.broker.service.tasks.TransactionalTask;

public class DeleteOsSecurityGroupTask extends TransactionalTask {

    private final Logger log = Logger.getLogger(DeleteOsSecurityGroupTask.class);

    private static final int SLEEP_RETRIES = 5 * 1000; // 5 seconds
    private static final int MAX_ATTEMPTS = 3;

    private DeploymentSpec ds;
    private OsSecurityGroupReference sgReference;

    public DeleteOsSecurityGroupTask(DeploymentSpec ds, OsSecurityGroupReference sgReference) {
        this.ds = ds;
        this.sgReference = sgReference;
    }

    @Override
    public void executeTransaction(Session session) throws Exception {

        int count = MAX_ATTEMPTS;

        boolean osSgCanBeDeleted = DeploymentSpecEntityMgr.findDeploymentSpecsByVirtualSystemTenantAndRegion(session,
                this.ds.getVirtualSystem(), this.ds.getTenantId(), this.ds.getRegion()).size() <= 1;

        if (osSgCanBeDeleted) {
            this.log.info(String.format("Deleting Openstack Security Group with id '%s' from region '%s'",
                    this.sgReference.getSgRefId(), this.ds.getRegion()));

            this.sgReference = (OsSecurityGroupReference) session.get(OsSecurityGroupReference.class,
                    this.sgReference.getId());

            Endpoint endPoint = new Endpoint(this.ds);
            try (JCloudNeutron neutron = new JCloudNeutron(endPoint)) {
                boolean success = false;
                // check if the security group exist on Openstack
                SecurityGroup osSg = neutron.getSecurityGroupById(this.ds.getRegion(), this.sgReference.getSgRefId());
                if (osSg != null) {
                    while (!success) {
                        try {
                            success = neutron.deleteSecurityGroupById(this.ds.getRegion(),
                                    this.sgReference.getSgRefId());
                        } catch (IllegalStateException ex) {
                            log.info(" Openstack Security Group id:" + this.sgReference.getSgRefId() + " in use.");
                            Thread.sleep(SLEEP_RETRIES);
                        } finally {
                            if (--count <= 0) {
                                throw (new Exception("Unable to delete the Openstack Security Group id: "
                                        + this.sgReference.getSgRefId()));
                            }
                        }
                    }
                }
            }

            for (DeploymentSpec ds : this.sgReference.getDeploymentSpecs()) {
                ds.setOsSecurityGroupReference(null);
                EntityManager.update(session, ds);
            }
            this.sgReference.getDeploymentSpecs().clear();
            EntityManager.delete(session, this.sgReference);
        }
    }

    @Override
    public String getName() {
        return String.format("Deleting Openstack Security Group with id '%s' from tenant '%s' in region '%s'",
                this.sgReference.getSgRefId(), this.ds.getTenantName(), this.ds.getRegion());

    };

}
