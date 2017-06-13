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

import java.util.List;
import java.util.Set;

import javax.persistence.EntityManager;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.osc.core.broker.model.entities.appliance.Appliance;
import org.osc.core.broker.model.entities.appliance.ApplianceSoftwareVersion;
import org.osc.core.broker.model.entities.appliance.DistributedAppliance;
import org.osc.core.broker.model.entities.appliance.DistributedApplianceInstance;
import org.osc.core.broker.model.entities.appliance.VirtualSystem;
import org.osc.core.broker.model.entities.appliance.VirtualizationType;
import org.osc.core.broker.model.entities.management.ApplianceManagerConnector;
import org.osc.core.broker.model.entities.management.Domain;
import org.osc.core.broker.model.entities.virtualization.VirtualizationConnector;
import org.osc.core.broker.service.exceptions.VmidcBrokerValidationException;
import org.osc.core.broker.service.exceptions.VmidcException;
import org.osc.core.broker.service.test.InMemDB;
import org.osc.core.broker.service.validator.UpdateDaiConsolePasswordRequestValidator;
import org.osc.core.broker.util.TransactionalBroadcastUtil;

import com.google.common.collect.Sets;

public class UpdateDaiConsolePasswordRequestValidatorTest {

    private static final String PASSWORD = "password";

    private static final String DA_NAME = "daName";
    private static final String OTHER_DA_NAME = "otherDaName";
    private static final String DAI_NAME_WITHOUT_VS = "daiNameWithoutVS";
    private static final String DAI_NAME_WITH_OTHER_VS = "daiNameWithOtherVs";
    private static final String DAI_NAME_WITH_VS = "daiNameWithVs";

    private static final Long INVALID_VS_ID = 47L;

    private static final String VALID_VS_NAME = "vsName";
    private static final String OTHER_VS_NAME = "otherVsName";
    private static final String VS_NO_DAI_NAME = "vsNoDAIName";
    private static final String NON_EXISTENT_VS_NAME = "nonExistentName_" + INVALID_VS_ID;
    private static final String INVALID_VS_NAME = "invalidName";

    private static final UpdateDaiConsolePasswordRequest VALID_REQUEST_WITH_DAI_WITHOUT_VS = createRequest(PASSWORD, VALID_VS_NAME, Sets.newHashSet(DAI_NAME_WITHOUT_VS));
    private static final UpdateDaiConsolePasswordRequest VALID_REQUEST_WITH_DAI_WITH_OTHER_VS = createRequest(PASSWORD, VALID_VS_NAME, Sets.newHashSet(DAI_NAME_WITH_OTHER_VS));
    private static final UpdateDaiConsolePasswordRequest VALID_REQUEST_WITH_DAI_WITH_VS = createRequest(PASSWORD, VALID_VS_NAME, Sets.newHashSet(DAI_NAME_WITH_VS));
    private static final UpdateDaiConsolePasswordRequest VALID_REQUEST_WITHOUT_DAI_NAME = createRequest(PASSWORD, VALID_VS_NAME, null);
    private static final UpdateDaiConsolePasswordRequest VALID_REQUEST_WITH_NO_DAI = createRequest(PASSWORD, VS_NO_DAI_NAME, null);
    private static final UpdateDaiConsolePasswordRequest REQUEST_WITHOUT_VS_NAME = createRequest(PASSWORD, null, null);
    private static final UpdateDaiConsolePasswordRequest REQUEST_WITH_VS_NAME_WITHOUT_VS = createRequest(PASSWORD, NON_EXISTENT_VS_NAME, null);
    private static final UpdateDaiConsolePasswordRequest REQUEST_WITH_INVALID_VS_NAME = createRequest(PASSWORD, INVALID_VS_NAME, null);
    private static final UpdateDaiConsolePasswordRequest REQUEST_WITHOUT_NEW_PASSWORD = createRequest(null, VALID_VS_NAME, null);

    private VirtualSystem VIRTUAL_SYSTEM;

