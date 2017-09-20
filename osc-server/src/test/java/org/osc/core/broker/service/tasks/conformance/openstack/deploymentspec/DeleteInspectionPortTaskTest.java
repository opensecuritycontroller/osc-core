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
package org.osc.core.broker.service.tasks.conformance.openstack.deploymentspec;

import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.UUID;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.ArgumentMatcher;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.osc.core.broker.model.entities.appliance.DistributedApplianceInstance;
import org.osc.core.broker.model.entities.appliance.VirtualSystem;
import org.osc.core.broker.model.entities.virtualization.VirtualizationConnector;
import org.osc.core.broker.model.entities.virtualization.openstack.DeploymentSpec;
import org.osc.core.broker.model.plugin.ApiFactoryService;
import org.osc.core.broker.service.exceptions.VmidcBrokerValidationException;
import org.osc.core.broker.util.TransactionalBroadcastUtil;
import org.osc.core.broker.util.db.DBConnectionManager;
import org.osc.core.broker.util.db.HibernateUtil;
import org.osc.core.common.virtualization.VirtualizationType;
import org.osc.core.test.util.TestTransactionControl;
import org.osc.sdk.controller.api.SdnRedirectionApi;
import org.osc.sdk.controller.element.InspectionPortElement;
import org.osc.sdk.controller.element.NetworkElement;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ HibernateUtil.class, OpenstackUtil.class })
public class DeleteInspectionPortTaskTest {
    @InjectMocks
    DeleteInspectionPortTask factory;

    @Mock
    protected EntityManager em;

    @Mock
    protected EntityTransaction tx;

    @Mock(answer = Answers.CALLS_REAL_METHODS)
    TestTransactionControl txControl;

    @Mock
    TransactionalBroadcastUtil txBroadcastUtil;

    @Mock
    DBConnectionManager dbMgr;

    @Mock
    public ApiFactoryService apiFactoryServiceMock;

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Before
    public void testInitialize() throws Exception {
        MockitoAnnotations.initMocks(this);
        when(this.em.getTransaction()).thenReturn(this.tx);

        this.txControl.setEntityManager(this.em);

        Mockito.when(this.dbMgr.getTransactionalEntityManager()).thenReturn(this.em);
        Mockito.when(this.dbMgr.getTransactionControl()).thenReturn(this.txControl);

        PowerMockito.mockStatic(OpenstackUtil.class);
    }

    @Test
    public void testExecute_WhenCreateNetworkRedirectionApiFails_ThrowsTheUnhandledException() throws Exception {
        // Arrange.
        DistributedApplianceInstance dai = registerNewDAI(1L);

        when(this.apiFactoryServiceMock.createNetworkRedirectionApi(dai)).thenThrow(new IllegalStateException());

        DeleteInspectionPortTask task = this.factory.create("region", dai);

        this.exception.expect(IllegalStateException.class);

        // Act.
        task.execute();
    }

    @Test
    public void testExecute_WhenDomainNotFound_ThrowsValidationException() throws Exception {
        // Arrange.
        DistributedApplianceInstance dai = registerNewDAI(1L);

        registerDomain(null, dai);
        when(this.apiFactoryServiceMock.supportsPortGroup(dai.getVirtualSystem())).thenReturn(true);
        DeleteInspectionPortTask task = this.factory.create("region", dai);

        this.exception.expect(VmidcBrokerValidationException.class);
        this.exception.expectMessage("A domain was not found for the ingress port");

        // Act.
        task.execute();
    }

    @Test
    public void testExecute_WhenRemoveInspectionPortFails_ThrowsTheUnhandledException() throws Exception {
        // Arrange.
        DistributedApplianceInstance dai = registerNewDAI(1L);
        String domainId = UUID.randomUUID().toString();

        registerDomain(domainId, dai);

        SdnRedirectionApi redirectionApi = mockRemoveInspectionPort(dai, domainId, new IllegalStateException());

        registerNetworkRedirectionApi(redirectionApi, dai);

        DeleteInspectionPortTask task = this.factory.create("region", dai);

        this.exception.expect(IllegalStateException.class);

        // Act.
        task.execute();
    }

    @Test
    public void testExecute_WhenDomainFound_DeletionIsSuccesfull() throws Exception {
        // Arrange.
        DistributedApplianceInstance dai = registerNewDAI(1L);
        String domainId = UUID.randomUUID().toString();

        registerDomain(domainId, dai);

        SdnRedirectionApi redirectionApi = mockRemoveInspectionPort(dai, domainId, null);

        registerNetworkRedirectionApi(redirectionApi, dai);

        DeleteInspectionPortTask task = this.factory.create("region", dai);

        // Act.
        task.execute();

        // Assert.
        verify(redirectionApi, times(1)).removeInspectionPort(argThat(new InspectionPortMatcher(dai, domainId)));
    }

