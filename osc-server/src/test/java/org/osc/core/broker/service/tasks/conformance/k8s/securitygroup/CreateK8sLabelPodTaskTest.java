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
package org.osc.core.broker.service.tasks.conformance.k8s.securitygroup;

import static org.mockito.Mockito.when;
import static org.osc.core.broker.service.tasks.conformance.k8s.securitygroup.CreateK8sLabelPodTaskTestData.*;

import javax.persistence.EntityManager;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.osc.core.broker.model.entities.virtualization.SecurityGroupMember;
import org.osc.core.broker.model.entities.virtualization.k8s.Label;
import org.osc.core.broker.model.entities.virtualization.k8s.Pod;
import org.osc.core.broker.model.entities.virtualization.k8s.PodPort;
import org.osc.core.broker.model.plugin.ApiFactoryService;
import org.osc.core.broker.rest.client.k8s.KubernetesPod;
import org.osc.core.broker.service.exceptions.VmidcException;
import org.osc.core.broker.service.persistence.PodEntityMgr;
import org.osc.core.broker.service.test.InMemDB;
import org.osc.core.broker.util.TransactionalBroadcastUtil;
import org.osc.core.broker.util.db.DBConnectionManager;
import org.osc.core.test.util.TestTransactionControl;
import org.osc.sdk.controller.api.SdnRedirectionApi;
import org.osc.sdk.controller.element.NetworkElement;

public class CreateK8sLabelPodTaskTest {
    public EntityManager em;

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Mock(answer = Answers.CALLS_REAL_METHODS)
    private TestTransactionControl txControl;

    @Mock
    DBConnectionManager dbMgr;

    @Mock
    TransactionalBroadcastUtil txBroadcastUtil;

    @Mock
    public ApiFactoryService apiFactoryServiceMock;

    @Mock
    public SdnRedirectionApi redirectionApi;

    @InjectMocks
    CreateK8sLabelPodTask factoryTask;

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
    public void testExecute_WhenK8sPodAlreadyBelongsToAnotherSG_ThrowsVmidException() throws Exception {
        // Arrange.
        CreateK8sLabelPodTask task = this.factoryTask.create(ALREADY_PROTECTED_K8S_POD, ALREADY_PROTECTED_POD_SGM_LABEL);
        this.exception.expect(VmidcException.class);
        this.exception.expectMessage("is already part of the security group");

        // Act.
        task.execute();
    }

    @Test
    public void testExecute_WhenSDNReturnsNullNetworkElement_ThrowsVmidcException() throws Exception {
        // Arrange.
        CreateK8sLabelPodTask task = this.factoryTask.create(NETWORK_ELEMENT_NOT_FOUND_K8S_POD, NETWORK_ELEMENT_NOT_FOUND_POD_SGM_LABEL);
        registerNetworkElement(NETWORK_ELEMENT_NOT_FOUND_POD_SGM_LABEL.getSecurityGroupMembers().iterator().next(), null, NETWORK_ELEMENT_NOT_FOUND_K8S_POD);
        this.exception.expect(VmidcException.class);
        this.exception.expectMessage("The SDN controller did not return a network element for the device");

        // Act.
        task.execute();
    }

    @Test
    public void testExecute_WithAValidNewPod_PodAndPortArePersisted() throws Exception {
        // Arrange.
        CreateK8sLabelPodTask task = this.factoryTask.create(VALID_K8S_POD, VALID_POD_SGM_LABEL);
        NetworkElement podPort = createNetworkElement();
        registerNetworkElement(VALID_POD_SGM_LABEL.getSecurityGroupMembers().iterator().next(), podPort, VALID_K8S_POD);

        // Act.
        task.execute();

        // Assert.
        Pod createdPod = PodEntityMgr.findExternalId(this.em, VALID_K8S_POD.getUid());
        Assert.assertNotNull("The created pod should no be null", createdPod);
        Assert.assertTrue("The persisted pod is different than expected", isExpectedPod(createdPod, VALID_K8S_POD, podPort));
        Assert.assertTrue("The label SGM should contain the created pod", VALID_POD_SGM_LABEL.getPods().contains(createdPod));
    }

