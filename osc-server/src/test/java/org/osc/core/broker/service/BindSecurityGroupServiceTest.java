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
import javax.persistence.EntityTransaction;

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
import org.mockito.runners.MockitoJUnitRunner;
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
import org.osc.core.broker.service.exceptions.VmidcBrokerInvalidEntryException;
import org.osc.core.broker.service.exceptions.VmidcBrokerInvalidRequestException;
import org.osc.core.broker.service.exceptions.VmidcBrokerValidationException;
import org.osc.core.broker.service.request.BindSecurityGroupRequest;
import org.osc.core.broker.service.securitygroup.BindSecurityGroupService;
import org.osc.core.broker.util.TransactionalBroadcastUtil;
import org.osc.core.broker.util.db.DBConnectionManager;
import org.osc.core.test.util.TestTransactionControl;

@RunWith(MockitoJUnitRunner.class)
public class BindSecurityGroupServiceTest {
    private static final String DEFAULT_SG_NAME = "sg-name";
    private static final String DEFAULT_CONTROLLER_TYPE = "NSC";
    private static final Long VALID_ID = 1L;
    private static final Long INVALID_ID = 2L;
    private static final Long VALID_ID_MARK_FOR_DELETION = 3L;

    private static final SecurityGroup VALID_SG = createSecurityGroup(VALID_ID, false);
    private static final SecurityGroup VALID_SG_MARK_FOR_DELETION = createSecurityGroup(VALID_ID_MARK_FOR_DELETION,
            true);
    private static final SecurityGroupInterface VALID_SGI = createSecurityGroupInterface(VALID_ID);
    private static final SecurityGroupInterface VALID_SGI2 = createSecurityGroupInterface(VALID_ID + 1);

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Mock
    EntityManager em;

    @Mock
    EntityTransaction tx;

    @Mock(answer = Answers.CALLS_REAL_METHODS)
    TestTransactionControl txControl;

    @Mock
    private UserContextApi userContext;

    @Mock
    private DBConnectionManager dbMgr;

    @Mock
    private SecurityGroupConformJobFactory sgConformJobFactory;

    @Mock
    private TransactionalBroadcastUtil txBroadcastUtil;

    @InjectMocks
    private BindSecurityGroupService bindSecurityGroupService;

    @Before
    public void testInitialize() throws Exception {

        MockitoAnnotations.initMocks(this);
        ApiFactoryService apiFactoryService = Mockito.mock(ApiFactoryService.class);
        this.bindSecurityGroupService.apiFactoryService = apiFactoryService;

        Mockito.when(this.em.getTransaction()).thenReturn(this.tx);
        this.txControl.setEntityManager(this.em);

        Mockito.when(this.dbMgr.getTransactionalEntityManager()).thenReturn(this.em);
        Mockito.when(this.dbMgr.getTransactionControl()).thenReturn(this.txControl);

        Mockito.when(this.em.find(Mockito.eq(SecurityGroup.class), Mockito.eq(VALID_ID))).thenReturn(VALID_SG);
        Mockito.when(this.em.find(Mockito.eq(SecurityGroup.class), Mockito.eq(VALID_ID_MARK_FOR_DELETION)))
                .thenReturn(VALID_SG_MARK_FOR_DELETION);
        Mockito.when(this.em.find(Mockito.eq(SecurityGroupInterface.class), Mockito.eq(VALID_ID)))
                .thenReturn(VALID_SGI);
        Mockito.when(this.em.find(Mockito.eq(SecurityGroupInterface.class), Mockito.eq(VALID_ID + 1)))
                .thenReturn(VALID_SGI2);

    }

    @Test
    public void testValidate_WhenVcIdNull_ThrowsInvalidEntryException() throws Exception {
        // Arrange
        String field = "Virtualization Connector Id";
        BindSecurityGroupRequest request = createRequest(null, VALID_ID, VALID_ID);
        this.exception.expect(VmidcBrokerInvalidEntryException.class);
        this.exception.expectMessage(field + " should not have an empty value.");

        // Act
        this.bindSecurityGroupService.dispatch(request);
    }

