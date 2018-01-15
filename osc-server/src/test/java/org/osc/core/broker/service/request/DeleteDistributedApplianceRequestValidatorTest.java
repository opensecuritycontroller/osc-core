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
package org.osc.core.broker.service.request;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.persistence.EntityManager;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.osc.core.broker.model.entities.appliance.DistributedAppliance;
import org.osc.core.broker.model.entities.appliance.TagEncapsulationType;
import org.osc.core.broker.model.entities.appliance.VirtualSystem;
import org.osc.core.broker.model.entities.virtualization.SecurityGroup;
import org.osc.core.broker.model.entities.virtualization.ServiceFunctionChain;
import org.osc.core.broker.service.exceptions.VmidcBrokerValidationException;
import org.osc.core.broker.service.persistence.DistributedApplianceEntityMgr;
import org.osc.core.broker.service.persistence.SecurityGroupEntityMgr;
import org.osc.core.broker.service.validator.DeleteDistributedApplianceRequestValidator;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
//TODO balmukund: Because the class under test now has a dependency on a complex DB query listReferencedVSBySecurityGroup it must be refactored to use an in mem db.
// Until then powermock is being used to mock static dependencies. This should be removed when this refactoring happens.
@RunWith(PowerMockRunner.class)
@PrepareForTest({SecurityGroupEntityMgr.class,DistributedApplianceEntityMgr.class})
public class DeleteDistributedApplianceRequestValidatorTest {

    private static final Long VALID_ID = 1l;
    private static final Long VALID_ID_FOR_DELETION = 2l;
    private static final Long INVALID_ID = 3l;

    private static final BaseDeleteRequest VALID_REQUEST = createRequest(VALID_ID, false);
    private static final BaseDeleteRequest VALID_REQUEST_FORCE_DELETE = createRequest(VALID_ID, true);
    private static final BaseDeleteRequest VALID_REQUEST_DA_MARKED_TO_DELETE = createRequest(VALID_ID_FOR_DELETION, false);
    private static final BaseDeleteRequest VALID_REQUEST_FORCE_DELETE_DA_MARKED_TO_DELETE = createRequest(VALID_ID_FOR_DELETION, true);
    private static final BaseDeleteRequest REQUEST_DA_NOT_FOUND = createRequest(INVALID_ID, false);

    private static final DistributedAppliance MARKED_FOR_DELETION_DA = createDA(VALID_ID_FOR_DELETION, true);
    private static final DistributedAppliance NOT_MARKED_FOR_DELETION_DA = createDA(VALID_ID, false);

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Mock
    EntityManager em;

    private DeleteDistributedApplianceRequestValidator validator;

    @Before
    public void testInitialize() throws Exception {
        MockitoAnnotations.initMocks(this);

        this.validator = new DeleteDistributedApplianceRequestValidator(this.em);

        Mockito.when(this.em.find(Mockito.eq(DistributedAppliance.class), Mockito.eq(VALID_ID))).thenReturn(NOT_MARKED_FOR_DELETION_DA);
        Mockito.when(this.em.find(Mockito.eq(DistributedAppliance.class), Mockito.eq(VALID_ID_FOR_DELETION))).thenReturn(MARKED_FOR_DELETION_DA);
    }

    @Test
    public void testValidate_WithValidRequest_ThrowsUnsupportedOperationException() throws Exception {
        // Arrange
        this.exception.expect(UnsupportedOperationException.class);

        // Act
        this.validator.validate(VALID_REQUEST);
    }

    @Test
    public void testValidateAndLoad_WhenDANotFound_ThrowsValidationException() throws Exception {
        // Arrange
        this.exception.expect(VmidcBrokerValidationException.class);
        this.exception.expectMessage("Distributed Appliance entry with ID '" + INVALID_ID + "' is not found.");

        // Act
        this.validator.validateAndLoad(REQUEST_DA_NOT_FOUND);
    }

    @Test
    public void testValidateAndLoad_WithForceDeleteRequestAndStandardDA_ThrowsValidationException() throws Exception {
        // Arrange
        this.exception.expect(VmidcBrokerValidationException.class);
        this.exception.expectMessage("Distributed Appilance with ID "
                        + VALID_ID
                        + " is not marked for deletion and force delete operation is applicable only for entries marked for deletion.");

        // Act
        this.validator.validateAndLoad(VALID_REQUEST_FORCE_DELETE);
    }

    @Test
    public void testValidateAndLoad_WithNonForceDeleteRequestAndStandardDA_ExpectsSuccess() throws Exception {
        // Act
        DistributedAppliance da = this.validator.validateAndLoad(VALID_REQUEST);

        // Assert
        Assert.assertEquals("The received ID is different than expected VALID_ID.", VALID_ID, da.getId());
    }

    @Test
    public void testValidateAndLoad_WithForceDeleteRequestAndMarkedForDeletionDA_ExpectsSuccess() throws Exception {
        // Act
        DistributedAppliance da = this.validator.validateAndLoad(VALID_REQUEST_FORCE_DELETE_DA_MARKED_TO_DELETE);

        // Assert
        Assert.assertEquals("The received ID in force delete case is different than expected VALID_ID_FOR_DELETION.", VALID_ID_FOR_DELETION, da.getId());
    }