    @Test
    public void testExecute_WithAValidExistingPod_LabelIsUpdatedWithExistingPod() throws Exception {
        // Arrange.
        CreateK8sLabelPodTask task = this.factoryTask.create(SAME_SG_ALREADY_PROTECTED_K8S_POD, VALID_EXISTING_POD_SGM_LABEL);
        NetworkElement podPort = SAME_SG_ALREADY_PROTECTED_POD_NETWORK_ELEMENT;
        registerNetworkElement(VALID_EXISTING_POD_SGM_LABEL.getSecurityGroupMembers().iterator().next(), podPort, SAME_SG_ALREADY_PROTECTED_K8S_POD);

        // Act.
        task.execute();

        // Assert.
        Pod updatedPod = PodEntityMgr.findExternalId(this.em, SAME_SG_ALREADY_PROTECTED_K8S_POD.getUid());

        Assert.assertNotNull("The updated pod should no be null", updatedPod);
        Assert.assertTrue("The updated pod is different than expected", isExpectedPod(updatedPod, SAME_SG_ALREADY_PROTECTED_K8S_POD, podPort));

        Label updatedLabel = this.em.find(Label.class, VALID_EXISTING_POD_SGM_LABEL.getId());

        Assert.assertTrue("The label SGM should contain the created pod", updatedLabel.getPods().contains(updatedPod));
        // The existing port should not contain its previous label and its new label
        Assert.assertTrue("", updatedPod.getLabels().contains(EXISTING_PROTECTED_POD_SAME_SG_LABEL));
        Assert.assertTrue("", updatedPod.getLabels().contains(VALID_EXISTING_POD_SGM_LABEL));
    }

    private void populateDatabase() {
        if (!DB_POPULATED) {
            this.em.getTransaction().begin();
            persist(EXISTING_PROTECTED_POD_SGM_LABEL, this.em);
            persist(VALID_POD_SGM_LABEL, this.em);
            persist(ALREADY_PROTECTED_POD_SGM_LABEL, this.em);
            persist(NETWORK_ELEMENT_NOT_FOUND_POD_SGM_LABEL, this.em);
            persist(EXISTING_PROTECTED_POD_SAME_SG_LABEL, this.em);
            persist(VALID_EXISTING_POD_SGM_LABEL, this.em);
            this.em.getTransaction().commit();
            DB_POPULATED = true;
        }
    }

    private void registerNetworkElement(SecurityGroupMember sgm, NetworkElement networkElement, KubernetesPod k8sPod) throws Exception {
        when(this.redirectionApi.getNetworkElementByDeviceOwnerId(k8sPod.getNamespace() + ":" + k8sPod.getName())).thenReturn(networkElement);
        when(this.apiFactoryServiceMock.createNetworkRedirectionApi(sgm.getSecurityGroup().getVirtualizationConnector())).thenReturn(this.redirectionApi);
    }

    private boolean isExpectedPod(Pod podEntity, KubernetesPod k8sPod, NetworkElement podPort) {
        PodPort entityPort = podEntity.getPorts().iterator().next();
        return podEntity.getName().equals(k8sPod.getName()) &&
                podEntity.getNamespace().equals(k8sPod.getNamespace()) &&
                podEntity.getNode().equals(k8sPod.getNode()) &&
                podEntity.getExternalId().equals(k8sPod.getUid()) &&
                entityPort.getExternalId().equals(podPort.getElementId()) &&
                entityPort.getParentId().equals(podPort.getParentId()) &&
                entityPort.getMacAddress().equals(podPort.getMacAddresses().get(0)) &&
                entityPort.getIpAddresses().get(0).equals(podPort.getPortIPs().get(0));
    }
}
