package org.osc.core.broker.service.tasks.conformance.manager;

import java.util.Set;

import org.hibernate.Session;
import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.model.entities.appliance.DistributedApplianceInstance;
import org.osc.core.broker.model.entities.appliance.VirtualSystem;
import org.osc.core.broker.model.plugin.manager.ManagerApiFactory;
import org.osc.core.broker.service.persistence.EntityManager;
import org.osc.core.broker.service.tasks.TransactionalTask;
import org.osc.sdk.manager.api.ManagerDeviceApi;
import org.osc.sdk.manager.element.ManagerDeviceMemberElement;

public class UpdateDAISManagerDeviceId extends TransactionalTask {

    private VirtualSystem vs;

    public UpdateDAISManagerDeviceId(VirtualSystem vs) {
        this.vs = vs;
    }

    @Override
    public void executeTransaction(Session session) throws Exception {

        try (ManagerDeviceApi mgrApi = ManagerApiFactory.createManagerDeviceApi(this.vs)) {
            for (ManagerDeviceMemberElement mgrDeviceMember : mgrApi.listDeviceMembers()) {
                for (DistributedApplianceInstance dai : this.vs.getDistributedApplianceInstances()) {
                    if (dai.getName().equals(mgrDeviceMember.getName())
                            && !mgrDeviceMember.getId().equals(dai.getMgrDeviceId())) {
                        dai.setMgrDeviceId(mgrDeviceMember.getId());
                        EntityManager.update(session, dai);
                    }
                }
            }
        }
    }

    @Override
    public String getName() {
        return String.format("Updating distributed appliance instance with manager information of Virtual System %s", this.vs.getName());
    }

    @Override
    public Set<LockObjectReference> getObjects() {
        return LockObjectReference.getObjectReferences(this.vs);
    }

}
