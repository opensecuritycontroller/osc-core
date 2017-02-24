package org.osc.core.broker.service.tasks.conformance.manager;

import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.hibernate.Session;
import org.osc.core.broker.job.TaskGraph;
import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.model.entities.appliance.DistributedApplianceInstance;
import org.osc.core.broker.model.entities.appliance.VirtualSystem;
import org.osc.core.broker.model.plugin.manager.ManagerApiFactory;
import org.osc.core.broker.service.persistence.DistributedApplianceInstanceEntityMgr;
import org.osc.core.broker.service.tasks.TransactionalMetaTask;
import org.osc.sdk.manager.api.ManagerDeviceApi;
import org.osc.sdk.manager.element.ManagerDeviceMemberElement;

public class MgrCheckDevicesMetaTask extends TransactionalMetaTask {

    private VirtualSystem vs;
    private TaskGraph tg;

    public MgrCheckDevicesMetaTask(VirtualSystem vs) {
        this.vs = vs;
        this.name = getName();
    }

    @Override
    public void executeTransaction(Session session) throws Exception {

        this.vs = (VirtualSystem) session.get(VirtualSystem.class, this.vs.getId());
        this.tg = new TaskGraph();

        try (ManagerDeviceApi mgrApi = ManagerApiFactory.createManagerDeviceApi(this.vs)) {

            // Check Container Appliance first
            if (mgrApi.isDeviceGroupSupported()) {
                checkVSSDevice(this.vs, mgrApi, session);
            }

            // TODO: Future. If deleting members tasks fails, we may still want to continue with member device checks
            // Remove dangling members

            // If device grouping not supported, then skip deleting the members on manager
            if (mgrApi.isDeviceGroupSupported() && this.vs.getMgrId() != null) {
                for (ManagerDeviceMemberElement device : mgrApi.listDeviceMembers()) {
                    DistributedApplianceInstance dai = DistributedApplianceInstanceEntityMgr.findByName(session,
                            device.getName());
                    if (dai == null) {
                        this.tg.appendTask(new MgrDeleteMemberDeviceTask(this.vs, device));
                    }
                }
            }

            // Check DAIs
            if (mgrApi.isDeviceGroupSupported()) {
                for (DistributedApplianceInstance dai : this.vs.getDistributedApplianceInstances()) {
                    checkMemberDevice(session, dai, mgrApi, this.tg);
                }
            } else {
                this.tg.appendTask(new UpdateDAISManagerDeviceId(this.vs));
            }

        }
    }

    private void checkVSSDevice(VirtualSystem vs, ManagerDeviceApi mgrApi, Session session) throws Exception {

        // Check if already been created in Manager
        if (vs.getMgrId() != null) {
            this.tg.appendTask(new MgrUpdateVSSDeviceTask(vs));
        } else {
            // Add device to Manager if not yet added.
            this.tg.appendTask(new MgrCreateVSSDeviceTask(vs));
        }

    }

    private static void checkMemberDevice(Session session, DistributedApplianceInstance dai, ManagerDeviceApi mgrApi,
            TaskGraph tg) throws Exception {

        // Check if already been created in Manager
        if (dai.getMgrDeviceId() != null) {

            // Verify existence in Manager.
            tg.appendTask(new MgrUpdateMemberDeviceTask(dai));
        } else {
            // Add device to Manager if not yet added.
            if (!StringUtils.isEmpty(dai.getIpAddress())) {
                tg.appendTask(new MgrCreateMemberDeviceTask(dai));
            }
        }

    }

    @Override
    public String getName() {
        return "Checking Manager Devices for '" + this.vs.getVirtualizationConnector().getName() + "'";
    }

    @Override
    public Set<LockObjectReference> getObjects() {
        return LockObjectReference.getObjectReferences(this.vs);
    }

    @Override
    public TaskGraph getTaskGraph() {
        return this.tg;
    }

}
