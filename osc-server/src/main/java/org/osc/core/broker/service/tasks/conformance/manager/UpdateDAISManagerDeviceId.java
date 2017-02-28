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
