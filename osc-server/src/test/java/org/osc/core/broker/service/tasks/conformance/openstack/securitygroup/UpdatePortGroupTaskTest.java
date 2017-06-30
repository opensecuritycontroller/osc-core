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

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.osc.core.broker.model.entities.virtualization.SecurityGroup;
import org.osc.core.broker.model.entities.virtualization.SecurityGroupMember;
import org.osc.core.broker.model.entities.virtualization.VirtualizationConnector;
import org.osc.core.broker.model.entities.virtualization.openstack.OsProtectionEntity;
import org.osc.core.broker.model.entities.virtualization.openstack.VM;
import org.osc.core.broker.model.entities.virtualization.openstack.VMPort;
import org.osc.core.broker.model.plugin.ApiFactoryService;
import org.osc.core.broker.model.plugin.sdncontroller.NetworkElementImpl;
import org.osc.core.broker.service.tasks.conformance.openstack.deploymentspec.OpenstackUtil;
import org.osc.core.broker.service.tasks.conformance.openstack.securitygroup.element.PortGroup;
import org.osc.core.broker.util.TransactionalBroadcastUtil;
import org.osc.core.broker.util.db.DBConnectionManager;
import org.osc.core.test.util.TestTransactionControl;
import org.osc.sdk.controller.api.SdnRedirectionApi;
import org.osc.sdk.controller.element.NetworkElement;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ OpenstackUtil.class })
public class UpdatePortGroupTaskTest {
	public static final String OPENSTACK_ID = "openstack_id";

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
	UpdatePortGroupTask factoryTask;

	@Mock(answer = Answers.CALLS_REAL_METHODS)
	TestTransactionControl txControl;

	@Rule
	public ExpectedException exception = ExpectedException.none();

	@Captor
	private ArgumentCaptor<List<NetworkElement>> networkElementCaptor;

	@Captor
	private ArgumentCaptor<List<NetworkElement>> domainIdNetworkElementCaptor;

	@Captor
	private ArgumentCaptor<PortGroup> portGroupCaptor;

	private List<VMPort> vmProtectedPorts;

	@Before
	public void testInitialize() throws Exception {
		MockitoAnnotations.initMocks(this);
		when(this.em.getTransaction()).thenReturn(this.tx);

		this.txControl.setEntityManager(this.em);

		Mockito.when(this.dbMgr.getTransactionalEntityManager()).thenReturn(this.em);
		Mockito.when(this.dbMgr.getTransactionControl()).thenReturn(this.txControl);

		PowerMockito.mockStatic(OpenstackUtil.class);

		this.vmProtectedPorts = new ArrayList<>();
	}

	@Test
	public void testExecute_WhenElementIdMatches_UpdateNotNeeded() throws Exception {
		// Arrange.
		SecurityGroup sg = registerSecurityGroup(1L, "tenantId", "tenantName", 1L, "sgName");
		sg.addSecurityGroupMember(newSGMWithPort(1L, OPENSTACK_ID));
		PortGroup portGroup = createPortGroup(OPENSTACK_ID);

		ostRegisterPorts(sg);

		String domainId = UUID.randomUUID().toString();
		registerDomain(domainId, sg);

		NetworkElement ne = createNetworkElement(OPENSTACK_ID);

		SdnRedirectionApi redirectionApi = registerNetworkElement(portGroup, ne);
		registerNetworkRedirectionApi(redirectionApi, sg.getVirtualizationConnector());

		UpdatePortGroupTask task = this.factoryTask.create(sg, portGroup);

		// Act.
		task.execute();

		// Assert.
		verify(this.em, Mockito.never()).merge(any());
	}

	@Test
	public void testExecute_WhenElementIdMismatches_UpdateDone() throws Exception {
		// Arrange.
		SecurityGroup sg = registerSecurityGroup(1L, "tenantId", "tenantName", 1L, "sgName");
		sg.addSecurityGroupMember(newSGMWithPort(1L, OPENSTACK_ID));
		PortGroup portGroup = createPortGroup("different_openstackId");

		List<NetworkElement> neList = ostRegisterPorts(sg);

		String domainId = UUID.randomUUID().toString();
		registerDomain(domainId, sg);

		NetworkElement ne = createNetworkElement(OPENSTACK_ID);

		SdnRedirectionApi redirectionApi = registerNetworkElement(portGroup, ne);
		registerNetworkRedirectionApi(redirectionApi, sg.getVirtualizationConnector());

		UpdatePortGroupTask task = this.factoryTask.create(sg, portGroup);

		// Act.
		task.execute();

		// Assert.
		verify(this.em, Mockito.times(1)).merge(sg);
		Assert.assertEquals("Network element security group element id mismatch", ne.getElementId(),
				sg.getNetworkElementId());
		assertNetworkElementsParentIdWithDomainId(neList, domainId);
	}

	@Test
	public void testExecute_WhenDomainIsNotFound_ThrowsException() throws Exception {
		// Arrange.
		SecurityGroup sg = registerSecurityGroup(1L, "tenantId", "tenantName", 1L, "sgName");
		sg.addSecurityGroupMember(newSGMWithPort(1L, OPENSTACK_ID));
		PortGroup portGroup = createPortGroup(OPENSTACK_ID);

		ostRegisterPorts(sg);

		// domain id null
		String domainId = null;
		registerDomain(domainId, sg);

		this.exception.expect(Exception.class);
		this.exception.expectMessage(
				String.format("Failed to retrieve domainId for given tenant: '%s' and Security Group: '%s",
						sg.getTenantName(), sg.getName()));

		UpdatePortGroupTask task = this.factoryTask.create(sg, portGroup);

		// Act.
		task.execute();

		// Assert.
		verify(this.em, Mockito.never()).merge(any());
	}

