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

import java.util.Set;

import javax.persistence.EntityManager;

import org.apache.log4j.Logger;
import org.osc.core.broker.job.TaskGraph;
import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.model.entities.virtualization.SecurityGroup;
import org.osc.core.broker.service.tasks.TransactionalMetaTask;
import org.osc.core.broker.service.tasks.conformance.openstack.securitygroup.element.PortGroup;

public class PortGroupCheckMetaTask extends TransactionalMetaTask {
    private static final Logger LOG = Logger.getLogger(PortGroupCheckMetaTask.class);

    private SecurityGroup securityGroup;
    boolean deleteTg;
    private final String domainId;
    TaskGraph tg;

    public PortGroupCheckMetaTask(SecurityGroup sg, boolean deleteTg, String domainId) {
        this.securityGroup = sg;
        this.deleteTg = deleteTg;
        this.domainId = domainId;
    }

    @Override
    public void executeTransaction(EntityManager em) throws Exception {
        LOG.info("Start executing PortGroupCheckMetaTask Task. Security Group '" + this.securityGroup + "'");
        this.tg = new TaskGraph();
        this.securityGroup = em.find(SecurityGroup.class, this.securityGroup.getId());

        String portGroupId = this.securityGroup.getNetworkElementId();
        PortGroup portGroup = new PortGroup();
        portGroup.setPortGroupId(portGroupId);
        portGroup.setParentId(this.domainId);

        if (portGroupId != null) {
            if (this.deleteTg) {
                this.tg.appendTask(new DeletePortGroupTask(this.securityGroup, portGroup));
            } else {
                this.tg.appendTask(new UpdatePortGroupTask(this.securityGroup, portGroup));
            }
        } else {
            this.tg.appendTask(new CreatePortGroupTask(this.securityGroup));
        }
    }

    @Override
    public String getName() {
        return String.format("Checking Port Group for security group '%s'", this.securityGroup.getName());
    }

    @Override
    public Set<LockObjectReference> getObjects() {
        return LockObjectReference.getObjectReferences(this.securityGroup);
    }

    @Override
    public TaskGraph getTaskGraph() {
        return this.tg;
    }
}
