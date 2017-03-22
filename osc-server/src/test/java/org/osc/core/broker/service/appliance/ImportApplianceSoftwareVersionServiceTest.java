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
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.spy;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;
import static org.powermock.api.mockito.PowerMockito.when;

import java.io.File;
import java.io.IOException;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

import org.apache.commons.io.FileUtils;
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
import org.osc.core.broker.model.entities.appliance.Appliance;
import org.osc.core.broker.model.entities.appliance.ApplianceSoftwareVersion;
import org.osc.core.broker.model.entities.appliance.VirtualizationType;
import org.osc.core.broker.model.image.ImageMetadata;
import org.osc.core.broker.model.plugin.ApiFactoryService;
import org.osc.core.broker.model.plugin.manager.ManagerApiFactory;
import org.osc.core.broker.model.plugin.manager.ManagerType;
import org.osc.core.broker.model.virtualization.VmwareSoftwareVersion;
import org.osc.core.broker.service.exceptions.VmidcBrokerValidationException;
import org.osc.core.broker.service.exceptions.VmidcException;
import org.osc.core.broker.service.response.BaseResponse;
import org.osc.core.broker.service.test.InMemDB;
import org.osc.core.broker.util.db.HibernateUtil;
import org.osc.core.util.FileUtil;
import org.osc.core.util.ServerUtil;
import org.osc.core.util.VersionUtil.Version;
import org.osc.sdk.manager.api.ApplianceManagerApi;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest( {ServerUtil.class, FileUtils.class, FileUtil.class, HibernateUtil.class, ManagerApiFactory.class})
public class ImportApplianceSoftwareVersionServiceTest {

    private static final Long APPLIANCE_ID = 123L;

    private static final String META_JSON_FILE_NAME = "meta.json";

    private static final String MANAGER_VERSION = "8.2";

    private static final String SOFTWARE_MODEL = "valid_model";
    private static final String NON_EXISTING_SOFTWARE_MODEL = "invalid_model";

    private static final String OVF_IMAGE_NAME = "Foo.ovf";

    private static final String SOFTWARE_VERSION = "a_version";
    private static final String NON_EXISTING_SOFTWARE_VERSION = "invalid_version";

    //TODO: Future. Testing. Expected Exception reporting does not work as expected. Makes it harder to debug test failures.
    // test in testDispatch_ImportApplianceMissingPayloadFile_ExpectsErrorResponse
    // see https://github.com/jayway/powermock/issues/396
    @Rule
    public ExpectedException exception = ExpectedException.none();

    private EntityManager em;

    @Mock
    private ImageMetadataValidator imageMetaDataValidator;

    @Mock
    private File tmpUploadFolder;

    @Mock
    private ImageMetadata imageMetaData;

    @Mock
    private ApiFactoryService apiFactoryService;

    @InjectMocks
    private ImportApplianceSoftwareVersionService service;

    private ApplianceSoftwareVersion validAsv;

