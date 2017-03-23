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
package org.osc.core.broker.service.tasks.conformance.securitygroup;

import java.util.List;
import java.util.Set;

import javax.persistence.EntityManager;

import org.apache.log4j.Logger;
import org.osc.core.broker.job.TaskGraph;
import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.model.entities.appliance.VirtualSystem;
import org.osc.core.broker.model.entities.virtualization.SecurityGroup;
import org.osc.core.broker.model.entities.virtualization.SecurityGroupInterface;
import org.osc.core.broker.model.plugin.manager.ManagerApiFactory;
import org.osc.core.broker.service.persistence.SecurityGroupEntityMgr;
import org.osc.core.broker.service.tasks.TransactionalMetaTask;
import org.osc.sdk.manager.api.ManagerSecurityGroupApi;
import org.osc.sdk.manager.element.ManagerSecurityGroupElement;

public class MgrSecurityGroupCheckMetaTask extends TransactionalMetaTask {
    private static final Logger log = Logger.getLogger(MgrSecurityGroupCheckMetaTask.class);

    private VirtualSystem vs;
    private SecurityGroup sg;
    private TaskGraph tg;

    public MgrSecurityGroupCheckMetaTask(VirtualSystem vs) {
        this.vs = vs;
        this.name = getName();
    }

    public MgrSecurityGroupCheckMetaTask(VirtualSystem vs, SecurityGroup sg) {
        this.vs = vs;
        this.name = getName();
        this.sg = sg;
    }

    @Override
    public void executeTransaction(EntityManager em) throws Exception {
        this.vs = em.find(VirtualSystem.class, this.vs.getId());

        if (this.vs.getMgrId() == null) {
            log.warn("Manager VSS Device is not yet present. Do nothing.");
            return;
        }

        this.tg = new TaskGraph();
        ManagerSecurityGroupApi mgrSgApi = ManagerApiFactory.createManagerSecurityGroupApi(this.vs);

        if (this.sg == null) {
            // Sync all security groups across the vs'es
            List<? extends ManagerSecurityGroupElement> mgrEndpointGroups;
            try {
                mgrEndpointGroups = mgrSgApi.getSecurityGroupList();
            } finally {
                mgrSgApi.close();
            }

            for (SecurityGroupInterface sgi : this.vs.getSecurityGroupInterfaces()) {
                for (SecurityGroup sg : sgi.getSecurityGroups()) {
                    ManagerSecurityGroupElement mepg = findByMgrSecurityGroupId(mgrEndpointGroups, sg.getMgrId());
                    if (mepg == null) {
                        // It is possible it exists but have not been persisted in database.
                        // Search security group by name
                        mepg = findBySecurityGroupByName(mgrEndpointGroups, sg.getName());
                    }
                    if (!sgi.getMarkedForDeletion()) {
                        if (mepg == null) {
                            // Add new security group to Manager
                            this.tg.appendTask(new CreateMgrSecurityGroupTask(this.vs, sg));
                        } else {
                            this.tg.appendTask(new UpdateMgrSecurityGroupTask(this.vs, sg));
                        }
                    } else if (mepg != null) {
                        this.tg.appendTask(new DeleteMgrSecurityGroupTask(this.vs, mepg));
                    }
                }
            }

            // Remove any security groups which has no policy binding
            for (ManagerSecurityGroupElement mepge : mgrEndpointGroups) {
                // Check if SG present in our Database
                SecurityGroup sg = findVmidcSecurityGroupByMgrId(em, mepge);
                if (sg == null || sg.getSecurityGroupInterfaces().isEmpty()) {
                    // Delete security group from Manager if SG is not in our DB
                    this.tg.appendTask(new DeleteMgrSecurityGroupTask(this.vs, mepge));
                }
            }
        } else {
            // Sync members only for the specified security group
            ManagerSecurityGroupElement mepg = mgrSgApi.getSecurityGroupById(this.sg.getMgrId());
            if (mepg == null) {
                // Add new security group to Manager
                this.tg.appendTask(new CreateMgrSecurityGroupTask(this.vs, this.sg));
            } else {
                this.tg.appendTask(new UpdateMgrSecurityGroupTask(this.vs, this.sg));
            }
        }
    }

    private SecurityGroup findVmidcSecurityGroupByMgrId(EntityManager em, ManagerSecurityGroupElement mgrSecurityGroup)
            throws Exception {
        return SecurityGroupEntityMgr.listSecurityGroupsByVcIdAndMgrId(em, this.vs.getVirtualizationConnector()
                .getId(), mgrSecurityGroup.getSGId());
    }

    private ManagerSecurityGroupElement findBySecurityGroupByName(
            List<? extends ManagerSecurityGroupElement> mgrSecurityGroups, String securityGroupName) {
        for (ManagerSecurityGroupElement mgrSecurityGroup : mgrSecurityGroups) {
            if (mgrSecurityGroup.getName().equals(securityGroupName)) {
                return mgrSecurityGroup;
            }
        }
        return null;
    }

    private ManagerSecurityGroupElement findByMgrSecurityGroupId(
            List<? extends ManagerSecurityGroupElement> mgrSecurityGroups, String mgrSecurityGroupId) {
        for (ManagerSecurityGroupElement mgrSecurityGroup : mgrSecurityGroups) {
            if (mgrSecurityGroup.getSGId().equals(mgrSecurityGroupId)) {
                return mgrSecurityGroup;
            }
        }
        return null;
    }

    @Override
    public String getName() {
        String taskName;
        if (this.sg == null) {
            taskName = String.format("Checking Security Groups for Virtual System '%s' and Manager '%s'",
                    this.vs.getName(), this.vs.getDistributedAppliance().getApplianceManagerConnector().getName());
        } else {
            taskName = String.format("Checking '%s' Security Group for Virtual System '%s' and Manager '%s'",
                    this.sg.getName(), this.vs.getName(),
                    this.vs.getDistributedAppliance().getApplianceManagerConnector().getName());
        }
        return taskName;
    }

    @Override
    public TaskGraph getTaskGraph() {
        return this.tg;
    }

    @Override
    public Set<LockObjectReference> getObjects() {
        return LockObjectReference
                .getObjectReferences(this.vs.getDistributedAppliance().getApplianceManagerConnector());
    }

}