    @Test
    public void testValidate_WhenSgIdNull_ThrowsInvalidEntryException() throws Exception {
        // Arrange
        String field = "Security Group Id";
        BindSecurityGroupRequest request = createRequest(VALID_ID, null, VALID_ID);
        this.exception.expect(VmidcBrokerInvalidEntryException.class);
        this.exception.expectMessage(field + " should not have an empty value.");

        // Act
        this.bindSecurityGroupService.dispatch(request);
    }

    @Test
    public void testValidate_WhenSfcIdAndOrderNull_ThrowsInvalidEntryException() throws Exception {
        // Arrange
        String field = "Service Order";
        BindSecurityGroupRequest request = createRequest(VALID_ID, VALID_ID, null);
        VirtualSystemPolicyBindingDto serviceToBindTo = createRequestWithService(VALID_ID);
        request.addServiceToBindTo(serviceToBindTo);
        this.exception.expect(VmidcBrokerInvalidEntryException.class);
        this.exception.expectMessage(field + " should not have an empty value.");

        // Act
        this.bindSecurityGroupService.dispatch(request);
    }

    @Test
    public void testValidate_WhenServiceVirtualSystemIdNull_ThrowsInvalidEntryException() throws Exception {
        // Arrange
        String field = "Virtual System Id";
        BindSecurityGroupRequest request = createRequest(VALID_ID, VALID_ID, null);
        VirtualSystemPolicyBindingDto serviceToBindTo = createRequestWithService(null);
        request.addServiceToBindTo(serviceToBindTo);
        this.exception.expect(VmidcBrokerInvalidEntryException.class);
        this.exception.expectMessage(field + " should not have an empty value.");

        // Act
        this.bindSecurityGroupService.dispatch(request);
    }

    @Test
    public void testValidate_WhenServiceNameNull_ThrowsInvalidEntryException() throws Exception {
        // Arrange
        String field = "Service Name";
        BindSecurityGroupRequest request = createRequest(VALID_ID, VALID_ID, null);
        VirtualSystemPolicyBindingDto serviceToBindTo = createRequestWithService(VALID_ID);
        serviceToBindTo.setName(null);
        request.addServiceToBindTo(serviceToBindTo);
        this.exception.expect(VmidcBrokerInvalidEntryException.class);
        this.exception.expectMessage(field + " should not have an empty value.");

        // Act
        this.bindSecurityGroupService.dispatch(request);
    }

    @Test
    public void testValidate_WithInValidSgId_ThrowsValidationException() throws Exception {
        // Arrange
        BindSecurityGroupRequest request = createRequest(VALID_ID, INVALID_ID, VALID_ID);
        this.exception.expect(VmidcBrokerValidationException.class);
        this.exception.expectMessage("Security Group with Id: " + request.getSecurityGroupId() + "  is not found.");

        // Act
        this.bindSecurityGroupService.dispatch(request);

    }

    @Test
    public void testValidate_WhenSgIdsVcIdAndActualVcIdMisMatch_ThrowsValidationException() throws Exception {
        // Arrange
        BindSecurityGroupRequest request = createRequest(INVALID_ID, VALID_ID, VALID_ID);
        this.exception.expect(VmidcBrokerValidationException.class);
        this.exception
                .expectMessage(String.format("The Security Group '%s' does not belong to the Parent Object with ID %d",
                        DEFAULT_SG_NAME, INVALID_ID));

        // Act
        this.bindSecurityGroupService.dispatch(request);

    }

    @Test
    public void testValidate_WhenSgIsMarkedForDeletion_ThrowsInvalidRequestException() throws Exception {
        // Arrange
        BindSecurityGroupRequest request = createRequest(VALID_ID_MARK_FOR_DELETION, VALID_ID_MARK_FOR_DELETION,
                VALID_ID_MARK_FOR_DELETION);
        this.exception.expect(VmidcBrokerInvalidRequestException.class);
        this.exception.expectMessage("Invalid Request '" + DEFAULT_SG_NAME + "' is marked for deletion");

        // Act
        this.bindSecurityGroupService.dispatch(request);

    }

