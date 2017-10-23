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
package org.osc.core.broker.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import javax.persistence.EntityManager;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.osc.core.broker.service.api.server.EncryptionApi;
import org.osc.core.broker.service.api.server.UserContextApi;
import org.osc.core.broker.service.exceptions.VmidcBrokerInvalidEntryException;
import org.osc.core.broker.service.request.AddUserRequest;
import org.osc.core.broker.service.response.AddUserResponse;
import org.osc.core.broker.service.test.InMemDB;
import org.osc.core.broker.service.validator.UserDtoValidator;
import org.osc.core.broker.util.TransactionalBroadcastUtil;
import org.osc.core.broker.util.db.DBConnectionManager;
import org.osc.core.test.util.TestTransactionControl;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
public class AddUserServiceTest {
    @Rule
    public ExpectedException exception = ExpectedException.none();

    private EntityManager em;

    @Mock(answer=Answers.CALLS_REAL_METHODS)
    TestTransactionControl txControl;

    @Mock
    private UserDtoValidator validatorMock;

    @Mock
    private UserContextApi userContext;

    @Mock
    private EncryptionApi encryption;

    @Mock
    private DBConnectionManager dbMgr;

    @Mock
    private TransactionalBroadcastUtil txBroadcastUtil;

    @InjectMocks
    private AddUserService service;

    private AddUserRequest invalidUserRequest;

    @Before
    public void testInitialize() throws Exception{
        MockitoAnnotations.initMocks(this);

        this.em = InMemDB.getEntityManagerFactory().createEntityManager();

        this.txControl.setEntityManager(this.em);

        Mockito.when(this.dbMgr.getTransactionalEntityManager()).thenReturn(this.em);
        Mockito.when(this.dbMgr.getTransactionControl()).thenReturn(this.txControl);


        this.invalidUserRequest = new AddUserRequest();
        this.invalidUserRequest.setLoginName("invalidUserName");

        Mockito.doThrow(VmidcBrokerInvalidEntryException.class).when(this.validatorMock).validateForCreate(this.invalidUserRequest);
    }

    @After
    public void testTearDown() {
        InMemDB.shutdown();
    }

    @Test
    public void testDispatch_WhenUserValidationFails_ThrowsInvalidEntryException() throws Exception{
        // Arrange.
        this.exception.expect(VmidcBrokerInvalidEntryException.class);

        // Act.
        this.service.dispatch(this.invalidUserRequest);

        // Assert.
        Mockito.verify(this.validatorMock).validateForCreate(this.invalidUserRequest);
    }

    @Test
    public void testDispatch_AddingNewUser_ExpectsValidResponse() throws Exception{
        // Arrange.
        AddUserRequest request = new AddUserRequest();
        request.setLoginName("userName");

        // Act.
        AddUserResponse response = this.service.dispatch(request);

        // Assert.
        Mockito.verify(this.validatorMock).validateForCreate(request);
        assertNotNull("The response of add user should not be null.", response);
        assertEquals("The returned id was different than expected.",
                this.em.createQuery("Select u.id from User u where u.loginName = 'userName'").getSingleResult(),
                        response.getId());
    }
}
