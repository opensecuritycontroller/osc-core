package org.osc.core.broker.service.tasks.conformance.openstack;

import java.util.Set;

import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.model.entities.appliance.ApplianceSoftwareVersion;
import org.osc.core.broker.model.entities.appliance.VirtualSystem;
import org.osc.core.broker.model.entities.virtualization.openstack.OsFlavorReference;
import org.osc.core.broker.rest.client.openstack.jcloud.Endpoint;
import org.osc.core.broker.rest.client.openstack.jcloud.JCloudNova;
import org.osc.core.broker.service.persistence.EntityManager;
import org.osc.core.broker.service.tasks.TransactionalTask;

class CreateFlavorTask extends TransactionalTask {

    private final Logger log = Logger.getLogger(CreateFlavorTask.class);

    private String region;
    private VirtualSystem vs;
    private String flavorName;
    private ApplianceSoftwareVersion applianceSoftwareVersion;
    private Endpoint osEndPoint;

    public CreateFlavorTask(VirtualSystem vs, String region, String flavorName, ApplianceSoftwareVersion applianceSoftwareVersion, Endpoint osEndPoint) {
        this.vs = vs;
        this.region = region;
        this.applianceSoftwareVersion = applianceSoftwareVersion;
        this.flavorName = flavorName;
        this.osEndPoint = osEndPoint;
    }

    @Override
    public void executeTransaction(Session session) throws Exception {
        EntityManager<VirtualSystem> vsEntityMgr = new EntityManager<VirtualSystem>(VirtualSystem.class, session);
        EntityManager<ApplianceSoftwareVersion> asvEntiyMgr = new EntityManager<ApplianceSoftwareVersion>(ApplianceSoftwareVersion.class, session);

        this.vs = vsEntityMgr.findByPrimaryKey(this.vs.getId());
        this.applianceSoftwareVersion = asvEntiyMgr.findByPrimaryKey(this.applianceSoftwareVersion.getId());

        JCloudNova nova = new JCloudNova(this.osEndPoint);
        try {
            this.log.info("Creating flavor " + this.flavorName + " in region + " + this.region);
            String newFlavorId = this.vs.getName() +  "_" + this.region;

            String flavorId = nova.createFlavor(this.region, newFlavorId, this.flavorName,
                    this.applianceSoftwareVersion.getDiskSizeInGb(), this.applianceSoftwareVersion.getMemoryInMb(),
                    this.applianceSoftwareVersion.getMinCpus());

            this.vs.addOsFlavorReference(new OsFlavorReference(this.vs, this.region, flavorId));

            EntityManager.update(session, this.vs);
        } finally {
            nova.close();
        }
    }

    @Override
    public String getName() {
        return String.format("Creating flavor '%s' in region '%s'", this.flavorName, this.region);
    };

    @Override
    public Set<LockObjectReference> getObjects() {
        return LockObjectReference.getObjectReferences(this.vs);
    }

}
