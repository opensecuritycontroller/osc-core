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

import java.lang.reflect.Field;
import java.util.Set;

import javax.persistence.EntityManager;

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
import org.osc.core.broker.model.entities.appliance.Appliance;
import org.osc.core.broker.model.entities.appliance.ApplianceSoftwareVersion;
import org.osc.core.broker.model.entities.appliance.DistributedAppliance;
import org.osc.core.broker.model.entities.appliance.DistributedApplianceInstance;
import org.osc.core.broker.model.entities.appliance.VirtualSystem;
import org.osc.core.broker.model.entities.management.ApplianceManagerConnector;
import org.osc.core.broker.model.entities.management.Domain;
import org.osc.core.broker.model.entities.virtualization.VirtualizationConnector;
import org.osc.core.broker.model.plugin.ApiFactoryService;
import org.osc.core.broker.service.api.server.EncryptionApi;
import org.osc.core.broker.service.api.server.UserContextApi;
import org.osc.core.broker.service.dto.DistributedApplianceDto;
import org.osc.core.broker.service.dto.VirtualSystemDto;
import org.osc.core.broker.service.exceptions.VmidcBrokerInvalidEntryException;
import org.osc.core.broker.service.request.BaseRequest;
import org.osc.core.broker.service.response.BaseJobResponse;
import org.osc.core.broker.service.tasks.conformance.UnlockObjectMetaTask;
import org.osc.core.broker.service.test.InMemDB;
import org.osc.core.broker.service.validator.DistributedApplianceDtoValidator;
import org.osc.core.broker.util.TransactionalBroadcastUtil;
import org.osc.core.broker.util.db.DBConnectionManager;
import org.osc.core.common.virtualization.VirtualizationType;
import org.osc.core.test.util.TestTransactionControl;
import org.osc.sdk.manager.api.ManagerDeviceApi;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.google.common.collect.Sets;

@RunWith(PowerMockRunner.class)
@PrepareForTest(LockUtil.class)
public class UpdateDistributedApplianceServiceTest {
    private static long JOB_ID = 12345L;
    private static String NEW_APPLIANCE_SW_VERSION = "NEWVERSION";

    @Rule
    public ExpectedException exception = ExpectedException.none();

    private EntityManager em;

    @Mock(answer=Answers.CALLS_REAL_METHODS)
    TestTransactionControl txControl;

    @Mock(name = "daValidator")
    private DistributedApplianceDtoValidator validatorMock;

    @Mock
    private DistributedApplianceConformJobFactory daConformJobFactoryMock;

    @Mock
    private UserContextApi userContext;

    @Mock
    private EncryptionApi encryption;

    @Mock
    private ApiFactoryService apiFactoryService;

    @Mock
    private DBConnectionManager dbMgr;

    @Mock
    private TransactionalBroadcastUtil txBroadcastUtil;

    @InjectMocks
    private UpdateDistributedApplianceService service;

    private BaseRequest<DistributedApplianceDto> request;
    private Appliance app;
    private ApplianceManagerConnector amc;
    private ApplianceSoftwareVersion asv;
    private DistributedApplianceDto daDto;
    private DistributedAppliance da;
    private VirtualizationConnector vcToBeDeleted;
    private VirtualSystemDto vsDto;
    private VirtualSystemDto vsDtoToBeDeleted;
    private VirtualSystem vsToBeDeleted;
    private VirtualSystem vs;
    private DistributedApplianceInstance daInstance;
    private VirtualizationConnector vc;
    private Domain domain;
    private UnlockObjectMetaTask ult;

    private String invalidDaName = "invalidDaName";