    @Before
    public void testInitialize() throws Exception {
        MockitoAnnotations.initMocks(this);

        EntityManagerFactory entityManagerFactory = InMemDB.getEntityManagerFactory();

        PowerMockito.mockStatic(HibernateUtil.class);
        Mockito.when(HibernateUtil.getEntityManagerFactory()).thenReturn(entityManagerFactory);

        this.em = entityManagerFactory.createEntityManager();

        File mockMetaDataFile = mock(File.class);
        File mockPayloadFile = mock(File.class);

        when(mockMetaDataFile.getName()).thenReturn(META_JSON_FILE_NAME);
        when(mockPayloadFile.getName()).thenReturn(OVF_IMAGE_NAME);

        PowerMockito.mockStatic(FileUtil.class);
        PowerMockito.when(FileUtil.getFileListFromDirectory(anyString())).thenReturn(new File[] { mockMetaDataFile, mockPayloadFile});

        // Image meta data setup
        when(this.imageMetaData.getImageName()).thenReturn(OVF_IMAGE_NAME);
        when(this.imageMetaData.getVirtualizationType()).thenReturn(org.osc.core.broker.model.virtualization.VirtualizationType.VMWARE);
        when(this.imageMetaData.getModel()).thenReturn(SOFTWARE_MODEL);
        when(this.imageMetaData.getManagerType()).thenReturn(ManagerType.NSM);
        when(this.imageMetaData.getManagerVersion()).thenReturn(MANAGER_VERSION);
        when(this.imageMetaData.getVirtualizationType()).thenReturn(org.osc.core.broker.model.virtualization.VirtualizationType.VMWARE);
        when(this.imageMetaData.getVmwareVirtualizationVersion()).thenReturn(org.osc.core.broker.model.virtualization.VmwareSoftwareVersion.VMWARE_V5_5);
        when(this.imageMetaData.getVirtualizationVersionString()).thenReturn(VmwareSoftwareVersion.VMWARE_V5_5.toString());
        when(this.imageMetaData.getSoftwareVersion()).thenReturn(SOFTWARE_VERSION);
        when(this.imageMetaData.getImageName()).thenReturn(OVF_IMAGE_NAME);
        when(this.imageMetaData.getMinIscVersion()).thenReturn(new Version(9L, 9L, "9-abc"));
        when(this.imageMetaData.getMinCpus()).thenReturn(4);
        when(this.imageMetaData.getMemoryInMb()).thenReturn(4);
        when(this.imageMetaData.getDiskSizeInGb()).thenReturn(4);
        when(this.imageMetaData.hasAdditionalNicForInspection()).thenReturn(Boolean.FALSE);

        mockStatic(FileUtils.class, Answers.RETURNS_SMART_NULLS.get());

        spy(ServerUtil.class);
        when(ServerUtil.isEnoughSpace()).thenReturn(Boolean.TRUE);

        // Appliance Mocking
        Appliance mockExistingMatchingAppliance = mock(Appliance.class);
        when(mockExistingMatchingAppliance.getManagerType()).thenReturn(ManagerType.NSM.getValue());
        when(mockExistingMatchingAppliance.getManagerSoftwareVersion()).thenReturn(MANAGER_VERSION);
        when(mockExistingMatchingAppliance.getId()).thenReturn(APPLIANCE_ID);

        ApplianceManagerApi applianceMgrPolicyMappingSupported = Mockito.mock(ApplianceManagerApi.class);

        PowerMockito.mockStatic(ManagerApiFactory.class);
        Mockito.when(this.apiFactoryService.createApplianceManagerApi(ManagerType.NSM))
                .thenReturn(applianceMgrPolicyMappingSupported);
    }

    @After
    public void testTearDown() {
        InMemDB.shutdown();
    }

    @Test
    public void testDispatch_ImportApplianceNonExistingModel_ExpectsValidResponse() throws Exception {
        // Arrange.
        when(this.imageMetaData.getModel()).thenReturn(NON_EXISTING_SOFTWARE_MODEL);

        // Act.
        BaseResponse response = this.service.dispatch(new ImportFileRequest(""));

        // Assert.
        Appliance appliance = this.em.createQuery(
                "Select a from Appliance a where a.model = '" + NON_EXISTING_SOFTWARE_MODEL + "'",
                Appliance.class).getSingleResult();
        Assert.assertNotNull(appliance);
//        verify(this.sessionMock).save(argThat(new ApplianceMatcher(NON_EXISTING_SOFTWARE_MODEL)));
        verifySuccessfulImport(response, appliance.getId());
    }

    @Test
    public void testDispatch_ImportApplianceWithExistingModel_ExpectsValidResponse() throws Exception {
        // Arrange.
        when(this.imageMetaData.getModel()).thenReturn(SOFTWARE_MODEL);

        // Act.
        BaseResponse response = this.service.dispatch(new ImportFileRequest(""));

        // Assert.
        Appliance appliance = this.em.createQuery(
                "Select a from Appliance a where a.model = '" + SOFTWARE_MODEL + "'",
                Appliance.class).getSingleResult();
        Assert.assertNotNull(appliance);
        verifySuccessfulImport(response, appliance.getId());
    }

    @Test
    public void testDispatch_ReImportApplianceWithFileMissing_ExpectsValidResponse() throws Exception {
        // Arrange.
        when(this.imageMetaData.getModel()).thenReturn(NON_EXISTING_SOFTWARE_MODEL);

        // Act.
        BaseResponse response = this.service.dispatch(new ImportFileRequest(""));

        // Assert.
        Appliance appliance = this.em.createQuery(
                "Select a from Appliance a where a.model = '" + NON_EXISTING_SOFTWARE_MODEL + "'",
                Appliance.class).getSingleResult();
        Assert.assertNotNull(appliance);
        verifySuccessfulImport(response, appliance.getId());
    }