    @Test
    public void testName_WithValidDAI_NameContainsDAIName() {
        // Arrange.
        DistributedApplianceInstance dai = registerNewDAI(1L);

        DeleteInspectionPortTask task = this.factory.create("region", dai);
        String name = null;

        // Act.
        name = task.getName();

        // Assert.
        Assert.assertEquals("The task name is different than expected.",
                String.format("Deleting Inspection Port of Server '%s' using SDN Controller plugin", dai.getName()),
                name);
    }

    private SdnRedirectionApi mockRemoveInspectionPort(DistributedApplianceInstance dai, String domainId, Exception e)
            throws Exception {
        SdnRedirectionApi redirectionApi = mock(SdnRedirectionApi.class);
        if (e != null) {
            doThrow(e).when(redirectionApi).removeInspectionPort(argThat(new InspectionPortMatcher(dai, domainId)));
        } else {
            doNothing().when(redirectionApi).removeInspectionPort(argThat(new InspectionPortMatcher(dai, domainId)));
        }

        return redirectionApi;
    }

    protected void registerNetworkRedirectionApi(SdnRedirectionApi redirectionApi, DistributedApplianceInstance dai)
            throws Exception {
        when(this.apiFactoryServiceMock.createNetworkRedirectionApi(dai)).thenReturn(redirectionApi);
        when(this.apiFactoryServiceMock.supportsPortGroup(dai.getVirtualSystem())).thenReturn(true);
    }

    private DistributedApplianceInstance registerNewDAI(Long entityId) {
        VirtualizationConnector vc = new VirtualizationConnector();
        vc.setId(entityId);
        vc.setVirtualizationType(VirtualizationType.OPENSTACK);

        VirtualSystem vs = new VirtualSystem(null);
        vs.setId(entityId);
        vs.setVirtualizationConnector(vc);

        DeploymentSpec ds = new DeploymentSpec(vs, null, null, null, null, null);
        ds.setId(entityId);

        DistributedApplianceInstance dai = new DistributedApplianceInstance(vs);
        dai.setId(entityId);
        dai.setInspectionOsIngressPortId(UUID.randomUUID().toString());
        dai.setInspectionOsEgressPortId(UUID.randomUUID().toString());
        dai.setInspectionIngressMacAddress(UUID.randomUUID().toString());
        dai.setInspectionEgressMacAddress(UUID.randomUUID().toString());
        dai.setName(UUID.randomUUID().toString());
        dai.setDeploymentSpec(ds);

        when(this.em.find(DistributedApplianceInstance.class, dai.getId())).thenReturn(dai);
        return dai;
    }

    private void registerDomain(String domainId, DistributedApplianceInstance dai) throws Exception {
        PowerMockito.doReturn(domainId).when(OpenstackUtil.class, "extractDomainId",
                eq(dai.getDeploymentSpec().getProjectId()), eq(dai.getDeploymentSpec().getProjectName()),
                eq(dai.getDeploymentSpec().getVirtualSystem().getVirtualizationConnector()),
                argThat(new NetworkElementsMatcher(dai)));
    }

    private class NetworkElementsMatcher extends ArgumentMatcher<List<NetworkElement>> {
        private DistributedApplianceInstance dai;

        public NetworkElementsMatcher(DistributedApplianceInstance dai) {
            this.dai = dai;
        }

        @Override
        public boolean matches(Object object) {
            if (object == null || !(object instanceof List<?>)) {
                return false;
            }

            List<?> netElements = (List<?>) object;

            if (netElements.size() != 1 || !(netElements.get(0) instanceof NetworkElement)) {
                return false;
            }

            NetworkElement netElement = (NetworkElement) netElements.get(0);

            return inspectionPortMatchesDAI(netElement, this.dai, true);
        }
    }

    private class InspectionPortMatcher extends ArgumentMatcher<InspectionPortElement> {
        private DistributedApplianceInstance dai;
        private String domainId;

        public InspectionPortMatcher(DistributedApplianceInstance dai, String domainId) {
            this.dai = dai;
            this.domainId = domainId;
        }

        @Override
        public boolean matches(Object object) {
            if (object == null || !(object instanceof InspectionPortElement)) {
                return false;
            }

            InspectionPortElement inspectionPort = (InspectionPortElement) object;

            return inspectionPortMatchesDAI(inspectionPort.getIngressPort(), this.dai, true)
                    && inspectionPortMatchesDAI(inspectionPort.getEgressPort(), this.dai, false)
                    && this.domainId.equals(inspectionPort.getIngressPort().getParentId())
                    && this.domainId.equals(inspectionPort.getEgressPort().getParentId());
        }
    }

    private boolean inspectionPortMatchesDAI(NetworkElement port, DistributedApplianceInstance dai, boolean ingress) {
        String macAddress = ingress ? dai.getInspectionIngressMacAddress() : dai.getInspectionEgressMacAddress();
        String portId = ingress ? dai.getInspectionOsIngressPortId() : dai.getInspectionOsEgressPortId();
        return port.getMacAddresses().size() == 1 && port.getMacAddresses().get(0).equals(macAddress)
                && port.getElementId().equals(portId);
    }
}
