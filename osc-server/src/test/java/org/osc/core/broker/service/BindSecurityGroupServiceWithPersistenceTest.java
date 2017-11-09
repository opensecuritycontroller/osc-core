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
package org.osc.core.broker.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.persistence.EntityManager;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.osc.core.broker.job.Job;
import org.osc.core.broker.job.JobEngine;
import org.osc.core.broker.job.TaskGraph;
import org.osc.core.broker.model.entities.appliance.Appliance;
import org.osc.core.broker.model.entities.appliance.ApplianceSoftwareVersion;
import org.osc.core.broker.model.entities.appliance.DistributedAppliance;
import org.osc.core.broker.model.entities.appliance.VirtualSystem;
import org.osc.core.broker.model.entities.management.ApplianceManagerConnector;
import org.osc.core.broker.model.entities.management.Domain;
import org.osc.core.broker.model.entities.management.Policy;
import org.osc.core.broker.model.entities.virtualization.FailurePolicyType;
import org.osc.core.broker.model.entities.virtualization.SecurityGroup;
import org.osc.core.broker.model.entities.virtualization.SecurityGroupInterface;
import org.osc.core.broker.model.entities.virtualization.ServiceFunctionChain;
import org.osc.core.broker.model.entities.virtualization.VirtualizationConnector;
import org.osc.core.broker.model.plugin.ApiFactoryService;
import org.osc.core.broker.service.api.server.UserContextApi;
import org.osc.core.broker.service.dto.PolicyDto;
import org.osc.core.broker.service.dto.VirtualSystemPolicyBindingDto;
import org.osc.core.broker.service.exceptions.VmidcBrokerValidationException;
import org.osc.core.broker.service.request.BindSecurityGroupRequest;
import org.osc.core.broker.service.response.BaseJobResponse;
import org.osc.core.broker.service.securitygroup.BindSecurityGroupService;
import org.osc.core.broker.service.tasks.conformance.UnlockObjectMetaTask;
import org.osc.core.broker.service.test.InMemDB;
import org.osc.core.broker.util.TransactionalBroadcastUtil;
import org.osc.core.broker.util.db.DBConnectionManager;
import org.osc.core.common.virtualization.VirtualizationType;
import org.osc.core.test.util.TestTransactionControl;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest(LockUtil.class)
public class BindSecurityGroupServiceWithPersistenceTest {

    private static final String DEFAULT_NAME = "test-sg-name";
    private static final Long VALID_ID = 1L;

    @Rule
    public ExpectedException exception = ExpectedException.none();

    public EntityManager em;

    @Mock(answer = Answers.CALLS_REAL_METHODS)
    protected TestTransactionControl txControl;

    @Mock
    protected TransactionalBroadcastUtil txBroadcastUtil;

    @Mock
    private SecurityGroupConformJobFactory sgConformJobFactory;

    @Mock
    protected UserContextApi userContext;

    @Mock
    protected DBConnectionManager dbMgr;

    @InjectMocks
    private BindSecurityGroupService bindSecurityGroupService;

    protected VirtualizationConnector vc;
    protected VirtualSystem vs;
    protected VirtualSystem vs1;
    protected ServiceFunctionChain sfc;
    protected ServiceFunctionChain sfc_dup;
    protected SecurityGroup sg;
    protected SecurityGroup sg_dup;
    protected SecurityGroupInterface sgi_1;
    protected Long job_id;
    private UnlockObjectMetaTask ult;

    @Before
    public void testInitialize() throws Exception {

        MockitoAnnotations.initMocks(this);

        this.em = InMemDB.getEntityManagerFactory().createEntityManager();

        this.txControl.setEntityManager(this.em);

        ApiFactoryService apiFactoryService = Mockito.mock(ApiFactoryService.class);
        this.bindSecurityGroupService.apiFactoryService = apiFactoryService;

        populateDatabase();

        Mockito.when(this.dbMgr.getTransactionalEntityManager()).thenReturn(this.em);
        Mockito.when(this.dbMgr.getTransactionControl()).thenReturn(this.txControl);

        JobEngine jobEngine = JobEngine.getEngine();
        Job job = jobEngine.submit("testJob", new TaskGraph(), false);
        this.job_id = job.getId();

        Mockito.when(this.sgConformJobFactory.startBindSecurityGroupConformanceJob(Mockito.any(EntityManager.class),
                Mockito.any(), Mockito.any(UnlockObjectMetaTask.class))).thenReturn(job);

        PowerMockito.mockStatic(LockUtil.class);
        Mockito.when(LockUtil.tryLockSecurityGroup(this.sg, this.sg.getVirtualizationConnector())).thenReturn(this.ult);
    }