    private VirtualSystem OTHER_VIRTUAL_SYSTEM;
    private VirtualSystem VIRTUAL_SYSTEM_WITHOUT_DAI;

//    private static final DistributedApplianceInstance DISTRIBUTED_APPLIANCE_INSTANCE_NULL = null;
    private DistributedApplianceInstance DISTRIBUTED_APPLIANCE_INSTANCE_WITH_OTHER_VS = new DistributedApplianceInstance(new VirtualSystem());
    private DistributedApplianceInstance DISTRIBUTED_APPLIANCE_INSTANCE_WITH_VS = new DistributedApplianceInstance(this.VIRTUAL_SYSTEM);

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Mock
    TransactionalBroadcastUtil txBroadcastUtil;

    EntityManager em;

    private UpdateDaiConsolePasswordRequestValidator validator;

    @Before
    public void testInitialize() throws Exception {
        MockitoAnnotations.initMocks(this);

        this.em = InMemDB.getEntityManagerFactory().createEntityManager();

        populateDatabase();

        this.validator = new UpdateDaiConsolePasswordRequestValidator(this.em, this.txBroadcastUtil);
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

        ApplianceManagerConnector amc = new ApplianceManagerConnector();
        amc.setManagerType("buzz");
        amc.setIpAddress("127.0.0.1");
        amc.setName("Steve");
        amc.setServiceType("foobar");

        this.em.persist(amc);

        Domain domain = new Domain(amc);
        domain.setName("domainName");

        this.em.persist(domain);

        VirtualizationConnector vc = new VirtualizationConnector();
        vc.setVirtualizationType(VirtualizationType.OPENSTACK);
        vc.setVirtualizationSoftwareVersion("vcSoftwareVersion");
        vc.setName("vcName");
        vc.setProviderIpAddress("127.0.0.1");
        vc.setProviderUsername("Natasha");
        vc.setProviderPassword("********");

        this.em.persist(vc);

        VirtualizationConnector otherVc = new VirtualizationConnector();
        otherVc.setVirtualizationType(VirtualizationType.OPENSTACK);
        otherVc.setVirtualizationSoftwareVersion("vcSoftwareVersion");
        otherVc.setName("otherVcName");
        otherVc.setProviderIpAddress("127.0.0.2");
        otherVc.setProviderUsername("Natasha");
        otherVc.setProviderPassword("********");

        this.em.persist(otherVc);

        ApplianceSoftwareVersion asv = new ApplianceSoftwareVersion(app);
        asv.setApplianceSoftwareVersion("softwareVersion");
        asv.setImageUrl("bar");
        asv.setVirtualizarionSoftwareVersion(vc.getVirtualizationSoftwareVersion());
        asv.setVirtualizationType(vc.getVirtualizationType());

        this.em.persist(asv);

        DistributedAppliance da = new DistributedAppliance(amc);
        da.setName(DA_NAME);
        da.setApplianceVersion(asv.getApplianceSoftwareVersion());
        da.setAppliance(app);

        this.em.persist(da);

        this.VIRTUAL_SYSTEM = new VirtualSystem(da);
        this.VIRTUAL_SYSTEM.setApplianceSoftwareVersion(asv);
        this.VIRTUAL_SYSTEM.setDomain(domain);
        this.VIRTUAL_SYSTEM.setVirtualizationConnector(vc);
        this.VIRTUAL_SYSTEM.setMarkedForDeletion(false);
        this.VIRTUAL_SYSTEM.setName(VALID_VS_NAME);

        this.em.persist(this.VIRTUAL_SYSTEM);
        da.addVirtualSystem(this.VIRTUAL_SYSTEM);

        DistributedAppliance otherDa = new DistributedAppliance(amc);
        otherDa.setName(OTHER_DA_NAME);
        otherDa.setApplianceVersion(asv.getApplianceSoftwareVersion());
        otherDa.setAppliance(app);

        this.em.persist(otherDa);

        this.OTHER_VIRTUAL_SYSTEM = new VirtualSystem(otherDa);
        this.OTHER_VIRTUAL_SYSTEM.setApplianceSoftwareVersion(asv);
        this.OTHER_VIRTUAL_SYSTEM.setDomain(domain);
        this.OTHER_VIRTUAL_SYSTEM.setVirtualizationConnector(otherVc);
        this.OTHER_VIRTUAL_SYSTEM.setMarkedForDeletion(false);
        this.OTHER_VIRTUAL_SYSTEM.setName(OTHER_VS_NAME);

        this.em.persist(this.OTHER_VIRTUAL_SYSTEM);

        DistributedAppliance daNoDAI = new DistributedAppliance(amc);
        daNoDAI.setName("NoDAI");
        daNoDAI.setApplianceVersion(asv.getApplianceSoftwareVersion());
        daNoDAI.setAppliance(app);

        this.em.persist(daNoDAI);

        this.VIRTUAL_SYSTEM_WITHOUT_DAI = new VirtualSystem(daNoDAI);
        this.VIRTUAL_SYSTEM_WITHOUT_DAI.setApplianceSoftwareVersion(asv);
        this.VIRTUAL_SYSTEM_WITHOUT_DAI.setDomain(domain);
        this.VIRTUAL_SYSTEM_WITHOUT_DAI.setVirtualizationConnector(otherVc);
        this.VIRTUAL_SYSTEM_WITHOUT_DAI.setMarkedForDeletion(false);
        this.VIRTUAL_SYSTEM_WITHOUT_DAI.setName(VS_NO_DAI_NAME);

        this.em.persist(this.VIRTUAL_SYSTEM_WITHOUT_DAI);
        daNoDAI.addVirtualSystem(this.VIRTUAL_SYSTEM_WITHOUT_DAI);

        this.DISTRIBUTED_APPLIANCE_INSTANCE_WITH_VS = new DistributedApplianceInstance(this.VIRTUAL_SYSTEM);
        this.DISTRIBUTED_APPLIANCE_INSTANCE_WITH_VS.setName(DAI_NAME_WITH_VS);
        this.DISTRIBUTED_APPLIANCE_INSTANCE_WITH_VS.setIpAddress("REQUEST_WITHOUT_NAME_IP");
        this.DISTRIBUTED_APPLIANCE_INSTANCE_WITH_VS.setApplianceConfig(new byte[3]);

        this.em.persist(this.DISTRIBUTED_APPLIANCE_INSTANCE_WITH_VS);

        this.DISTRIBUTED_APPLIANCE_INSTANCE_WITH_OTHER_VS = new DistributedApplianceInstance(this.OTHER_VIRTUAL_SYSTEM);
        this.DISTRIBUTED_APPLIANCE_INSTANCE_WITH_OTHER_VS.setName(DAI_NAME_WITH_OTHER_VS);
        this.DISTRIBUTED_APPLIANCE_INSTANCE_WITH_OTHER_VS.setIpAddress("REQUEST_WITHOUT_NAME_IP");
        this.DISTRIBUTED_APPLIANCE_INSTANCE_WITH_OTHER_VS.setApplianceConfig(new byte[3]);

        this.em.persist(this.DISTRIBUTED_APPLIANCE_INSTANCE_WITH_OTHER_VS);

        this.em.getTransaction().commit();
    }

