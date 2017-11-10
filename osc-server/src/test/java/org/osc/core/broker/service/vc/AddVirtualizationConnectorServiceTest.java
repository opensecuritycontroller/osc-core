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
package org.osc.core.broker.service.vc;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;
import static org.osc.core.broker.service.vc.VirtualizationConnectorServiceData.OPENSTACK_NAME_ALREADY_EXISTS_NSC_REQUEST;
import static org.osc.core.broker.service.vc.VirtualizationConnectorServiceData.OPENSTACK_NOCONTROLLER_REQUEST;

import java.util.ArrayList;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

import org.junit.After;
import org.junit.Assert;
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
import org.osc.core.broker.job.Job;
import org.osc.core.broker.model.entities.virtualization.VirtualizationConnector;
import org.osc.core.broker.service.LockUtil;
import org.osc.core.broker.service.VirtualizationConnectorConformJobFactory;
import org.osc.core.broker.service.api.server.EncryptionApi;
import org.osc.core.broker.service.api.server.UserContextApi;
import org.osc.core.broker.service.exceptions.VmidcBrokerValidationException;
import org.osc.core.broker.service.request.ErrorTypeException;
import org.osc.core.broker.service.request.ErrorTypeException.ErrorType;
import org.osc.core.broker.service.response.BaseJobResponse;
import org.osc.core.broker.service.response.BaseResponse;
import org.osc.core.broker.service.ssl.CertificateResolverModel;
import org.osc.core.broker.service.ssl.SslCertificatesExtendedException;
import org.osc.core.broker.service.tasks.conformance.UnlockObjectTask;
import org.osc.core.broker.service.test.InMemDB;
import org.osc.core.broker.service.validator.AddVirtualizationConnectorServiceRequestValidator;
import org.osc.core.broker.util.TransactionalBroadcastUtil;
import org.osc.core.broker.util.crypto.X509TrustManagerFactory;
import org.osc.core.broker.util.db.DBConnectionManager;
import org.osc.core.test.util.TestTransactionControl;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({X509TrustManagerFactory.class, LockUtil.class })
public class AddVirtualizationConnectorServiceTest {
    @Rule
    public ExpectedException exception = ExpectedException.none();

    private EntityManager em;

    @Mock
    private Job job;

    @Mock
    private UnlockObjectTask unlockObjectTask;

    @Mock
    private AddVirtualizationConnectorServiceRequestValidator validatorMock;

    @Mock
    private VirtualizationConnectorConformJobFactory vcConformJobFactory;

    @Mock
    private UserContextApi userContext;

    @Mock
    private EncryptionApi encryption;

    @Mock
    private DBConnectionManager dbmgr;

    @Mock
    private TransactionalBroadcastUtil txBroadcastUtil;

    @InjectMocks()
    private AddVirtualizationConnectorService service;

    @Mock(answer=Answers.CALLS_REAL_METHODS)
    private TestTransactionControl txControl;

    private static final String NAME_ALREADY_EXISTS = "Name already exists in the System";

    @Before
    public void testInitialize() throws Exception {
        MockitoAnnotations.initMocks(this);

        EntityManagerFactory entityManagerFactory = InMemDB.getEntityManagerFactory();
        this.em = entityManagerFactory.createEntityManager();

        Mockito.when(this.dbmgr.getTransactionalEntityManager()).thenReturn(this.em);
        Mockito.when(this.dbmgr.getTransactionControl()).thenReturn(this.txControl);

        this.txControl.setEntityManager(this.em);

        populateDatabase();

        PowerMockito.mockStatic(X509TrustManagerFactory.class);
        when(X509TrustManagerFactory.getInstance()).thenReturn(mock(X509TrustManagerFactory.class));

        when(this.encryption.encryptAESCTR(any(String.class))).thenReturn("Encrypted String");

        when(this.job.getId()).thenReturn(5L);
        //PowerMockito.mockStatic(ConformService.class);
        when(this.vcConformJobFactory.startVCSyncJob(any(VirtualizationConnector.class), any(EntityManager.class))).thenReturn(this.job);
    }

    @After
    public void testTearDown() {
        InMemDB.shutdown();
    }

    private void populateDatabase() {
        this.em.getTransaction().begin();

        this.em.getTransaction().commit();
    }