    @Test
    public void testValidate_WhenSgSupportsSFCAndServicesSizeGreaterThanOne_ThrowsValidationException()
            throws Exception {
        // Arrange
        BindSecurityGroupRequest request = createRequest(VALID_ID, VALID_ID, null);

        VirtualSystemPolicyBindingDto serviceToBindTo = createRequestWithService(VALID_ID);
        serviceToBindTo.setOrder(0);
        request.addServiceToBindTo(serviceToBindTo);

        VirtualSystemPolicyBindingDto serviceToBindTo1 = createRequestWithService(VALID_ID + 1);
        serviceToBindTo1.setOrder(1);
        request.addServiceToBindTo(serviceToBindTo1);

        Mockito.when(this.bindSecurityGroupService.apiFactoryService.supportsServiceFunctionChaining(VALID_SG))
                .thenReturn(false);

        this.exception.expect(VmidcBrokerValidationException.class);
        this.exception.expectMessage("SDN Controller Plugin of type '" + DEFAULT_CONTROLLER_TYPE
                + "' does not support more then one Service (Service Function Chaining)");

        // Act
        this.bindSecurityGroupService.dispatch(request);
    }

    @Test
    public void testValidate_WhenServiceOrderIsNegative_ThrowsValidationException() throws Exception {
        // Arrange
        BindSecurityGroupRequest request = createRequest(VALID_ID, VALID_ID, null);

        VirtualSystemPolicyBindingDto serviceToBindTo = createRequestWithService(VALID_ID);
        serviceToBindTo.setOrder(-1);
        request.addServiceToBindTo(serviceToBindTo);

        VirtualSystemPolicyBindingDto serviceToBindTo1 = createRequestWithService(VALID_ID + 1);
        serviceToBindTo1.setOrder(1);
        request.addServiceToBindTo(serviceToBindTo1);

        Mockito.when(this.bindSecurityGroupService.apiFactoryService.supportsServiceFunctionChaining(VALID_SG))
                .thenReturn(true);

        this.exception.expect(VmidcBrokerValidationException.class);
        this.exception.expectMessage(
                String.format("Service '%s' needs to have a valid order specified. '%s' value for order is not valid.",
                        serviceToBindTo.getName(), serviceToBindTo.getOrder()));

        // Act
        this.bindSecurityGroupService.dispatch(request);
    }

    @Test
    public void testValidate_WhenServicesAreDuplicated_ThrowsValidationException() throws Exception {
        // Arrange
        BindSecurityGroupRequest request = createRequest(VALID_ID, VALID_ID, null);

        VirtualSystemPolicyBindingDto serviceToBindTo = createRequestWithService(VALID_ID);
        serviceToBindTo.setOrder(0);
        request.addServiceToBindTo(serviceToBindTo);

        VirtualSystemPolicyBindingDto serviceToBindTo1 = createRequestWithService(VALID_ID);
        serviceToBindTo1.setOrder(0);
        request.addServiceToBindTo(serviceToBindTo1);

        Mockito.when(this.bindSecurityGroupService.apiFactoryService.supportsServiceFunctionChaining(VALID_SG))
                .thenReturn(true);
        this.exception.expect(VmidcBrokerValidationException.class);
        this.exception.expectMessage(
                String.format("Duplicate Service found. Cannot Bind Security group to the same Service: '%s' twice.",
                        serviceToBindTo.getName()));

        // Act
        this.bindSecurityGroupService.dispatch(request);
    }

