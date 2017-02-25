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
package org.osc.core.broker.service.request;

import org.hibernate.Session;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.osc.core.broker.model.entities.appliance.DistributedAppliance;
import org.osc.core.broker.service.exceptions.VmidcBrokerValidationException;

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
    Session session;

    private DeleteDistributedApplianceRequestValidator validator;

    @Before
    public void testInitialize() throws Exception {
        MockitoAnnotations.initMocks(this);

        this.validator = new DeleteDistributedApplianceRequestValidator(this.session);

        Mockito.when((DistributedAppliance)session.get(Mockito.eq(DistributedAppliance.class), Mockito.eq(VALID_ID))).thenReturn(NOT_MARKED_FOR_DELETION_DA);
        Mockito.when((DistributedAppliance)session.get(Mockito.eq(DistributedAppliance.class), Mockito.eq(VALID_ID_FOR_DELETION))).thenReturn(MARKED_FOR_DELETION_DA);
    }

    @Test
    public void testValidate_WithValidRequest_ThrowsUnsupportedOperationException() throws Exception {
        // Arrange
        this.exception.expect(UnsupportedOperationException.class);

        // Act
        validator.validate(VALID_REQUEST);
    }

    @Test
    public void testValidateAndLoad_WhenDANotFound_ThrowsValidationException() throws Exception {
        // Arrange
        this.exception.expect(VmidcBrokerValidationException.class);
        this.exception.expectMessage("Distributed Appliance entry with ID '" + INVALID_ID + "' is not found.");

        // Act
        validator.validateAndLoad(REQUEST_DA_NOT_FOUND);
    }

    @Test
    public void testValidateAndLoad_WithForceDeleteRequestAndStandardDA_ThrowsValidationException() throws Exception {
        // Arrange
        this.exception.expect(VmidcBrokerValidationException.class);
        this.exception.expectMessage("Distributed Appilance with ID "
                        + VALID_ID
                        + " is not marked for deletion and force delete operation is applicable only for entries marked for deletion.");

        // Act
        validator.validateAndLoad(VALID_REQUEST_FORCE_DELETE);
    }

    @Test
    public void testValidateAndLoad_WithNonForceDeleteRequestAndStandardDA_ExpectsSuccess() throws Exception {
        // Act
        DistributedAppliance da = validator.validateAndLoad(VALID_REQUEST);

        // Assert
        Assert.assertEquals("The received ID is different than expected VALID_ID.", VALID_ID, da.getId());
    }

    @Test
    public void testValidateAndLoad_WithForceDeleteRequestAndMarkedForDeletionDA_ExpectsSuccess() throws Exception {
        // Act
        DistributedAppliance da = validator.validateAndLoad(VALID_REQUEST_FORCE_DELETE_DA_MARKED_TO_DELETE);

        // Assert
        Assert.assertEquals("The received ID in force delete case is different than expected VALID_ID_FOR_DELETION.", VALID_ID_FOR_DELETION, da.getId());
    }

    @Test
    public void testValidateAndLoad_WithNonForceDeleteRequestAndMarkedForDeletionDA_ExpectsSuccess() throws Exception {
        // Act
        DistributedAppliance da = validator.validateAndLoad(VALID_REQUEST_DA_MARKED_TO_DELETE);

        // Assert
        Assert.assertEquals("The received ID in non force delete case  is different than expected VALID_ID_FOR_DELETION.", VALID_ID_FOR_DELETION, da.getId());
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