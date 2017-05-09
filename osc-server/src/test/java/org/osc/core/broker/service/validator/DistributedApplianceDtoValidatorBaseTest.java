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

import static org.osc.core.broker.service.validator.DistributedApplianceDtoValidatorTestData.*;

import javax.persistence.EntityManager;

import org.junit.After;
import org.junit.Rule;
import org.junit.rules.ExpectedException;
import org.junit.runners.Parameterized;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.osc.core.broker.model.entities.appliance.Appliance;
import org.osc.core.broker.model.entities.appliance.ApplianceSoftwareVersion;
import org.osc.core.broker.model.entities.appliance.DistributedAppliance;
import org.osc.core.broker.model.entities.appliance.VirtualizationType;
import org.osc.core.broker.model.entities.management.ApplianceManagerConnector;
import org.osc.core.broker.model.entities.management.Domain;
import org.osc.core.broker.model.entities.virtualization.VirtualizationConnector;
import org.osc.core.broker.model.plugin.manager.ManagerApiFactory;
import org.osc.core.broker.model.plugin.manager.ManagerType;
import org.osc.core.broker.service.test.InMemDB;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.modules.junit4.PowerMockRunner;

import junitparams.JUnitParamsRunner;

/**
 * The base class for the {@link DistributedApplianceDtoValidator} unit tests.
 * The unit tests for {@link DistributedApplianceDtoValidator} have been split in two test classes.
 * The reason is because the runner {@link Parameterized} only supports data driven tests to be within the test class,
 * other non data driven tests need to go on a different test class.
 * We could optionally use the {@link JUnitParamsRunner}, which supports mixing data driven and non data driven
 * tests on the same class (as it was before) but this runner is not compatible with {@link PowerMockRunner} now needed for these tests.
 */
public class DistributedApplianceDtoValidatorBaseTest {
    @Mock
    protected EntityManager em;

    @Rule
    public ExpectedException exception = ExpectedException.none();

    protected DistributedAppliance mismatchingMcDa;

    protected DistributedApplianceDtoValidator validator;

    protected Appliance app;

    protected ApplianceManagerConnector amc;

    protected Domain domain;

    protected VirtualizationConnector vc;

    protected DistributedAppliance da;

    protected ApplianceSoftwareVersion asv;

    protected void testInitialize() throws Exception{
        MockitoAnnotations.initMocks(this);

        this.em = InMemDB.getEntityManagerFactory().createEntityManager();

        populateDatabase();

        this.validator = new DistributedApplianceDtoValidator(this.em);

        PowerMockito.mockStatic(ManagerApiFactory.class);
        ManagerType.addType(ManagerType.NSM.getValue());
        Mockito.when(ManagerApiFactory.syncsPolicyMapping(ManagerType.NSM)).thenReturn(true);
    }

    @After
    public void testTearDown() {
        InMemDB.shutdown();
    }

    private void populateDatabase() {
        this.em.getTransaction().begin();

        this.app = new Appliance();
        this.app.setManagerSoftwareVersion("mgrSoftwareVersion");
        this.app.setManagerType(ManagerType.NSM.getValue());
        this.app.setModel("fizzbuzz");

        this.em.persist(this.app);

        this.amc = new ApplianceManagerConnector();
        this.amc.setManagerType(ManagerType.NSM.getValue());
        this.amc.setIpAddress("127.0.0.1");
        this.amc.setName(MC_NAME_EXISTING_MC);
        this.amc.setServiceType("foobar");

        this.em.persist(this.amc);

        this.domain = new Domain(this.amc);
        this.domain.setName("domainName");

        this.em.persist(this.domain);

        this.vc = new VirtualizationConnector();
        this.vc.setVirtualizationType(VirtualizationType.OPENSTACK);
        this.vc.setVirtualizationSoftwareVersion("vcSoftwareVersion");
        this.vc.setName(VC_NAME_OPENSTACK);
        this.vc.setProviderIpAddress("127.0.0.1");
        this.vc.setProviderUsername("Natasha");
        this.vc.setProviderPassword("********");

        this.em.persist(this.vc);

        this.asv = new ApplianceSoftwareVersion(this.app);
        this.asv.setApplianceSoftwareVersion(SW_VERSION_EXISTING_VC);
        this.asv.setImageUrl("bar");
        this.asv.setVirtualizarionSoftwareVersion(this.vc.getVirtualizationSoftwareVersion());
        this.asv.setVirtualizationType(this.vc.getVirtualizationType());

        this.em.persist(this.asv);

        this.da = new DistributedAppliance(this.amc);
        this.da.setName(DA_NAME_EXISTING_DA);
        this.da.setApplianceVersion(this.asv.getApplianceSoftwareVersion());
        this.da.setAppliance(this.app);

        this.em.persist(this.da);

        this.em.getTransaction().commit();
    }
}