    // TODO: Arvind, looks like the file path indicated below not always is there.
    // The test currently fails for me due to the abscence of the file which is probably
    // the same reason why it fails on the official build. Please find a path
    // guaranteed to have the file or mock file.exists.
    ////@Test
    public void testDispatch_ReImportApplianceWithFilePresent_ExpectsErrorResponse() throws Exception {
        // Arrange.
        when(this.imageMetaData.getModel()).thenReturn(NON_EXISTING_SOFTWARE_MODEL);

        // Causes isImageMissing to return false, which means file is already present.
        this.validAsv.setImageUrl(".");
//        this.sessionStub.stubSaveEntity(new ApplianceSoftwareVersionMatcher(this.imageMetaData), ASV_ID);

        this.exception.expect(VmidcBrokerValidationException.class);
        this.exception.expectMessage("Virtualization Software Version already exists");

        // Act.
        this.service.dispatch(new ImportFileRequest(""));

    }

    @Test
    public void testDispatch_ImportApplianceWithSameURL_ExpectsErrorResponse() throws Exception {
        // Arrange.
        createExistingAppliance();
        when(this.imageMetaData.getModel()).thenReturn(NON_EXISTING_SOFTWARE_MODEL);
        when(this.imageMetaData.getSoftwareVersion()).thenReturn(NON_EXISTING_SOFTWARE_VERSION);

        this.exception.expect(VmidcBrokerValidationException.class);
        this.exception.expectMessage(" Cannot add an image with the same name.");

        // Act.
        this.service.dispatch(new ImportFileRequest(""));
    }

    private void createExistingAppliance() {
           this.em.getTransaction().begin();

           Appliance app = new Appliance();
           app.setManagerSoftwareVersion(NON_EXISTING_SOFTWARE_VERSION);
           app.setManagerType(ManagerType.NSM.getValue());
           app.setModel(SOFTWARE_MODEL);

           this.em.persist(app);

           ApplianceSoftwareVersion asv = new ApplianceSoftwareVersion(app);
           asv.setApplianceSoftwareVersion(NON_EXISTING_SOFTWARE_VERSION);
           asv.setImageUrl(OVF_IMAGE_NAME);
           asv.setVirtualizarionSoftwareVersion(VmwareSoftwareVersion.VMWARE_V5_5.toString());
           asv.setVirtualizationType(VirtualizationType.VMWARE);

           this.em.persist(asv);
    //
    //       this.sessionStub.stubFindApplianceByModel(SOFTWARE_MODEL, mockExistingMatchingAppliance);
    //       this.sessionStub.stubFindApplianceByModel(NON_EXISTING_SOFTWARE_MODEL, null);
    //
    //       this.sessionStub.stubSaveEntity(new InstanceOf(Appliance.class), APPLIANCE_ID);
    //
    //       // Appliance Software Version Mocking
    //       this.sessionStub.stubFindApplianceSoftwareVersion(
    //               APPLIANCE_ID, NON_EXISTING_SOFTWARE_VERSION,
    //               VirtualizationType.VMWARE,
    //               VmwareSoftwareVersion.VMWARE_V5_5.toString(),
    //               null);
    //
    //       this.sessionStub.stubFindApplianceSoftwareVersionByImageUrl(NON_EXISTING_OVF_IMAGE_NAME, null);
    //
    //       this.sessionStub.stubSaveEntity(new ApplianceSoftwareVersionMatcher(this.imageMetaData), ASV_ID);
    //
    //       this.validAsv = new ApplianceSoftwareVersion();
    //       this.validAsv.setId(ASV_ID);
    //
    //       this.sessionStub.stubFindApplianceSoftwareVersion(
    //               APPLIANCE_ID,
    //               SOFTWARE_VERSION,
    //               VirtualizationType.VMWARE,
    //               VmwareSoftwareVersion.VMWARE_V5_5.toString(),
    //               this.validAsv);
    //
    //       this.sessionStub.stubFindApplianceSoftwareVersionByImageUrl(OVF_IMAGE_NAME, this.validAsv);
    //
    //       ApplianceManagerConnector mcPolicyMappingSupported = new ApplianceManagerConnector();
    //       ManagerType mgrTypePolicyMappingSupported = ManagerType.NSM;
    //       mcPolicyMappingSupported.setManagerType(mgrTypePolicyMappingSupported.toString());
    //       Mockito.when(this.sessionMock.get(ApplianceManagerConnector.class, MC_ID_VALID_MC)).thenReturn(mcPolicyMappingSupported);
    //
           //Mockito.when(applianceMgrPolicyMappingSupported.isPolicyMappingSupported()).thenReturn(true);

           this.em.getTransaction().commit();
        }