    @Test
    public void testValidate_WhenNotAbleToFindVirtualSystem_ThrowsValidationException() throws Exception {
        // Arrange
        BindSecurityGroupRequest request = createRequest(VALID_ID, VALID_ID, null);

        VirtualSystemPolicyBindingDto serviceToBindTo = createRequestWithService(VALID_ID + 1);
        serviceToBindTo.setOrder(0);
        request.addServiceToBindTo(serviceToBindTo);

        VirtualSystemPolicyBindingDto serviceToBindTo1 = createRequestWithService(VALID_ID + 2);
        serviceToBindTo1.setOrder(0);
        request.addServiceToBindTo(serviceToBindTo1);

        Mockito.when(this.bindSecurityGroupService.apiFactoryService.supportsServiceFunctionChaining(VALID_SG))
                .thenReturn(true);
        this.exception.expect(VmidcBrokerValidationException.class);
        this.exception.expectMessage("Virtual System with Id: " + (VALID_ID + 2) + "  is not found.");

        // Act
        this.bindSecurityGroupService.dispatch(request);
    }

    @Test
    public void testValidate_WhenMultiPoliciesNotSupportedAndHaveMultipulePolcies_ThrowsValidationException()
            throws Exception {
        // Arrange
        BindSecurityGroupRequest request = createRequest(VALID_ID, VALID_ID, null);

        VirtualSystemPolicyBindingDto serviceToBindTo = createRequestWithService(VALID_ID);
        serviceToBindTo.setOrder(0);
        PolicyDto policyDto = createPolicyDto(VALID_ID + 1, "pol-name1");
        serviceToBindTo.addPolicies(policyDto);
        request.addServiceToBindTo(serviceToBindTo);

        VirtualSystem vs = createVirtualSystem(VALID_ID);

        Mockito.when(this.em.find(Mockito.eq(VirtualSystem.class), Mockito.eq(VALID_ID))).thenReturn(vs);
        Mockito.when(this.bindSecurityGroupService.apiFactoryService.supportsServiceFunctionChaining(VALID_SG))
                .thenReturn(true);
        this.exception.expect(VmidcBrokerValidationException.class);
        this.exception.expectMessage(
                "Security group interface cannot have more than one policy for security manager not supporting multiple policy binding");

        // Act
        this.bindSecurityGroupService.dispatch(request);
    }

    @Test
    public void testValidate_WhenVirtualSystemIsMarkedFroDeletion_ThrowsValidationException() throws Exception {
        // Arrange
        BindSecurityGroupRequest request = createRequest(VALID_ID, VALID_ID, null);

        VirtualSystemPolicyBindingDto serviceToBindTo = createRequestWithService(VALID_ID);
        serviceToBindTo.setOrder(0);
        PolicyDto policyDto = createPolicyDto(VALID_ID + 1, "pol-name1");
        serviceToBindTo.addPolicies(policyDto);
        request.addServiceToBindTo(serviceToBindTo);

        VirtualSystem vs = createVirtualSystem(VALID_ID);
        vs.setMarkedForDeletion(true);

        Mockito.when(this.em.find(Mockito.eq(VirtualSystem.class), Mockito.eq(VALID_ID))).thenReturn(vs);
        Mockito.when(this.bindSecurityGroupService.apiFactoryService.supportsServiceFunctionChaining(VALID_SG))
                .thenReturn(true);
        this.exception.expect(VmidcBrokerValidationException.class);
        this.exception.expectMessage(String.format(
                "Cannot bind security group '%s' to service" + " '%s' as the service is marked for deletion",
                DEFAULT_SG_NAME, vs.getDistributedAppliance().getName()));

        // Act
        this.bindSecurityGroupService.dispatch(request);
    }

