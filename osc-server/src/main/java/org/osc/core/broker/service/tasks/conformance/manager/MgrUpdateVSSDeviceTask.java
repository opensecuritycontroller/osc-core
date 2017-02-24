package org.osc.core.broker.service.tasks.conformance.manager;

import java.util.Set;

import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.model.entities.appliance.VirtualSystem;
import org.osc.core.broker.model.plugin.manager.ManagerApiFactory;
import org.osc.core.broker.service.persistence.EntityManager;
import org.osc.core.broker.service.tasks.TransactionalTask;
import org.osc.sdk.manager.api.ManagerDeviceApi;
import org.osc.sdk.manager.element.ManagerDeviceElement;

/**
 * Updates the VSS device for the provided VS.
 */
public class MgrUpdateVSSDeviceTask extends TransactionalTask {
    private static final Logger log = Logger.getLogger(MgrUpdateVSSDeviceTask.class);

    private VirtualSystem vs;

    public MgrUpdateVSSDeviceTask(VirtualSystem vs) {
        this.vs = vs;
        this.name = getName();
    }

    @Override
    public void executeTransaction(Session session) throws Exception {

        this.vs = (VirtualSystem) session.get(VirtualSystem.class, this.vs.getId());

        ManagerDeviceApi mgrApi = ManagerApiFactory.createManagerDeviceApi(this.vs);

        try {
            ManagerDeviceElement device = mgrApi.getDeviceById(vs.getMgrId());
            if (device != null) {
                if (device.getName().equals(this.vs.getName())) {
                    mgrApi.updateVSSDevice(device);
                } else {
                    throw new Exception("Found device with ID " + vs.getMgrId() + " but it seems to have a name ("
                            + device.getName() + ") which is different then our VSS name (" + vs.getName() + ")");
                }
            }

        } catch (Exception e) {

            log.info("Failed to locate vmidc device in Manager. Error:" + e.getMessage());
            String deviceId = mgrApi.findDeviceByName(vs.getName());
            if (deviceId != null) {
                ManagerDeviceElement device = mgrApi.getDeviceById(deviceId);
                mgrApi.updateVSSDevice(device);
            }

            vs.setMgrId(deviceId);
            EntityManager.update(session, vs);

        } finally {
            mgrApi.close();
        }
    }

    @Override
    public String getName() {
        return String.format("Update Manager VSS Device with id %s for Virtual System %s", vs.getMgrId(), this.vs.getVirtualizationConnector().getName());
    }

    @Override
    public Set<LockObjectReference> getObjects() {
        return LockObjectReference.getObjectReferences(this.vs);
    }

}
