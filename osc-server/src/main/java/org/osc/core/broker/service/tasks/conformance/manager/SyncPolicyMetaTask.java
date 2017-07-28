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

import java.util.List;
import java.util.Set;

import javax.persistence.EntityManager;

import org.apache.log4j.Logger;
import org.osc.core.broker.job.TaskGraph;
import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.model.entities.management.ApplianceManagerConnector;
import org.osc.core.broker.model.entities.management.Domain;
import org.osc.core.broker.model.entities.management.Policy;
import org.osc.core.broker.model.plugin.ApiFactoryService;
import org.osc.core.broker.service.tasks.TransactionalMetaTask;
import org.osc.sdk.manager.api.ManagerPolicyApi;
import org.osc.sdk.manager.element.ManagerPolicyElement;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(service = SyncPolicyMetaTask.class)
public class SyncPolicyMetaTask extends TransactionalMetaTask {

    private static final Logger log = Logger.getLogger(SyncPolicyMetaTask.class);

    @Reference
    CreatePolicyTask createPolicyTask;

    @Reference
    UpdatePolicyTask updatePolicyTask;

    @Reference
    DeletePolicyTask deletePolicyTask;

    @Reference
    ApiFactoryService apiFactoryService;

    private ApplianceManagerConnector mc;
    private TaskGraph tg;

    public SyncPolicyMetaTask create(ApplianceManagerConnector mc) {
        SyncPolicyMetaTask task = new SyncPolicyMetaTask();
        task.mc = mc;
        task.name = task.getName();
        task.createPolicyTask = this.createPolicyTask;
        task.updatePolicyTask = this.updatePolicyTask;
        task.deletePolicyTask = this.deletePolicyTask;
        task.apiFactoryService = this.apiFactoryService;
        task.dbConnectionManager = this.dbConnectionManager;
        task.txBroadcastUtil = this.txBroadcastUtil;

        return task;
    }

    @Override
    public void executeTransaction(EntityManager em) throws Exception {

        this.tg = new TaskGraph();
        log.info("Start executing SyncPolicyMetaTask task for MC '" + this.mc.getName() + "'");

        this.mc = em.find(ApplianceManagerConnector.class, this.mc.getId());

        Set<Domain> domains = this.mc.getDomains();

        for (Domain domain : domains) {
            ManagerPolicyApi mgrApi = this.apiFactoryService.createManagerPolicyApi(this.mc);
            List<? extends ManagerPolicyElement> mgrPolicies = mgrApi.getPolicyList(domain.getMgrId());
            if (mgrPolicies == null) {
                continue;
            }

            Set<Policy> policies = domain.getPolicies();

            for (ManagerPolicyElement mgrPolicy : mgrPolicies) {
                Policy policy = findByMgrPolicyId(policies, mgrPolicy.getId());
                if (policy == null) {
                    // Add new policy
                    policy = new Policy(this.mc, domain);
                    policy.setName(mgrPolicy.getName());
                    policy.setMgrPolicyId(mgrPolicy.getId().toString());
                    this.tg.appendTask(this.createPolicyTask.create(this.mc, domain, policy));
                } else {
                    if (!policy.getName().equals(mgrPolicy.getName())) {
                        // Update policy attributes
                        this.tg.appendTask(this.updatePolicyTask.create(policy, mgrPolicy.getName()));
                    }
                }
            }

            for (Policy policy : policies) {
                ManagerPolicyElement mgrPolicy = findByMgrPolicyId(mgrPolicies, policy.getMgrPolicyId());
                if (mgrPolicy == null) {

                    // Delete policy
                    this.tg.appendTask(this.deletePolicyTask.create(policy));
                }
            }
        }

    }

    private ManagerPolicyElement findByMgrPolicyId(List<? extends ManagerPolicyElement> mgrPolicies, String mgrPolicyId) {
        for (ManagerPolicyElement mgrPolicy : mgrPolicies) {
            if (mgrPolicy.getId().equals(mgrPolicyId)) {
                return mgrPolicy;
            }
        }
        return null;
    }

    private Policy findByMgrPolicyId(Set<Policy> policies, String mgrPolicyId) {
        for (Policy policy : policies) {
            if (policy.getMgrPolicyId().equals(mgrPolicyId)) {
                return policy;
            }
        }
        return null;
    }

    @Override
    public String getName() {
        return "Syncing Policies for Manager Connector '" + this.mc.getName() + "'";
    }

    @Override
    public TaskGraph getTaskGraph() {
        return this.tg;
    }

    @Override
    public Set<LockObjectReference> getObjects() {
        return LockObjectReference.getObjectReferences(this.mc);
    }

}