    @After
    public void testTearDown() {
        InMemDB.shutdown();
    }

    private void populateDatabase() {
        this.em.getTransaction().begin();

        Appliance app = new Appliance();
        app.setManagerSoftwareVersion("fizz");
        app.setManagerType("buzz");
        app.setModel("fizzbuzz");
        this.em.persist(app);

        Appliance app1 = new Appliance();
        app1.setManagerSoftwareVersion("fizz1");
        app1.setManagerType("buzz1");
        app1.setModel("fizzbuzz1");
        this.em.persist(app1);

        ApplianceManagerConnector amc = new ApplianceManagerConnector();
        amc.setManagerType("buzz");
        amc.setIpAddress("127.0.0.1");
        amc.setName("Steve");
        amc.setServiceType("foobar");
        this.em.persist(amc);

        Domain domain = new Domain(amc);
        domain.setName("domainName");
        this.em.persist(domain);

        this.vc = new VirtualizationConnector();
        this.vc.setVirtualizationType(VirtualizationType.OPENSTACK);
        this.vc.setVirtualizationSoftwareVersion("vcSoftwareVersion");
        this.vc.setName("vcName");
        this.vc.setProviderIpAddress("127.0.0.1");
        this.vc.setProviderUsername("Natasha");
        this.vc.setProviderPassword("********");
        this.vc.setControllerType("Neutron-sfc");
        this.em.persist(this.vc);

        ApplianceSoftwareVersion asv = new ApplianceSoftwareVersion(app);
        asv.setApplianceSoftwareVersion("softwareVersion");
        asv.setImageUrl("bar");
        asv.setVirtualizarionSoftwareVersion(this.vc.getVirtualizationSoftwareVersion());
        asv.setVirtualizationType(this.vc.getVirtualizationType());
        this.em.persist(asv);

        ApplianceSoftwareVersion asv1 = new ApplianceSoftwareVersion(app);
        asv1.setApplianceSoftwareVersion("softwareVersion1");
        asv1.setImageUrl("bar1");
        asv1.setVirtualizarionSoftwareVersion(this.vc.getVirtualizationSoftwareVersion());
        asv1.setVirtualizationType(this.vc.getVirtualizationType());
        this.em.persist(asv1);

        DistributedAppliance da = new DistributedAppliance(amc);
        da.setName("daName");
        da.setApplianceVersion(asv.getApplianceSoftwareVersion());
        da.setAppliance(app);
        this.em.persist(da);

        this.vs = new VirtualSystem(da);
        this.vs.setApplianceSoftwareVersion(asv);
        this.vs.setDomain(domain);
        this.vs.setVirtualizationConnector(this.vc);
        this.vs.setMarkedForDeletion(false);
        this.vs.setName("vsName");
        this.em.persist(this.vs);
        da.addVirtualSystem(this.vs);

        DistributedAppliance da1 = new DistributedAppliance(amc);
        da1.setName("daName1");
        da1.setApplianceVersion(asv.getApplianceSoftwareVersion());
        da1.setAppliance(app);
        this.em.persist(da1);

        this.vs1 = new VirtualSystem(da1);
        this.vs1.setApplianceSoftwareVersion(asv1);
        this.vs1.setDomain(domain);
        this.vs1.setVirtualizationConnector(this.vc);
        this.vs1.setMarkedForDeletion(false);
        this.vs1.setName("vsName1");
        this.em.persist(this.vs1);
        da1.addVirtualSystem(this.vs1);

        this.sfc = new ServiceFunctionChain("sfc-exist", this.vc);
        this.em.persist(this.sfc);

        this.sg = new SecurityGroup(this.vc, "111", "Demopp");
        this.sg.setName(DEFAULT_NAME + "PPP");
        this.sg.setMarkedForDeletion(false);
        this.em.persist(this.sg);

        Set<Policy> policies = new HashSet<Policy>();
        Policy policy = new Policy(amc, domain);
        policy.setId(VALID_ID);
        policy.setName("pol-name");
        policy.setMgrPolicyId("5");
        this.em.persist(policy);
        policies.add(policy);

        this.sgi_1 = new SecurityGroupInterface(this.vs, policies, "111", FailurePolicyType.NA, 0L);
        this.sgi_1.setName(DEFAULT_NAME);
        this.sgi_1.setSecurityGroup(this.sg);
        this.em.persist(this.sgi_1);

        this.sfc_dup = new ServiceFunctionChain("sfc-dup", this.vc);
        this.sfc_dup.addVirtualSystem(this.vs1);
        this.em.persist(this.sfc_dup);

        this.sg_dup = new SecurityGroup(this.vc, "111", "Demopp");
        this.sg_dup.setName(DEFAULT_NAME + "PPP-dup");
        this.sg_dup.setMarkedForDeletion(false);
        this.sg_dup.setServiceFunctionChain(this.sfc_dup);
        this.em.persist(this.sg_dup);

        SecurityGroupInterface sgi_2 = new SecurityGroupInterface(this.vs1, policies, "111", FailurePolicyType.NA, 0L);
        sgi_2.setName(DEFAULT_NAME + 1);
        sgi_2.setSecurityGroup(this.sg_dup);
        this.em.persist(sgi_2);

        this.em.getTransaction().commit();
    }

