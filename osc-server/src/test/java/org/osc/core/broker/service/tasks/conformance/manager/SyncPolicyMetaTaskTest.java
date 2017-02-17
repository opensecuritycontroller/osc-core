package org.osc.core.broker.service.tasks.conformance.manager;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.osc.core.broker.service.tasks.conformance.manager.SyncPolicyMetaTaskTestData.*;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.hibernate.Session;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.osc.core.broker.job.TaskGraph;
import org.osc.core.broker.model.entities.management.ApplianceManagerConnector;
import org.osc.core.broker.model.plugin.manager.ManagerApiFactory;
import org.osc.core.broker.util.SessionStub;
import org.osc.core.test.util.TaskGraphHelper;
import org.osc.sdk.manager.api.ManagerPolicyApi;
import org.osc.sdk.manager.element.ManagerPolicyElement;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.modules.junit4.PowerMockRunnerDelegate;

@RunWith(PowerMockRunner.class)
@PowerMockRunnerDelegate(value = Parameterized.class)
@PrepareForTest({ ManagerApiFactory.class })
public class SyncPolicyMetaTaskTest {
    @Mock
    public Session sessionMock;

    private ApplianceManagerConnector mc;
    private TaskGraph expectedGraph;
    private SessionStub sessionStub;

    public SyncPolicyMetaTaskTest(ApplianceManagerConnector mc, TaskGraph tg) {
        this.mc = mc;
        this.expectedGraph = tg;
    }

    @Before
    public void testInitialize() throws Exception {
        MockitoAnnotations.initMocks(this);

        for (ApplianceManagerConnector mc : TEST_MCS) {
        	Mockito.doReturn(this.mc).when(this.sessionMock).get(ApplianceManagerConnector.class, mc.getId());
        }

        this.sessionStub = new SessionStub(this.sessionMock);
        this.sessionStub.stubListVSPolicyByPolicyID(POLICY_WITHOUT_MGR_POLICY.getId(), null);
        this.sessionStub.stubListVSPolicyByPolicyID(POLICY_WITH_VS_POLICY.getId(), Arrays.asList(VS_POLICY));

        PowerMockito.mockStatic(ManagerApiFactory.class);
        registerMgrPolicies(NO_MGR_POLICY_MC, DOMAIN_WITHOUT_POLICY.getMgrId(), null);
        registerMgrPolicies(MGR_POLICY_WITHOUT_POLICY_MC, DOMAIN_WITHOUT_POLICY.getMgrId(), Arrays.asList(MGR_POLICY));
        registerMgrPolicies(MGR_POLICY_WITH_POLICY_MC, DOMAIN_WITH_POLICY.getMgrId(), Arrays.asList(MGR_POLICY));
        registerMgrPolicies(POLICY_WITHOUT_MGR_POLICY_MC, DOMAIN_WITH_POLICY.getMgrId(), Arrays.asList(MGR_POLICY));
        registerMgrPolicies(POLICY_WITH_VS_POLICY_MC, DOMAIN_WITH_POLICY.getMgrId(), Arrays.asList(MGR_POLICY));
    }

    @Test
    public void testExecuteTransasction_WithVariousManagerConnectors_ExpectsCorrectTaskGraph() throws Exception {
    	//Arrange.
    	SyncPolicyMetaTask task = new SyncPolicyMetaTask(this.mc);

    	//Act.
    	task.executeTransaction(this.sessionMock);

    	//Assert.
    	TaskGraphHelper.validateTaskGraph(task, this.expectedGraph);
    }

    @Parameters()
    public static Collection<Object[]> getTestData() {
        return Arrays.asList(new Object[][] {
            {NO_DOMAIN_MC, emptyGraph(NO_DOMAIN_MC)},
            {NO_MGR_POLICY_MC, emptyGraph(NO_MGR_POLICY_MC)},
            {MGR_POLICY_WITHOUT_POLICY_MC, createPolicyGraph(MGR_POLICY_WITHOUT_POLICY_MC)},
            {MGR_POLICY_WITH_POLICY_MC, updatePolicyGraph(MGR_POLICY_WITH_POLICY_MC)},
            {POLICY_WITHOUT_MGR_POLICY_MC, deletePolicyGraph(POLICY_WITHOUT_MGR_POLICY_MC)},
            {POLICY_WITH_VS_POLICY_MC, removeVendorTemplateAndDeletePolicyGraph(POLICY_WITH_VS_POLICY_MC)}
        });
    }

    private void registerMgrPolicies(ApplianceManagerConnector mc, String policyMgrId, List<ManagerPolicyElement> returnValue)
            throws Exception {
        ManagerPolicyApi mgrPolicyApi = mock(ManagerPolicyApi.class);
        Mockito.<List<? extends ManagerPolicyElement>> when(mgrPolicyApi.getPolicyList(policyMgrId)).thenReturn(returnValue);

        when(ManagerApiFactory.createManagerPolicyApi(mc)).thenReturn(mgrPolicyApi);
    }
}