    @Test
    public void testDispatch_ImportApplianceWithLowDiskSpace_ExpectsErrorResponse() throws Exception {
        // Arrange.
        spy(ServerUtil.class);
        when(ServerUtil.isEnoughSpace()).thenReturn(Boolean.FALSE);

        this.exception.expect(VmidcException.class);

        // Act.
        this.service.dispatch(new ImportFileRequest(""));

        // Assert.
        verifyStatic(Mockito.times(2));
        ServerUtil.isEnoughSpace();

    }

    @Test
    public void testDispatch_ImportApplianceMissingMetaDataFile_ExpectsErrorResponse() throws Exception {
        // Arrange. Make sure input is missing all files
        PowerMockito.when(FileUtil.getFileListFromDirectory(anyString())).thenReturn(new File[]{});

        this.exception.expect(VmidcBrokerValidationException.class);
        this.exception.expectMessage("Missing metadata file");

        // Act.
        this.service.dispatch(new ImportFileRequest(""));
    }

    @Test
    public void testDispatch_ImportApplianceMissingPayloadFile_ExpectsErrorResponse() throws Exception {
        // Arrange. Make sure input is missing a file
        File mockMetaDataFile = mock(File.class);

        when(mockMetaDataFile.getName()).thenReturn(META_JSON_FILE_NAME);
        PowerMockito.when(FileUtil.getFileListFromDirectory(anyString())).thenReturn(new File[] { mockMetaDataFile });

        this.exception.expect(VmidcBrokerValidationException.class);
        this.exception.expectMessage("missing in archive");

        // Act.
        this.service.dispatch(new ImportFileRequest(""));
    }

//    private class ApplianceSoftwareVersionMatcher extends ArgumentMatcher<Object> {
//
//        private ImageMetadata imageMetaData;
//
//        public ApplianceSoftwareVersionMatcher(ImageMetadata imageMetaData) {
//            this.imageMetaData = imageMetaData;
//        }
//
//        @Override
//        public boolean matches(Object argument) {
//            if (argument == null || !(argument instanceof ApplianceSoftwareVersion)) {
//                return false;
//            }
//            ApplianceSoftwareVersion otherAsv = (ApplianceSoftwareVersion) argument;
//
//            return (otherAsv.getAppliance().getId().equals(APPLIANCE_ID)
//                    && otherAsv.getApplianceSoftwareVersion().equals(this.imageMetaData.getSoftwareVersion())
//                    && otherAsv.getVirtualizationType().equals(this.imageMetaData.getVirtualizationType())
//                    && otherAsv.getVirtualizarionSoftwareVersion().equals(this.imageMetaData.getVirtualizationVersionString())
//                    && otherAsv.getImageUrl().equals(this.imageMetaData.getImageName())
//                    && otherAsv.getMinCpus() == this.imageMetaData.getMinCpus()
//                    && otherAsv.getMemoryInMb() == this.imageMetaData.getMemoryInMb()
//                    && otherAsv.getDiskSizeInGb() == this.imageMetaData.getDiskSizeInGb()
//                    && otherAsv.hasAdditionalNicForInspection() == this.imageMetaData.hasAdditionalNicForInspection()
//                    && CollectionUtils.isEqualCollection(otherAsv.getEncapsulationTypes(), this.imageMetaData.getEncapsulationTypes())
//                    && otherAsv.getImageProperties().equals(this.imageMetaData.getImageProperties())
//                    && otherAsv.getConfigProperties().equals(this.imageMetaData.getConfigProperties()));
//        }
//
//    }

//    private class ApplianceMatcher extends ArgumentMatcher<Object> {
//
//        private String model;
//
//        public ApplianceMatcher(String model) {
//            this.model = model;
//        }
//        @Override
//        public boolean matches(Object argument) {
//            if (argument == null || !(argument instanceof Appliance)) {
//                return false;
//            }
//            Appliance otherAppliance = (Appliance) argument;
//
//            return this.model.equals(otherAppliance.getModel());
//        }
//
//    }

    private void verifySuccessfulImport(BaseResponse response, Long id) throws IOException, Exception {
        // Verify Space check is made
        verifyStatic(Mockito.times(2));
        ServerUtil.isEnoughSpace();

        // validate is called
        verify(this.imageMetaDataValidator).validate(this.imageMetaData);

        // asv ID matches
        assertEquals("Appliance Software Version Id mismatch", id, response.getId());

        FileUtils.copyFileToDirectory(any(File.class), any(File.class), any(Boolean.class));

        verifyStatic();
        FileUtils.deleteDirectory(this.tmpUploadFolder);
    }

}
