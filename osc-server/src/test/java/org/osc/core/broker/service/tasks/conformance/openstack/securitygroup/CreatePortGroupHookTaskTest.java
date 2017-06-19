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

import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatcher;
import org.mockito.InjectMocks;
import org.mockito.Mockito;
import org.osc.core.broker.model.entities.appliance.DistributedApplianceInstance;
import org.osc.core.broker.model.entities.virtualization.SecurityGroup;
import org.osc.core.broker.model.entities.virtualization.SecurityGroupInterface;
import org.osc.core.broker.service.exceptions.VmidcException;
import org.osc.core.broker.util.db.HibernateUtil;
import org.osc.sdk.controller.FailurePolicyType;
import org.osc.sdk.controller.TagEncapsulationType;
import org.osc.sdk.controller.api.SdnRedirectionApi;
import org.osc.sdk.controller.element.InspectionPortElement;
import org.osc.sdk.controller.element.NetworkElement;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ HibernateUtil.class })
public class CreatePortGroupHookTaskTest extends BasePortGroupHookTaskTest {
    @InjectMocks
    CreatePortGroupHookTask factory;

    @Test
    public void testExecute_WhenSGHasNoNetworkElementId_ThrowsValidationException() throws Exception {
        super.testExecute_WhenSGHasNoNetworkElementId_ThrowsValidationException(this.factory);
    }

    @Test
    public void testExecute_WhenCreateNetworkRedirectionApiFails_ThrowsTheUnhandledException() throws Exception {
        super.testExecute_WhenCreateNetworkRedirectionApiFails_ThrowsTheUnhandledException(this.factory);
    }

    @Test
    public void testExecute_WhenInstallInspectionHookFails_ThrowsTheUnhandledException() throws Exception {
        // Arrange.
        SecurityGroup sg = newSecurityGroup();
        SecurityGroupInterface sgi = registerNewSGI(sg, 2L);
        DistributedApplianceInstance dai = registerNewDAI();

        SdnRedirectionApi redirectionApi = mockInstallInspectionHook(sgi, dai, new IllegalStateException());

        registerNetworkRedirectionApi(redirectionApi, sgi.getVirtualSystem());

        this.exception.expect(IllegalStateException.class);

        CreatePortGroupHookTask task = this.factory.create(sgi, dai);

        // Act.
        task.execute();
    }

    @Test
    public void testExecute_WhenInstallInspectionHookReturnsNullId_ThrowsVmidcException() throws Exception {
        // Arrange.
        SecurityGroup sg = newSecurityGroup();
        SecurityGroupInterface sgi = registerNewSGI(sg, 3L);
        DistributedApplianceInstance dai = registerNewDAI();

        SdnRedirectionApi redirectionApi = mockInstallInspectionHook(sgi, dai, (String) null);

        registerNetworkRedirectionApi(redirectionApi, sgi.getVirtualSystem());

        CreatePortGroupHookTask task = this.factory.create(sgi, dai);

        this.exception.expect(VmidcException.class);
        this.exception
                .expectMessage(String.format("The creation of the inspection hook for the security group interface %s."
                        + "succeeded but the returned identifier was null.", sgi.getName()));

        // Act.
        task.execute();
    }

    @Test
    public void testExecute_WhenInstallInspectionHookSucceeds_SGIIsUpdatedWithHookId() throws Exception {
        // Arrange.
        SecurityGroup sg = newSecurityGroup();
        SecurityGroupInterface sgi = registerNewSGI(sg, 4L);
        DistributedApplianceInstance dai = registerNewDAI();

        String inspectionHookId = UUID.randomUUID().toString();

        SdnRedirectionApi redirectionApi = mockInstallInspectionHook(sgi, dai, inspectionHookId);

        registerNetworkRedirectionApi(redirectionApi, sgi.getVirtualSystem());

        CreatePortGroupHookTask task = this.factory.create(sgi, dai);

        // Act.
        task.execute();

        // Assert.
        Assert.assertEquals("", inspectionHookId, sgi.getNetworkElementId());
        verify(this.em, Mockito.timeout(1)).merge(sgi);
    }

    private SdnRedirectionApi mockInstallInspectionHook(SecurityGroupInterface sgi, DistributedApplianceInstance dai,
            Exception e) throws Exception {
        SdnRedirectionApi redirectionApi = mock(SdnRedirectionApi.class);
        when(redirectionApi.installInspectionHook(argThat(new NetworkElementMatcher(sgi.getSecurityGroup())),
                argThat(new InspectionPortElementMatcher(dai)), eq(Long.parseLong(sgi.getTag())),
                (TagEncapsulationType) isNull(), eq(sgi.getOrder()), (FailurePolicyType) isNull())).thenThrow(e);

        return redirectionApi;
    }

    private SdnRedirectionApi mockInstallInspectionHook(SecurityGroupInterface sgi, DistributedApplianceInstance dai,
            String inspectionHookId) throws Exception {
        SdnRedirectionApi redirectionApi = mock(SdnRedirectionApi.class);
        when(redirectionApi.installInspectionHook(argThat(new NetworkElementMatcher(sgi.getSecurityGroup())),
                argThat(new InspectionPortElementMatcher(dai)), eq(Long.parseLong(sgi.getTag())),
                (TagEncapsulationType) isNull(), eq(sgi.getOrder()), (FailurePolicyType) isNull()))
                        .thenReturn(inspectionHookId);

        return redirectionApi;
    }

    private class InspectionPortElementMatcher extends ArgumentMatcher<InspectionPortElement> {
        private DistributedApplianceInstance dai;

        public InspectionPortElementMatcher(DistributedApplianceInstance dai) {
            this.dai = dai;
        }

        @Override
        public boolean matches(Object object) {
            if (object == null || !(object instanceof InspectionPortElement)) {
                return false;
            }

            InspectionPortElement inspectionPort = (InspectionPortElement) object;

            return inspectionPortMatchesDAI(inspectionPort, this.dai);
        }
    }

    private class NetworkElementMatcher extends ArgumentMatcher<NetworkElement> {
        private SecurityGroup sg;

        public NetworkElementMatcher(SecurityGroup sg) {
            this.sg = sg;
        }

        @Override
        public boolean matches(Object object) {
            if (object == null || !(object instanceof NetworkElement)) {
                return false;
            }

            NetworkElement netElement = (NetworkElement) object;

            return netElement.getElementId().equals(this.sg.getNetworkElementId());
        }
    }
}
