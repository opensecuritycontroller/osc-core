package org.osc.core.broker.service.tasks.conformance.openstack;

import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.osc.core.broker.model.entities.virtualization.openstack.OsImageReference;
import org.osc.core.broker.rest.client.openstack.jcloud.Endpoint;
import org.osc.core.broker.rest.client.openstack.jcloud.JCloudGlance;
import org.osc.core.broker.service.persistence.EntityManager;
import org.osc.core.broker.service.tasks.TransactionalTask;

public class DeleteImageTask  extends TransactionalTask {
    private final Logger log = Logger.getLogger(DeleteImageTask.class);

    private String region;
    private OsImageReference imageReference;
    private Endpoint osEndPoint;

    public DeleteImageTask(String region, OsImageReference imageReference, Endpoint osEndPoint) {
        this.region = region;
        this.osEndPoint = osEndPoint;
        this.imageReference = imageReference;
    }

    public DeleteImageTask(OsImageReference imageReference) {
        this.imageReference = imageReference;
    }

    @Override
    public void executeTransaction(Session session) throws Exception {
        this.imageReference = (OsImageReference) session.get(OsImageReference.class, this.imageReference.getId());

        if (this.region == null) {
            this.log.info("Deleting image " + this.imageReference.getImageRefId());
        } else {
            this.log.info("Deleting image " + this.imageReference.getImageRefId() + " from region " + this.region);

            JCloudGlance glance = new JCloudGlance(this.osEndPoint);
            try {
                glance.deleteImageById(this.region, this.imageReference.getImageRefId());
            } finally {
                glance.close();
            }
        }
        EntityManager.delete(session, this.imageReference);
    }

    @Override
    public String getName() {
        if (this.region == null) {
            return String.format("Deleting image with id: '%s'", this.imageReference.getImageRefId());
        } else {
            return String.format("Deleting image with id: '%s' from region '%s'", this.imageReference.getImageRefId(), this.region);
        }
    }

}
