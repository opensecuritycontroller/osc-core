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
package org.osc.core.broker.service.tasks.conformance.openstack.securitygroup;

import javax.persistence.EntityManager;

import org.apache.log4j.Logger;
import org.osc.core.broker.model.entities.virtualization.SecurityGroupMember;
import org.osc.core.broker.service.persistence.OSCEntityManager;
import org.osc.core.broker.service.tasks.TransactionalTask;

public class MarkSecurityGroupMemberDeleteTask extends TransactionalTask {

    private static final Logger LOG = Logger.getLogger(MarkSecurityGroupMemberDeleteTask.class);

    @Override
    public String getName() {
        return "Delete Security Group Member " + this.sgm.getMemberName() + "; id:" + this.sgm.getId();
    }

    private SecurityGroupMember sgm;

    @Override
    public void executeTransaction(EntityManager em) throws Exception {
        LOG.debug("Start executing " + getClass().getSimpleName() + "; SGM: " + this.sgm);
        this.sgm = em.find(SecurityGroupMember.class, this.sgm.getId());
        OSCEntityManager.markDeleted(em, this.sgm, this.txBroadcastUtil);
    }

    public MarkSecurityGroupMemberDeleteTask create(SecurityGroupMember sgm) {
        MarkSecurityGroupMemberDeleteTask task = new MarkSecurityGroupMemberDeleteTask();
        task.sgm = sgm;
        task.dbConnectionManager = this.dbConnectionManager;
        task.txBroadcastUtil = this.txBroadcastUtil;

        return task;
    }
}
