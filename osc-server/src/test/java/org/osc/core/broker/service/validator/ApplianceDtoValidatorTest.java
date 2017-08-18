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
package org.osc.core.broker.service.validator;

import java.util.UUID;

import javax.persistence.EntityManager;

import org.apache.commons.lang.StringUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.osc.core.broker.model.entities.appliance.Appliance;
import org.osc.core.broker.service.dto.ApplianceDto;
import org.osc.core.broker.service.exceptions.VmidcBrokerInvalidEntryException;
import org.osc.core.broker.service.exceptions.VmidcBrokerValidationException;
import org.osc.core.broker.service.test.InMemDB;
import org.osc.core.broker.util.TransactionalBroadcastUtil;
import org.osc.core.broker.util.ValidateUtil;
import org.osc.core.broker.util.db.DBConnectionManager;
import org.osc.core.test.util.TestTransactionControl;

public class ApplianceDtoValidatorTest {
    private EntityManager em;

    @Mock(answer=Answers.CALLS_REAL_METHODS)
    TestTransactionControl txControl;

    @Mock
    private DBConnectionManager dbMgr;

    @Mock
    private TransactionalBroadcastUtil txBroadcastUtil;

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @InjectMocks
    private ApplianceDtoValidator validator;

    private final static String NULL_FIELD_MESSAGE = "%s should not have an empty value.";

    private final static String MAX_LEN_FIELD_MESSAGE = "%s length should not exceed %s characters. The provided field exceeds this limit by %s characters.";

    private final static String EXISTING_MODEL = "existing-model";


    @Before
    public void testInitialize() {
        MockitoAnnotations.initMocks(this);

        this.em = InMemDB.getEntityManagerFactory().createEntityManager();

        this.validator.setEntityManager(this.em);

        this.txControl.setEntityManager(this.em);

        Mockito.when(this.dbMgr.getTransactionalEntityManager()).thenReturn(this.em);
        Mockito.when(this.dbMgr.getTransactionControl()).thenReturn(this.txControl);

        populateDatabase();
    }

    @Test
    public void testValidateforCreate_UsingNullModel_ThrowsVmidcBrokerInvalidEntryException() throws Exception {
        ApplianceDto dto = new ApplianceDto(null, UUID.randomUUID().toString(), UUID.randomUUID().toString());
        testValidateforCreate_UsingNullField_ThrowsVmidcBrokerInvalidEntryException(dto, "Appliance Model");
    }

    @Test
    public void testValidateforCreate_UsingNullManagerType_ThrowsVmidcBrokerInvalidEntryException() throws Exception {
        ApplianceDto dto = new ApplianceDto(UUID.randomUUID().toString(), null, UUID.randomUUID().toString());
        testValidateforCreate_UsingNullField_ThrowsVmidcBrokerInvalidEntryException(dto, "Appliance Manager Type");
    }

    @Test
    public void testValidateforCreate_UsingNullManagerVersion_ThrowsVmidcBrokerInvalidEntryException() throws Exception {
        ApplianceDto dto = new ApplianceDto(UUID.randomUUID().toString(), UUID.randomUUID().toString(), null);
        testValidateforCreate_UsingNullField_ThrowsVmidcBrokerInvalidEntryException(dto, "Appliance Manager Version");
    }

    @Test
    public void testValidateforCreate_UsingApplianceModelTooLong_ThrowsVmidcBrokerInvalidEntryException() throws Exception {
        ApplianceDto dto = new ApplianceDto(StringUtils.rightPad("m", ValidateUtil.DEFAULT_MAX_LEN + 1, 'e'), UUID.randomUUID().toString(), UUID.randomUUID().toString());
        testValidateforCreate_UsingFieldTooLong_ThrowsVmidcBrokerInvalidEntryException(dto, "Appliance Model");
    }

    @Test
    public void testValidateforCreate_UsingApplianceManagerVersionTooLong_ThrowsVmidcBrokerInvalidEntryException() throws Exception {
        ApplianceDto dto = new ApplianceDto(UUID.randomUUID().toString(), UUID.randomUUID().toString(), StringUtils.rightPad("m", ValidateUtil.DEFAULT_MAX_LEN + 1, 'e'));
        testValidateforCreate_UsingFieldTooLong_ThrowsVmidcBrokerInvalidEntryException(dto, "Appliance Manager Version");
    }

    @Test
    public void testValidateforCreate_UsingExistingModel_ThrowsVmidcBrokerValidationException() throws Exception {
        // Arrange.
        ApplianceDto dto = new ApplianceDto(EXISTING_MODEL, UUID.randomUUID().toString(), UUID.randomUUID().toString());
        this.exception.expect(VmidcBrokerValidationException.class);
        this.exception.expectMessage(String.format("Appliance already exists for model: " + EXISTING_MODEL));

        // Act.
        this.validator.validateForCreate(dto);
    }

    @Test
    public void testValidateforCreate_UsingValidNewModel_Suceeds() throws Exception {
        // Arrange.
        ApplianceDto dto = new ApplianceDto(UUID.randomUUID().toString(), UUID.randomUUID().toString(), UUID.randomUUID().toString());

        // Act.
        this.validator.validateForCreate(dto);
    }

    public void testValidateforCreate_UsingNullField_ThrowsVmidcBrokerInvalidEntryException(ApplianceDto dto, String fieldName) throws Exception {
        // Arrange.
        this.exception.expect(VmidcBrokerInvalidEntryException.class);
        this.exception.expectMessage(String.format(NULL_FIELD_MESSAGE, fieldName));

        // Act.
        this.validator.validateForCreate(dto);
    }

    public void testValidateforCreate_UsingFieldTooLong_ThrowsVmidcBrokerInvalidEntryException(ApplianceDto dto, String fieldName) throws Exception {
        // Arrange.
        this.exception.expect(VmidcBrokerInvalidEntryException.class);
        this.exception.expectMessage(String.format(MAX_LEN_FIELD_MESSAGE, fieldName, ValidateUtil.DEFAULT_MAX_LEN, 1));

        // Act.
        this.validator.validateForCreate(dto);
    }

    @After
    public void testTearDown() {
        InMemDB.shutdown();
    }

    private void populateDatabase() {
        this.em.getTransaction().begin();

        Appliance appliance = new Appliance();

        appliance.setModel(EXISTING_MODEL);
        appliance.setManagerSoftwareVersion(UUID.randomUUID().toString());
        appliance.setManagerType(UUID.randomUUID().toString());

        this.em.persist(appliance);
        this.em.getTransaction().commit();
    }
}
