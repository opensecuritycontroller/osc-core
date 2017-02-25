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
package org.osc.core.broker.service.tasks.conformance.securitygroup;

import java.util.Set;

import org.hibernate.Session;
import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.model.entities.appliance.VirtualSystem;
import org.osc.core.broker.model.entities.virtualization.SecurityGroup;
import org.osc.core.broker.model.plugin.manager.ManagerApiFactory;
import org.osc.core.broker.service.persistence.EntityManager;
import org.osc.core.broker.service.tasks.TransactionalTask;
import org.osc.sdk.manager.api.ManagerSecurityGroupApi;

class CreateMgrSecurityGroupTask extends TransactionalTask {
    //private static final Logger log = Logger.getLogger(CreateMgrEndpointGroupTask.class);

    private SecurityGroup sg;
    private VirtualSystem vs;

    public CreateMgrSecurityGroupTask(VirtualSystem vs, SecurityGroup sg) {
        this.vs = vs;
        this.sg = sg;
        this.name = getName();
    }

    @Override
    public void executeTransaction(Session session) throws Exception {
        this.sg = (SecurityGroup) session.get(SecurityGroup.class, this.sg.getId());

        ManagerSecurityGroupApi mgrApi = ManagerApiFactory.createManagerSecurityGroupApi(this.vs);
        try {
            // The immutable id generated by NSX in case of VMware.
            // Otherwise, for openstack, we'll use ISC id generated by our database.
            String iscId = this.sg.getNsxId();
            if (iscId == null) {
                iscId = this.sg.getId().toString();
            }
            String mgrEndpointGroupId = mgrApi.createSecurityGroup(this.sg.getName(), iscId,
                    this.sg.getSecurityGroupMemberListElement());
            this.sg.setMgrId(mgrEndpointGroupId);
            EntityManager.update(session, this.sg);

        } finally {
            mgrApi.close();
        }

    }

    @Override
    public String getName() {
        return "Create Manager Security Group for Virtualization System '"
                + this.vs.getVirtualizationConnector().getName() + "'";
    }

    @Override
    public Set<LockObjectReference> getObjects() {
        return LockObjectReference.getObjectReferences(this.vs);
    }

}
