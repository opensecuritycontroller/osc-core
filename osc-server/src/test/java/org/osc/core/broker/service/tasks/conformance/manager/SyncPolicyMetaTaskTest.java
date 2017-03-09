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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.osc.core.broker.service.tasks.conformance.manager.SyncPolicyMetaTaskTestData.DOMAINS_WITHOUT_POLICIES_AND_WITH_ORPHAN_POLICIES_MC;
import static org.osc.core.broker.service.tasks.conformance.manager.SyncPolicyMetaTaskTestData.DOMAINS_WITH_ORPHAN_AND_OUT_OF_SYNC_POLICIES_MC;
import static org.osc.core.broker.service.tasks.conformance.manager.SyncPolicyMetaTaskTestData.DOMAIN_WITHOUT_MGR_POLICY;
import static org.osc.core.broker.service.tasks.conformance.manager.SyncPolicyMetaTaskTestData.DOMAIN_WITHOUT_POLICY;
import static org.osc.core.broker.service.tasks.conformance.manager.SyncPolicyMetaTaskTestData.DOMAIN_WITHOUT_POLICY_2;
import static org.osc.core.broker.service.tasks.conformance.manager.SyncPolicyMetaTaskTestData.DOMAIN_WITH_MULTIPLE_POLICIES_MC;
import static org.osc.core.broker.service.tasks.conformance.manager.SyncPolicyMetaTaskTestData.DOMAIN_WITH_POLICY;
import static org.osc.core.broker.service.tasks.conformance.manager.SyncPolicyMetaTaskTestData.MGR_POLICY;
import static org.osc.core.broker.service.tasks.conformance.manager.SyncPolicyMetaTaskTestData.MGR_POLICY_WITHOUT_POLICY_MC;
import static org.osc.core.broker.service.tasks.conformance.manager.SyncPolicyMetaTaskTestData.MGR_POLICY_WITH_POLICY_MC;
import static org.osc.core.broker.service.tasks.conformance.manager.SyncPolicyMetaTaskTestData.NO_DOMAIN_MC;
import static org.osc.core.broker.service.tasks.conformance.manager.SyncPolicyMetaTaskTestData.NO_MGR_POLICY_MC;
import static org.osc.core.broker.service.tasks.conformance.manager.SyncPolicyMetaTaskTestData.POLICY_WITHOUT_MGR_POLICY_MC;
import static org.osc.core.broker.service.tasks.conformance.manager.SyncPolicyMetaTaskTestData.POLICY_WITH_VS_POLICY_MC;
import static org.osc.core.broker.service.tasks.conformance.manager.SyncPolicyMetaTaskTestData.VS_POLICY;
import static org.osc.core.broker.service.tasks.conformance.manager.SyncPolicyMetaTaskTestData.VS_POLICY_1;
import static org.osc.core.broker.service.tasks.conformance.manager.SyncPolicyMetaTaskTestData.createPolicyGraph;
import static org.osc.core.broker.service.tasks.conformance.manager.SyncPolicyMetaTaskTestData.deletePoliciesFromDomainGraph;
import static org.osc.core.broker.service.tasks.conformance.manager.SyncPolicyMetaTaskTestData.deletePolicyGraph;
import static org.osc.core.broker.service.tasks.conformance.manager.SyncPolicyMetaTaskTestData.emptyGraph;
import static org.osc.core.broker.service.tasks.conformance.manager.SyncPolicyMetaTaskTestData.removeVendorTemplateAndDeletePolicyGraph;
import static org.osc.core.broker.service.tasks.conformance.manager.SyncPolicyMetaTaskTestData.updatePolicyGraph;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import javax.persistence.EntityManager;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.osc.core.broker.job.TaskGraph;
import org.osc.core.broker.model.entities.appliance.VirtualSystemPolicy;
import org.osc.core.broker.model.entities.management.ApplianceManagerConnector;
import org.osc.core.broker.model.plugin.manager.ManagerApiFactory;
import org.osc.core.broker.service.test.InMemDB;
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

    public EntityManager em;

    private ApplianceManagerConnector mc;
    private TaskGraph expectedGraph;

    public SyncPolicyMetaTaskTest(ApplianceManagerConnector mc, TaskGraph tg) {
        this.mc = mc;
        this.expectedGraph = tg;
    }

    @Before
    public void testInitialize() throws Exception {
        MockitoAnnotations.initMocks(this);

        this.em = InMemDB.getEntityManagerFactory().createEntityManager();

        populateDatabase();

        PowerMockito.mockStatic(ManagerApiFactory.class);
        registerMgrPolicies(NO_MGR_POLICY_MC, DOMAIN_WITHOUT_POLICY.getMgrId(), null);
        registerMgrPolicies(MGR_POLICY_WITHOUT_POLICY_MC, DOMAIN_WITHOUT_POLICY_2.getMgrId(), Arrays.asList(MGR_POLICY));
        registerMgrPolicies(MGR_POLICY_WITH_POLICY_MC, DOMAIN_WITH_POLICY.getMgrId(), Arrays.asList(MGR_POLICY));
        registerMgrPolicies(POLICY_WITHOUT_MGR_POLICY_MC, DOMAIN_WITHOUT_MGR_POLICY.getMgrId(), Arrays.asList());
        registerMgrPolicies(POLICY_WITH_VS_POLICY_MC, DOMAIN_WITH_POLICY.getMgrId(), Arrays.asList(MGR_POLICY));
        registerMgrPolicies(DOMAINS_WITH_ORPHAN_AND_OUT_OF_SYNC_POLICIES_MC, DOMAIN_WITH_POLICY.getMgrId(), Arrays.asList(MGR_POLICY));
        registerMgrPolicies(DOMAINS_WITHOUT_POLICIES_AND_WITH_ORPHAN_POLICIES_MC, DOMAIN_WITHOUT_POLICY.getMgrId(), null);
        registerMgrPolicies(DOMAIN_WITH_MULTIPLE_POLICIES_MC, DOMAIN_WITH_POLICY.getMgrId(), Arrays.asList(MGR_POLICY));
    }

    @After
    public void testTearDown() {
        InMemDB.shutdown();
    }

    private void populateDatabase() {
       this.em.getTransaction().begin();

       this.em.persist(this.mc);

       // These tests proably shouldn't be parameterised, but instead have
       // a proper database setup phase at the start of each.


       VirtualSystemPolicy policy = null;
       if("POLICY_WITH_VS_POLICY_MC_mc".equals(this.mc.getName())) {
           policy = VS_POLICY;
       } else if ("DOMAIN_WITH_MULTIPLE_POLICIES_MC_mc".equals(this.mc.getName())) {
           policy = VS_POLICY_1;
       }

       if(policy != null) {
           this.em.persist(policy.getVirtualSystem().getVirtualizationConnector());
           this.em.persist(policy.getVirtualSystem().getApplianceSoftwareVersion()
                   .getAppliance());
           this.em.persist(policy.getVirtualSystem().getApplianceSoftwareVersion());
           this.em.persist(policy.getVirtualSystem().getDistributedAppliance());
           this.em.persist(policy.getVirtualSystem());
           this.em.persist(policy);
       }


       this.em.getTransaction().commit();
    }

    @Test
    public void testExecuteTransasction_WithVariousManagerConnectors_ExpectsCorrectTaskGraph() throws Exception {
        //Arrange.
        SyncPolicyMetaTask task = new SyncPolicyMetaTask(this.mc);

        //Act.
        task.executeTransaction(this.em);

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
            {POLICY_WITH_VS_POLICY_MC, removeVendorTemplateAndDeletePolicyGraph(POLICY_WITH_VS_POLICY_MC)},
            // TODO hailee: Looks like the test below is still failing inconsistently.
            //{DOMAINS_WITH_ORPHAN_AND_OUT_OF_SYNC_POLICIES_MC,
            //   deleteOrphanAndVSPoliciesGraph(DOMAINS_WITH_ORPHAN_AND_OUT_OF_SYNC_POLICIES_MC)},
            //{DOMAINS_WITHOUT_POLICIES_AND_WITH_ORPHAN_POLICIES_MC,
            //  deletePoliciesWithoutMgrPoliciesGraph(DOMAINS_WITHOUT_POLICIES_AND_WITH_ORPHAN_POLICIES_MC)},
            {DOMAIN_WITH_MULTIPLE_POLICIES_MC, deletePoliciesFromDomainGraph(DOMAIN_WITH_MULTIPLE_POLICIES_MC)}
        });
    }

    private void registerMgrPolicies(ApplianceManagerConnector mc, String policyMgrId, List<ManagerPolicyElement> returnValue)
            throws Exception {
        ManagerPolicyApi mgrPolicyApi = mock(ManagerPolicyApi.class);
        Mockito.<List<? extends ManagerPolicyElement>>when(mgrPolicyApi.getPolicyList(policyMgrId)).thenReturn(returnValue);

        when(ManagerApiFactory.createManagerPolicyApi(mc)).thenReturn(mgrPolicyApi);
    }
}