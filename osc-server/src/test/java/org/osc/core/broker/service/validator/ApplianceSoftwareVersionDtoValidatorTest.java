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
import org.osc.core.broker.model.entities.appliance.ApplianceSoftwareVersion;
import org.osc.core.broker.service.dto.ApplianceSoftwareVersionDto;
import org.osc.core.broker.service.exceptions.VmidcBrokerInvalidEntryException;
import org.osc.core.broker.service.exceptions.VmidcBrokerValidationException;
import org.osc.core.broker.service.test.InMemDB;
import org.osc.core.broker.util.TransactionalBroadcastUtil;
import org.osc.core.broker.util.ValidateUtil;
import org.osc.core.broker.util.db.DBConnectionManager;
import org.osc.core.common.virtualization.VirtualizationType;
import org.osc.core.test.util.TestTransactionControl;

public class ApplianceSoftwareVersionDtoValidatorTest {
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
    private ApplianceSoftwareVersionDtoValidator validator;

    private final static String NULL_FIELD_MESSAGE = "%s should not have an empty value.";

    private final static String MAX_LEN_FIELD_MESSAGE = "%s length should not exceed %s characters. The provided field exceeds this limit by %s characters.";

    private final static String EXISTING_IMAGE_URL = "existing-image-url";

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
    public void testValidateforCreate_UsingNullImageUrl_ThrowsVmidcBrokerInvalidEntryException() throws Exception {
        ApplianceSoftwareVersionDto dto = newApplianceSoftwareVersionDto();
        dto.setImageUrl(null);
        testValidateforCreate_UsingNullField_ThrowsVmidcBrokerInvalidEntryException(dto, "Image Url");
    }

    @Test
    public void testValidateforCreate_UsingNullVirtualizationVersion_ThrowsVmidcBrokerInvalidEntryException() throws Exception {
        ApplianceSoftwareVersionDto dto = newApplianceSoftwareVersionDto();
        dto.setVirtualizationVersion(null);
        testValidateforCreate_UsingNullField_ThrowsVmidcBrokerInvalidEntryException(dto, "Virtualization Version");
    }

    @Test
    public void testValidateforCreate_UsingNullVirtualizationType_ThrowsVmidcBrokerInvalidEntryException() throws Exception {
        ApplianceSoftwareVersionDto dto = newApplianceSoftwareVersionDto();
        dto.setVirtualizationType(null);
        testValidateforCreate_UsingNullField_ThrowsVmidcBrokerInvalidEntryException(dto, "Virtualization Type");
    }

    @Test
    public void testValidateforCreate_UsingNullApplianceSoftwareVersion_ThrowsVmidcBrokerInvalidEntryException() throws Exception {
        ApplianceSoftwareVersionDto dto = newApplianceSoftwareVersionDto();
        dto.setSwVersion(null);
        testValidateforCreate_UsingNullField_ThrowsVmidcBrokerInvalidEntryException(dto, "Appliance Software Version");
    }

    @Test
    public void testValidateforCreate_UsingNullApplianceId_ThrowsVmidcBrokerInvalidEntryException() throws Exception {
        ApplianceSoftwareVersionDto dto = newApplianceSoftwareVersionDto();
        dto.setParentId(null);
        testValidateforCreate_UsingNullField_ThrowsVmidcBrokerInvalidEntryException(dto, "Appliance Id");
    }

    @Test
    public void testValidateforCreate_UsingApplianceImageUrlTooLong_ThrowsVmidcBrokerInvalidEntryException() throws Exception {
        ApplianceSoftwareVersionDto dto = newApplianceSoftwareVersionDto();
        dto.setImageUrl(StringUtils.rightPad("m", ValidateUtil.DEFAULT_MAX_LEN + 1, 'e'));
        testValidateforCreate_UsingFieldTooLong_ThrowsVmidcBrokerInvalidEntryException(dto, "Image Url");
    }

