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
import static org.mockito.Mockito.*;

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
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.osc.core.broker.model.entities.appliance.VirtualSystem;
import org.osc.core.broker.model.entities.virtualization.SecurityGroupInterface;
import org.osc.core.broker.model.plugin.sdncontroller.SdnControllerApiFactory;
import org.osc.core.broker.util.db.HibernateUtil;
import org.osc.core.test.util.TestTransactionControl;
import org.osc.sdk.controller.api.SdnRedirectionApi;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ HibernateUtil.class, SdnControllerApiFactory.class })
public class RemovePortGroupHookTaskTest {
    @Mock protected EntityManager em;
    @Mock protected EntityTransaction tx;

    @Mock(answer=Answers.CALLS_REAL_METHODS)
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
    public void testExecute_WhenSGIHasNoNetworkElementId_NothingToRemove() throws Exception {
        // Arrange.
        SecurityGroupInterface sgi = registerNewSGI(1L);
        sgi.setNetworkElementId(null);

        RemovePortGroupHookTask task = new RemovePortGroupHookTask(sgi);

        // Act.
        task.execute();

        // Assert.
        verify(this.em, Mockito.never()).merge(any());
    }

    @Test
    public void testExecute_WhenCreateNetworkRedirectionApiFails_ThrowsTheUnhandledException() throws Exception {
        // Arrange.
        SecurityGroupInterface sgi = registerNewSGI(2L);

        PowerMockito.spy(SdnControllerApiFactory.class);
        PowerMockito.doThrow(new IllegalStateException()).when(SdnControllerApiFactory.class, "createNetworkRedirectionApi", sgi.getVirtualSystem());

        this.exception.expect(IllegalStateException.class);

        RemovePortGroupHookTask task = new RemovePortGroupHookTask(sgi);

        // Act.
        task.execute();
    }

    @Test
    public void testExecute_WhenRemoveInspectionHookFails_ThrowsTheUnhandledException() throws Exception {
        // Arrange.
        SecurityGroupInterface sgi = registerNewSGI(3L);

        SdnRedirectionApi redirectionApi = mockRemoveInspectionHook(sgi, new IllegalStateException());

        registerNetworkRedirectionApi(redirectionApi, sgi.getVirtualSystem());

        this.exception.expect(IllegalStateException.class);

        RemovePortGroupHookTask task = new RemovePortGroupHookTask(sgi);
        String currentNetworkElementId = sgi.getNetworkElementId();

        // Act.
        task.execute();

        // Assert.
        Assert.assertNotNull("The SGI network element id should not be null", sgi.getNetworkElementId());
        Assert.assertEquals("The sgi network element id should not have changed.", currentNetworkElementId, sgi.getNetworkElementId());
        verify(this.em, Mockito.never()).merge(any());
    }

    @Test
    public void testExecute_WhenRemoveInspectionHookSucceeds_SGINetworkElementSetToNull() throws Exception {
        // Arrange.
        SecurityGroupInterface sgi = registerNewSGI(4L);

        SdnRedirectionApi redirectionApi = mockRemoveInspectionHook(sgi, null);

        registerNetworkRedirectionApi(redirectionApi, sgi.getVirtualSystem());

        RemovePortGroupHookTask task = new RemovePortGroupHookTask(sgi);
        // Act.
        task.execute();

        // Assert.
        Assert.assertNull("The SGI network element id should be null", sgi.getNetworkElementId());
        verify(this.em, times(1)).merge(sgi);
    }

    protected void registerNetworkRedirectionApi(SdnRedirectionApi redirectionApi, VirtualSystem vs) throws Exception {
        PowerMockito.spy(SdnControllerApiFactory.class);
        PowerMockito.doReturn(redirectionApi).when(SdnControllerApiFactory.class, "createNetworkRedirectionApi", vs);

    }

    private SdnRedirectionApi mockRemoveInspectionHook(SecurityGroupInterface sgi, Exception e) throws Exception {
        SdnRedirectionApi redirectionApi = mock(SdnRedirectionApi.class);
        if (e != null) {
            doThrow(e).when(redirectionApi).removeInspectionHook(sgi.getNetworkElementId());
        } else {
            doNothing().when(redirectionApi).removeInspectionHook(sgi.getNetworkElementId());
        }

        return redirectionApi;
    }

    private SecurityGroupInterface registerNewSGI(Long entityId) {
        VirtualSystem vs = new VirtualSystem(null);
        vs.setId(entityId);

        SecurityGroupInterface sgi = new SecurityGroupInterface(vs, null, UUID.randomUUID().toString(), null, 1L);
        sgi.setId(entityId);
        sgi.setNetworkElementId(UUID.randomUUID().toString());

        when(this.em.find(SecurityGroupInterface.class, sgi.getId())).thenReturn(sgi);
        return sgi;
    }
}
