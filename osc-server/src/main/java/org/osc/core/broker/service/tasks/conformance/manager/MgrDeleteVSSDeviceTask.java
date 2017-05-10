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
import org.osc.core.broker.model.entities.appliance.DistributedApplianceInstance;
import org.osc.core.broker.model.entities.appliance.VirtualSystem;
import org.osc.core.broker.model.plugin.ApiFactoryService;
import org.osc.core.broker.service.tasks.TransactionalTask;
import org.osc.sdk.manager.api.ManagerDeviceApi;
import org.osc.sdk.manager.element.ManagerDeviceElement;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(service = MgrDeleteVSSDeviceTask.class)
public class MgrDeleteVSSDeviceTask extends TransactionalTask {
    private static final Logger log = Logger.getLogger(MgrDeleteVSSDeviceTask.class);

    @Reference
    public ApiFactoryService apiFactoryService;

    private VirtualSystem vs;

    public MgrDeleteVSSDeviceTask create(VirtualSystem vs) {
        MgrDeleteVSSDeviceTask task = new MgrDeleteVSSDeviceTask();
        task.apiFactoryService = this.apiFactoryService;
        task.vs = vs;
        task.name = task.getName();
        return task;
    }

    @Override
    public void executeTransaction(EntityManager em) throws Exception {
        this.vs = em.find(VirtualSystem.class, this.vs.getId());

        ManagerDeviceApi mgrApi = this.apiFactoryService.createManagerDeviceApi(this.vs);
        try {
            for (DistributedApplianceInstance dai : this.vs.getDistributedApplianceInstances()) {
                // Delete individual device
                MgrDeleteMemberDeviceTask.deleteMemberDevice(mgrApi, dai);
            }

            deleteDevice(mgrApi);

        } finally {

            mgrApi.close();
        }
    }

    private void deleteDevice(ManagerDeviceApi mgrApi) throws Exception {
        if (this.vs.getMgrId() == null) {
            return;
        }

        try {
            mgrApi.deleteVSSDevice();

        } catch (Exception ex) {

            try {
                ManagerDeviceElement device = mgrApi.getDeviceById(this.vs.getMgrId());
                if (device != null) {
                    throw ex;
                }
            } catch (Exception e) {
                log.info("Fail to load Device: " + this.vs.getMgrId() + ". Assume already deleted.");
            }
        }
    }

    @Override
    public String getName() {
        return "Delete Manager VSS Device '" + this.vs.getName() + "'";
    }

    @Override
    public Set<LockObjectReference> getObjects() {
        return LockObjectReference.getObjectReferences(this.vs);
    }

}
