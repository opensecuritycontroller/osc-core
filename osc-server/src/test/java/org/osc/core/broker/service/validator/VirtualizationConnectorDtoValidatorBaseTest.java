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
package org.osc.core.broker.service.validator;

import static org.osc.core.broker.service.vc.VirtualizationConnectorServiceData.*;

import javax.persistence.EntityManager;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.osc.core.broker.service.test.InMemDB;
import org.osc.core.broker.util.TransactionalBroadcastUtil;

/**
 * The base class for the {@link VirtualizationConnectorDtoValidator} unit tests.
 * The unit tests for {@link VirtualizationConnectorDtoValidator} have been split in two test classes.
 * The reason is because the runner {@link org.junit.runners.Parameterized} only supports data driven tests to be within the test class,
 * other non data driven tests need to go on a different test class.
 * We could optionally use the {@link junitparams.JUnitParamsRunner}, which supports mixing data driven and non data driven
 * tests on the same class (as it was before) but this runner is not compatible with {@link org.powermock.modules.junit4.PowerMockRunner} now needed for these tests.
 */
public class VirtualizationConnectorDtoValidatorBaseTest {

    @Mock
    TransactionalBroadcastUtil txBroadcastUtil;

    private EntityManager em;

    @Rule
    public ExpectedException exception = ExpectedException.none();

    protected VirtualizationConnectorDtoValidator dtoValidator;

    @Before
    public void testInitialize() {
        MockitoAnnotations.initMocks(this);

        this.em = InMemDB.getEntityManagerFactory().createEntityManager();

        populateDatabase();

        this.dtoValidator = new VirtualizationConnectorDtoValidator(this.em, this.txBroadcastUtil);

    }

    @After
    public void testTearDown() {
        InMemDB.shutdown();
    }

    private void populateDatabase() {
       this.em.getTransaction().begin();

       this.em.persist(createVirtualisationConnector(VMWARE_NAME_ALREADY_EXISTS,
               CONTROLLER_IP_ALREADY_EXISTS, PROVIDER_IP_ALREADY_EXISTS));

       this.em.persist(createVirtualisationConnector(OPENSTACK_NAME_ALREADY_EXISTS,
               CONTROLLER_IP_ALREADY_EXISTS_2, PROVIDER_IP_ALREADY_EXISTS_2));

       this.em.getTransaction().commit();

    }
}
