/*******************************************************************************
 * Copyright (c) 2017 Intel Corporation
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

import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.model.entities.appliance.DistributedApplianceInstance;
import org.osc.core.broker.model.entities.appliance.VirtualSystem;
import org.osc.core.broker.model.plugin.manager.ManagerApiFactory;
import org.osc.core.broker.service.persistence.EntityManager;
import org.osc.core.broker.service.tasks.TransactionalTask;
import org.osc.sdk.manager.api.ManagerDeviceApi;

/**
 * Creates the VSS device and updates the VS entity.
 */
public class MgrCreateVSSDeviceTask extends TransactionalTask {
    private static final Logger log = Logger.getLogger(MgrCreateVSSDeviceTask.class);

    private VirtualSystem vs;

    public MgrCreateVSSDeviceTask(VirtualSystem vs) {
        this.vs = vs;
        this.name = getName();
    }

    @Override
    public void executeTransaction(Session session) throws Exception {

        this.vs = (VirtualSystem) session.get(VirtualSystem.class, this.vs.getId());

        ManagerDeviceApi mgrApi = ManagerApiFactory.createManagerDeviceApi(this.vs);
        String deviceId = null;

        try {

            deviceId = mgrApi.createVSSDevice();
            vs.setMgrId(deviceId);
            EntityManager.update(session, vs);
            log.info("New VSS device (" + deviceId + ") successfully created.");

        } catch (Exception e) {

            log.info("Failed to create device in Manager.");
            deviceId = mgrApi.findDeviceByName(vs.getName());
            if (deviceId != null) {
                vs.setMgrId(deviceId);
                EntityManager.update(session, vs);
            } else {
                throw e;
            }
        } finally {
            mgrApi.close();
        }

        //Update the latest id for DAI's under the new VSS
        Set<DistributedApplianceInstance> daiList = this.vs.getDistributedApplianceInstances();
        daiList.forEach(dai -> {
            dai.setMgrDeviceId(null);
            EntityManager.update(session, dai);
        });
    }

    @Override
    public String getName() {
        return "Create Manager VSS Device for '" + this.vs.getVirtualizationConnector().getName() + "'";
    }

    @Override
    public Set<LockObjectReference> getObjects() {
        return LockObjectReference.getObjectReferences(this.vs);
    }

}
