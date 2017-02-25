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
package org.osc.core.broker.service.tasks.conformance.manager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.mockito.Mockito;
import org.osc.core.broker.job.TaskGraph;
import org.osc.core.broker.job.TaskGuard;
import org.osc.core.broker.model.entities.appliance.VirtualSystem;
import org.osc.core.broker.model.entities.appliance.VirtualSystemPolicy;
import org.osc.core.broker.model.entities.management.ApplianceManagerConnector;
import org.osc.core.broker.model.entities.management.Domain;
import org.osc.core.broker.model.entities.management.Policy;
import org.osc.core.broker.model.entities.virtualization.VirtualizationConnector;
import org.osc.core.broker.service.tasks.conformance.virtualsystem.RemoveVendorTemplateTask;
import org.osc.sdk.manager.element.ManagerPolicyElement;

public class SyncPolicyMetaTaskTestData {

    public static List<ApplianceManagerConnector> TEST_MCS = new ArrayList<ApplianceManagerConnector>();

    public static ManagerPolicyElement MGR_POLICY = createManagerPolicyElement("MGR_POLICY", "ID");

    private static Policy CREATED_POLICY_FROM_MGR_POLICY = createPolicy("MGR_POLICY", "ID", null, true);
    private static Policy POLICY_WITH_MGR_POLICY = createPolicy("POLICY_WITH_MGR_POLICY", "ID", 2L, true);
    public static Policy POLICY_WITHOUT_MGR_POLICY = createPolicy("POLICY_WITHOUT_MGR_POLICY", null, 3L, false);
    public static Policy POLICY_WITH_VS_POLICY = createPolicy("POLICY_WITH_VS_POLICY", null, 1L, false);

    public static Domain DOMAIN_WITHOUT_MGR_POLICY = createDomain("DOMAIN_WITHOUT_MGR_POLICY", POLICY_WITHOUT_MGR_POLICY);
    private static Domain DOMAIN_WITH_VS_POLICY = createDomain("DOMAIN_WITH_VS_POLICY", POLICY_WITH_VS_POLICY);
    public static Domain DOMAIN_WITHOUT_POLICY = createDomain("DOMAIN_WITHOUT_POLICY", null);
    public static Domain DOMAIN_WITH_POLICY = createDomain("DOMAIN_WITH_POLICY", POLICY_WITH_MGR_POLICY);

    public static VirtualSystemPolicy VS_POLICY = createVSPolicy("vc_name", POLICY_WITH_VS_POLICY, 1L);

    // Test data for DOMAIN_WITH_MULTIPLE_POLICIES_MC
    public static ManagerPolicyElement MGR_POLICY_1 = createManagerPolicyElement("MGR_POLICY_1", "ID_1");
    public static Policy POLICY_WITHOUT_MGR_POLICY_1 = createPolicy("POLICY_WITHOUT_MGR_POLICY_1", null, 13L, false);
    public static Policy POLICY_WITH_VS_POLICY_1 = createPolicy("POLICY_WITH_VS_POLICY_1", null, 12L, false);
    private static Policy POLICY_WITH_MGR_POLICY_1 = createPolicy("POLICY_WITH_MGR_POLICY_1", "ID_1", 11L, true);
    public static VirtualSystemPolicy VS_POLICY_1 = createVSPolicy("vc_name_1", POLICY_WITH_VS_POLICY_1, 12L);
    private static List<Policy> ALL_POLICIES_FOR_SINGLE_DOMAIN =
            new ArrayList<Policy>(Arrays.asList(POLICY_WITH_MGR_POLICY_1, POLICY_WITHOUT_MGR_POLICY_1, POLICY_WITH_VS_POLICY_1));
    private static Domain DOMAIN_WITH_POLICIES = createDomainWithPolicies("DOMAIN_WITH_POLICIES", ALL_POLICIES_FOR_SINGLE_DOMAIN);

