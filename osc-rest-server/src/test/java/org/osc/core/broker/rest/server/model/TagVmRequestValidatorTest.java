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
package org.osc.core.broker.rest.server.model;

import javax.persistence.EntityManager;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.MockitoAnnotations;
import org.osc.core.broker.model.entities.appliance.Appliance;
import org.osc.core.broker.model.entities.appliance.ApplianceSoftwareVersion;
import org.osc.core.broker.model.entities.appliance.DistributedAppliance;
import org.osc.core.broker.model.entities.appliance.DistributedApplianceInstance;
import org.osc.core.broker.model.entities.appliance.VirtualSystem;
import org.osc.core.broker.model.entities.appliance.VirtualizationType;
import org.osc.core.broker.model.entities.management.ApplianceManagerConnector;
import org.osc.core.broker.model.entities.virtualization.VirtualizationConnector;
import org.osc.core.broker.service.exceptions.VmidcBrokerValidationException;
import org.osc.core.broker.service.request.TagVmRequest;
import org.osc.core.broker.service.test.InMemDB;

public class TagVmRequestValidatorTest {

    private static final String VALID_APPLIANCE_NAME = "appliance_name";
    private static final String INVALID_APPLIANCE_NAME = "other_applicance_name";

    private static final TagVmRequest VALID_REQUEST = createRequest(VALID_APPLIANCE_NAME);
    private static final TagVmRequest INVALID_REQUEST = createRequest(INVALID_APPLIANCE_NAME);

    private static final DistributedApplianceInstance DISTRIBUTED_APPLIANCE_INSTANCE = createDistributedApplianceInstance();

    EntityManager em;

    @Rule
    public ExpectedException exception = ExpectedException.none();

    private TagVmRequestValidator validator;

    @Before
    public void testInitialize() throws Exception {
        MockitoAnnotations.initMocks(this);

        this.em = InMemDB.getEntityManagerFactory().createEntityManager();

        this.validator = new TagVmRequestValidator(this.em);
    }

    @After
    public void testTearDown() {
        InMemDB.shutdown();
    }

    private static DistributedApplianceInstance createDistributedApplianceInstance() {
        DistributedApplianceInstance dai = new DistributedApplianceInstance(createVirtualSystem());

        dai.setName(VALID_APPLIANCE_NAME);

        return dai;
    }

    private static VirtualSystem createVirtualSystem() {

        VirtualSystem virtualSystem = new VirtualSystem();
        virtualSystem.setId(1L);

        Appliance app = new Appliance();
        app.setManagerSoftwareVersion("fizz");
        app.setManagerType("buzz");
        app.setModel("fizzbuzz");

        ApplianceSoftwareVersion asv = new ApplianceSoftwareVersion(app);
        asv.setApplianceSoftwareVersion("foo");
        asv.setImageUrl("bar");
        asv.setVirtualizarionSoftwareVersion("baz");
        asv.setVirtualizationType(VirtualizationType.OPENSTACK);

        virtualSystem.setApplianceSoftwareVersion(asv);

        ApplianceManagerConnector amc = new ApplianceManagerConnector();
        amc.setManagerType("buzz");
        amc.setIpAddress("127.0.0.1");
        amc.setName("Steve");
        amc.setServiceType("foobar");

        DistributedAppliance da = new DistributedAppliance(amc);
        da.addVirtualSystem(virtualSystem);
        da.setAppliance(app);
        da.setApplianceVersion("foobarbaz");
        da.setName("Tony");

        virtualSystem.setDistributedAppliance(da);

        VirtualizationConnector vc = new VirtualizationConnector();
        vc.setName("Clint");
        vc.setProviderIpAddress("127.0.0.1");
        vc.setProviderUsername("Natasha");
        vc.setProviderPassword("********");
        vc.setVirtualizationType(VirtualizationType.OPENSTACK);

        virtualSystem.setVirtualizationConnector(vc);

        return virtualSystem;
    }

    @Test
    public void testValidate_WithValidRequest_ThrowsUnsupportedOperationException() throws Exception {
        // Arrange.
        this.exception.expect(UnsupportedOperationException.class);

        // Act.
        this.validator.validate(VALID_REQUEST);
    }

    @Test
    public void testValidateAndLoad_WithNullRequest_ThrowsVmidcBrokerValidationException() throws Exception {
        // Arrange.
        this.exception.expect(VmidcBrokerValidationException.class);
        this.exception.expectMessage("Null request or invalid Appliance Instance Name.");

        // Act.
        this.validator.validateAndLoad(null);
    }

    @Test
    public void testValidateAndLoad_WithNullSession_ThrowsNullPointerException() throws Exception {
        // Arrange.
        this.exception.expect(NullPointerException.class);
        TagVmRequestValidator nullInitializedValidator = new TagVmRequestValidator(null);

        // Act.
        nullInitializedValidator.validateAndLoad(VALID_REQUEST);
    }

    @Test
    public void testExec_WhenDaiNotFound_ThrowsVmidcBrokerValidationException() throws Exception {
        // Arrange.
        this.exception.expect(VmidcBrokerValidationException.class);
        this.exception.expectMessage("Appliance Instance Name '" + INVALID_APPLIANCE_NAME + "' not found.");

        // Act.
        this.validator.validateAndLoad(INVALID_REQUEST);
    }

    @Test
    public void testExec_WithValidRequest_ExpectsSuccess() throws Exception {
        // Arrange.
        this.em.getTransaction().begin();
        this.em.persist(DISTRIBUTED_APPLIANCE_INSTANCE.getVirtualSystem()
                .getVirtualizationConnector());
        this.em.persist(DISTRIBUTED_APPLIANCE_INSTANCE.getVirtualSystem()
                .getApplianceSoftwareVersion().getAppliance());
        this.em.persist(DISTRIBUTED_APPLIANCE_INSTANCE.getVirtualSystem()
                .getApplianceSoftwareVersion());
        this.em.persist(DISTRIBUTED_APPLIANCE_INSTANCE.getVirtualSystem()
                .getDistributedAppliance().getApplianceManagerConnector());
        this.em.persist(DISTRIBUTED_APPLIANCE_INSTANCE.getVirtualSystem()
                .getDistributedAppliance());
        this.em.persist(DISTRIBUTED_APPLIANCE_INSTANCE.getVirtualSystem());
        this.em.persist(DISTRIBUTED_APPLIANCE_INSTANCE);
        this.em.getTransaction().commit();

        // Act.
        DistributedApplianceInstance loadedDistributedApplianceInstance = this.validator.validateAndLoad(VALID_REQUEST);

        // Assert.
        Assert.assertEquals("The received DistributedApplianceInstance is different than expected.", DISTRIBUTED_APPLIANCE_INSTANCE, loadedDistributedApplianceInstance);
    }

    private static TagVmRequest createRequest(String applianceName) {
        TagVmRequest request = new TagVmRequest();
        request.setApplianceInstanceName(applianceName);
        return request;
    }
}