    @Test
    public void testValidate_WhenSupportsFailurePolicyAndFailurePolicyTypeIsNull_ThrowsValidationException()
            throws Exception {
        // Arrange
        BindSecurityGroupRequest request = new BindSecurityGroupRequest();
        request.setVcId(this.vc.getId());
        request.setSecurityGroupId(this.sg.getId());

        VirtualSystemPolicyBindingDto serviceToBindTo = createService(VALID_ID, this.vs);
        serviceToBindTo.setOrder(0);
        request.addServiceToBindTo(serviceToBindTo);

        Mockito.when(this.bindSecurityGroupService.apiFactoryService.supportsServiceFunctionChaining(this.sg))
                .thenReturn(true);
        Mockito.when(this.bindSecurityGroupService.apiFactoryService.supportsMultiplePolicies(this.vs))
                .thenReturn(true);
        Mockito.when(this.bindSecurityGroupService.apiFactoryService.syncsPolicyMapping(this.vs)).thenReturn(true);
        Mockito.when(this.bindSecurityGroupService.apiFactoryService.supportsFailurePolicy(this.sg)).thenReturn(true);

        this.exception.expect(VmidcBrokerValidationException.class);
        this.exception.expectMessage("Failure Policy should not have an empty value.");

        // Act
        this.bindSecurityGroupService.dispatch(request);
    }

    @Test
    public void testValidate_WhenSupportsFailurePolicyAndFailurePolicyTypeIsNA_ThrowsValidationException()
            throws Exception {
        // Arrange
        BindSecurityGroupRequest request = new BindSecurityGroupRequest();
        request.setVcId(this.vc.getId());
        request.setSecurityGroupId(this.sg.getId());

        VirtualSystemPolicyBindingDto serviceToBindTo = createService(VALID_ID, this.vs);
        serviceToBindTo.setFailurePolicyType(org.osc.sdk.controller.FailurePolicyType.NA);
        serviceToBindTo.setOrder(0);
        request.addServiceToBindTo(serviceToBindTo);

        Mockito.when(this.bindSecurityGroupService.apiFactoryService.supportsServiceFunctionChaining(this.sg))
                .thenReturn(true);
        Mockito.when(this.bindSecurityGroupService.apiFactoryService.supportsMultiplePolicies(this.vs))
                .thenReturn(true);
        Mockito.when(this.bindSecurityGroupService.apiFactoryService.syncsPolicyMapping(this.vs)).thenReturn(true);
        Mockito.when(this.bindSecurityGroupService.apiFactoryService.supportsFailurePolicy(this.sg)).thenReturn(true);

        this.exception.expect(VmidcBrokerValidationException.class);
        this.exception.expectMessage("Failure Policy should not have an empty value.");

        // Act
        this.bindSecurityGroupService.dispatch(request);
    }

    @Test
    public void testValidate_WhenFailurePolicyNotSupportedAndFailurePolicyTypeIsFAIL_CLOSE_ThrowsValidationException()
            throws Exception {
        // Arrange
        BindSecurityGroupRequest request = new BindSecurityGroupRequest();
        request.setVcId(this.vc.getId());
        request.setSecurityGroupId(this.sg.getId());

        VirtualSystemPolicyBindingDto serviceToBindTo = createService(VALID_ID, this.vs);
        serviceToBindTo.setFailurePolicyType(org.osc.sdk.controller.FailurePolicyType.FAIL_CLOSE);
        serviceToBindTo.setOrder(0);
        request.addServiceToBindTo(serviceToBindTo);

        Mockito.when(this.bindSecurityGroupService.apiFactoryService.supportsServiceFunctionChaining(this.sg))
                .thenReturn(true);
        Mockito.when(this.bindSecurityGroupService.apiFactoryService.supportsMultiplePolicies(this.vs))
                .thenReturn(true);
        Mockito.when(this.bindSecurityGroupService.apiFactoryService.syncsPolicyMapping(this.vs)).thenReturn(true);

        this.exception.expect(VmidcBrokerValidationException.class);
        this.exception.expectMessage(
                "SDN Controller Plugin of type '" + this.sg.getVirtualizationConnector().getControllerType()
                        + "' does not support Failure Policy. Only valid values are null or NA");

        // Act
        this.bindSecurityGroupService.dispatch(request);
    }