    @Test
    public void testDispatch_WhenOpenStackRequest_ReturnsResponse() throws Exception {

        // Arrange.
        doNothing().when(this.validatorMock).validate(OPENSTACK_NOCONTROLLER_REQUEST);

        // Act.
        BaseJobResponse response = this.service.dispatch(OPENSTACK_NOCONTROLLER_REQUEST);

        // Assert.
        VirtualizationConnector vc = this.em.createQuery("Select vc from VirtualizationConnector vc where vc.name = '" + OPENSTACK_NOCONTROLLER_REQUEST.getDto().getName() + "'", VirtualizationConnector.class)
                .getSingleResult();
        validateResponse(response, vc.getId());
        verify(this.validatorMock).validate(OPENSTACK_NOCONTROLLER_REQUEST);
        Assert.assertNotNull("Not updated", vc.getUpdatedTimestamp());
        Assert.assertTrue("Job id should be equal", 5L == response.getJobId());
    }

    @Test
    public void testDispatch_WhenVcNameAlreadyExists_ThrowsValidationException() throws Exception {

        // Arrange.
        this.exception.expect(VmidcBrokerValidationException.class);
        doThrow(new VmidcBrokerValidationException(NAME_ALREADY_EXISTS)).when(this.validatorMock)
        .validate(VirtualizationConnectorServiceData.OPENSTACK_NAME_ALREADY_EXISTS_NSC_REQUEST);

        // Act.
        this.service.dispatch(VirtualizationConnectorServiceData.OPENSTACK_NAME_ALREADY_EXISTS_NSC_REQUEST);

        // Assert.
        verify(this.validatorMock)
        .validate(VirtualizationConnectorServiceData.OPENSTACK_NAME_ALREADY_EXISTS_NSC_REQUEST);
    }

    @Test
    public void testDispatch_WhenValidationThrowsSSLCertificateException_WhenForceAddCertificate_ThrowsSslCertificateException() throws Exception {

        // Arrange.
        this.exception.expect(SslCertificatesExtendedException.class);
        ErrorTypeException exception = new ErrorTypeException("Error Thrown", ErrorType.CONTROLLER_EXCEPTION);

        OPENSTACK_NAME_ALREADY_EXISTS_NSC_REQUEST.getDto().setForceAddSSLCertificates(true);

        doThrow(new SslCertificatesExtendedException(exception, new ArrayList<CertificateResolverModel>())).when(this.validatorMock)
        .validate(VirtualizationConnectorServiceData.OPENSTACK_NAME_ALREADY_EXISTS_NSC_REQUEST);

        // Act.
        this.service.dispatch(VirtualizationConnectorServiceData.OPENSTACK_NAME_ALREADY_EXISTS_NSC_REQUEST);

        // Assert.
        verify(this.validatorMock)
        .validate(VirtualizationConnectorServiceData.OPENSTACK_NAME_ALREADY_EXISTS_NSC_REQUEST);

        // clean up
        OPENSTACK_NAME_ALREADY_EXISTS_NSC_REQUEST.getDto().setForceAddSSLCertificates(false);
    }

    @Test
    public void testDispatch_WhenValidationThrowsSSLCertificateException_WhenForceAddCertificate_ReturnsResponse() throws Exception {

        // Arrange.
        ErrorTypeException exception = new ErrorTypeException("Error Thrown", ErrorType.CONTROLLER_EXCEPTION);

        OPENSTACK_NAME_ALREADY_EXISTS_NSC_REQUEST.getDto().setForceAddSSLCertificates(true);

        doThrow(new SslCertificatesExtendedException(exception, new ArrayList<>())).doNothing().when(this.validatorMock)
        .validate(OPENSTACK_NAME_ALREADY_EXISTS_NSC_REQUEST);

        // Act.
        BaseJobResponse response = this.service.dispatch(OPENSTACK_NAME_ALREADY_EXISTS_NSC_REQUEST);

        // Assert.
        verify(this.validatorMock, times(2))
        .validate(OPENSTACK_NAME_ALREADY_EXISTS_NSC_REQUEST);
        VirtualizationConnector vc = this.em.createQuery("Select vc from VirtualizationConnector vc where vc.name = '" + OPENSTACK_NAME_ALREADY_EXISTS_NSC_REQUEST.getDto().getName() + "'", VirtualizationConnector.class)
                .getSingleResult();
        validateResponse(response, vc.getId());
        Assert.assertNotNull("Not updated", vc.getUpdatedTimestamp());

        // clean up
        OPENSTACK_NAME_ALREADY_EXISTS_NSC_REQUEST.getDto().setForceAddSSLCertificates(false);
    }

    private void validateResponse(BaseResponse response, Long id) {

        Assert.assertNotNull("Response shouldn't be null", response);
        Assert.assertEquals("Both VC id's should be equal", id, response.getId());
    }
}
