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
package org.osc.core.broker.service.tasks.conformance.openstack.sfc;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.osc.core.broker.service.tasks.conformance.openstack.sfc.CreateServiceFunctionChainTaskTestData.*;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.runners.MockitoJUnitRunner;
import org.osc.core.broker.model.entities.virtualization.SecurityGroup;
import org.osc.core.broker.model.plugin.ApiFactoryService;
import org.osc.core.broker.model.sdn.PortPairGroupNetworkElementImpl;
import org.osc.core.broker.service.exceptions.VmidcException;
import org.osc.core.broker.service.persistence.SecurityGroupEntityMgr;
import org.osc.core.broker.service.test.InMemDB;
import org.osc.core.broker.util.TransactionalBroadcastUtil;
import org.osc.core.broker.util.db.DBConnectionManager;
import org.osc.core.test.util.TestTransactionControl;
import org.osc.sdk.controller.api.SdnRedirectionApi;
import org.osc.sdk.controller.element.NetworkElement;

@RunWith(MockitoJUnitRunner.class)
public class CreateServiceFunctionChainTaskTest {

	@Rule
	public ExpectedException exception = ExpectedException.none();

	@Mock
	private EntityManager em;

	@Mock
	private EntityTransaction tx;

	@Mock(answer = Answers.CALLS_REAL_METHODS)
	private TestTransactionControl txControl;

	@Mock
	private DBConnectionManager dbMgr;

	@Mock
	private TransactionalBroadcastUtil txBroadcastUtil;

	@Mock
	private ApiFactoryService apiFactoryServiceMock;

	@Mock
	private SdnRedirectionApi sdnApi;

	@InjectMocks
	private CreateServiceFunctionChainTask task;

	private List<NetworkElement> portPairGroup;

	@Before
	public void testInitialize() throws VmidcException {
		MockitoAnnotations.initMocks(this);
		this.em = InMemDB.getEntityManagerFactory().createEntityManager();

		this.txControl.setEntityManager(this.em);
		Mockito.when(this.dbMgr.getTransactionalEntityManager()).thenReturn(this.em);
		Mockito.when(this.dbMgr.getTransactionControl()).thenReturn(this.txControl);

		populateDatabase();
	}

	@AfterClass
	public static void testTearDowm() {
		InMemDB.shutdown();
	}

	@Test
	public void testExecute_WithSecurityGroupSFCBound_ExpectCreate() throws Exception {
		// Arrange.
		CreateServiceFunctionChainTask task = this.task.create(SECURITY_GROUP_SFC_BINDED_EXPECT_CREATE,
				this.portPairGroup);
		registerNetworkElement(SECURITY_GROUP_SFC_BINDED_EXPECT_CREATE,
				new PortPairGroupNetworkElementImpl(SECURITY_GROUP_SFC_BINDED_EXPECT_CREATE.getNetworkElementId()), null);

		// Act.
		String taskName = task.getName();

		// Assert
		assertEquals(SecurityGroupEntityMgr.findById(this.em, SECURITY_GROUP_SFC_BINDED_EXPECT_CREATE.getId())
				.getNetworkElementId(), SECURITY_GROUP_SFC_BINDED_EXPECT_CREATE.getNetworkElementId());
		assertEquals(String.format("Creating Service Function Chain '%s' for Security Group '%s' under Project '%s'",
				SECURITY_GROUP_SFC_BINDED_EXPECT_CREATE.getServiceFunctionChain().getName(),
				SECURITY_GROUP_SFC_BINDED_EXPECT_CREATE.getName(),
				SECURITY_GROUP_SFC_BINDED_EXPECT_CREATE.getProjectName()), taskName);
	}

	@Test
	public void testExecute_WithSecurityGroupBoundToSFCAlreadyBound_ExpectUpdateSFCElementId() throws Exception {
		// Arrange.
		CreateServiceFunctionChainTask task = this.task.create(NEW_SECURITY_GROUP_SAME_SFC_BINDED_UPDATE_ELEMENT_ID,
				this.portPairGroup);

		// Act.
		task.execute();

		// Assert
		assertEquals(SecurityGroupEntityMgr
				.findById(this.em, NEW_SECURITY_GROUP_SAME_SFC_BINDED_UPDATE_ELEMENT_ID.getId()).getNetworkElementId(),
				SECURITY_GROUP_SFC_BINDED.getNetworkElementId());
	}

	@Test
	public void testExecute_WhenSDNReturnsNullNetworkElement_ThrowsUnhandledException() throws Exception {
		// Arrange.
		CreateServiceFunctionChainTask task = this.task.create(SECURITY_GROUP_SFC, this.portPairGroup);
		registerNetworkElement(SECURITY_GROUP_SFC, null, new IllegalStateException());

		this.exception.expect(IllegalStateException.class);

		// Act.
		task.execute();

		// Assert
		Mockito.verify(this.em, Mockito.never()).merge(any());
	}

	private void populateDatabase() {
		if (!DB_POPULATED) {
			this.em.getTransaction().begin();
			persist(SECURITY_GROUP_SFC_BINDED, this.em);
			persist(SECURITY_GROUP_SFC_BINDED_EXPECT_CREATE, this.em);
			persist(NEW_SECURITY_GROUP_SAME_SFC_BINDED_UPDATE_ELEMENT_ID, this.em);
			persist(SECURITY_GROUP_SFC, this.em);
			this.em.getTransaction().commit();
			DB_POPULATED = true;
		}
	}

	private void registerNetworkElement(SecurityGroup sg, NetworkElement networkElement, Exception e) throws Exception {
		if (e != null) {
			doThrow(new IllegalStateException()).when(this.sdnApi).registerNetworkElement(any());
		} else {
			when(this.sdnApi.registerNetworkElement(any())).thenReturn(networkElement);
		}
		when(this.apiFactoryServiceMock.createNetworkRedirectionApi(sg.getVirtualizationConnector()))
				.thenReturn(this.sdnApi);
	}

}
