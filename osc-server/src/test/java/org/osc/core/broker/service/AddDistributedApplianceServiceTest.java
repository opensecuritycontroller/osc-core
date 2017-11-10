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

import java.text.MessageFormat;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.ArgumentMatcher;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.internal.util.collections.Sets;
import org.mockito.runners.MockitoJUnitRunner;
import org.osc.core.broker.model.entities.appliance.Appliance;
import org.osc.core.broker.model.entities.appliance.ApplianceSoftwareVersion;
import org.osc.core.broker.model.entities.appliance.DistributedAppliance;
import org.osc.core.broker.model.entities.management.ApplianceManagerConnector;
import org.osc.core.broker.model.entities.management.Domain;
import org.osc.core.broker.model.entities.virtualization.VirtualizationConnector;
import org.osc.core.broker.service.api.server.EncryptionApi;
import org.osc.core.broker.service.api.server.UserContextApi;
import org.osc.core.broker.service.dto.DistributedApplianceDto;
import org.osc.core.broker.service.dto.VirtualSystemDto;
import org.osc.core.broker.service.exceptions.VmidcBrokerInvalidEntryException;
import org.osc.core.broker.service.request.BaseRequest;
import org.osc.core.broker.service.response.AddDistributedApplianceResponse;
import org.osc.core.broker.service.test.InMemDB;
import org.osc.core.broker.service.validator.DistributedApplianceDtoValidator;
import org.osc.core.broker.util.TransactionalBroadcastUtil;
import org.osc.core.broker.util.db.DBConnectionManager;
import org.osc.core.common.virtualization.VirtualizationType;
import org.osc.core.test.util.TestTransactionControl;

@RunWith(MockitoJUnitRunner.class)
public class AddDistributedApplianceServiceTest {

    private static final String SECRET_KEY = "secret";
    private static final String ENCRYPTED_KEY = "encrypted";

    @Rule
    public ExpectedException exception = ExpectedException.none();

    private EntityManager em;

    @Mock(answer=Answers.CALLS_REAL_METHODS)
    TestTransactionControl txControl;

    @Mock
    private DistributedApplianceDtoValidator validatorMock;

    @Mock
    private DistributedApplianceConformJobFactory daConformJobFactoryMock;

    @Mock
    private UserContextApi userContext;

    @Mock
    private EncryptionApi encryption;

    @Mock
    DBConnectionManager dbMgr;

    @Mock
    private TransactionalBroadcastUtil txBroadcastUtil;

    @InjectMocks
    private AddDistributedApplianceService service;

    private BaseRequest<DistributedApplianceDto> request;
    private Appliance app;
    private ApplianceManagerConnector amc;
    private ApplianceSoftwareVersion asv;
    private DistributedApplianceDto daDto;
    private VirtualSystemDto vsDto;
    private VirtualizationConnector vc;
    private Domain domain;

    private String invalidDaName = "invalidDaName";

    @Before
    public void testInitialize() throws Exception {
        MockitoAnnotations.initMocks(this);

        // @InjectMocks should do this
        this.service.validator = this.validatorMock;

        this.em = InMemDB.getEntityManagerFactory().createEntityManager();

        this.txControl.setEntityManager(this.em);

        Mockito.when(this.encryption.encryptAESCTR(SECRET_KEY)).
            thenReturn(ENCRYPTED_KEY);
        Mockito.when(this.encryption.decryptAESCTR(ENCRYPTED_KEY)).
            thenReturn(SECRET_KEY);

        Mockito.when(this.dbMgr.getTransactionalEntityManager()).thenReturn(this.em);
        Mockito.when(this.dbMgr.getTransactionControl()).thenReturn(this.txControl);


        populateDatabase();

        this.request = new BaseRequest<DistributedApplianceDto>();
        this.daDto = new DistributedApplianceDto();
        this.daDto.setName("daName");
        this.daDto.setSecretKey(SECRET_KEY);
        this.request.setDto(this.daDto);

        this.daDto.setApplianceId(this.app.getId());
        this.daDto.setApplianceSoftwareVersionName(this.asv.getApplianceSoftwareVersion());
        this.daDto.setMcId(this.amc.getId());

        this.vsDto = new VirtualSystemDto();
        this.vsDto.setVcId(this.vc.getId());
        this.vsDto.setDomainId(this.domain.getId());
        this.daDto.setVirtualizationSystems(Sets.newSet(this.vsDto));

        Mockito.doThrow(VmidcBrokerInvalidEntryException.class).when(this.validatorMock)
        .validateForCreate(Mockito.argThat(new DistributedApplianceDtoMatcher(this.invalidDaName)));

    }

    @After
    public void testTearDown() {
        InMemDB.shutdown();
    }