    @Test
    public void testValidate_WhenFailurePolicyNotSupportedAndFailurePolicyTypeIsFAIL_OPEN_ThrowsValidationException()
            throws Exception {
        // Arrange
        BindSecurityGroupRequest request = new BindSecurityGroupRequest();
        request.setVcId(this.vc.getId());
        request.setSecurityGroupId(this.sg.getId());

        VirtualSystemPolicyBindingDto serviceToBindTo = createService(VALID_ID, this.vs);
        serviceToBindTo.setFailurePolicyType(org.osc.sdk.controller.FailurePolicyType.FAIL_OPEN);
        serviceToBindTo.setOrder(0);
        request.addServiceToBindTo(serviceToBindTo);

        Mockito.when(this.bindSecurityGroupService.apiFactoryService.supportsServiceFunctionChaining(this.sg))
                .thenReturn(true);
        Mockito.when(this.bindSecurityGroupService.apiFactoryService.supportsMultiplePolicies(this.vs))
                .thenReturn(true);
        Mockito.when(this.bindSecurityGroupService.apiFactoryService.syncsPolicyMapping(this.vs)).thenReturn(true);

        this.exception.expect(VmidcBrokerValidationException.class);
        this.exception.expectMessage(
                "SDN Controller Plugin of type '" + this.sg.getVirtualizationConnector().getControllerType()
                        + "' does not support Failure Policy. Only valid values are null or NA");

        // Act
        this.bindSecurityGroupService.dispatch(request);
    }

    @Test
    public void testValidate_WhenBindSecurityGroupService_UpdateSuccessful() throws Exception {
        // Arrange
        BindSecurityGroupRequest request = new BindSecurityGroupRequest();
        request.setVcId(this.vc.getId());
        request.setSecurityGroupId(this.sg.getId());

        VirtualSystemPolicyBindingDto serviceToBindTo = createService(VALID_ID, this.vs);
        serviceToBindTo.setOrder(0);
        request.addServiceToBindTo(serviceToBindTo);

        Mockito.when(this.bindSecurityGroupService.apiFactoryService.supportsServiceFunctionChaining(this.sg))
                .thenReturn(true);
        Mockito.when(this.bindSecurityGroupService.apiFactoryService.supportsMultiplePolicies(this.vs))
                .thenReturn(true);
        Mockito.when(this.bindSecurityGroupService.apiFactoryService.syncsPolicyMapping(this.vs)).thenReturn(true);

        // Act
        BaseJobResponse response = this.bindSecurityGroupService.dispatch(request);

        // Assert
        Assert.assertNotNull("The returned response should not be null.", response);
        Assert.assertEquals("The job id was different than expected.", this.job_id, response.getJobId());
    }

    @Test
    public void testValidate_WhenBindSecurityGroupServiceWithSfc_UpdateSuccessful() throws Exception {
        // Arrange
        BindSecurityGroupRequest request = new BindSecurityGroupRequest();
        request.setVcId(this.vc.getId());
        request.setSecurityGroupId(this.sg.getId());
        request.setSfcId(this.sfc.getId());
        request.setBindSfc(true);

        this.sfc.addVirtualSystem(this.vs);

        VirtualSystemPolicyBindingDto serviceToBindTo = createService(VALID_ID, this.vs);
        serviceToBindTo.setOrder(0);
        request.addServiceToBindTo(serviceToBindTo);

        Mockito.when(this.bindSecurityGroupService.apiFactoryService.supportsServiceFunctionChaining(this.sg))
                .thenReturn(true);
        Mockito.when(this.bindSecurityGroupService.apiFactoryService.supportsMultiplePolicies(this.vs))
                .thenReturn(true);
        Mockito.when(this.bindSecurityGroupService.apiFactoryService.syncsPolicyMapping(this.vs)).thenReturn(true);
        Mockito.when(this.bindSecurityGroupService.apiFactoryService.supportsNeutronSFC(this.sg)).thenReturn(true);

        // Act
        BaseJobResponse response = this.bindSecurityGroupService.dispatch(request);

        // Assert
        Assert.assertNotNull("The returned response should not be null.", response);
        Assert.assertEquals("The job id was different than expected.", this.job_id, response.getJobId());
    }

