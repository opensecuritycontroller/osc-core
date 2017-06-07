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
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatcher;
import org.mockito.InjectMocks;
import org.mockito.Mockito;
import org.osc.core.broker.model.entities.appliance.DistributedApplianceInstance;
import org.osc.core.broker.model.entities.virtualization.SecurityGroup;
import org.osc.core.broker.model.entities.virtualization.SecurityGroupInterface;
import org.osc.core.broker.util.db.HibernateUtil;
import org.osc.sdk.controller.api.SdnRedirectionApi;
import org.osc.sdk.controller.element.InspectionHookElement;
import org.osc.sdk.controller.element.InspectionPortElement;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ HibernateUtil.class })
public class UpdatePortGroupHookTaskTest extends BasePortGroupHookTaskTest {
    @InjectMocks
    UpdatePortGroupHookTask factory;

    @Test
    public void testExecute_WhenSGHasNoNetworkElementId_ThrowsValidationException() throws Exception {
        super.testExecute_WhenSGHasNoNetworkElementId_ThrowsValidationException(this.factory);
    }

    @Test
    public void testExecute_WhenCreateNetworkRedirectionApiFails_ThrowsTheUnhandledException() throws Exception {
        super.testExecute_WhenCreateNetworkRedirectionApiFails_ThrowsTheUnhandledException(this.factory);
    }

    @Test
    public void testExecute_WhenUpdateInspectionHookFails_ThrowsTheUnhandledException() throws Exception {
        // Arrange.
        SecurityGroup sg = newSecurityGroup();
        SecurityGroupInterface sgi = registerNewSGI(sg, 2L);
        DistributedApplianceInstance dai = registerNewDAI();

        SdnRedirectionApi redirectionApi = mockUpdateInspectionHook(sgi, dai, new IllegalStateException());

        registerNetworkRedirectionApi(redirectionApi, sgi.getVirtualSystem());

        this.exception.expect(IllegalStateException.class);

        UpdatePortGroupHookTask task = this.factory.create(sgi, dai);

        // Act.
        task.execute();
    }

    @Test
    public void testExecute_WhenUpdateInspectionHookSucceeds_ExecutionFinishes() throws Exception {
        // Arrange.
        SecurityGroup sg = newSecurityGroup();
        SecurityGroupInterface sgi = registerNewSGI(sg, 2L);
        DistributedApplianceInstance dai = registerNewDAI();

        SdnRedirectionApi redirectionApi = mockUpdateInspectionHook(sgi, dai, null);

        registerNetworkRedirectionApi(redirectionApi, sgi.getVirtualSystem());

        UpdatePortGroupHookTask task = this.factory.create(sgi, dai);

        // Act.
        task.execute();

        // Assert.
        Mockito.verify(redirectionApi, Mockito.times(1))
                .updateInspectionHook(argThat(new InspectionHookMatcher(dai, sgi)));
    }

    private SdnRedirectionApi mockUpdateInspectionHook(SecurityGroupInterface sgi, DistributedApplianceInstance dai,
            Exception e) throws Exception {
        SdnRedirectionApi redirectionApi = mock(SdnRedirectionApi.class);
        if (e != null) {
            doThrow(e).when(redirectionApi).updateInspectionHook(argThat(new InspectionHookMatcher(dai, sgi)));
        } else {
            doNothing().when(redirectionApi).updateInspectionHook(argThat(new InspectionHookMatcher(dai, sgi)));
        }

        return redirectionApi;
    }

    private class InspectionHookMatcher extends ArgumentMatcher<InspectionHookElement> {
        private DistributedApplianceInstance dai;
        private SecurityGroupInterface sgi;

        public InspectionHookMatcher(DistributedApplianceInstance dai, SecurityGroupInterface sgi) {
            this.dai = dai;
            this.sgi = sgi;
        }

        @Override
        public boolean matches(Object object) {
            if (object == null || !(object instanceof InspectionHookElement)) {
                return false;
            }

            InspectionHookElement inspectionHook = (InspectionHookElement) object;
            InspectionPortElement inspectionPort = inspectionHook.getInspectionPort();

            return inspectionPortMatchesDAI(inspectionPort, this.dai)
                    && inspectionHook.getInspectedPort().getElementId()
                            .equals(this.sgi.getSecurityGroup().getNetworkElementId())
                    && inspectionHook.getTag().equals(this.sgi.getTagValue())
                    && inspectionHook.getEncType().toString()
                            .equals(this.sgi.getVirtualSystem().getEncapsulationType().name())
                    && inspectionHook.getOrder().equals(this.sgi.getOrder())
                    && inspectionHook.getFailurePolicyType().toString().equals(this.sgi.getFailurePolicyType().name());
        }
    }
}
