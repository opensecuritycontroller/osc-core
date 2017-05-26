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
package org.osc.core.broker.service.tasks.conformance.securitygroupinterface;

import java.util.Set;

import javax.persistence.EntityManager;

import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.model.entities.appliance.VirtualSystemPolicy;
import org.osc.core.broker.model.entities.virtualization.SecurityGroupInterface;
import org.osc.core.broker.service.persistence.OSCEntityManager;
import org.osc.core.broker.service.tasks.TransactionalTask;
import org.osc.sdk.sdn.element.ServiceProfileElement;
import org.osgi.service.component.annotations.Component;

@Component(service=CreateSecurityGroupInterfaceTask.class)
public class CreateSecurityGroupInterfaceTask extends TransactionalTask {
    private VirtualSystemPolicy vsp;
    private ServiceProfileElement serviceProfile;

    public CreateSecurityGroupInterfaceTask create(VirtualSystemPolicy vsp, ServiceProfileElement nsxServiceProfile) {
        CreateSecurityGroupInterfaceTask task = new CreateSecurityGroupInterfaceTask();
        this.vsp = vsp;
        this.serviceProfile = nsxServiceProfile;
        this.name = getName();
        task.dbConnectionManager = this.dbConnectionManager;
        task.txBroadcastUtil = this.txBroadcastUtil;

        return task;
    }

    @Override
    public void executeTransaction(EntityManager em) throws Exception {
        this.vsp = em.find(VirtualSystemPolicy.class, this.vsp.getId());

        SecurityGroupInterface securityGroupInterface = new SecurityGroupInterface(this.vsp,
                this.serviceProfile.getId());

        securityGroupInterface.setName(this.serviceProfile.getName());
        securityGroupInterface.setTag(this.serviceProfile.getId());
        securityGroupInterface.setNsxVsmUuid(this.serviceProfile.getVsmId());

        OSCEntityManager.create(em, securityGroupInterface, this.txBroadcastUtil);
    }

    @Override
    public String getName() {
        return "Creating Security Group Interface '" + this.serviceProfile.getName() + "' ("
                + this.serviceProfile.getId() + ") assigned to Policy '" + this.vsp.getPolicy().getName() + "'";
    }

    @Override
    public Set<LockObjectReference> getObjects() {
        return LockObjectReference.getObjectReferences(this.vsp.getVirtualSystem());
    }

}