    @Before
    public void testInitialize() throws Exception {
        MockitoAnnotations.initMocks(this);

        this.em = InMemDB.getEntityManagerFactory().createEntityManager();

        this.txControl.setEntityManager(this.em);

        Mockito.when(this.dbMgr.getTransactionalEntityManager()).thenReturn(this.em);
        Mockito.when(this.dbMgr.getTransactionControl()).thenReturn(this.txControl);

        Mockito.when(this.apiFactoryService.createManagerDeviceApi(Mockito.any())).thenReturn(Mockito.mock(ManagerDeviceApi.class));

        populateDatabase();

        this.request = new BaseRequest<>();
        this.daDto = new DistributedApplianceDto();
        this.request.setDto(this.daDto);

        this.daDto.setName(this.da.getName());
        this.daDto.setId(this.da.getId());
        this.daDto.setApplianceId(this.app.getId());
        this.daDto.setApplianceSoftwareVersionName(this.asv.getApplianceSoftwareVersion());
        this.daDto.setMcId(this.amc.getId());

        this.vsDto = new VirtualSystemDto();
        this.vsDto.setId(this.vs.getId());
        this.vsDto.setVcId(this.vc.getId());
        this.vsDto.setDomainId(this.domain.getId());

        this.vsDtoToBeDeleted = new VirtualSystemDto();
        this.vsDtoToBeDeleted.setId(this.vsToBeDeleted.getId());
        this.vsDtoToBeDeleted.setVcId(this.vcToBeDeleted.getId());
        this.vsDtoToBeDeleted.setDomainId(this.domain.getId());

        this.daDto.setVirtualizationSystems(Sets.newHashSet(this.vsDto, this.vsDtoToBeDeleted));

        this.ult = new UnlockObjectMetaTask(null);

        Mockito.doThrow(VmidcBrokerInvalidEntryException.class).when(this.validatorMock)
        .validateForUpdate(Mockito.argThat(new DistributedApplianceDtoMatcher(this.invalidDaName)));
        Mockito.doAnswer(i -> {

            Field f = ServiceDispatcher.class.getDeclaredField("em");
            f.setAccessible(true);
            return ((EntityManager)f.get(this.service)).find(DistributedAppliance.class, this.da.getId());

        }).when(this.validatorMock)
        .validateForUpdate(Mockito.argThat(new DistributedApplianceDtoMatcher(this.daDto.getName())));

        Mockito.when(this.daConformJobFactoryMock.startDAConformJob(Mockito.any(EntityManager.class),
                (DistributedAppliance)Mockito.argThat(new DistributedApplianceMatcher(this.da)),
                Mockito.any(UnlockObjectMetaTask.class))).thenReturn(JOB_ID);

        PowerMockito.mockStatic(LockUtil.class);
        Mockito.when(LockUtil.tryLockDA(this.da, this.da.getApplianceManagerConnector())).thenReturn(this.ult);

        ApiFactoryService apiFactoryService = Mockito.mock(ApiFactoryService.class);
        Mockito.when(apiFactoryService.createManagerDeviceApi(this.vs)).thenReturn(Mockito.mock(ManagerDeviceApi.class));
        Mockito.when(apiFactoryService.createManagerDeviceApi(this.vsToBeDeleted)).thenReturn(Mockito.mock(ManagerDeviceApi.class));
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

        this.vcToBeDeleted = new VirtualizationConnector();
        this.vcToBeDeleted.setVirtualizationType(VirtualizationType.OPENSTACK);
        this.vcToBeDeleted.setVirtualizationSoftwareVersion("vcSoftwareVersion");
        this.vcToBeDeleted.setName("vcToDeleteName");
        this.vcToBeDeleted.setProviderIpAddress("192.168.1.1");
        this.vcToBeDeleted.setProviderUsername("Bruce");
        this.vcToBeDeleted.setProviderPassword("********");

        this.em.persist(this.vcToBeDeleted);

        this.asv = new ApplianceSoftwareVersion(this.app);
        this.asv.setApplianceSoftwareVersion("softwareVersion");
        this.asv.setImageUrl("bar");
        this.asv.setVirtualizarionSoftwareVersion(this.vc.getVirtualizationSoftwareVersion());
        this.asv.setVirtualizationType(this.vc.getVirtualizationType());

        this.em.persist(this.asv);

        this.da = new DistributedAppliance(this.amc);
        this.da.setName("daName");
        this.da.setApplianceVersion(this.asv.getApplianceSoftwareVersion());
        this.da.setAppliance(this.app);

        this.em.persist(this.da);

        this.vsToBeDeleted = new VirtualSystem(this.da);
        this.vsToBeDeleted.setApplianceSoftwareVersion(this.asv);
        this.vsToBeDeleted.setDomain(this.domain);
        this.vsToBeDeleted.setVirtualizationConnector(this.vcToBeDeleted);
        this.vsToBeDeleted.setMarkedForDeletion(false);
        this.vsToBeDeleted.setName("vsToDeleteName");

        this.em.persist(this.vsToBeDeleted);
        this.da.addVirtualSystem(this.vsToBeDeleted);

        this.vs = new VirtualSystem(this.da);
        this.vs.setApplianceSoftwareVersion(this.asv);
        this.vs.setDomain(this.domain);
        this.vs.setVirtualizationConnector(this.vc);
        this.vs.setMarkedForDeletion(false);
        this.vs.setName("vsName");

        this.em.persist(this.vs);
        this.da.addVirtualSystem(this.vs);

        this.daInstance = new DistributedApplianceInstance(this.vs);
        this.daInstance.setName("daiName");
        this.daInstance.setApplianceConfig(new byte[3]);

        this.em.persist(this.daInstance);

        this.em.getTransaction().commit();
        this.em.clear();
     }

