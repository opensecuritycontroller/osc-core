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

import java.util.Set;

import javax.persistence.EntityManager;

import org.apache.log4j.Logger;
import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.model.entities.appliance.VirtualSystem;
import org.osc.core.broker.model.plugin.manager.ManagerApiFactory;
import org.osc.core.broker.service.persistence.OSCEntityManager;
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
    public void executeTransaction(EntityManager em) throws Exception {

        this.vs = em.find(VirtualSystem.class, this.vs.getId());

        ManagerDeviceApi mgrApi = ManagerApiFactory.createManagerDeviceApi(this.vs);

        try {
            ManagerDeviceElement device = mgrApi.getDeviceById(this.vs.getMgrId());
            if (device != null) {
                if (device.getName().equals(this.vs.getName())) {
                    mgrApi.updateVSSDevice(device);
                } else {
                    throw new Exception("Found device with ID " + this.vs.getMgrId() + " but it seems to have a name ("
                            + device.getName() + ") which is different then our VSS name (" + this.vs.getName() + ")");
                }
            }

        } catch (Exception e) {

            log.info("Failed to locate vmidc device in Manager. Error:" + e.getMessage());
            String deviceId = mgrApi.findDeviceByName(this.vs.getName());
            if (deviceId != null) {
                ManagerDeviceElement device = mgrApi.getDeviceById(deviceId);
                mgrApi.updateVSSDevice(device);
            }

            this.vs.setMgrId(deviceId);
            OSCEntityManager.update(em, this.vs);

        } finally {
            mgrApi.close();
        }
    }

    @Override
    public String getName() {
        return String.format("Update Manager VSS Device with id %s for Virtual System %s", this.vs.getMgrId(), this.vs.getVirtualizationConnector().getName());
    }

    @Override
    public Set<LockObjectReference> getObjects() {
        return LockObjectReference.getObjectReferences(this.vs);
    }

}
