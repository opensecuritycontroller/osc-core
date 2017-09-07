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

import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManager;

import org.apache.commons.collections4.CollectionUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.ExpectedException;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.osc.core.broker.model.entities.appliance.TagEncapsulationType;
import org.osc.core.broker.model.entities.appliance.VirtualSystem;
import org.osc.core.broker.model.entities.virtualization.ServiceFunctionChain;
import org.osc.core.broker.model.entities.virtualization.VirtualizationConnector;
import org.osc.core.broker.model.plugin.ApiFactoryService;
import org.osc.core.broker.service.api.server.UserContextApi;
import org.osc.core.broker.service.validator.ServiceFunctionChainRequestValidator;
import org.osc.core.broker.util.TransactionalBroadcastUtil;
import org.osc.core.broker.util.db.DBConnectionManager;
import org.osc.core.common.virtualization.VirtualizationType;
import org.osc.core.test.util.TestTransactionControl;

public class BaseServiceFunctionChainRequestValidatorTest {
	@Rule
	public ExpectedException exception = ExpectedException.none();

	@Mock
	public EntityManager em;

	@Mock(answer = Answers.CALLS_REAL_METHODS)
	TestTransactionControl txControl;

	@Mock
	public TransactionalBroadcastUtil txBroadcastUtil;

	@Mock
	public UserContextApi userContext;

	@Mock
	public DBConnectionManager dbMgr;

	@InjectMocks
	public ServiceFunctionChainRequestValidator validator;

	@Before
	public void testInitialize() throws Exception {
		MockitoAnnotations.initMocks(this);

		this.txControl.setEntityManager(this.em);

		ApiFactoryService apiFactoryService = Mockito.mock(ApiFactoryService.class);

		this.validator = new ServiceFunctionChainRequestValidator().create(this.em);
		this.validator.apiFactoryService = apiFactoryService;

		Mockito.when(this.dbMgr.getTransactionalEntityManager()).thenReturn(this.em);
		Mockito.when(this.dbMgr.getTransactionControl()).thenReturn(this.txControl);
		
		Mockito.when(apiFactoryService.supportsNeutronSFC("Neutron-sfc")).thenReturn(true);
	}

	public VirtualizationConnector newVirtualConnector(String controllerType) {
		VirtualizationConnector vc;
		vc = new VirtualizationConnector();
		vc.setVirtualizationType(VirtualizationType.OPENSTACK);
		vc.setVirtualizationSoftwareVersion("vcSoftwareVersion");
		vc.setName("vcName");
		vc.setProviderIpAddress("127.0.0.1");
		vc.setProviderUsername("User-name");
		vc.setProviderPassword("********");
		vc.setControllerType(controllerType);
		return vc;
	}

	public VirtualizationConnector registerVirtualConnector(String controllerType, Long entityId) {
		VirtualizationConnector vc = newVirtualConnector(controllerType);
		vc.setId(entityId);
		when(this.em.find(VirtualizationConnector.class, vc.getId())).thenReturn(vc);
		return vc;
	}

	public VirtualSystem registerVirtualSystem(Long entityId, VirtualizationConnector vc) {
		VirtualSystem vs = newVirtualSystem(vc);
		vs.setId(entityId);
		when(this.em.find(VirtualSystem.class, vs.getId())).thenReturn(vs);
		return vs;
	}

	protected VirtualSystem newVirtualSystem(VirtualizationConnector vc) {
		VirtualSystem vs = new VirtualSystem(null);
		vs.setEncapsulationType(TagEncapsulationType.VLAN);
		vs.setVirtualizationConnector(vc);
		return vs;
	}

	protected ServiceFunctionChain registerServiceFunctionChain(String name, Long entityId, VirtualizationConnector vc,
			List<Long> vsIds) {
		List<VirtualSystem> virutalSystems = new ArrayList<VirtualSystem>();

		for (Long vsId : CollectionUtils.emptyIfNull(vsIds)) {
			VirtualSystem vs = newVirtualSystem(vc);
			vs.setId(vsId);
			virutalSystems.add(vs);
		}
		ServiceFunctionChain sfc = new ServiceFunctionChain(name, vc);
		sfc.setVirtualSystems(virutalSystems);
		sfc.setId(entityId);
		when(this.em.find(ServiceFunctionChain.class, sfc.getId())).thenReturn(sfc);
		return sfc;
	}

}
