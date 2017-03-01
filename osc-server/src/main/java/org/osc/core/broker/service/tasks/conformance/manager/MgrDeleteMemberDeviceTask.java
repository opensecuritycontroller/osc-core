/*******************************************************************************
 * Copyright (c) Intel Corporation
 * Copyright (c) 2017
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package org.osc.core.broker.service.tasks.conformance.manager;

import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.osc.core.broker.model.entities.appliance.DistributedApplianceInstance;
import org.osc.core.broker.model.entities.appliance.VirtualSystem;
import org.osc.core.broker.model.plugin.manager.ManagerApiFactory;
import org.osc.core.broker.service.tasks.TransactionalTask;
import org.osc.sdk.manager.api.ManagerDeviceApi;
import org.osc.sdk.manager.element.ManagerDeviceMemberElement;

public class MgrDeleteMemberDeviceTask extends TransactionalTask {
    private static final Logger log = Logger.getLogger(MgrDeleteMemberDeviceTask.class);

    private ManagerDeviceMemberElement device;
    private VirtualSystem vs;
    private String deviceName;

    public MgrDeleteMemberDeviceTask(VirtualSystem vs, ManagerDeviceMemberElement device) {
        this.vs = vs;
        this.device = device;
        this.deviceName = device.getName(); 
    }

    @Override
    public void executeTransaction(Session session) throws Exception {
        deleteMemberDevice(this.vs, device);
    }

    public static boolean deleteMemberDevice(DistributedApplianceInstance dai) throws Exception {
        ManagerDeviceApi mgrApi = ManagerApiFactory.createManagerDeviceApi(dai.getVirtualSystem());
        try {
            return deleteMemberDevice(mgrApi, dai);
        } finally {
            mgrApi.close();
        }
    }

    public static boolean deleteMemberDevice(ManagerDeviceApi mgrApi, DistributedApplianceInstance dai) {
        String deviceId = dai.getMgrDeviceId();
        if (deviceId == null) {
            return true;
        }

        return deleteMemberDevice(mgrApi, deviceId);
    }

    private static boolean deleteMemberDevice(VirtualSystem vs, ManagerDeviceMemberElement device) throws Exception {
        ManagerDeviceApi mgrApi = ManagerApiFactory.createManagerDeviceApi(vs);
        try {
            return deleteMemberDevice(mgrApi, device.getId());
        } finally {
            mgrApi.close();
        }
    }

    private static boolean deleteMemberDevice(ManagerDeviceApi mgrApi, String deviceId) {
        try {
            mgrApi.deleteDeviceMember(deviceId);
            return true;

        } catch (Exception ex) {

            try {
                ManagerDeviceMemberElement device = mgrApi.getDeviceMemberById(deviceId);
                if (device != null) {
                    log.error("Fail to delete member device: " + deviceId, ex);
                    return false;
                }

            } catch (Exception e) {

                log.info("Failed to delete device member id '" + deviceId + "') from MC. Assume already deleted.");
            }
        }
        return true;
    }

    @Override
    public String getName() {
        return "Deleting Manager Member Device for '" + this.deviceName + "'";
    }

}
