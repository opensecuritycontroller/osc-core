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
package org.osc.core.broker.service.tasks.conformance.openstack.securitygroup;

import static org.mockito.Mockito.*;

import java.util.UUID;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;

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
import org.osc.core.broker.model.entities.virtualization.SecurityGroup;
import org.osc.core.broker.model.entities.virtualization.VirtualizationConnector;
import org.osc.core.broker.model.plugin.ApiFactoryService;
import org.osc.core.broker.service.tasks.conformance.openstack.securitygroup.element.PortGroup;
import org.osc.core.broker.util.TransactionalBroadcastUtil;
import org.osc.core.broker.util.db.DBConnectionManager;
import org.osc.core.test.util.TestTransactionControl;
import org.osc.sdk.controller.api.SdnRedirectionApi;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
public class DeletePortGroupTaskTest {

	@Mock
	protected EntityManager em;
	@Mock
	protected EntityTransaction tx;

	@Mock
	DBConnectionManager dbMgr;

	@Mock
	TransactionalBroadcastUtil txBroadcastUtil;

	@Mock
	public ApiFactoryService apiFactoryServiceMock;

	@InjectMocks
	DeletePortGroupTask factoryTask;

	@Mock(answer = Answers.CALLS_REAL_METHODS)
	TestTransactionControl txControl;

	@Rule
	public ExpectedException exception = ExpectedException.none();

	@Before
	public void testInitialize() throws Exception {
		MockitoAnnotations.initMocks(this);
		when(this.em.getTransaction()).thenReturn(this.tx);

		this.txControl.setEntityManager(this.em);

		Mockito.when(this.dbMgr.getTransactionalEntityManager()).thenReturn(this.em);
		Mockito.when(this.dbMgr.getTransactionControl()).thenReturn(this.txControl);
	}

	@Test
	public void testExecute_WhenCreateNetworkRedirectionApiFails_ThrowsTheUnhandledException() throws Exception {
		// Arrange.
		SecurityGroup sg = registerSecurityGroup("SG", 1L, null);

		when(this.apiFactoryServiceMock.createNetworkRedirectionApi(sg.getVirtualizationConnector()))
				.thenThrow(new IllegalStateException());

		this.exception.expect(IllegalStateException.class);

		DeletePortGroupTask task = this.factoryTask.create(sg, null);

		// Act.
		task.execute();
	}

	@Test
	public void testExecute_WhenDeleteNetworkElementFails_ThrowsTheUnhandledException() throws Exception {
		// Arrange.
		SecurityGroup sg = registerSecurityGroup("SG", 1L, null);
		PortGroup portGroup = createPortGroup(sg.getNetworkElementId(), sg.getName());

		SdnRedirectionApi redirectionApi = mockDeleteNetworkElement(portGroup, new IllegalStateException());

		registerNetworkRedirectionApi(redirectionApi, sg.getVirtualizationConnector());

		this.exception.expect(IllegalStateException.class);

		DeletePortGroupTask task = this.factoryTask.create(sg, portGroup);

		// Act.
		task.execute();
	}

	@Test
	public void testExecute_WhenDeleteNetworkElementSucceeds_ExecutionFinishes() throws Exception {
		// Arrange.
		SecurityGroup sg = registerSecurityGroup("SG", 1L, null);
		PortGroup portGroup = createPortGroup(sg.getNetworkElementId(), sg.getName());

		SdnRedirectionApi redirectionApi = mockDeleteNetworkElement(portGroup, null);

		registerNetworkRedirectionApi(redirectionApi, sg.getVirtualizationConnector());

		DeletePortGroupTask task = this.factoryTask.create(sg, portGroup);

		// Act.
		task.execute();

		// Assert.
		verify(redirectionApi, times(1)).deleteNetworkElement(portGroup);

	}

	private SecurityGroup registerSecurityGroup(String name, Long sgId, String netElementId) {
		VirtualizationConnector vc = createVC(name);

		SecurityGroup sg = new SecurityGroup(vc, UUID.randomUUID().toString(), name + "_project");
		sg.setName(name + "_SG");
		sg.setNetworkElementId(netElementId);

		when(this.em.find(SecurityGroup.class, sg.getId())).thenReturn(sg);
		return sg;
	}

	private static VirtualizationConnector createVC(String name) {
		VirtualizationConnector vc = new VirtualizationConnector();
		vc.setId(1L);
		vc.setName(name + "_vc");

		return vc;
	}

	private static PortGroup createPortGroup(String id, String parentId) {
		PortGroup portGroup = new PortGroup();

		portGroup.setPortGroupId(id);
		portGroup.setParentId(parentId);

		return portGroup;
	}

	private void registerNetworkRedirectionApi(SdnRedirectionApi redirectionApi, VirtualizationConnector vc)
			throws Exception {
		when(this.apiFactoryServiceMock.createNetworkRedirectionApi(vc)).thenReturn(redirectionApi);
	}

	private SdnRedirectionApi mockDeleteNetworkElement(PortGroup portGroup, Exception e) throws Exception {
		SdnRedirectionApi redirectionApi = mock(SdnRedirectionApi.class);
		if (e != null) {
			doThrow(e).when(redirectionApi).deleteNetworkElement(portGroup);
		} else {
			doNothing().when(redirectionApi).deleteNetworkElement(portGroup);
		}

		return redirectionApi;
	}

}