    @Test
    public void testValidate_WhenPolicyMappingNotSupportedAndOneOrMorePolcies_ThrowsValidationException()
            throws Exception {
        // Arrange
        BindSecurityGroupRequest request = createRequest(VALID_ID, VALID_ID, null);

        VirtualSystemPolicyBindingDto serviceToBindTo = createRequestWithService(VALID_ID);
        serviceToBindTo.setOrder(0);
        PolicyDto policyDto = createPolicyDto(VALID_ID + 1, "pol-name1");
        serviceToBindTo.addPolicies(policyDto);
        request.addServiceToBindTo(serviceToBindTo);

        VirtualSystem vs = createVirtualSystem(VALID_ID);

        Mockito.when(this.em.find(Mockito.eq(VirtualSystem.class), Mockito.eq(VALID_ID))).thenReturn(vs);
        Mockito.when(this.bindSecurityGroupService.apiFactoryService.supportsServiceFunctionChaining(VALID_SG))
                .thenReturn(true);
        Mockito.when(this.bindSecurityGroupService.apiFactoryService.supportsMultiplePolicies(vs)).thenReturn(true);
        this.exception.expect(VmidcBrokerValidationException.class);
        this.exception.expectMessage("Security manager not supporting policy mapping cannot have one or more policies");

        // Act
        this.bindSecurityGroupService.dispatch(request);
    }

    @Test
    public void testValidate_WhenPolicyMappingSupportedAndHaveEmptyPolicyIdList_ThrowsValidationException()
            throws Exception {
        // Arrange
        BindSecurityGroupRequest request = createRequest(VALID_ID, VALID_ID, null);

        VirtualSystemPolicyBindingDto serviceToBindTo = createRequestWithService(VALID_ID);
        serviceToBindTo.setOrder(0);
        Set<Long> policyIds = new HashSet<Long>();
        serviceToBindTo.setPolicyIds(policyIds);
        request.addServiceToBindTo(serviceToBindTo);

        VirtualSystem vs = createVirtualSystem(VALID_ID);

        Mockito.when(this.em.find(Mockito.eq(VirtualSystem.class), Mockito.eq(VALID_ID))).thenReturn(vs);
        Mockito.when(this.bindSecurityGroupService.apiFactoryService.supportsServiceFunctionChaining(VALID_SG))
                .thenReturn(true);
        Mockito.when(this.bindSecurityGroupService.apiFactoryService.supportsMultiplePolicies(vs)).thenReturn(true);
        Mockito.when(this.bindSecurityGroupService.apiFactoryService.syncsPolicyMapping(vs)).thenReturn(true);
        this.exception.expect(VmidcBrokerValidationException.class);
        this.exception.expectMessage("Service to bind: " + serviceToBindTo + " must have a policy id.");

        // Act
        this.bindSecurityGroupService.dispatch(request);
    }

    @Test
    public void testValidate_WhenSfcIdIsNotFound_ThrowsValidationException() throws Exception {
        // Arrange
        BindSecurityGroupRequest request = createRequest(VALID_ID, VALID_ID, VALID_ID);
        request.setBindSfc(true);

        Mockito.when(this.bindSecurityGroupService.apiFactoryService.supportsNeutronSFC(VALID_SG)).thenReturn(true);
        this.exception.expect(VmidcBrokerValidationException.class);
        this.exception.expectMessage("Service Function Chain entry with Id: " + request.getSfcId() + " is not found.");

        // Act
        this.bindSecurityGroupService.dispatch(request);
    }

    @Test
    public void testValidate_WhenSfcVcIdAndActualVcIdDoNotMatch_ThrowsValidationException() throws Exception {
        // Arrange
        BindSecurityGroupRequest request = createRequest(VALID_ID, VALID_ID, VALID_ID);
        ServiceFunctionChain sfc = createSFC(VALID_ID, INVALID_ID);
        request.setBindSfc(true);

        Mockito.when(this.em.find(Mockito.eq(VirtualizationConnector.class), Mockito.eq(VALID_ID)))
                .thenReturn(VALID_SG.getVirtualizationConnector());
        Mockito.when(this.em.find(Mockito.eq(ServiceFunctionChain.class), Mockito.eq(VALID_ID))).thenReturn(sfc);
        Mockito.when(this.bindSecurityGroupService.apiFactoryService.supportsNeutronSFC(VALID_SG)).thenReturn(true);
        this.exception.expect(VmidcBrokerValidationException.class);
        this.exception.expectMessage(
                String.format("The Service Function Chain '%s' does not belong to the Parent Object with ID %d",
                        sfc.getName(), VALID_ID));

        // Act
        this.bindSecurityGroupService.dispatch(request);
    }

