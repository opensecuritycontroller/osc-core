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
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.osc.core.broker.service.tasks.conformance.openstack.sfc.DeleteServiceFunctionChainTaskTestData.*;

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
import org.osc.core.broker.model.sdn.NetworkElementImpl;
import org.osc.core.broker.service.exceptions.VmidcException;
import org.osc.core.broker.service.persistence.SecurityGroupEntityMgr;
import org.osc.core.broker.service.test.InMemDB;
import org.osc.core.broker.util.TransactionalBroadcastUtil;
import org.osc.core.broker.util.db.DBConnectionManager;
import org.osc.core.test.util.TestTransactionControl;
import org.osc.core.test.util.mockito.matchers.ElementIdMatcher;
import org.osc.sdk.controller.api.SdnRedirectionApi;
import org.osc.sdk.controller.element.NetworkElement;

@RunWith(MockitoJUnitRunner.class)
public class DeleteServiceFunctionChainTaskTest {

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
	private DeleteServiceFunctionChainTask task;

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
	public void testExecute_WithSFCBoundAndSGUnbound_NullsSGNetworkElementId() throws Exception {
		// Arrange.
		DeleteServiceFunctionChainTask deleteTask = this.task.create(SG_SFC_UNBINDED);

		// Act.
		deleteTask.execute();

		// Assert.
		assertEquals(SecurityGroupEntityMgr.findById(this.em, SG_SFC_BINDED.getId()).getNetworkElementId(),
				SG_SFC_BINDED.getNetworkElementId());

		assertEquals(
				SecurityGroupEntityMgr.findById(this.em, SG_SFC_UNBINDED.getId()).getNetworkElementId(),
				null);
	}

	@Test
	public void testExecute_WhenDeleteNetworkElementSucceeds_DeleteSFCAndNullsSGNetworkElementId() throws Exception {
		// Arrange
		DeleteServiceFunctionChainTask deleteTask = this.task.create(SG_SFC_UNBIND_DELETE_SFC);

		mockDeleteNetworkElement(SG_SFC_UNBIND_DELETE_SFC,
				new NetworkElementImpl(SG_SFC_UNBIND_DELETE_SFC.getNetworkElementId()), null);

		// Act.
		deleteTask.execute();

		// Assert.
		assertEquals(
				SecurityGroupEntityMgr.findById(this.em, SG_SFC_UNBIND_DELETE_SFC.getId()).getNetworkElementId(),
				null);
	}

	@Test
	public void testExecute_WhenDeleteNetworkElementFails_ThrowsUnhandledException() throws Exception {
		// Arrange
		DeleteServiceFunctionChainTask deleteTask = this.task.create(SG_SFC_FAIL_ELEMENT_EXISTS);

		mockDeleteNetworkElement(SG_SFC_FAIL_ELEMENT_EXISTS,
				new NetworkElementImpl(SG_SFC_FAIL_ELEMENT_EXISTS.getNetworkElementId()),
				new IllegalStateException());

		this.exception.expect(IllegalStateException.class);

		// Act.
		deleteTask.execute();

		// Assert.
		assertEquals(
				SecurityGroupEntityMgr.findById(this.em, SG_SFC_FAIL_ELEMENT_EXISTS.getId()).getNetworkElementId(),
				SG_SFC_FAIL_ELEMENT_EXISTS.getNetworkElementId());
	}

	@Test
	public void testGetName_WithSecurityGroupSFCId_ExpectCorrect() {
		// Arrange
		DeleteServiceFunctionChainTask deleteTask = this.task.create(SG_SFC_VALID);

		// Act.
		String taskName = deleteTask.getName();

		// Assert
		assertEquals(String.format("Deleting Service Function Chain '%s' for Security Group '%s' under Project '%s'",
				SG_SFC_VALID.getNetworkElementId(), SG_SFC_VALID.getName(),
				SG_SFC_VALID.getProjectName()), taskName);
	}

	private void populateDatabase() {
		if (!DB_POPULATED) {
			this.em.getTransaction().begin();
			persist(SG_SFC_BINDED, this.em);
			persist(SG_SFC_UNBINDED, this.em);
			persist(SG_SFC_UNBIND_DELETE_SFC, this.em);
			persist(SG_SFC_FAIL_ELEMENT_EXISTS, this.em);
			persist(SG_SFC_VALID, this.em);
			this.em.getTransaction().commit();
			DB_POPULATED = true;
		}
	}

	private void mockDeleteNetworkElement(SecurityGroup sg, NetworkElement networkElement, Exception e) throws Exception {
		if (e != null) {

			doThrow(e).when(this.sdnApi)
					.deleteNetworkElement(argThat(new ElementIdMatcher<NetworkElement>(sg.getNetworkElementId())));
		}

		when(this.apiFactoryServiceMock.createNetworkRedirectionApi(sg.getVirtualizationConnector()))
				.thenReturn(this.sdnApi);
	}

}
