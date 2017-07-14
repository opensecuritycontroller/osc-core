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

import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.Collection;

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
import org.osc.core.broker.model.entities.appliance.VirtualSystem;
import org.osc.core.broker.model.entities.virtualization.SecurityGroup;
import org.osc.core.broker.model.entities.virtualization.SecurityGroupInterface;
import org.osc.core.broker.model.plugin.ApiFactoryService;
import org.osc.core.broker.service.tasks.conformance.securitygroupinterface.MgrSecurityGroupInterfacesCheckMetaTask;
import org.osc.core.broker.service.test.InMemDB;
import org.osc.core.test.util.TaskGraphHelper;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.modules.junit4.PowerMockRunnerDelegate;

@RunWith(PowerMockRunner.class)
@PowerMockRunnerDelegate(value = Parameterized.class)
public class SecurityGroupCheckMetaTaskTest {

    public EntityManager em;

    private SecurityGroup sg;

    private TaskGraph expectedGraph;

    private ApiFactoryService apiFactoryService = Mockito.mock(ApiFactoryService.class);

    // This must be an instance because otherwise the entities
    // it creates retain state between tests (which is bad!)
    private SecurityGroupCheckMetaTaskTestData testData;

    public SecurityGroupCheckMetaTaskTest(SecurityGroupCheckMetaTaskTestData sgcmttd, SecurityGroup sg, TaskGraph tg) {
        this.testData = sgcmttd;
        this.sg = sg;
        this.expectedGraph = tg;
    }

    @Before
    public void testInitialize() throws Exception{
        MockitoAnnotations.initMocks(this);

        this.em = InMemDB.getEntityManagerFactory().createEntityManager();

        populateDatabase();

        when(this.apiFactoryService.syncsPolicyMapping(this.testData.MC_POLICY_MAPPING_NOT_SUPPORTED_VS)).thenReturn(false);

        for (VirtualSystem vs: this.testData.MC_POLICY_MAPPING_SUPPORTED_VS_LIST) {
            when(this.apiFactoryService.syncsPolicyMapping(vs)).thenReturn(true);
        }
    }

    @After
    public void testTearDown() {
        InMemDB.shutdown();
    }

    private void populateDatabase() {
       this.em.getTransaction().begin();

       for (SecurityGroup sg: this.testData.TEST_SECURITY_GROUPS) {
           for(SecurityGroupInterface sgi : sg.getSecurityGroupInterfaces()) {
               this.em.persist(sgi.getVirtualSystem()
                       .getVirtualizationConnector());
               this.em.persist(sgi.getVirtualSystem()
                       .getDistributedAppliance().getApplianceManagerConnector());
               this.em.persist(sgi.getVirtualSystem()
                       .getDistributedAppliance().getAppliance());
               this.em.persist(sgi.getVirtualSystem().getDistributedAppliance());
               this.em.persist(sgi.getVirtualSystem().getApplianceSoftwareVersion());
               this.em.persist(sgi.getVirtualSystem().getDomain());
               this.em.persist(sgi.getPolicy());
               this.em.persist(sgi.getVirtualSystem());
               this.em.persist(sgi);
           }
           this.em.persist(sg);
       }

       this.em.getTransaction().commit();
    }

    @Test
    public void testExecuteTransaction_WithVariousDeploymentSpecs_ExpectsCorrectTaskGraph() throws Exception {
        // Arrange.
        SecurityGroupCheckMetaTask task = new SecurityGroupCheckMetaTask();
        task.mgrSecurityGroupInterfacesCheckMetaTask = new MgrSecurityGroupInterfacesCheckMetaTask();
        task.securityGroupUpdateOrDeleteMetaTask = new SecurityGroupUpdateOrDeleteMetaTask();
        task.validateSecurityGroupProjectTask = new ValidateSecurityGroupProjectTask();

        task = task.create(this.sg);
        task.mgrSecurityGroupInterfacesCheckMetaTask = new MgrSecurityGroupInterfacesCheckMetaTask();
        task.apiFactoryService = this.apiFactoryService;

        // Act.
        task.executeTransaction(this.em);

        // Assert.
        TaskGraphHelper.validateTaskGraph(task, this.expectedGraph);
    }

    @Parameters()
    public static Collection<Object[]> getTestData() {

        // These must be separate instances because otherwise the
        // entities retain state between tests (which is bad!)
        SecurityGroupCheckMetaTaskTestData sgcmttd1 = new SecurityGroupCheckMetaTaskTestData();
        SecurityGroupCheckMetaTaskTestData sgcmttd2 = new SecurityGroupCheckMetaTaskTestData();
        SecurityGroupCheckMetaTaskTestData sgcmttd3 = new SecurityGroupCheckMetaTaskTestData();

        return Arrays.asList(new Object[][] {
            {sgcmttd1, sgcmttd1.NO_MC_POLICY_MAPPING_SUPPORTED_SG, sgcmttd1.createNoMcPolicyMappingGraph(sgcmttd1.NO_MC_POLICY_MAPPING_SUPPORTED_SG)},
            {sgcmttd2, sgcmttd2.SINGLE_MC_POLICY_MAPPING_SUPPORTED_SG, sgcmttd2.createSingleMcPolicyMappingGraph(sgcmttd2.SINGLE_MC_POLICY_MAPPING_SUPPORTED_SG)},
            {sgcmttd3, sgcmttd3.MULTIPLE_MC_POLICY_MAPPING_SUPPORTED_SG, sgcmttd3.createMultipleMcPolicyMappingGraph(sgcmttd3.MULTIPLE_MC_POLICY_MAPPING_SUPPORTED_SG)}
        });
    }
}