    @Test
    public void testValidate_WithValidRequest_ThrowsUnsupportedOperationException() throws Exception {
        // Arrange
        this.exception.expect(UnsupportedOperationException.class);

        // Act
        this.validator.validate(VALID_REQUEST_WITH_DAI_WITH_VS);
    }

    @Test
    public void testValidateAndLoad_WithValidRequest_ThrowsUnsupportedOperationException() throws Exception {
        // Arrange
        this.exception.expect(UnsupportedOperationException.class);

        // Act
        this.validator.validateAndLoad(VALID_REQUEST_WITH_DAI_WITH_VS);
    }

    @Test
    public void testValidateAndLoad_WithRequestWithoutVsName_ThrowsValidationException() throws Exception {
        // Arrange
        this.exception.expect(VmidcBrokerValidationException.class);
        this.exception.expectMessage("Invalid Virtual System Name.");

        // Act
        this.validator.validateAndLoadList(REQUEST_WITHOUT_VS_NAME);
    }

    @Test
    public void testValidateAndLoad_WithRequestWithVsNameWithoutVs_ThrowsValidationException() throws Exception {
        // Arrange
        this.exception.expect(VmidcBrokerValidationException.class);
        this.exception.expectMessage("Virtual System with ID: " + INVALID_VS_ID + " not found.");

        // Act
        this.validator.validateAndLoadList(REQUEST_WITH_VS_NAME_WITHOUT_VS);
    }

