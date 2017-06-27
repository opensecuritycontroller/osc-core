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

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.osc.core.broker.model.entities.virtualization.SecurityGroup;
import org.osc.core.broker.model.entities.virtualization.VirtualizationConnector;
import org.osc.core.broker.model.plugin.sdncontroller.SdnControllerApiFactory;
import org.osc.core.broker.service.tasks.conformance.openstack.securitygroup.element.PortGroup;
import org.osc.core.broker.util.db.HibernateUtil;
import org.osc.core.test.util.TestTransactionControl;
import org.osc.sdk.controller.api.SdnRedirectionApi;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ HibernateUtil.class, SdnControllerApiFactory.class })
public class DeletePortGroupTaskTest {
	@Mock
	protected EntityManager em;
	@Mock
	protected EntityTransaction tx;

	@Mock(answer = Answers.CALLS_REAL_METHODS)
	TestTransactionControl txControl;

	@Rule
	public ExpectedException exception = ExpectedException.none();

	@Before
	public void testInitialize() throws Exception {
		MockitoAnnotations.initMocks(this);
		when(this.em.getTransaction()).thenReturn(this.tx);

		this.txControl.setEntityManager(this.em);

		PowerMockito.mockStatic(HibernateUtil.class);
		when(HibernateUtil.getTransactionalEntityManager()).thenReturn(this.em);
		when(HibernateUtil.getTransactionControl()).thenReturn(this.txControl);
	}

	@Test
	public void testExecute_WhenCreateNetworkRedirectionApiFails_ThrowsTheUnhandledException() throws Exception {
		// Arrange.
		SecurityGroup sg = createSG("SG", 1L, null);

		PowerMockito.spy(SdnControllerApiFactory.class);
		PowerMockito.doThrow(new IllegalStateException()).when(SdnControllerApiFactory.class,
				"createNetworkRedirectionApi", sg.getVirtualizationConnector());

		this.exception.expect(IllegalStateException.class);

		DeletePortGroupTask task = new DeletePortGroupTask(sg, null);

		// Act.
		task.execute();
	}

	@Test
	public void testExecute_WhenDeleteNetworkElementFails_ThrowsTheUnhandledException() throws Exception {
		// Arrange.
		SecurityGroup sg = createSG("SG", 1L, null);
		PortGroup portGroup = createPortGroup(sg.getNetworkElementId(), sg.getName());

		SdnRedirectionApi redirectionApi = mockDeleteNetworkElement(portGroup, new IllegalStateException());

		registerNetworkRedirectionApi(redirectionApi, sg.getVirtualizationConnector());

		this.exception.expect(IllegalStateException.class);

		DeletePortGroupTask task = new DeletePortGroupTask(sg, portGroup);

		// Act.
		task.execute();
	}

	@Test
	public void testExecute_WhenDeleteNetworkElementSucceeds_ExecutionFinishes() throws Exception {
		// Arrange.
		SecurityGroup sg = createSG("SG", 1L, null);
		PortGroup portGroup = createPortGroup(sg.getNetworkElementId(), sg.getName());

		SdnRedirectionApi redirectionApi = mockDeleteNetworkElement(portGroup, null);

		registerNetworkRedirectionApi(redirectionApi, sg.getVirtualizationConnector());

		DeletePortGroupTask task = new DeletePortGroupTask(sg, portGroup);

		// Act.
		task.execute();

		// Assert.
		verify(redirectionApi, times(1)).deleteNetworkElement(portGroup);

	}

	private SecurityGroup createSG(String name, Long sgId, String netElementId) {
		VirtualizationConnector vc = createVC(name);

		SecurityGroup sg = new SecurityGroup(vc, UUID.randomUUID().toString(), name + "_tenant");
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
		PortGroup portGroup = new PortGroup() {
			@Override
			public boolean equals(Object obj) {
				if (obj == null || !(obj instanceof PortGroup)) {
					return false;
				}

				PortGroup portGroup = (PortGroup) obj;

				return portGroup.getElementId().equals(getElementId()) && portGroup.getParentId().equals(getParentId());
			}
		};

		portGroup.setPortGroupId(id);
		portGroup.setParentId(parentId);

		return portGroup;
	}

	private void registerNetworkRedirectionApi(SdnRedirectionApi redirectionApi, VirtualizationConnector vc)
			throws Exception {
		PowerMockito.spy(SdnControllerApiFactory.class);
		PowerMockito.doReturn(redirectionApi).when(SdnControllerApiFactory.class, "createNetworkRedirectionApi", vc);

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