    @Test
    public void testValidateAndLoad_WithNonForceDeleteRequestAndMarkedForDeletionDA_ExpectsSuccess() throws Exception {
        // Act
        DistributedAppliance da = this.validator.validateAndLoad(VALID_REQUEST_DA_MARKED_TO_DELETE);

        // Assert
        Assert.assertEquals("The received ID in non force delete case  is different than expected VALID_ID_FOR_DELETION.", VALID_ID_FOR_DELETION, da.getId());
    }

    //TODO balmukund: Because the class under test now has a dependency on a complex DB query listReferencedVSBySecurityGroup it must be refactored to use an in mem db.
    // until then Powermock is being used to mock static dependencies. This should be removed when this refactoring happens.
    @Test
    public void testValidateAndLoad_WhenChainedToSfc_ThrowsValidationException() throws Exception {
        // Arrange
        final Long VALID_ID_WITH_SFC = 4l;
        DistributedAppliance da = createDA(VALID_ID_WITH_SFC, false);
        ServiceFunctionChain sfc = new ServiceFunctionChain("sfc-1", null);

        VirtualSystem vs = new VirtualSystem(null);
        vs.setName("vs-1");
        vs.setEncapsulationType(TagEncapsulationType.VLAN);
        vs.setServiceFunctionChains(Arrays.asList(sfc));
        da.addVirtualSystem(vs);
        PowerMockito.mockStatic(DistributedApplianceEntityMgr.class);
        PowerMockito.when(DistributedApplianceEntityMgr.isProtectingWorkload(da)).thenReturn(true);
        Mockito.when(this.em.find(Mockito.eq(DistributedAppliance.class), Mockito.eq(VALID_ID_WITH_SFC)))
                .thenReturn(da);
        this.exception.expect(VmidcBrokerValidationException.class);
        this.exception.expectMessage(
                String.format("The distributed appliance with name '%s' and id '%s' is currently protecting a workload",
                        da.getName(), da.getId()));

        // Act
        this.validator.validateAndLoad(createRequest(VALID_ID_WITH_SFC, false));
    }

    @Test
    public void testValidateAndLoad_WhenChainedToSfcAndSg_WhenSfcMode_ThrowsValidationException()
            throws Exception {
        // Arrange
        final Long VALID_ID_WITH_SFC = 4l;
        DistributedAppliance da = createDA(VALID_ID_WITH_SFC, false);
        List<SecurityGroup> sgList=new ArrayList<>();
        SecurityGroup sg=new SecurityGroup(null,null,null);
        sg.setName("sg");
        sgList.add(sg);
        ServiceFunctionChain sfc = new ServiceFunctionChain("sfc-1", null);
        sfc.setId(VALID_ID_WITH_SFC);
        VirtualSystem vs = new VirtualSystem(null);
        vs.setName("vs-1");
        vs.setEncapsulationType(TagEncapsulationType.VLAN);
        vs.setServiceFunctionChains(Arrays.asList(sfc));

        da.addVirtualSystem(vs);
        PowerMockito.mockStatic(SecurityGroupEntityMgr.class);
        Mockito.when(SecurityGroupEntityMgr.listSecurityGroupsBySfcId(em,VALID_ID_WITH_SFC)).thenReturn(sgList);
        Mockito.when(this.em.find(Mockito.eq(DistributedAppliance.class), Mockito.eq(VALID_ID_WITH_SFC)))
                .thenReturn(da);
        this.exception.expect(VmidcBrokerValidationException.class);
        this.exception.expectMessage(String.format("Distributed appliance is referencing to Service Function Chain '%s' and binded to a Security Group(s) '%s'",
                sfc.getName(), sg.getName()));

        // Act
        this.validator.validateAndLoad(createRequest(VALID_ID_WITH_SFC, false));
    }

    @Test
    public void testValidateAndLoad_WhenChainedToSfc_WhenSfcMode_ThrowsValidationException()
            throws Exception {
        // Arrange
        final Long VALID_ID_WITH_SFC = 4l;
        DistributedAppliance da = createDA(VALID_ID_WITH_SFC, false);
        ServiceFunctionChain sfc = new ServiceFunctionChain("sfc-1", null);
        List<SecurityGroup> sgList=new ArrayList<>();
        VirtualSystem vs = new VirtualSystem(null);
        vs.setName("vs-1");
        vs.setEncapsulationType(TagEncapsulationType.VLAN);
        vs.setServiceFunctionChains(Arrays.asList(sfc));
        da.addVirtualSystem(vs);
        PowerMockito.mockStatic(SecurityGroupEntityMgr.class);
        Mockito.when(SecurityGroupEntityMgr.listSecurityGroupsBySfcId(em,VALID_ID_WITH_SFC)).thenReturn(sgList);
        Mockito.when(em.find(Mockito.eq(DistributedAppliance.class), Mockito.eq(VALID_ID_WITH_SFC)))
                .thenReturn(da);
        this.exception.expect(VmidcBrokerValidationException.class);
        this.exception.expectMessage(String.format("Distributed appliance is referencing to Service Function Chain '%s'",sfc.getName()));

        // Act
        this.validator.validateAndLoad(createRequest(VALID_ID_WITH_SFC, false));
    }

    private static BaseDeleteRequest createRequest(Long id, boolean forceDelete) {
        BaseDeleteRequest request = new BaseDeleteRequest(id);
        request.setForceDelete(forceDelete);
        return request;
    }

    private static DistributedAppliance createDA(Long id, boolean markedFOrDeletion) {
        DistributedAppliance da = new DistributedAppliance();
        da.setId(id);
        da.setMarkedForDeletion(markedFOrDeletion);
        return da;
    }
}