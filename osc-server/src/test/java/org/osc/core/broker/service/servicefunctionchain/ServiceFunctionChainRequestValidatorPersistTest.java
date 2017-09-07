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
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.runners.MockitoJUnitRunner;
import org.osc.core.broker.model.entities.virtualization.ServiceFunctionChain;
import org.osc.core.broker.model.entities.virtualization.VirtualizationConnector;
import org.osc.core.broker.model.plugin.ApiFactoryService;
import org.osc.core.broker.service.api.server.UserContextApi;
import org.osc.core.broker.service.dto.BaseDto;
import org.osc.core.broker.service.exceptions.VmidcBrokerValidationException;
import org.osc.core.broker.service.request.AddOrUpdateServiceFunctionChainRequest;
import org.osc.core.broker.service.test.InMemDB;
import org.osc.core.broker.service.validator.ServiceFunctionChainRequestValidator;
import org.osc.core.broker.util.TransactionalBroadcastUtil;
import org.osc.core.broker.util.db.DBConnectionManager;
import org.osc.core.common.virtualization.VirtualizationType;
import org.osc.core.test.util.TestTransactionControl;

@RunWith(MockitoJUnitRunner.class)
public class ServiceFunctionChainRequestValidatorPersistTest {
	@Rule
	public ExpectedException exception = ExpectedException.none();

	@Mock
	private EntityManager em;

	@Mock(answer = Answers.CALLS_REAL_METHODS)
	TestTransactionControl txControl;

	@Mock
	private TransactionalBroadcastUtil txBroadcastUtil;

	@Mock
	private UserContextApi userContext;

	@Mock
	private DBConnectionManager dbMgr;

	private ServiceFunctionChainRequestValidator validator;

	protected VirtualizationConnector vc;

	protected ServiceFunctionChain sfc;

	@Before
	public void testInitialize() throws Exception {
		MockitoAnnotations.initMocks(this);

		this.em = InMemDB.getEntityManagerFactory().createEntityManager();

		populateDatabase();

		ApiFactoryService apiFactoryService = Mockito.mock(ApiFactoryService.class);

		this.validator = new ServiceFunctionChainRequestValidator().create(this.em);
		this.validator.apiFactoryService = apiFactoryService;

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

		this.vc = new VirtualizationConnector();
		this.vc.setVirtualizationType(VirtualizationType.OPENSTACK);
		this.vc.setVirtualizationSoftwareVersion("vcSoftwareVersion");
		this.vc.setName("vc-1");
		this.vc.setProviderIpAddress("127.0.0.1");
		this.vc.setProviderUsername("your-name");
		this.vc.setProviderPassword("********");
		this.vc.setControllerType("Neutron-sfc");	
		this.em.persist(this.vc);

		this.sfc = new ServiceFunctionChain("sfc-1", this.vc);
		this.em.persist(this.sfc);

		this.em.getTransaction().commit();
	}

	@Test
	public void testValidate_WhenSfcNameAlreadyExist_ThrowsVmidcBrokerValidationException() throws Exception {
		AddOrUpdateServiceFunctionChainRequest request = new AddOrUpdateServiceFunctionChainRequest();
		BaseDto dto = new BaseDto();
		request.setName("sfc-1");
		dto.setParentId(this.vc.getId());
		request.setDto(dto);
		// Arrange.
		this.exception.expect(VmidcBrokerValidationException.class);
		this.exception.expectMessage("Service Function Name: " + request.getName() + " already exists.");

		// Act.
		this.validator.validate(request);
	}

	@Test
	public void testValidate_WhenValidationIsSuccessful_ValidationSucceeds() throws Exception {
		AddOrUpdateServiceFunctionChainRequest request = new AddOrUpdateServiceFunctionChainRequest();
		BaseDto dto = new BaseDto();
		request.setName("sfc-2");
		dto.setParentId(this.vc.getId());
		request.setDto(dto);

		// Act.
		this.validator.validate(request);
	}

}