    private static List<Domain> DOMAINS_WITH_POLICIES =
            new ArrayList<Domain>(Arrays.asList(DOMAIN_WITHOUT_MGR_POLICY, DOMAIN_WITH_VS_POLICY, DOMAIN_WITH_POLICY));
    private static List<Domain> DOMAINS_WITHOUT_MGR_POLICIES =
            new ArrayList<Domain>(Arrays.asList(DOMAIN_WITHOUT_POLICY, DOMAIN_WITH_VS_POLICY, DOMAIN_WITHOUT_MGR_POLICY));
    private static List<Policy> POLICIES_WITHOUT_MGR_POLICIES =
            new ArrayList<Policy>(Arrays.asList(POLICY_WITHOUT_MGR_POLICY, POLICY_WITH_VS_POLICY));
    private static List<Policy> ALL_POLICIES =
            new ArrayList<Policy>(Arrays.asList(POLICY_WITHOUT_MGR_POLICY, POLICY_WITH_VS_POLICY, POLICY_WITH_MGR_POLICY));

    public static ApplianceManagerConnector NO_DOMAIN_MC =
            createMC(null, null);
    public static ApplianceManagerConnector NO_MGR_POLICY_MC =
            createMC(DOMAIN_WITHOUT_POLICY, null);
    public static ApplianceManagerConnector MGR_POLICY_WITHOUT_POLICY_MC =
            createMC(DOMAIN_WITHOUT_POLICY, null);
    public static ApplianceManagerConnector MGR_POLICY_WITH_POLICY_MC =
            createMC(DOMAIN_WITH_POLICY, POLICY_WITH_MGR_POLICY);
    public static ApplianceManagerConnector POLICY_WITHOUT_MGR_POLICY_MC =
            createMC(DOMAIN_WITHOUT_MGR_POLICY, POLICY_WITHOUT_MGR_POLICY);
    public static ApplianceManagerConnector POLICY_WITH_VS_POLICY_MC =
            createMC(DOMAIN_WITH_VS_POLICY, POLICY_WITH_VS_POLICY);
    public static ApplianceManagerConnector DOMAINS_WITH_ORPHAN_AND_OUT_OF_SYNC_POLICIES_MC =
            createMCWithSets(DOMAINS_WITH_POLICIES, ALL_POLICIES);
    public static ApplianceManagerConnector DOMAINS_WITHOUT_POLICIES_AND_WITH_ORPHAN_POLICIES_MC =
            createMCWithSets(DOMAINS_WITHOUT_MGR_POLICIES, POLICIES_WITHOUT_MGR_POLICIES);
    public static ApplianceManagerConnector DOMAIN_WITH_MULTIPLE_POLICIES_MC =
            createMCWithSets(Arrays.asList(DOMAIN_WITH_POLICIES), ALL_POLICIES);

    public static TaskGraph emptyGraph(ApplianceManagerConnector mc) {
        TaskGraph expectedGraph = new TaskGraph();

        return expectedGraph;
    }

    public static TaskGraph createPolicyGraph(ApplianceManagerConnector mc) {
        TaskGraph expectedGraph = new TaskGraph();
        expectedGraph.appendTask(new CreatePolicyTask(mc, DOMAIN_WITHOUT_POLICY, CREATED_POLICY_FROM_MGR_POLICY));

        return expectedGraph;
    }

    public static TaskGraph updatePolicyGraph(ApplianceManagerConnector mc) {
        TaskGraph expectedGraph = new TaskGraph();
        expectedGraph.appendTask(new UpdatePolicyTask(POLICY_WITH_MGR_POLICY, "MGR_POLICY"));

        return expectedGraph;
    }

    public static TaskGraph deletePolicyGraph(ApplianceManagerConnector mc) {
        TaskGraph expectedGraph = new TaskGraph();
        expectedGraph.appendTask(new DeletePolicyTask(POLICY_WITHOUT_MGR_POLICY));

        return expectedGraph;
    }

    public static TaskGraph removeVendorTemplateAndDeletePolicyGraph(ApplianceManagerConnector mc) {
        TaskGraph expectedGraph = new TaskGraph();
        expectedGraph.appendTask(new RemoveVendorTemplateTask(VS_POLICY), TaskGuard.ALL_PREDECESSORS_COMPLETED);
        expectedGraph.appendTask(new DeletePolicyTask(POLICY_WITH_VS_POLICY));

        return expectedGraph;
    }

    public static TaskGraph deleteOrphanAndVSPoliciesGraph(ApplianceManagerConnector mc) {
        TaskGraph expectedGraph = new TaskGraph();
        expectedGraph.appendTask(new UpdatePolicyTask(POLICY_WITH_MGR_POLICY, "MGR_POLICY"));
        expectedGraph.appendTask(new DeletePolicyTask(POLICY_WITHOUT_MGR_POLICY));
        expectedGraph.appendTask(new RemoveVendorTemplateTask(VS_POLICY), TaskGuard.ALL_PREDECESSORS_COMPLETED);
        expectedGraph.appendTask(new DeletePolicyTask(POLICY_WITH_VS_POLICY));

        return expectedGraph;
    }

