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
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(service=PortGroupCheckMetaTask.class)
public class PortGroupCheckMetaTask extends TransactionalMetaTask {
    private static final Logger LOG = Logger.getLogger(PortGroupCheckMetaTask.class);

    @Reference
    CreatePortGroupTask createPortGroupTask;

    @Reference
    UpdatePortGroupTask updatePortGroupTask;

    @Reference
    DeletePortGroupTask deletePortGroupTask;

    private SecurityGroup securityGroup;
    boolean deleteTg;
    private String domainId;
    TaskGraph tg;

    public PortGroupCheckMetaTask create(SecurityGroup sg, boolean deleteTg, String domainId) {
        PortGroupCheckMetaTask task = new PortGroupCheckMetaTask();
        task.createPortGroupTask = this.createPortGroupTask;
        task.updatePortGroupTask = this.updatePortGroupTask;
        task.deletePortGroupTask = this.deletePortGroupTask;
        task.securityGroup = sg;
        task.deleteTg = deleteTg;
        task.domainId = domainId;
        task.dbConnectionManager = this.dbConnectionManager;
        task.txBroadcastUtil = this.txBroadcastUtil;

        return task;
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
                this.tg.appendTask(this.deletePortGroupTask.create(this.securityGroup, portGroup));
            } else {
                this.tg.appendTask(this.updatePortGroupTask.create(this.securityGroup, portGroup));
            }
        } else {
            this.tg.appendTask(this.createPortGroupTask.create(this.securityGroup));
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