    @Test
    public void testValidateAndLoad_WithRequestWithInvalidVsName_NumberFormatException() throws Exception {
        // Arrange
        this.exception.expect(NumberFormatException.class);

        // Act
        this.validator.validateAndLoadList(REQUEST_WITH_INVALID_VS_NAME);
    }

    @Test
    public void testValidateAndLoad_WithRequestWithoutNewPassword_ThrowsValidationException() throws Exception {
        // Arrange
        this.exception.expect(VmidcBrokerValidationException.class);
        this.exception.expectMessage("Invalid password.");

        // Act
        this.validator.validateAndLoadList(REQUEST_WITHOUT_NEW_PASSWORD);
    }

    @Test
    public void testValidateAndLoad_WithValidRequestWithDaiWithOtherVs_ThrowsValidationException() throws Exception {
        // Arrange
        this.exception.expect(VmidcException.class);
        this.exception.expectMessage("DAI '" + DAI_NAME_WITH_OTHER_VS + "' is not a member of VSS '" + this.VIRTUAL_SYSTEM.getName() + "'.");

        // Act
        this.validator.validateAndLoadList(VALID_REQUEST_WITH_DAI_WITH_OTHER_VS);
    }

    @Test
    public void testValidateAndLoad_WithValidRequestWithDaiWithVs_ExpectsSuccess() throws Exception {
        // Act
        List<DistributedApplianceInstance> distributedApplianceInstances = this.validator.validateAndLoadList(VALID_REQUEST_WITH_DAI_WITH_VS);

        // Assert
        Assert.assertEquals("The received list size is different than expected in test with DAI.", 1, distributedApplianceInstances.size());
        Assert.assertEquals("The received list element is different than expected in test with DAI.", this.DISTRIBUTED_APPLIANCE_INSTANCE_WITH_VS, distributedApplianceInstances.get(0));
    }

    @Test
    public void testValidateAndLoad_WithValidRequestWithDaiWithoutVs_ThrowsValidationException() throws Exception {
        // Arrange
        this.exception.expect(VmidcException.class);
        this.exception.expectMessage("DAI '" + DAI_NAME_WITHOUT_VS + "' not found.");

        // Act
        this.validator.validateAndLoadList(VALID_REQUEST_WITH_DAI_WITHOUT_VS);
    }


    @Test
    public void testValidateAndLoad_WithValidRequestWithoutDaiAndDaiNotListedByVs_ThrowsValidationException() throws Exception {
        // Arrange
        this.exception.expect(VmidcException.class);
        this.exception.expectMessage("VSS '" + VS_NO_DAI_NAME + "' does not have members.");

        // Act
        this.validator.validateAndLoadList(VALID_REQUEST_WITH_NO_DAI);
    }

    @Test
    public void testValidateAndLoad_WithValidRequestWithoutDaiAndDaiListedByVs_ExpectsCorrectDais() throws Exception {
        // Act
        List<DistributedApplianceInstance> distributedApplianceInstances = this.validator.validateAndLoadList(VALID_REQUEST_WITHOUT_DAI_NAME);

        // Assert
        Assert.assertEquals("The received list size is different than expected in test without DAI.", 1, distributedApplianceInstances.size());
        Assert.assertEquals("The received list element is different than expected in test without DAI.", this.DISTRIBUTED_APPLIANCE_INSTANCE_WITH_VS, distributedApplianceInstances.get(0));
    }

    private static UpdateDaiConsolePasswordRequest createRequest(String newPassword, String vsName, Set<String> daiSet) {
        UpdateDaiConsolePasswordRequest request = new UpdateDaiConsolePasswordRequest();
        request.setNewPassword(newPassword);
        request.setVsName(vsName);
        request.setDaiList(daiSet);
        return request;
    }
}