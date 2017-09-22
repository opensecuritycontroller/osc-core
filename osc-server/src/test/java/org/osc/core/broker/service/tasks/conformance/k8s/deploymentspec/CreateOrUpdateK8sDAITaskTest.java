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
package org.osc.core.broker.service.tasks.conformance.k8s.deploymentspec;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Answers;
import org.mockito.ArgumentMatcher;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.osc.core.broker.model.entities.appliance.ApplianceSoftwareVersion;
import org.osc.core.broker.model.entities.appliance.DistributedApplianceInstance;
import org.osc.core.broker.model.entities.appliance.VirtualSystem;
import org.osc.core.broker.model.entities.virtualization.VirtualizationConnector;
import org.osc.core.broker.model.entities.virtualization.openstack.DeploymentSpec;
import org.osc.core.broker.model.plugin.ApiFactoryService;
import org.osc.core.broker.rest.client.k8s.KubernetesPod;
import org.osc.core.broker.service.exceptions.VmidcException;
import org.osc.core.broker.util.TransactionalBroadcastUtil;
import org.osc.core.broker.util.db.DBConnectionManager;
import org.osc.core.test.util.TestTransactionControl;
import org.osc.sdk.controller.DefaultNetworkPort;
import org.osc.sdk.controller.api.SdnRedirectionApi;
import org.osc.sdk.controller.element.NetworkElement;

public class CreateOrUpdateK8sDAITaskTest  {
    @Mock
    protected EntityManager em;

    @Mock
    protected EntityTransaction tx;

    @Mock(answer = Answers.CALLS_REAL_METHODS)
    TestTransactionControl txControl;

    @Mock
    DBConnectionManager dbMgr;

    @Mock
    TransactionalBroadcastUtil txBroadcastUtil;

    @Mock
    public ApiFactoryService apiFactoryServiceMock;

    @InjectMocks
    CreateOrUpdateK8sDAITask factory;

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
    public void testExecute_WhenSDNReturnsNetworkElement_NoOrphanDAI_DAICreatedWithoutInspectionElementId() throws Exception {
        // Arrange.
        KubernetesPod k8sPod = createKubernetesPod();
        DefaultNetworkPort podPort = createNetworkElement();
        podPort.setParentId(UUID.randomUUID().toString());
        DeploymentSpec ds = createAndRegisterDeploymentSpec(null);

        registerNetworkElement(ds, podPort, k8sPod);

        CreateOrUpdateK8sDAITask task = this.factory.create(ds, k8sPod);

        // Act.
        task.execute();

        // Assert.
        verify(this.em, Mockito.times(1)).persist(Mockito.argThat(new DAIMatcher(podPort, k8sPod, null, podPort.getParentId())));
        verify(this.em, Mockito.never()).remove(Mockito.any(DistributedApplianceInstance.class));
    }

    @Test
    public void testExecute_WhenSDNReturnsNetworkElement_OrphanDAI_DAICreatedWithInspectionElementId() throws Exception {
        // Arrange.
        KubernetesPod k8sPod = createKubernetesPod();
        NetworkElement podPort = createNetworkElement();
        DistributedApplianceInstance dai = new DistributedApplianceInstance();
        dai.setInspectionElementId(UUID.randomUUID().toString());
        dai.setInspectionElementParentId(UUID.randomUUID().toString());
        dai.setId(101L);

        DeploymentSpec ds = createAndRegisterDeploymentSpec(dai);

        registerNetworkElement(ds, podPort, k8sPod);

        CreateOrUpdateK8sDAITask task = this.factory.create(ds, k8sPod);

        // Act.
        task.execute();

        // Assert.
        verify(this.em, Mockito.times(1)).persist(Mockito.argThat(new DAIMatcher(podPort, k8sPod, dai.getInspectionElementId(), dai.getInspectionElementParentId())));
        verify(this.em, Mockito.times(1)).remove(dai);
    }

    @Test
    public void testExecute_WhenSDNReturnsNullNetworkElement_ThrowsVmidcException() throws Exception {
        // Arrange.
        KubernetesPod k8sPod = createKubernetesPod();
        DeploymentSpec ds = createAndRegisterDeploymentSpec(null);

        registerNetworkElement(ds, null, k8sPod);

        CreateOrUpdateK8sDAITask task = this.factory.create(ds, k8sPod);
        this.exception.expect(VmidcException.class);
        this.exception.expectMessage("The SDN controller did not return a network element");

        // Act.
        task.execute();
    }

