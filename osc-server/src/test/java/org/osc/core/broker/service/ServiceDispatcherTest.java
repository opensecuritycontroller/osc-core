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

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.osc.core.broker.service.request.Request;
import org.osc.core.broker.service.response.Response;

public class ServiceDispatcherTest {

    private EntityManagerFactory mockEMF;
    private EntityManager mockEM;
    private EntityTransaction mockedTransaction;

    @Before
    public void setUp() {
        this.mockEMF = Mockito.mock(EntityManagerFactory.class);
        this.mockEM = Mockito.mock(EntityManager.class);
        this.mockedTransaction = Mockito.mock(EntityTransaction.class);
        Mockito.when(this.mockEMF.createEntityManager()).thenReturn(this.mockEM);
        Mockito.when(this.mockEM.getTransaction()).thenReturn(this.mockedTransaction);
    }

    @Test
    public void testExecuteValidRequest() throws Exception {
        ServiceDispatcher<?, ?> mockServiceDispatcher = new ServiceDispatcher<Request, Response>() {

            @Override
            public Response exec(Request request, EntityManager em) throws Exception {
                return null;
            }

            @Override
            protected EntityManagerFactory getEntityManagerFactory() {
                return ServiceDispatcherTest.this.mockEMF;
            }

        };
        mockServiceDispatcher.dispatch(null);
        Mockito.verify(this.mockedTransaction).commit();
    }

    @Test(expected = Exception.class)
    public void testExecuteInvalidRequest() throws Exception {
        ServiceDispatcher<?, ?> mockServiceDispatcher = new ServiceDispatcher<Request, Response>() {

            @Override
            protected EntityManagerFactory getEntityManagerFactory() {
                return ServiceDispatcherTest.this.mockEMF;
            }

            @Override
            public Response exec(Request request, EntityManager em) throws Exception {
                throw new Exception("");
            }

        };
        mockServiceDispatcher.dispatch(null);
        Mockito.verify(this.mockedTransaction).rollback();
    }

}