    @Test
    public void testValidate_WhenSfcVsDoNotMatchWithBindSeviceVs_ThrowsValidationException() throws Exception {
        // Arrange
        BindSecurityGroupRequest request = createRequest(VALID_ID, VALID_ID, VALID_ID);
        ServiceFunctionChain sfc = createSFC(VALID_ID, VALID_ID);
        VirtualSystemPolicyBindingDto serviceToBindTo = createRequestWithService(INVALID_ID);
        request.addServiceToBindTo(serviceToBindTo);
        request.setBindSfc(true);

        Mockito.when(this.em.find(Mockito.eq(VirtualizationConnector.class), Mockito.eq(VALID_ID)))
                .thenReturn(VALID_SG.getVirtualizationConnector());
        Mockito.when(this.em.find(Mockito.eq(ServiceFunctionChain.class), Mockito.eq(VALID_ID))).thenReturn(sfc);
        Mockito.when(this.bindSecurityGroupService.apiFactoryService.supportsNeutronSFC(VALID_SG)).thenReturn(true);
        this.exception.expect(VmidcBrokerValidationException.class);
        this.exception.expectMessage("Binding request Virtual System Id : " + serviceToBindTo.getVirtualSystemId() +
                " do not match with any of the ids in Service Function Chain Virtual System Id list");

        // Act
        this.bindSecurityGroupService.dispatch(request);
    }

    @Test
    public void testValidate_WhenRequestSfcBindFlagIsFalseForVcOfTypeNeutronSfc_ThrowsValidationException() throws Exception {
        // Arrange
        BindSecurityGroupRequest request = createRequest(VALID_ID, VALID_ID, VALID_ID);
        ServiceFunctionChain sfc = createSFC(VALID_ID, VALID_ID);
        request.setBindSfc(false);

        Mockito.when(this.em.find(Mockito.eq(VirtualizationConnector.class), Mockito.eq(VALID_ID)))
                .thenReturn(VALID_SG.getVirtualizationConnector());
        Mockito.when(this.em.find(Mockito.eq(ServiceFunctionChain.class), Mockito.eq(VALID_ID))).thenReturn(sfc);
        Mockito.when(this.bindSecurityGroupService.apiFactoryService.supportsNeutronSFC(VALID_SG)).thenReturn(true);
        this.exception.expect(VmidcBrokerValidationException.class);
        this.exception.expectMessage(String.format(
                "Binding individual Virtual Systems/Services is not supported for the Security Group %s. Please bind to a Service Function Chain",
                VALID_SG.getName()));

        // Act
        this.bindSecurityGroupService.dispatch(request);
    }

    @Test
    public void testValidate_WhenSfcVirtualSystemListIsEmpty_ThrowsValidationException() throws Exception {
        // Arrange
        BindSecurityGroupRequest request = createRequest(VALID_ID, VALID_ID, VALID_ID);
        ServiceFunctionChain sfc = createSFC(VALID_ID, VALID_ID);
        List<VirtualSystem> vsList = new ArrayList<VirtualSystem>();
        sfc.setVirtualSystems(vsList);
        VirtualSystemPolicyBindingDto serviceToBindTo = createRequestWithService(INVALID_ID);
        request.addServiceToBindTo(serviceToBindTo);
        request.setBindSfc(true);

        Mockito.when(this.em.find(Mockito.eq(VirtualizationConnector.class), Mockito.eq(VALID_ID)))
                .thenReturn(VALID_SG.getVirtualizationConnector());
        Mockito.when(this.em.find(Mockito.eq(ServiceFunctionChain.class), Mockito.eq(VALID_ID))).thenReturn(sfc);
        Mockito.when(this.bindSecurityGroupService.apiFactoryService.supportsNeutronSFC(VALID_SG)).thenReturn(true);
        this.exception.expect(VmidcBrokerValidationException.class);
        this.exception.expectMessage(String.format(
                "Service Function Chain : %s has no Virtual System references, cannot be binded",
                sfc.getName()));

        // Act
        this.bindSecurityGroupService.dispatch(request);
    }