    private void populateDatabase() {
       this.em.getTransaction().begin();

       this.app = new Appliance();
       this.app.setManagerSoftwareVersion("fizz");
       this.app.setManagerType("buzz");
       this.app.setModel("fizzbuzz");

       this.em.persist(this.app);

       this.amc = new ApplianceManagerConnector();
       this.amc.setManagerType("buzz");
       this.amc.setIpAddress("127.0.0.1");
       this.amc.setName("Steve");
       this.amc.setServiceType("foobar");

       this.em.persist(this.amc);

       this.domain = new Domain(this.amc);
       this.domain.setName("domainName");

       this.em.persist(this.domain);

       this.vc = new VirtualizationConnector();
       this.vc.setVirtualizationType(VirtualizationType.OPENSTACK);
       this.vc.setVirtualizationSoftwareVersion("vcSoftwareVersion");
       this.vc.setName("vcName");
       this.vc.setProviderIpAddress("127.0.0.1");
       this.vc.setProviderUsername("Natasha");
       this.vc.setProviderPassword("********");

       this.em.persist(this.vc);

       this.asv = new ApplianceSoftwareVersion(this.app);
       this.asv.setApplianceSoftwareVersion("softwareVersion");
       this.asv.setImageUrl("bar");
       this.asv.setVirtualizarionSoftwareVersion(this.vc.getVirtualizationSoftwareVersion());
       this.asv.setVirtualizationType(this.vc.getVirtualizationType());

       this.em.persist(this.asv);

       this.em.getTransaction().commit();
    }

    @Test
    public void testDispatch_WhenDistributedApplianceValidationFails_ThrowsInvalidEntryException() throws Exception {
        // Arrange.
        this.daDto.setName(this.invalidDaName);
        this.exception.expect(VmidcBrokerInvalidEntryException.class);

        // Act.
        this.service.dispatch(this.request);

        // Assert.
        Mockito.verify(this.validatorMock).validateForCreate(this.request.getDto());
    }

    @Test
    public void testDispatch_AddingNewAppliance_ExpectsValidResponse() throws Exception {
        // Arrange.
        Long jobId = new Long(1234L);

        Mockito.when(this.daConformJobFactoryMock.startDAConformJob(Mockito.any(EntityManager.class), (DistributedAppliance)Mockito.argThat(new DistributedApplianceMatcher(this.daDto.getName())))).thenReturn(jobId);

        // Act.
        AddDistributedApplianceResponse response = this.service.dispatch(this.request);

        // Assert.
        Mockito.verify(this.validatorMock).validateForCreate(this.request.getDto());
        Assert.assertNotNull("The returned response should not be null.", response);
        Assert.assertNotNull("The secret key should not be null when the request is not "
                + "orginated from the REST API.", response.getSecretKey());
        Assert.assertEquals("The job id was different than expected.", jobId, response.getJobId());
        Assert.assertEquals("The response id was different than expected.",
                this.em.createQuery("Select da.id from DistributedAppliance da where da.name = 'daName'").getSingleResult(),
                response.getId());
        Assert.assertEquals("The response name was different than expected.", this.daDto.getName(), response.getName());
        Assert.assertEquals("The count of virtualization systems was different than expected.", this.daDto.getVirtualizationSystems().size(), response.getVirtualizationSystems().size());
        for (VirtualSystemDto vs: this.daDto.getVirtualizationSystems()) {
            TypedQuery<Long> query = this.em.createQuery("Select vs.id from VirtualSystem vs where vs.distributedAppliance.id = ?", Long.class);
            query.setParameter(0, response.getId());
            vs.setId(query.getSingleResult());
            Assert.assertTrue(MessageFormat.format("The expected vs with id {0} was not found.", vs.getId()),
                    contains(response.getVirtualizationSystems(), vs));
        }
    }

    private boolean contains(Set<VirtualSystemDto> vsDtos, VirtualSystemDto expectedVsDto) {
        if (vsDtos == null) {
            return false;
        }

        for (VirtualSystemDto vsDto : vsDtos) {
            if (vsDto.getId() == expectedVsDto.getId() &&
                    vsDto.getDomainId() == this.domain.getId() &&
                    vsDto.getVcId() == this.vc.getId()) {
                return true;
            }
        }

        return false;
    }

    private class DistributedApplianceDtoMatcher extends ArgumentMatcher<DistributedApplianceDto> {
        private String daDtoName;

        public DistributedApplianceDtoMatcher(String daDtoName) {
            this.daDtoName = daDtoName;
        }

        @Override
        public boolean matches(Object object) {
            if (object == null || !(object instanceof DistributedApplianceDto)) {
                return false;
            }
            return this.daDtoName.equals(AddDistributedApplianceServiceTest.this.daDto.getName());
        }
    }

    private class DistributedApplianceMatcher extends ArgumentMatcher<Object> {
        private String daName;

        public DistributedApplianceMatcher(String daName) {
            this.daName = daName;
        }

        @Override
        public boolean matches(Object object) {
            if (object == null || !(object instanceof DistributedAppliance)) {
                return false;
            }
            DistributedAppliance da = (DistributedAppliance)object;
            return this.daName.equals(da.getName());
        }
    }
}