	@Test
	public void testExecute_WhenPortGroupIsNotFound_ThrowsException() throws Exception {
		// Arrange.
		SecurityGroup sg = registerSecurityGroup(1L, "tenantId", "tenantName", 1L, "sgName");
		sg.addSecurityGroupMember(newSGMWithPort(1L, ""));
		PortGroup portGroup = createPortGroup(OPENSTACK_ID);

		ostRegisterPorts(sg);

		String domainId = UUID.randomUUID().toString();
		registerDomain(domainId, sg);

		// network element/portGroup null
		NetworkElement ne = null;
		SdnRedirectionApi redirectionApi = registerNetworkElement(portGroup, ne);
		registerNetworkRedirectionApi(redirectionApi, sg.getVirtualizationConnector());

		this.exception.expect(Exception.class);
		this.exception.expectMessage(String.format("Failed to update Port Group : '%s'", portGroup.getElementId()));

		UpdatePortGroupTask task = this.factoryTask.create(sg, portGroup);

		// Act.
		task.execute();

		// Assert.
		verify(this.em, Mockito.never()).merge(any());
	}

	private SecurityGroupMember newSGMWithPort(Long sgmId, String openstackId) {
		VMPort port = null;
		OsProtectionEntity protectionEntity;

		protectionEntity = new VM("region", UUID.randomUUID().toString(), "name");
		port = newVMPort((VM) protectionEntity, openstackId);

		this.vmProtectedPorts.add(port);
		return newSGM(protectionEntity, sgmId);
	}

	private SecurityGroupMember newSGM(OsProtectionEntity protectionEntity, Long sgmId) {
		// TODO emanoel: Remove this mock once the SGM is no longer kept in a
		// TreeSet in the SGM.
		SecurityGroupMember sgm = Mockito.spy(new SecurityGroupMember(protectionEntity));
		Mockito.doReturn(-1).when(sgm).compareTo(Mockito.any());
		sgm.setId(sgmId);

		return sgm;
	}

	private VMPort newVMPort(VM vm, String openstackId) {
		return new VMPort(vm, "mac-address" + UUID.randomUUID().toString(), UUID.randomUUID().toString(), openstackId,
				null);
	}

	private SecurityGroup registerSecurityGroup(Long vcId, String tenantId, String tenantName, Long sgId,
			String sgName) {
		VirtualizationConnector vc = new VirtualizationConnector();
		vc.setId(1L);
		SecurityGroup sg = new SecurityGroup(vc, tenantId, tenantName);
		sg.setId(sgId);
		sg.setName(sgName);

		when(this.em.find(SecurityGroup.class, sg.getId())).thenReturn(sg);
		return sg;
	}

	private List<NetworkElement> ostRegisterPorts(SecurityGroup sg) throws Exception {
		List<NetworkElement> neList = this.vmProtectedPorts.stream().map(NetworkElementImpl::new)
				.collect(Collectors.toList());
		PowerMockito.doReturn(neList).when(OpenstackUtil.class, "getPorts",
				sg.getSecurityGroupMembers().iterator().next());
		return neList;
	}

	private void registerNetworkRedirectionApi(SdnRedirectionApi redirectionApi, VirtualizationConnector vc)
			throws Exception {
		when(this.apiFactoryServiceMock.createNetworkRedirectionApi(vc)).thenReturn(redirectionApi);
	}

	private SdnRedirectionApi registerNetworkElement(PortGroup portGroup, NetworkElement ne) throws Exception {
		SdnRedirectionApi redirectionApi = mock(SdnRedirectionApi.class);
		when(redirectionApi.updateNetworkElement(this.portGroupCaptor.capture(), this.networkElementCaptor.capture()))
				.thenReturn(ne);

		return redirectionApi;
	}

	private void registerDomain(String domainId, SecurityGroup sg) throws Exception {
		PowerMockito.doReturn(domainId).when(OpenstackUtil.class, "extractDomainId", eq(sg.getTenantId()),
				eq(sg.getTenantName()), eq(sg.getVirtualizationConnector()),
				this.domainIdNetworkElementCaptor.capture());
	}

	private NetworkElement createNetworkElement(String openstackId) throws Exception {
		VMPort port = null;
		OsProtectionEntity protectionEntity;
		protectionEntity = new VM("region", UUID.randomUUID().toString(), "name");
		port = newVMPort((VM) protectionEntity, openstackId);
		NetworkElementImpl ne = new NetworkElementImpl(port);

		return ne;
	}

	private static PortGroup createPortGroup(String id) {
		PortGroup portGroup = new PortGroup();
		portGroup.setPortGroupId(id);

		return portGroup;
	}

	private void assertNetworkElementsParentIdWithDomainId(List<NetworkElement> neList, String domainId) {
		for (NetworkElement ne : neList) {
			Assert.assertEquals("Network element parent id mismatch", ne.getParentId(), domainId);
		}
	}

}