    @Test
    public void testValidate_WhenSfcsAreRedundantAndOneSfcIsBindedThenBindingSecondSfc_ThrowValidationException()
            throws Exception {
        // Arrange
        BindSecurityGroupRequest request = new BindSecurityGroupRequest();
        request.setVcId(this.vc.getId());
        request.setSecurityGroupId(this.sg.getId());
        request.setSfcId(this.sfc.getId());
        request.setBindSfc(true);

        this.sfc.addVirtualSystem(this.vs1);

        VirtualSystemPolicyBindingDto serviceToBindTo = createService(VALID_ID, this.vs1);
        serviceToBindTo.setOrder(0);
        request.addServiceToBindTo(serviceToBindTo);

        Mockito.when(this.bindSecurityGroupService.apiFactoryService.supportsNeutronSFC(this.sg)).thenReturn(true);

        this.exception.expect(VmidcBrokerValidationException.class);
        this.exception.expectMessage(
                "Service with VirtualSystem " + this.vs1.getId() + " is already chained to ServiceFunctionChain Id "
                        + this.sfc_dup.getId() + " and binded to SecurityGroup : " + this.sg.getName());

        // Act
        this.bindSecurityGroupService.dispatch(request);
    }

    @Test
    public void testValidate_WhenSfcVsCountDoNotMatchWithBindSeviceVsCount_ThrowsValidationException()
            throws Exception {
        // Arrange
        BindSecurityGroupRequest request = new BindSecurityGroupRequest();
        request.setVcId(this.vc.getId());
        request.setSecurityGroupId(this.sg_dup.getId());
        request.setSfcId(this.sfc.getId());
        request.setBindSfc(true);

        this.sfc.addVirtualSystem(this.vs1);
        this.sfc.addVirtualSystem(this.vs);

        VirtualSystemPolicyBindingDto serviceToBindTo = createService(VALID_ID, this.vs1);
        serviceToBindTo.setOrder(0);
        request.addServiceToBindTo(serviceToBindTo);

        Mockito.when(this.bindSecurityGroupService.apiFactoryService.supportsNeutronSFC(this.sg_dup)).thenReturn(true);

        this.exception.expect(VmidcBrokerValidationException.class);
        this.exception.expectMessage(String.format(
                "Number of Virtual Systems in Service Function Chain(%s:%s)"
                        + "and Binding request Virtual Systems:%s do not match",
                this.sfc.getName(), this.sfc.getVirtualSystems().size(), request.getServicesToBindTo().size()));

        // Act
        this.bindSecurityGroupService.dispatch(request);
    }

    @Test
    public void testValidate_WhenBindSGServiceWithSfcWhichIsAlreadyBindToAnotheSG_UpdateSuccessful() throws Exception {
        // Arrange
        BindSecurityGroupRequest request = new BindSecurityGroupRequest();
        request.setVcId(this.vc.getId());
        request.setSecurityGroupId(this.sg.getId());
        request.setSfcId(this.sfc_dup.getId());
        request.setBindSfc(true);

        VirtualSystemPolicyBindingDto serviceToBindTo = createService(VALID_ID, this.vs1);
        serviceToBindTo.setOrder(0);
        request.addServiceToBindTo(serviceToBindTo);

        Mockito.when(this.bindSecurityGroupService.apiFactoryService.supportsServiceFunctionChaining(this.sg))
                .thenReturn(true);
        Mockito.when(this.bindSecurityGroupService.apiFactoryService.supportsMultiplePolicies(this.vs1))
                .thenReturn(true);
        Mockito.when(this.bindSecurityGroupService.apiFactoryService.syncsPolicyMapping(this.vs1)).thenReturn(true);
        Mockito.when(this.bindSecurityGroupService.apiFactoryService.supportsNeutronSFC(this.sg)).thenReturn(true);

        // Act
        BaseJobResponse response = this.bindSecurityGroupService.dispatch(request);

        // Assert
        Assert.assertNotNull("The returned response should not be null.", response);
        Assert.assertEquals("The job id was different than expected.", this.job_id, response.getJobId());
    }

    private static VirtualSystemPolicyBindingDto createService(Long id, VirtualSystem vs) {
        Set<Long> policyIds = new HashSet<Long>();
        policyIds.add(id);
        PolicyDto policyDto = new PolicyDto();
        policyDto.setId(id);
        policyDto.setPolicyName("pol-name");
        List<PolicyDto> policies = new ArrayList<PolicyDto>();
        policies.add(policyDto);

        VirtualSystemPolicyBindingDto serviceDto = new VirtualSystemPolicyBindingDto(vs.getId(), vs.getName(),
                policyIds, policies);
        return serviceDto;

    }
}
