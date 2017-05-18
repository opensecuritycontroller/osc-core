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

import static org.assertj.core.api.Assertions.assertThat;

import javax.persistence.EntityManager;

import org.junit.After;
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
import org.osc.core.broker.service.test.InMemDB;
import org.osc.core.broker.service.validator.AgentRegisterServiceRequestValidator;
import org.osc.core.broker.util.TransactionalBroadcastUtil;

public class AgentRegisterServiceRequestValidatorTest {

    @Mock
    private TransactionalBroadcastUtil txBroadcastUtil;

    private EntityManager em;

    @Rule
    public ExpectedException exception = ExpectedException.none();

    private AgentRegisterServiceRequestValidator validator;
    private static DistributedApplianceInstance DAI_WITH_VS;

    private static AgentRegisterServiceRequest REQUEST_WITH_NAME =
            createRequest("REQUEST_NAME", "REQUEST_WITH_NAME_IP", 1L);

    private static AgentRegisterServiceRequest REQUEST_DAI_FOUND_BY_IP =
            createRequest("REQUEST_DAI_FOUND_BY_IP_NAME", "REQUEST_WITHOUT_NAME_IP", 2L);

    private static AgentRegisterServiceRequest REQUEST_NO_NAME_DAI_FOUND_BY_IP =
            createRequest(null, "REQUEST_WITHOUT_NAME_IP", 3L);

    private static AgentRegisterServiceRequest REQUEST_DAI_WITHOUT_VS =
            createRequest("REQUEST_DAI_WITHOUT_VS_NAME", "REQUEST_DAI_WITHOUT_VS_IP", 4L);

    private static AgentRegisterServiceRequest REQUEST_VS_FOUND =
            createRequest("REQUEST_DAI_VS_FOUND_NAME", "REQUEST_DAI_VS_FOUND_IP", 5L);

    private static AgentRegisterServiceRequest REQUEST_VS_NOT_FOUND =
            createRequest("REQUEST_VS_NOT_FOUND_NAME", "REQUEST_VS_NOT_FOUND_IP", 6L);

    private VirtualSystem vs;

    @Before
    public void testInitialize() {
        MockitoAnnotations.initMocks(this);

        this.em = InMemDB.getEntityManagerFactory().createEntityManager();

        populateDatabase();

        REQUEST_VS_FOUND.setVirtualSystemId(this.vs.getId());

        this.validator = new AgentRegisterServiceRequestValidator(this.em, this.txBroadcastUtil);
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
        vc.setVirtualizationType(VirtualizationType.VMWARE);
        vc.setVirtualizationSoftwareVersion("vcSoftwareVersion");
        vc.setName("vcName");
        vc.setProviderIpAddress("127.0.0.1");
        vc.setProviderUsername("Natasha");
        vc.setProviderPassword("********");

        this.em.persist(vc);

        ApplianceSoftwareVersion asv = new ApplianceSoftwareVersion(app);
        asv.setApplianceSoftwareVersion("softwareVersion");
        asv.setImageUrl("bar");
        asv.setVirtualizarionSoftwareVersion(vc.getVirtualizationSoftwareVersion());
        asv.setVirtualizationType(vc.getVirtualizationType());

        this.em.persist(asv);

        DistributedAppliance da = new DistributedAppliance(amc);
        da.setName("daName");
        da.setApplianceVersion(asv.getApplianceSoftwareVersion());
        da.setAppliance(app);

        this.em.persist(da);

        this.vs = new VirtualSystem(da);
        this.vs.setApplianceSoftwareVersion(asv);
        this.vs.setDomain(domain);
        this.vs.setVirtualizationConnector(vc);
        this.vs.setMarkedForDeletion(false);
        this.vs.setName("vsName");

        this.em.persist(this.vs);
        da.addVirtualSystem(this.vs);

        DAI_WITH_VS = new DistributedApplianceInstance(this.vs);
        DAI_WITH_VS.setName("REQUEST_NAME");
        DAI_WITH_VS.setIpAddress("REQUEST_WITHOUT_NAME_IP");
        DAI_WITH_VS.setApplianceConfig(new byte[3]);

        this.em.persist(DAI_WITH_VS);

        this.em.getTransaction().commit();
    }

    @Test
    public void testValidateAndLoad_WithNullRequest_ThrowsNullPointerException() throws Exception {
        // Arrange.
        this.exception.expect(NullPointerException.class);

        // Act.
        this.validator.validateAndLoad(null);
    }

    @Test
    public void testValidateAndLoad_WithoutIpAddress_ThrowsValidationException() throws Exception {
        // Arrange.
        this.exception.expect(VmidcBrokerValidationException.class);
        this.exception.expectMessage("Missing agent IP address.");

        // Act.
        this.validator.validateAndLoad(new AgentRegisterServiceRequest());
    }

    @Test
    public void testValidateAndLoad_WithoutVirtualSystemId_ThrowsValidationException() throws Exception {
        // Arrange.
        this.exception.expect(VmidcBrokerValidationException.class);
        this.exception.expectMessage("Invalid virtual system identifier.");

        AgentRegisterServiceRequest request = new AgentRegisterServiceRequest();
        request.setApplianceIp("10.1.1.1");

        // Act.
        this.validator.validateAndLoad(request);
    }

    @Test
    public void testValidateAndLoad_WhenDaiFoundByName_ExpectsRespectiveDai() throws Exception {
        testValidateAndLoand_ExpectsRespectiveDai(REQUEST_WITH_NAME, DAI_WITH_VS);
    }

    @Test
    public void testValidateAndLoad_WhenDaiFoundByIp_ExpectsRespectiveDai() throws Exception {
        testValidateAndLoand_ExpectsRespectiveDai(REQUEST_DAI_FOUND_BY_IP, DAI_WITH_VS);
    }

    @Test
    public void testValidateAndLoad_WithoutNameDaiFoundByIp_ExpectsRespectiveDai() throws Exception {
        testValidateAndLoand_ExpectsRespectiveDai(REQUEST_NO_NAME_DAI_FOUND_BY_IP, DAI_WITH_VS);
    }

    @Test
    public void testValidateAndLoad_DaiWithoutVs_ThrowsValidationException() throws Exception {
        testValidateAndLoad_NoVs(REQUEST_DAI_WITHOUT_VS);
    }

    @Test
    public void testValidateAndLoad_WhenVsNotFound_ThrowsValidationException() throws Exception {
        testValidateAndLoad_NoVs(REQUEST_VS_NOT_FOUND);
    }

    private void testValidateAndLoad_NoVs(AgentRegisterServiceRequest request) throws Exception {
        // Arrange.
        this.exception.expect(VmidcBrokerValidationException.class);
        this.exception.expectMessage("VS ID " + request.getVsId() + " not found.");

        // Act.
        this.validator.validateAndLoad(request);
    }

    private void testValidateAndLoand_ExpectsRespectiveDai(AgentRegisterServiceRequest request, DistributedApplianceInstance expectedDai) throws Exception {
        // Act.
        DistributedApplianceInstance dai = this.validator.validateAndLoad(request);

        // Assert.
        assertThat(dai).as("dai").isNotNull();
        assertThat(dai).isEqualTo(expectedDai);
    }

    private static AgentRegisterServiceRequest createRequest(String name, String ipAddress, Long vsId) {
        AgentRegisterServiceRequest request = new AgentRegisterServiceRequest();
        request.setName(name);
        request.setApplianceIp(ipAddress);
        request.setVirtualSystemId(vsId);

        return request;
    }
}