    @Test
    public void testValidateforCreate_UsingApplianceVirtualizationVersionTooLong_ThrowsVmidcBrokerInvalidEntryException() throws Exception {
        ApplianceSoftwareVersionDto dto = newApplianceSoftwareVersionDto();
        dto.setVirtualizationVersion(StringUtils.rightPad("m", ValidateUtil.DEFAULT_MAX_LEN + 1, 'e'));
        testValidateforCreate_UsingFieldTooLong_ThrowsVmidcBrokerInvalidEntryException(dto, "Virtualization Version");
    }

    @Test
    public void testValidateforCreate_UsingApplianceSoftwareVersionTooLong_ThrowsVmidcBrokerInvalidEntryException() throws Exception {
        ApplianceSoftwareVersionDto dto = newApplianceSoftwareVersionDto();
        dto.setSwVersion(StringUtils.rightPad("m", ValidateUtil.DEFAULT_MAX_LEN + 1, 'e'));
        testValidateforCreate_UsingFieldTooLong_ThrowsVmidcBrokerInvalidEntryException(dto, "Appliance Software Version");
    }

    @Test
    public void testValidateforCreate_UsingExistingImageUrl_ThrowsVmidcBrokerValidationException() throws Exception {
        // Arrange.
        ApplianceSoftwareVersionDto dto = newApplianceSoftwareVersionDto();
        dto.setImageUrl(EXISTING_IMAGE_URL);

        this.exception.expect(VmidcBrokerValidationException.class);
        this.exception.expectMessage(String.format("Image file: %s already exists. Cannot add an image with the same name.", EXISTING_IMAGE_URL));

        // Act.
        this.validator.validateForCreate(dto);
    }

    @Test
    public void testValidateforCreate_UsingValidNewImage_Suceeds() throws Exception {
        // Arrange.
        ApplianceSoftwareVersionDto dto = newApplianceSoftwareVersionDto();

        // Act.
        this.validator.validateForCreate(dto);
    }

    private void testValidateforCreate_UsingNullField_ThrowsVmidcBrokerInvalidEntryException(ApplianceSoftwareVersionDto dto, String fieldName) throws Exception {
        // Arrange.
        this.exception.expect(VmidcBrokerInvalidEntryException.class);
        this.exception.expectMessage(String.format(NULL_FIELD_MESSAGE, fieldName));

        // Act.
        this.validator.validateForCreate(dto);
    }

    private void testValidateforCreate_UsingFieldTooLong_ThrowsVmidcBrokerInvalidEntryException(ApplianceSoftwareVersionDto dto, String fieldName) throws Exception {
        // Arrange.
        this.exception.expect(VmidcBrokerInvalidEntryException.class);
        this.exception.expectMessage(String.format(MAX_LEN_FIELD_MESSAGE, fieldName, ValidateUtil.DEFAULT_MAX_LEN, 1));

        // Act.
        this.validator.validateForCreate(dto);
    }

    private ApplianceSoftwareVersionDto newApplianceSoftwareVersionDto() {
        ApplianceSoftwareVersionDto dto = new ApplianceSoftwareVersionDto();
        dto.setParentId(100L);
        dto.setSwVersion("SwVersion");
        dto.setVirtualizationType(VirtualizationType.OPENSTACK);
        dto.setVirtualizationVersion("VirtualizationVersion");
        dto.setImageUrl("ImageUrl");

        return dto;
    }

    @After
    public void testTearDown() {
        InMemDB.shutdown();
    }

    private void populateDatabase() {
        this.em.getTransaction().begin();

        Appliance appliance = new Appliance();

        appliance.setModel("Model");
        appliance.setManagerSoftwareVersion(UUID.randomUUID().toString());
        appliance.setManagerType(UUID.randomUUID().toString());

        this.em.persist(appliance);

        ApplianceSoftwareVersion asv = new ApplianceSoftwareVersion(appliance);
        asv.setApplianceSoftwareVersion("SoftwareVersion");
        asv.setVirtualizationType(VirtualizationType.OPENSTACK);
        asv.setVirtualizarionSoftwareVersion("VirtualizationVersion");
        asv.setImageUrl(EXISTING_IMAGE_URL);
        asv.setAdditionalNicForInspection(false);

        this.em.persist(asv);

        this.em.getTransaction().commit();
    }
}
