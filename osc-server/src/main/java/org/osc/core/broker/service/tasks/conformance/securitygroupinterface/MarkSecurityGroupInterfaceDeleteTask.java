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

import javax.persistence.EntityManager;

import org.osc.core.broker.model.entities.virtualization.SecurityGroupInterface;
import org.osc.core.broker.service.persistence.OSCEntityManager;
import org.osc.core.broker.service.tasks.TransactionalTask;
import org.osgi.service.component.annotations.Component;

@Component(service=MarkSecurityGroupInterfaceDeleteTask.class)
public class MarkSecurityGroupInterfaceDeleteTask extends TransactionalTask {
    @Override
    public String getName() {
        return String.format("Delete Security Group Interface %s", this.sgi.getName());
    }

    private SecurityGroupInterface sgi;

    @Override
    public void executeTransaction(EntityManager em) throws Exception {
        this.sgi = em.find(SecurityGroupInterface.class, this.sgi.getId());
        OSCEntityManager.markDeleted(em, this.sgi, this.txBroadcastUtil);
    }

    public MarkSecurityGroupInterfaceDeleteTask create(SecurityGroupInterface sgi) {
        MarkSecurityGroupInterfaceDeleteTask task = new MarkSecurityGroupInterfaceDeleteTask();
        task.sgi = sgi;
        task.dbConnectionManager = this.dbConnectionManager;
        task.txBroadcastUtil = this.txBroadcastUtil;

        return task;
    }
}