    public static TaskGraph deletePoliciesWithoutMgrPoliciesGraph(ApplianceManagerConnector mc) {
        TaskGraph expectedGraph = new TaskGraph();
        expectedGraph.appendTask(new DeletePolicyTask(POLICY_WITHOUT_MGR_POLICY));
        expectedGraph.appendTask(new RemoveVendorTemplateTask(VS_POLICY), TaskGuard.ALL_PREDECESSORS_COMPLETED);
        expectedGraph.appendTask(new DeletePolicyTask(POLICY_WITH_VS_POLICY));

        return expectedGraph;
    }

    public static TaskGraph deletePoliciesFromDomainGraph(ApplianceManagerConnector mc) {
        TaskGraph expectedGraph = new TaskGraph();
        expectedGraph.appendTask(new DeletePolicyTask(POLICY_WITHOUT_MGR_POLICY_1));
        expectedGraph.appendTask(new RemoveVendorTemplateTask(VS_POLICY_1), TaskGuard.ALL_PREDECESSORS_COMPLETED);
        expectedGraph.appendTask(new DeletePolicyTask(POLICY_WITH_VS_POLICY_1));
        expectedGraph.appendTask(new DeletePolicyTask(POLICY_WITH_MGR_POLICY_1));

        return expectedGraph;
    }

    private static ApplianceManagerConnector createBaseMC() {
        ApplianceManagerConnector mc = new ApplianceManagerConnector();
        TEST_MCS.add(mc);

        return mc;
    }

    private static ApplianceManagerConnector createMC(Domain domain, Policy policy) {
        ApplianceManagerConnector mc = createBaseMC();
        if (domain != null) {
            mc.addDomain(domain);
        }
        if (policy != null) {
            mc.addPolicy(policy);
        }

        return mc;
    }

    private static ApplianceManagerConnector createMCWithSets(List<Domain> domains, List<Policy> policies) {
        ApplianceManagerConnector mc = createBaseMC();
        for (Domain domain : domains) {
            mc.addDomain(domain);
        }
        for (Policy policy : policies) {
            mc.addPolicy(policy);
        }

        return mc;
    }

    private static ManagerPolicyElement createManagerPolicyElement(String policyName, String policyId) {
        ManagerPolicyElement managerPolicy = Mockito.mock(ManagerPolicyElement.class);
        Mockito.doReturn(policyName).when(managerPolicy).getName();
        Mockito.doReturn(policyId).when(managerPolicy).getId();

        return managerPolicy;
    }

    private static Policy createPolicy(String policyName, String policyMgrId, Long policyId, Boolean mgrPolicyExists) {
        Policy policy = new Policy();
        policy.setName(policyName);
        policy.setId(policyId);
        if(mgrPolicyExists) {
            policy.setMgrPolicyId(policyMgrId);
        }

        return policy;
    }

    private static Domain createDomain(String domainName, Policy policy) {
        Domain domain = new Domain();
        domain.setName(domainName);
        domain.setMgrId(domainName);
        if(policy != null) {
            domain.addPolicy(policy);
        }

        return domain;
    }

    private static Domain createDomainWithPolicies(String domainName, List<Policy> policies) {
        Domain domain = new Domain();
        domain.setName(domainName);
        domain.setMgrId(domainName);
        for (Policy policy : policies) {
            domain.addPolicy(policy);
        }

        return domain;
    }

    private static VirtualSystemPolicy createVSPolicy(String policyName, Policy policy, Long policyId) {
        VirtualizationConnector vc = new VirtualizationConnector();
        vc.setName(policyName);
        VirtualSystem vs = new VirtualSystem();
        vs.setId(policyId);
        vs.setVirtualizationConnector(vc);

        VirtualSystemPolicy vsPolicy = new VirtualSystemPolicy();
        vsPolicy.setId(policyId);
        policy.setId(policyId);
        vsPolicy.setPolicy(policy);

        vs.addVirtualSystemPolicy(vsPolicy);

        return vsPolicy;
    }
}