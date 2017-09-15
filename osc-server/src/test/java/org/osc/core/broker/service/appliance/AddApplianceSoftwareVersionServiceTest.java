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
package org.osc.core.broker.service.appliance;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.*;

import java.util.Arrays;
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
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.osc.core.broker.model.entities.appliance.Appliance;
import org.osc.core.broker.model.entities.appliance.ApplianceSoftwareVersion;
import org.osc.core.broker.service.api.server.UserContextApi;
import org.osc.core.broker.service.dto.ApplianceSoftwareVersionDto;
import org.osc.core.broker.service.exceptions.VmidcBrokerValidationException;
import org.osc.core.broker.service.request.BaseRequest;
import org.osc.core.broker.service.response.BaseResponse;
import org.osc.core.broker.service.validator.ApplianceSoftwareVersionDtoValidator;
import org.osc.core.broker.util.TransactionalBroadcastUtil;
import org.osc.core.broker.util.db.DBConnectionManager;
import org.osc.core.common.virtualization.VirtualizationType;
import org.osc.core.test.util.TestTransactionControl;
import org.osc.sdk.controller.TagEncapsulationType;

public class AddApplianceSoftwareVersionServiceTest {
    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Mock
    private EntityManager em;

    @Mock
    protected EntityTransaction tx;

    @Mock(answer=Answers.CALLS_REAL_METHODS)
    TestTransactionControl txControl;

    @Mock
    private ApplianceSoftwareVersionDtoValidator validatorMock;

    @Mock
    private UserContextApi userContext;

    @Mock
    DBConnectionManager dbMgr;

    @Mock
    private TransactionalBroadcastUtil txBroadcastUtil;

    @InjectMocks
    private AddApplianceSoftwareVersionService service;

    @Before
    public void testInitialize() throws Exception {
        MockitoAnnotations.initMocks(this);

        this.service.validator = this.validatorMock;
        when(this.em.getTransaction()).thenReturn(this.tx);

        when(this.dbMgr.getTransactionalEntityManager()).thenReturn(this.em);
        when(this.dbMgr.getTransactionControl()).thenReturn(this.txControl);

        this.txControl.setEntityManager(this.em);
    }

    @Test
    public void testDispatch_WithNullRequest_ThrowsNullPointerException() throws Exception {
        // Arrange.
        this.exception.expect(NullPointerException.class);

        // Act.
        this.service.dispatch(null);
    }

    @Test
    public void testDispatch_WhenAsvValidationFails_ThrowsUnhandledException() throws Exception {
        // Arrange.
        ApplianceSoftwareVersionDto dto = new ApplianceSoftwareVersionDto();
        BaseRequest<ApplianceSoftwareVersionDto> request = new BaseRequest<>(dto);

        doThrow(VmidcBrokerValidationException.class).when(this.validatorMock).validateForCreate(dto);
        this.exception.expect(VmidcBrokerValidationException.class);

        // Act.
        this.service.dispatch(request);
    }

    @Test
    public void testDispatch_WhenAsvApplianceNotFound_ThrowsValidationException() throws Exception {
        // Arrange.
        Long applianceId = 101L;
        ApplianceSoftwareVersionDto dto = new ApplianceSoftwareVersionDto();
        dto.setParentId(applianceId);
        BaseRequest<ApplianceSoftwareVersionDto> request = new BaseRequest<>(dto);

        when(this.em.find(Appliance.class, applianceId)).thenReturn(null);
        this.exception.expect(VmidcBrokerValidationException.class);

        // Act.
        this.service.dispatch(request);
    }

    @Test
    public void testDispatch_WhenAsvValidationSucceeds_AsvIsPersisted() throws Exception {
        // Arrange.
        ApplianceSoftwareVersionDto dto = new ApplianceSoftwareVersionDto();
        dto.setParentId(100L);
        dto.setSwVersion(UUID.randomUUID().toString());
        dto.setVirtualizationType(VirtualizationType.OPENSTACK);
        dto.setEncapsulationTypes(Arrays.asList(TagEncapsulationType.VLAN));
        dto.setMinCpus(1);
        dto.setMemoryInMb(100);
        dto.setDiskSizeInGb(100);

        BaseRequest<ApplianceSoftwareVersionDto> request = new BaseRequest<>(dto);
        Long asvId = 111L;

        when(this.em.find(Appliance.class, dto.getParentId())).thenReturn(new Appliance());

        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                ApplianceSoftwareVersion asv = invocation.getArgumentAt(0, ApplianceSoftwareVersion.class);
                asv.setId(asvId);
                return null;
            }
        }).when(this.em).persist(argThat(new ApplianceSoftwareVersionMatcher(dto.getSwVersion())));

        // Act.
        BaseResponse response = this.service.dispatch(request);

        // Assert
        assertNotNull("The returned response should not be null", response);
        assertEquals("The returned asv id was different than expected", asvId, response.getId());
    }

    private class ApplianceSoftwareVersionMatcher extends ArgumentMatcher<Object> {
        private String softwareVersion;

        public ApplianceSoftwareVersionMatcher(String softwareVersion) {
            this.softwareVersion = softwareVersion;
        }

        @Override
        public boolean matches(Object object) {
            if (object == null || !(object instanceof ApplianceSoftwareVersion)) {
                return false;
            }
            ApplianceSoftwareVersion asv = (ApplianceSoftwareVersion)object;
            return this.softwareVersion.equals(asv.getApplianceSoftwareVersion());
        }
    }
}
