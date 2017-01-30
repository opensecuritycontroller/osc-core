package org.osc.core.broker.service.tasks.conformance.manager;

import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.osc.core.broker.job.TaskGraph;
import org.osc.core.broker.job.TaskGuard;
import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.model.entities.appliance.VirtualSystemPolicy;
import org.osc.core.broker.model.entities.management.ApplianceManagerConnector;
import org.osc.core.broker.model.entities.management.Domain;
import org.osc.core.broker.model.entities.management.Policy;
import org.osc.core.broker.model.plugin.manager.ManagerApiFactory;
import org.osc.core.broker.service.persistence.VirtualSystemPolicyEntityMgr;
import org.osc.core.broker.service.tasks.TransactionalMetaTask;
import org.osc.core.broker.service.tasks.conformance.virtualsystem.RemoveVendorTemplateTask;
import org.osc.sdk.manager.api.ManagerPolicyApi;
import org.osc.sdk.manager.element.ManagerPolicyElement;

public class SyncPolicyMetaTask extends TransactionalMetaTask {

    private static final Logger log = Logger.getLogger(SyncDomainMetaTask.class);

    private ApplianceManagerConnector mc;
    private TaskGraph tg;

    public SyncPolicyMetaTask(ApplianceManagerConnector mc) {
        this.mc = mc;
        this.name = getName();
    }

    @Override
    public void executeTransaction(Session session) throws Exception {

        this.tg = new TaskGraph();
        log.info("Start executing SyncPolicyMetaTask task for MC '" + this.mc.getName() + "'");

        this.mc = (ApplianceManagerConnector) session.get(ApplianceManagerConnector.class, this.mc.getId());

        Set<Domain> domains = this.mc.getDomains();

        for (Domain domain : domains) {
            ManagerPolicyApi mgrApi = ManagerApiFactory.createManagerPolicyApi(this.mc);
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
                    this.tg.appendTask(new CreatePolicyTask(this.mc, domain, policy));
                } else {
                    if (!policy.getName().equals(mgrPolicy.getName())) {
                        // Update policy attributes
                        this.tg.appendTask(new UpdatePolicyTask(policy, mgrPolicy.getName()));
                    }
                }
            }

            for (Policy policy : policies) {
                ManagerPolicyElement mgrPolicy = findByMgrPolicyId(mgrPolicies, policy.getMgrPolicyId());
                if (mgrPolicy == null) {

                    // Need to delete the associated VirtualSystemPolicy entries first
                    List<VirtualSystemPolicy> vsPolicies = VirtualSystemPolicyEntityMgr.listVSPolicyByPolicyId(session,
                            policy.getId());

                    if (vsPolicies != null) {
                        for (VirtualSystemPolicy vsPolicy : vsPolicies) {
                            this.tg.appendTask(new RemoveVendorTemplateTask(vsPolicy), TaskGuard.ALL_PREDECESSORS_COMPLETED);
                        }
                    }

                    // Delete policy
                    this.tg.appendTask(new DeletePolicyTask(policy));
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