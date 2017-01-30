package org.osc.core.broker.service.tasks.conformance.manager;

import java.util.Set;

import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.model.entities.appliance.DistributedApplianceInstance;
import org.osc.core.broker.model.plugin.manager.ManagerApiFactory;
import org.osc.core.broker.service.persistence.EntityManager;
import org.osc.core.broker.service.tasks.TransactionalTask;
import org.osc.sdk.manager.api.ManagerDeviceApi;
import org.osc.sdk.manager.element.ManagerDeviceMemberElement;

/**
 * Updates the device member and subsequently updates the respective distributed appliance instance with the identifier of the updated device member.
 */
public class MgrUpdateMemberDeviceTask extends TransactionalTask {
    private static final Logger log = Logger.getLogger(MgrUpdateMemberDeviceTask.class);

    private DistributedApplianceInstance dai;

    public MgrUpdateMemberDeviceTask(DistributedApplianceInstance dai) {
        this.dai = dai;
        this.name = getName();
    }

    @Override
    public void executeTransaction(Session session) throws Exception {

        this.dai = (DistributedApplianceInstance) session.get(DistributedApplianceInstance.class, this.dai.getId());

        ManagerDeviceApi mgrApi = ManagerApiFactory.createManagerDeviceApi(dai.getVirtualSystem());

        try {
            ManagerDeviceMemberElement deviceElement = mgrApi.getDeviceMemberById(dai.getMgrDeviceId());

            String updatedDeviceId = mgrApi.updateDeviceMember(deviceElement, dai.getName(), dai.getHostName(),
                    dai.getIpAddress(), dai.getMgmtIpAddress(), dai.getMgmtGateway(), dai.getMgmtSubnetPrefixLength());
            dai.setMgrDeviceId(updatedDeviceId);

            updateApplianceConfigIfNeeded(dai, mgrApi);

            EntityManager.update(session, dai);

        } catch (Exception e) {

            log.info("Failed to locate device member in Manager.");
            ManagerDeviceMemberElement deviceElement = mgrApi.findDeviceMemberByName(dai.getName());
            if (deviceElement != null) {
                log.info("Member device ID " + dai.getMgrDeviceId() + " was located by '" + dai.getName() + "' and ID "
                        + deviceElement.getId());
                String updatedDeviceId = mgrApi.updateDeviceMember(deviceElement, dai.getName(), dai.getHostName(),
                        dai.getIpAddress(), dai.getMgmtIpAddress(), dai.getMgmtGateway(),
                        dai.getMgmtSubnetPrefixLength());
                dai.setMgrDeviceId(updatedDeviceId);
            } else {
                dai.setMgrDeviceId(null);
            }

            updateApplianceConfigIfNeeded(dai, mgrApi);
            EntityManager.update(session, dai);
        } finally {
            mgrApi.close();
        }

    }

    private static void updateApplianceConfigIfNeeded(DistributedApplianceInstance dai, ManagerDeviceApi mgrApi)
            throws Exception {
        if (dai.getApplianceConfig() == null) {
            byte[] applianceConfig = mgrApi.getDeviceMemberConfigById(dai.getMgrDeviceId());
            dai.setApplianceConfig(applianceConfig);
            if (applianceConfig != null) {
                log.info("Got appliance config for member device (" + dai.getMgrDeviceId() + ").");
            }
        }
    }

    @Override
    public String getName() {
        return "Update Manager Member Device for '" + this.dai.getName() + "'";
    }

    @Override
    public Set<LockObjectReference> getObjects() {
        return LockObjectReference.getObjectReferences(this.dai);
    }

}