    private static SecurityGroup createSecurityGroup(Long id, boolean markedFOrDeletion) {
        VirtualizationConnector vc = new VirtualizationConnector();
        vc.setId(id);
        vc.setControllerType(DEFAULT_CONTROLLER_TYPE);
        SecurityGroup sg = new SecurityGroup(vc, "111", "Demo");
        sg.setId(id);
        sg.setName(DEFAULT_SG_NAME);
        sg.setMarkedForDeletion(markedFOrDeletion);
        return sg;
    }

    private static ApplianceManagerConnector createApplianceManagerConnector() {
        ApplianceManagerConnector amc = new ApplianceManagerConnector();
        amc.setManagerType("buzz");
        amc.setIpAddress("127.0.0.1");
        amc.setName("Steve");
        amc.setServiceType("foobar");
        return amc;
    }

    private static Policy createPolicy(Long id) {

        ApplianceManagerConnector amc = createApplianceManagerConnector();

        Domain domain = new Domain(amc);
        domain.setName("domainName");

        Policy policy = new Policy(amc, domain);
        policy.setId(id);
        return policy;
    }

    private static PolicyDto createPolicyDto(Long id, String name) {

        PolicyDto policyDto = new PolicyDto();
        policyDto.setId(id);
        policyDto.setPolicyName(name);
        return policyDto;
    }

    private static VirtualSystem createVirtualSystem(Long id) {
        ApplianceManagerConnector amc = createApplianceManagerConnector();
        DistributedAppliance da = new DistributedAppliance(amc);
        da.setName("daName");
        VirtualSystem vs = new VirtualSystem(da);
        vs.setId(id);
        return vs;
    }

    private static SecurityGroupInterface createSecurityGroupInterface(Long id) {

        VirtualSystem vs = createVirtualSystem(id);

        Policy policy = createPolicy(VALID_ID);
        Set<Policy> policies = new HashSet<Policy>();
        policies.add(policy);

        SecurityGroupInterface sgi = new SecurityGroupInterface(vs, policies, "tag1" + id, FailurePolicyType.NA,
                0L + 1);
        return sgi;
    }

    private static BindSecurityGroupRequest createRequest(Long vcId, Long sgId, Long sfcId) {
        BindSecurityGroupRequest request = new BindSecurityGroupRequest();
        request.setVcId(vcId);
        request.setSecurityGroupId(sgId);
        request.setSfcId(sfcId);
        return request;
    }

    private static VirtualSystemPolicyBindingDto createRequestWithService(Long id) {
        Set<Long> policyIds = new HashSet<Long>();
        policyIds.add(id);
        PolicyDto policyDto = createPolicyDto(id, "pol-name" + id);
        List<PolicyDto> policies = new ArrayList<PolicyDto>();
        policies.add(policyDto);

        VirtualSystemPolicyBindingDto serviceDto = new VirtualSystemPolicyBindingDto(id, "name-vs" + id, policyIds,
                policies);
        return serviceDto;

    }

    private static ServiceFunctionChain createSFC(Long sfcId, Long vcId) {
        VirtualizationConnector vc = new VirtualizationConnector();
        vc.setId(vcId);
        vc.setControllerType(DEFAULT_CONTROLLER_TYPE);
        ServiceFunctionChain sfc = new ServiceFunctionChain("sfc" + sfcId, vc);
        VirtualSystem vs = createVirtualSystem(sfcId);
        sfc.addVirtualSystem(vs);
        return sfc;
    }

}
