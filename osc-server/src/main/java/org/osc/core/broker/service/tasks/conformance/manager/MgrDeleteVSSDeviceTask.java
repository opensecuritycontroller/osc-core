package org.osc.core.broker.service.tasks.conformance.manager;

import java.util.Set;

import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.model.entities.appliance.DistributedApplianceInstance;
import org.osc.core.broker.model.entities.appliance.VirtualSystem;
import org.osc.core.broker.model.plugin.manager.ManagerApiFactory;
import org.osc.core.broker.service.tasks.TransactionalTask;
import org.osc.sdk.manager.api.ManagerDeviceApi;
import org.osc.sdk.manager.element.ManagerDeviceElement;

public class MgrDeleteVSSDeviceTask extends TransactionalTask {
    private static final Logger log = Logger.getLogger(MgrDeleteVSSDeviceTask.class);

    private VirtualSystem vs;

    public MgrDeleteVSSDeviceTask(VirtualSystem vs) {
        this.vs = vs;
        this.name = getName();
    }

    @Override
    public void executeTransaction(Session session) throws Exception {
        vs = (VirtualSystem) session.get(VirtualSystem.class, vs.getId());

        ManagerDeviceApi mgrApi = ManagerApiFactory.createManagerDeviceApi(vs);
        try {
            for (DistributedApplianceInstance dai : vs.getDistributedApplianceInstances()) {
                // Delete individual device
                MgrDeleteMemberDeviceTask.deleteMemberDevice(mgrApi, dai);
            }

            deleteDevice(mgrApi);

        } finally {

            mgrApi.close();
        }
    }

    private void deleteDevice(ManagerDeviceApi mgrApi) throws Exception {
        if (vs.getMgrId() == null) {
            return;
        }

        try {
            mgrApi.deleteVSSDevice();

        } catch (Exception ex) {

            try {
                ManagerDeviceElement device = mgrApi.getDeviceById(vs.getMgrId());
                if (device != null) {
                    throw ex;
                }
            } catch (Exception e) {
                log.info("Fail to load Device: " + vs.getMgrId() + ". Assume already deleted.");
            }
        }
    }

    @Override
    public String getName() {
        return "Delete Manager VSS Device '" + vs.getName() + "'";
    }

    @Override
    public Set<LockObjectReference> getObjects() {
        return LockObjectReference.getObjectReferences(vs);
    }

}
