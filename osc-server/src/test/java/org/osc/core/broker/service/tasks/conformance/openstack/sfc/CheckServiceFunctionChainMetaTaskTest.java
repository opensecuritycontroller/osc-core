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

import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.when;
import static org.osc.core.broker.service.tasks.conformance.openstack.sfc.CheckServiceFunctionChainMetaTaskTestData.*;

import java.util.Arrays;
import java.util.Collection;

import javax.persistence.EntityManager;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.osc.core.broker.job.TaskGraph;
import org.osc.core.broker.model.entities.virtualization.SecurityGroup;
import org.osc.core.broker.model.plugin.ApiFactoryService;
import org.osc.core.broker.service.exceptions.VmidcException;
import org.osc.core.broker.service.test.InMemDB;
import org.osc.core.broker.util.TransactionalBroadcastUtil;
import org.osc.core.broker.util.db.DBConnectionManager;
import org.osc.core.broker.util.db.HibernateUtil;
import org.osc.core.test.util.TaskGraphHelper;
import org.osc.core.test.util.TestTransactionControl;
import org.osc.core.test.util.mockito.matchers.ElementIdMatcher;
import org.osc.sdk.controller.api.SdnRedirectionApi;
import org.osc.sdk.controller.element.NetworkElement;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.modules.junit4.PowerMockRunnerDelegate;

@RunWith(PowerMockRunner.class)
@PowerMockRunnerDelegate(value = Parameterized.class)
@PrepareForTest({ HibernateUtil.class })
public class CheckServiceFunctionChainMetaTaskTest {

	public EntityManager em;

	@Mock(answer = Answers.CALLS_REAL_METHODS)
	private TestTransactionControl txControl;

	@Mock
	DBConnectionManager dbMgr;

	@Mock
	TransactionalBroadcastUtil txBroadcastUtil;

	@Mock
	private ApiFactoryService apiFactoryServiceMock;

	@Mock
	private SdnRedirectionApi redirectionApi;

	@InjectMocks
	CheckServiceFunctionChainMetaTask task;

	private SecurityGroup sg;

	private TaskGraph expectedGraph;

	public CheckServiceFunctionChainMetaTaskTest(SecurityGroup sg, TaskGraph tg) {
		this.sg = sg;
		this.expectedGraph = tg;
	}

	@Before
	public void testInitialize() throws VmidcException, Exception {
		MockitoAnnotations.initMocks(this);
		this.em = InMemDB.getEntityManagerFactory().createEntityManager();

		this.txControl.setEntityManager(this.em);
		Mockito.when(this.dbMgr.getTransactionalEntityManager()).thenReturn(this.em);
		Mockito.when(this.dbMgr.getTransactionControl()).thenReturn(this.txControl);

		populateDatabase();

		getNetworkElements(PPG_NETWORK_ELEMENT_NOT_MATCHING, SECURITY_GROUP_SFC_BINDED_UPDATE_SFC);
	}

	@AfterClass
	public static void testTearDowm() {
		InMemDB.shutdown();
	}

	@Test
	public void testExecuteTransaction_WithVariousSecurityGroups_ExpectsCorrectTaskGraph() throws Exception {
		// Arrange.
		this.task.createServiceFunctionChainTask = new CreateServiceFunctionChainTask();
		this.task.updateServiceFunctionChainTask = new UpdateServiceFunctionChainTask();
		this.task.deleteServiceFunctionChainTask = new DeleteServiceFunctionChainTask();

		CheckServiceFunctionChainMetaTask task = this.task.create(this.sg);

		// Act.
		task.execute();

		// Assert.
		TaskGraphHelper.validateTaskGraph(task, this.expectedGraph);
	}

	private void registerNetworkRedirectionApi(SdnRedirectionApi redirectionApi, SecurityGroup sg) throws Exception {
		when(this.apiFactoryServiceMock.createNetworkRedirectionApi(sg.getVirtualizationConnector()))
				.thenReturn(redirectionApi);
	}

	private void getNetworkElements(NetworkElement networkElement, SecurityGroup sg) throws Exception {
		when(this.redirectionApi
				.getNetworkElements(argThat(new ElementIdMatcher<NetworkElement>(sg.getNetworkElementId()))))
						.thenReturn(Arrays.asList(networkElement));
		registerNetworkRedirectionApi(this.redirectionApi, sg);
	}

	@Parameters()
	public static Collection<Object[]> getTestData() {
		return Arrays.asList(new Object[][] {
				{ SECURITY_GROUP_SFC_BINDED_CREATE_SFC, createSFCGraph(SECURITY_GROUP_SFC_BINDED_CREATE_SFC) },
				{ SECURITY_GROUP_SFC_BINDED_UPDATE_SFC, updateSFCGraph(SECURITY_GROUP_SFC_BINDED_UPDATE_SFC) },
				{ SECURITY_GROUP_SFC_BINDED_DELETE_SFC, deleteSFCGraph(SECURITY_GROUP_SFC_BINDED_DELETE_SFC) }, });
	}

	private void populateDatabase() {
		if (!DB_POPULATED) {
			this.em.getTransaction().begin();
			persist(SECURITY_GROUP_SFC_BINDED_CREATE_SFC, this.em);
			persist(SECURITY_GROUP_SFC_BINDED_UPDATE_SFC, this.em);
			persist(SECURITY_GROUP_SFC_BINDED_DELETE_SFC, this.em);
			this.em.getTransaction().commit();
			DB_POPULATED = true;
		}
	}

}
