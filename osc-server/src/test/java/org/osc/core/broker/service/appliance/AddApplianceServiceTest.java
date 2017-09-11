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
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

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
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.osc.core.broker.model.entities.appliance.Appliance;
import org.osc.core.broker.service.api.server.EncryptionApi;
import org.osc.core.broker.service.api.server.UserContextApi;
import org.osc.core.broker.service.dto.ApplianceDto;
import org.osc.core.broker.service.exceptions.VmidcBrokerValidationException;
import org.osc.core.broker.service.request.BaseRequest;
import org.osc.core.broker.service.response.BaseResponse;
import org.osc.core.broker.service.validator.ApplianceDtoValidator;
import org.osc.core.broker.util.TransactionalBroadcastUtil;
import org.osc.core.broker.util.db.DBConnectionManager;
import org.osc.core.test.util.TestTransactionControl;

public class AddApplianceServiceTest {
    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Mock
    private EntityManager em;

    @Mock
    protected EntityTransaction tx;

    @Mock(answer=Answers.CALLS_REAL_METHODS)
    TestTransactionControl txControl;

    @Mock
    private ApplianceDtoValidator validatorMock;

    @Mock
    private UserContextApi userContext;

    @Mock
    private EncryptionApi encryption;

    @Mock
    DBConnectionManager dbMgr;

    @Mock
    private TransactionalBroadcastUtil txBroadcastUtil;

    @InjectMocks
    private AddApplianceService service;

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
    public void testDispatch_WhenApplianceValidationFails_ThrowsUnhandledException() throws Exception {
        // Arrange.
        ApplianceDto dto = new ApplianceDto(null, null, null);
        BaseRequest<ApplianceDto> request = new BaseRequest<>(dto);

        doThrow(VmidcBrokerValidationException.class).when(this.validatorMock).validateForCreate(dto);
        this.exception.expect(VmidcBrokerValidationException.class);

        // Act.
        this.service.dispatch(request);
    }

    @Test
    public void testDispatch_WhenApplianceValidationSucceeds_ApplianceIsPersisted() throws Exception {
        // Arrange.
        ApplianceDto dto = new ApplianceDto(UUID.randomUUID().toString(), UUID.randomUUID().toString(), UUID.randomUUID().toString());
        BaseRequest<ApplianceDto> request = new BaseRequest<>(dto);
        Long applianceId = 111L;

        Mockito.doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                Appliance appliance = invocation.getArgumentAt(0, Appliance.class);
                appliance.setId(applianceId);
                return null;
            }
        }).when(this.em).persist(Mockito.argThat(new ApplianceMatcher(dto.getManagerType(), dto.getModel(), dto.getManagerVersion())));

        // Act.
        BaseResponse response = this.service.dispatch(request);

        // Assert
        assertNotNull("The returned response should not be null", response);
        assertEquals("The returned appliance id was different than expected", applianceId, response.getId());
    }

    private class ApplianceMatcher extends ArgumentMatcher<Object> {
        private String managerType;
        private String model;
        private String softwareVersion;

        public ApplianceMatcher(String managerType, String model, String softwareVersion) {
            this.managerType = managerType;
            this.model = model;
            this.softwareVersion = softwareVersion;
        }

        @Override
        public boolean matches(Object object) {
            if (object == null || !(object instanceof Appliance)) {
                return false;
            }
            Appliance appliance = (Appliance)object;
            return this.model.equals(appliance.getModel()) &&
                    this.managerType.equals(appliance.getManagerType()) &&
                    this.softwareVersion.equals(appliance.getManagerSoftwareVersion());
        }
    }
}
