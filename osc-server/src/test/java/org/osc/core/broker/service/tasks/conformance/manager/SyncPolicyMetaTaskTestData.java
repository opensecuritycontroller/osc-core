package org.osc.core.broker.service.tasks.conformance.manager;

import java.util.ArrayList;
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

	public static ManagerPolicyElement MGR_POLICY = createManagerPolicyElement("MGR_POLICY");

	public static Policy CREATED_POLICY_FROM_MGR_POLICY = createPolicy("MGR_POLICY", true);
	public static Policy POLICY_WITH_MGR_POLICY = createPolicy("POLICY_WITH_MGR_POLICY", true);
	public static Policy POLICY_WITHOUT_MGR_POLICY = createPolicy("POLICY_WITHOUT_MGR_POLICY", false);
	public static Policy POLICY_WITH_VS_POLICY = createPolicy("POLICY_WITH_VS_POLICY", false);

	public static Domain DOMAIN_WITHOUT_POLICY = createDomain("DOMAIN_WITHOUT_POLICY", null);
	public static Domain DOMAIN_WITH_POLICY = createDomain("DOMAIN_WITH_POLICY", POLICY_WITH_MGR_POLICY);
	public static Domain DOMAIN_WITHOUT_MGR_POLICY = createDomain("DOMAIN_WITHOUT_MGR_POLICY", POLICY_WITHOUT_MGR_POLICY);
	public static Domain DOMAIN_WITH_VS_POLICY = createDomain("DOMAIN_WITH_VS_POLICY", POLICY_WITH_VS_POLICY);

	public static VirtualSystemPolicy VS_POLICY = createVSPolicy(3L, POLICY_WITH_VS_POLICY);

	public static ApplianceManagerConnector NO_DOMAIN_MC = createMC(null, null);
	public static ApplianceManagerConnector NO_MGR_POLICY_MC = createMC(DOMAIN_WITHOUT_POLICY, null);
	public static ApplianceManagerConnector MGR_POLICY_WITHOUT_POLICY_MC = createMC(DOMAIN_WITHOUT_POLICY, null);
	public static ApplianceManagerConnector MGR_POLICY_WITH_POLICY_MC = createMC(DOMAIN_WITH_POLICY, POLICY_WITH_MGR_POLICY);
	public static ApplianceManagerConnector POLICY_WITHOUT_MGR_POLICY_MC = createMC(DOMAIN_WITHOUT_MGR_POLICY, POLICY_WITHOUT_MGR_POLICY);
	public static ApplianceManagerConnector POLICY_WITH_VS_POLICY_MC = createMC(DOMAIN_WITH_VS_POLICY, POLICY_WITH_VS_POLICY);

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

	private static ManagerPolicyElement createManagerPolicyElement(String policyName) {
		ManagerPolicyElement managerPolicy = Mockito.mock(ManagerPolicyElement.class);
		Mockito.doReturn(policyName).when(managerPolicy).getName();
		Mockito.doReturn("ID").when(managerPolicy).getId();

		return managerPolicy;
	}

	private static Policy createPolicy(String policyName, Boolean mgrPolicyExists) {
		Policy policy = new Policy();
		policy.setName(policyName);
		if(mgrPolicyExists) {
		    policy.setMgrPolicyId("ID");
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

	private static VirtualSystemPolicy createVSPolicy(Long policyId, Policy policy) {
	    VirtualizationConnector vc = new VirtualizationConnector();
	    vc.setName("vc_name");
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