    private class DAIMatcher extends ArgumentMatcher<DistributedApplianceInstance> {
        NetworkElement networkElement;
        KubernetesPod k8sPod;
        String inspElementId;
        String inspElementParentId;

        public DAIMatcher(NetworkElement networkElement, KubernetesPod k8sPod, String inspElementId, String inspElementParentId) {
            this.networkElement = networkElement;
            this.k8sPod = k8sPod;
            this.inspElementId = inspElementId;
            this.inspElementParentId = inspElementParentId;
        }

        @Override
        public boolean matches(Object object) {
            if (object == null || !(object instanceof DistributedApplianceInstance)) {
                return false;
            }

            DistributedApplianceInstance dai = (DistributedApplianceInstance) object;
            return dai.getOsHostName().equals(this.k8sPod.getNode()) &&
                    dai.getName().equals(this.k8sPod.getNamespace() + "-" + this.k8sPod.getName()) &&
                    dai.getExternalId().equals(this.k8sPod.getUid()) &&
                    ((dai.getInspectionElementId() == null && this.inspElementId == null) ||
                            dai.getInspectionElementId().equals(this.inspElementId)) &&
                    ((dai.getInspectionElementParentId() == null && this.inspElementParentId == null) ||
                            dai.getInspectionElementParentId().equals(this.inspElementParentId)) &&
                    dai.getInspectionOsIngressPortId().equals(this.networkElement.getElementId()) &&
                    dai.getInspectionIngressMacAddress().equals(this.networkElement.getMacAddresses().get(0)) &&
                    dai.getIpAddress().equals(this.networkElement.getPortIPs().get(0));
        }
    }

    private void registerNetworkElement(DeploymentSpec ds, NetworkElement networkElement, KubernetesPod k8sPod) throws Exception {
        SdnRedirectionApi redirectionApi = Mockito.mock(SdnRedirectionApi.class);
        when(redirectionApi.getNetworkElementByDeviceOwnerId(k8sPod.getNamespace() + ":" + k8sPod.getName())).thenReturn(networkElement);
        when(this.apiFactoryServiceMock.createNetworkRedirectionApi(ds.getVirtualSystem())).thenReturn(redirectionApi);
    }

    private DeploymentSpec createAndRegisterDeploymentSpec(DistributedApplianceInstance dai) {
        VirtualizationConnector vc = new VirtualizationConnector();
        vc.setProviderIpAddress("1.1.1.1");
        VirtualSystem vs = new VirtualSystem(null);
        vs.setVirtualizationConnector(vc);
        vs.setId(102L);

        ApplianceSoftwareVersion avs = new ApplianceSoftwareVersion();
        avs.setImageUrl("ds-image-url");
        avs.setImagePullSecretName("ds-pull-secret-name");

        vs.setApplianceSoftwareVersion(avs);
        DeploymentSpec ds = new DeploymentSpec(vs, null, null, null, null, null);
        ds.setId(101L);
        ds.setName(UUID.randomUUID().toString());
        ds.setNamespace("ds-namespace");
        ds.setInstanceCount(5);
        Set<DistributedApplianceInstance> dais = new HashSet<>();
        if (dai!= null) {
            dai.setDeploymentSpec(ds);
            dais.add(dai);
        }

        ds.setDistributedApplianceInstances(dais);

        when(this.em.find(DeploymentSpec.class, ds.getId())).thenReturn(ds);

        return ds;
    }

    private DefaultNetworkPort createNetworkElement() {
        DefaultNetworkPort networkPort = new DefaultNetworkPort(UUID.randomUUID().toString(), UUID.randomUUID().toString());
        networkPort.setPortIPs(Arrays.asList(UUID.randomUUID().toString()));
        return networkPort;
    }

    private KubernetesPod createKubernetesPod() {
        return new KubernetesPod(
                UUID.randomUUID().toString(),
                UUID.randomUUID().toString(),
                UUID.randomUUID().toString(),
                UUID.randomUUID().toString());
    }
}
