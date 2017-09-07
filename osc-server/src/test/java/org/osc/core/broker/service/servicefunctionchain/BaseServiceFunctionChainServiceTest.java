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
package org.osc.core.broker.service.servicefunctionchain;

import javax.persistence.EntityManager;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.ExpectedException;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.osc.core.broker.model.entities.appliance.Appliance;
import org.osc.core.broker.model.entities.appliance.ApplianceSoftwareVersion;
import org.osc.core.broker.model.entities.appliance.DistributedAppliance;
import org.osc.core.broker.model.entities.appliance.VirtualSystem;
import org.osc.core.broker.model.entities.management.ApplianceManagerConnector;
import org.osc.core.broker.model.entities.management.Domain;
import org.osc.core.broker.model.entities.virtualization.ServiceFunctionChain;
import org.osc.core.broker.model.entities.virtualization.VirtualizationConnector;
import org.osc.core.broker.model.plugin.ApiFactoryService;
import org.osc.core.broker.service.api.server.UserContextApi;
import org.osc.core.broker.service.test.InMemDB;
import org.osc.core.broker.service.validator.ServiceFunctionChainRequestValidator;
import org.osc.core.broker.util.TransactionalBroadcastUtil;
import org.osc.core.broker.util.db.DBConnectionManager;
import org.osc.core.common.virtualization.VirtualizationType;
import org.osc.core.test.util.TestTransactionControl;

public class BaseServiceFunctionChainServiceTest {

	@Rule
	public ExpectedException exception = ExpectedException.none();

	public EntityManager em;

	@Mock(answer = Answers.CALLS_REAL_METHODS)
	protected TestTransactionControl txControl;

	@Mock
	protected TransactionalBroadcastUtil txBroadcastUtil;

	@Mock
	protected ServiceFunctionChainRequestValidator validatorMock;

	@Mock
	protected UserContextApi userContext;

	@Mock
	protected DBConnectionManager dbMgr;

	protected VirtualizationConnector vc;
	protected VirtualSystem vs;
	protected VirtualSystem vs1;
	protected ServiceFunctionChain sfc;

	@Before
	public void testInitialize() throws Exception {
		MockitoAnnotations.initMocks(this);

		this.em = InMemDB.getEntityManagerFactory().createEntityManager();

		this.txControl.setEntityManager(this.em);
		
		 ApiFactoryService apiFactoryService = Mockito.mock(ApiFactoryService.class);
		 
		 this.validatorMock.apiFactoryService = apiFactoryService;

		populateDatabase();

		Mockito.when(this.dbMgr.getTransactionalEntityManager()).thenReturn(this.em);
		Mockito.when(this.dbMgr.getTransactionControl()).thenReturn(this.txControl);
		
		Mockito.when(apiFactoryService.supportsNeutronSFC("Neutron-sfc")).thenReturn(true);
		
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

		this.em.getTransaction().commit();
	}

}