    @Test
    public void testDispatch_WhenDistributedApplianceValidationFails_ThrowsInvalidEntryException() throws Exception {
        // Arrange.
        this.daDto.setName(this.invalidDaName);
        this.exception.expect(VmidcBrokerInvalidEntryException.class);

        // Act.
        this.service.dispatch(this.request);

        // Assert.
        Mockito.verify(this.validatorMock).validateForUpdate(this.request.getDto());
    }

    @Test
    public void testDispatch_UsingExistingAppliance_ApplianceIsUpdated() throws Exception {
        // Act.
        BaseJobResponse response = this.service.dispatch(this.request);

        // Assert.
        assertDaUpdated(response);
    }

    @Test
    public void testDispatch_RemovingVirtualSystem_VirtualSystemIsDeleted() throws Exception {
        // Arrange.
        // If the da virtual system is not present on the correspondent daDto then it needs to be deleted.
        Set<VirtualSystemDto> virtualSystems = this.daDto.getVirtualizationSystems();
        virtualSystems.remove(this.vsDtoToBeDeleted);
        this.daDto.setVirtualizationSystems(virtualSystems);

        // Act.
        BaseJobResponse response = this.service.dispatch(this.request);

        // Assert.
        assertDaUpdated(response);
        Assert.assertTrue(
                this.em.createQuery("Select vs from VirtualSystem vs where vs.name = 'vsToDeleteName'", VirtualSystem.class)
                .getSingleResult().getMarkedForDeletion());
    }

    @Test
    public void testDispatch_ChangingTheSwVersion_DaInstanceIsUpdated() throws Exception {
        // Arrange.
        // If the da version is changing then the da instance must be updated.
        this.em.getTransaction().begin();
        ApplianceSoftwareVersion newVersion = new ApplianceSoftwareVersion(this.app);
        newVersion.setApplianceSoftwareVersion(NEW_APPLIANCE_SW_VERSION);
        newVersion.setImageUrl("foobar");
        newVersion.setVirtualizarionSoftwareVersion(this.vc.getVirtualizationSoftwareVersion());
        newVersion.setVirtualizationType(this.vc.getVirtualizationType());
        this.em.persist(newVersion);
        this.em.getTransaction().commit();
        this.daDto.setApplianceSoftwareVersionName(NEW_APPLIANCE_SW_VERSION);

        // Act.
        BaseJobResponse response = this.service.dispatch(this.request);

        // Assert.
        assertDaUpdated(response);
        Assert.assertNull("The da instance confi should be null.",
                this.em.createQuery("Select dai from DistributedApplianceInstance dai where dai.name = 'daiName'",
                        DistributedApplianceInstance.class).getSingleResult().getApplianceConfig());
    }

    private void assertDaUpdated(BaseJobResponse response) throws Exception {
        Mockito.verify(this.validatorMock).validateForUpdate(this.request.getDto());


        Assert.assertNotNull("Not updated",
                this.em.find(DistributedAppliance.class, this.da.getId()).getUpdatedTimestamp());

        PowerMockito.verifyStatic();
        LockUtil.tryLockDA(this.da, this.da.getApplianceManagerConnector());

        Assert.assertNotNull("The returned response should not be null.", response);
        Assert.assertEquals("The job id was different than expected.", JOB_ID, response.getJobId().longValue());
        Assert.assertNull("The response id should be null.", response.getId());
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
            return this.daDtoName.equals(UpdateDistributedApplianceServiceTest.this.daDto.getName());
        }
    }

    private class DistributedApplianceMatcher extends ArgumentMatcher<Object> {
        private DistributedAppliance da;

        public DistributedApplianceMatcher(DistributedAppliance da) {
            this.da = da;
        }

        @Override
        public boolean matches(Object object) {
            if (object == null || !(object instanceof DistributedAppliance)) {
                return false;
            }
            DistributedAppliance da = (DistributedAppliance)object;
            return this.da.getId() == da.getId() || this.da.getName().equals(da.getName());
        }
    }
}
