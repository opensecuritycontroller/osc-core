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

import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;
import static org.powermock.api.mockito.PowerMockito.*;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.spy;
import static org.powermock.api.mockito.PowerMockito.when;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;

import javax.persistence.EntityManager;

import org.apache.commons.io.FileUtils;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
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
import org.osc.core.broker.model.image.ImageMetadata;
import org.osc.core.broker.model.plugin.ApiFactoryService;
//import org.osc.core.broker.model.virtualization.VmwareSoftwareVersion;
import org.osc.core.broker.service.api.server.UserContextApi;
import org.osc.core.broker.service.exceptions.VmidcBrokerValidationException;
import org.osc.core.broker.service.exceptions.VmidcException;
import org.osc.core.broker.service.request.ImportFileRequest;
import org.osc.core.broker.service.response.BaseResponse;
import org.osc.core.broker.service.test.InMemDB;
import org.osc.core.broker.util.FileUtil;
import org.osc.core.broker.util.ServerUtil;
import org.osc.core.broker.util.TransactionalBroadcastUtil;
import org.osc.core.broker.util.VersionUtil.Version;
import org.osc.core.broker.util.db.DBConnectionManager;
import org.osc.core.common.manager.ManagerType;
import org.osc.core.common.virtualization.OpenstackSoftwareVersion;
import org.osc.core.common.virtualization.VirtualizationType;
import org.osc.core.server.Server;
import org.osc.core.test.util.TestTransactionControl;
import org.osc.sdk.manager.api.ApplianceManagerApi;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.google.gson.Gson;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ServerUtil.class, FileUtils.class, FileUtil.class})
public class ImportApplianceSoftwareVersionServiceTest {

    private static final String TEST_TMP_FOLDER = "testTmpFolder";
    private static final String TEST_UPLOAD_FOLDER = "testUploadFolder";

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

    @Mock(answer = Answers.CALLS_REAL_METHODS)
    private TestTransactionControl txControl;

    @Mock
    private ImageMetadataValidator imageMetaDataValidator;

    @Mock
    private ApiFactoryService apiFactoryService;

    @Mock
    private UserContextApi userContext;

    @Mock
    private DBConnectionManager dbMgr;

    @Mock
    private TransactionalBroadcastUtil txBroadcastUtil;

    @Mock
    private UploadConfig config;

    @InjectMocks
    private ImportApplianceSoftwareVersionService service;

    @Mock
    private Server mockServerInstance;

    private ApplianceSoftwareVersion validAsv;

    private ImageMetadata imageMetaData;

    private File mockMetaDataFile;

    @Before
    public void testInitialize() throws Exception {
        MockitoAnnotations.initMocks(this);

        this.em = InMemDB.getEntityManagerFactory().createEntityManager();

        this.txControl.setEntityManager(this.em);

        Mockito.when(this.dbMgr.getTransactionalEntityManager()).thenReturn(this.em);
        Mockito.when(this.dbMgr.getTransactionControl()).thenReturn(this.txControl);

        this.mockMetaDataFile = mock(File.class);
        File mockPayloadFile = mock(File.class);

        when(this.mockMetaDataFile.getName()).thenReturn(META_JSON_FILE_NAME);
        when(mockPayloadFile.getName()).thenReturn(OVF_IMAGE_NAME);

        PowerMockito.mockStatic(FileUtil.class);
        PowerMockito.when(FileUtil.getFileListFromDirectory(anyString())).thenReturn(new File[] { this.mockMetaDataFile, mockPayloadFile});

        this.imageMetaData = new ImageMetadata();
        this.imageMetaData.setImageName(OVF_IMAGE_NAME);
        this.imageMetaData.setVirtualizationType(VirtualizationType.OPENSTACK.toString());
        this.imageMetaData.setModel(SOFTWARE_MODEL);
        this.imageMetaData.setManagerType(ManagerType.NSM.toString());
        this.imageMetaData.setManagerVersion(MANAGER_VERSION);
        this.imageMetaData.setVirtualizationVersion(OpenstackSoftwareVersion.OS_ICEHOUSE.toString());
        this.imageMetaData.setSoftwareVersion(SOFTWARE_VERSION);
        this.imageMetaData.setImageName(OVF_IMAGE_NAME);
        this.imageMetaData.setMinIscVersion(new Version(9L, 9L, "9-abc"));
        this.imageMetaData.setMinCpus(4);
        this.imageMetaData.setMemoryInMb(4);
        this.imageMetaData.setDiskSizeInGb(4);

        ManagerType.addType("NSM");

        mockStatic(FileUtils.class, Answers.RETURNS_SMART_NULLS.get());

        spy(ServerUtil.class);
        when(ServerUtil.isEnoughSpace()).thenReturn(Boolean.TRUE);

        // Appliance Mocking
        Appliance mockExistingMatchingAppliance = mock(Appliance.class);
        when(mockExistingMatchingAppliance.getManagerType()).thenReturn(ManagerType.NSM.getValue());
        when(mockExistingMatchingAppliance.getManagerSoftwareVersion()).thenReturn(MANAGER_VERSION);
        when(mockExistingMatchingAppliance.getId()).thenReturn(APPLIANCE_ID);

        ApplianceManagerApi applianceMgrPolicyMappingSupported = Mockito.mock(ApplianceManagerApi.class);

        Mockito.when(this.apiFactoryService.createApplianceManagerApi(ManagerType.NSM))
        .thenReturn(applianceMgrPolicyMappingSupported);

        Mockito.when(this.config.upload_path()).thenReturn(TEST_UPLOAD_FOLDER);
        this.service.start(this.config);
    }

    @After
    public void testTearDown() {
        InMemDB.shutdown();
    }

    @Test
    public void testDispatch_ImportApplianceNonExistingModel_ExpectsValidResponse() throws Exception {
        // Arrange.
        this.imageMetaData.setModel(NON_EXISTING_SOFTWARE_MODEL);
        Mockito.when(FileUtils.readFileToString(this.mockMetaDataFile, Charset.defaultCharset()))
        .thenReturn(new Gson().toJson(this.imageMetaData));

        // Act.
        BaseResponse response = this.service.dispatch(new ImportFileRequest(TEST_TMP_FOLDER));

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
        this.imageMetaData.setModel(SOFTWARE_MODEL);
        Mockito.when(FileUtils.readFileToString(this.mockMetaDataFile, Charset.defaultCharset()))
        .thenReturn(new Gson().toJson(this.imageMetaData));

        // Act.
        BaseResponse response = this.service.dispatch(new ImportFileRequest(TEST_TMP_FOLDER));

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
        this.imageMetaData.setModel(NON_EXISTING_SOFTWARE_MODEL);
        Mockito.when(FileUtils.readFileToString(this.mockMetaDataFile, Charset.defaultCharset()))
        .thenReturn(new Gson().toJson(this.imageMetaData));

        // Act.
        BaseResponse response = this.service.dispatch(new ImportFileRequest(TEST_TMP_FOLDER));

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
        this.imageMetaData.setModel(NON_EXISTING_SOFTWARE_MODEL);
        Mockito.when(FileUtils.readFileToString(this.mockMetaDataFile, Charset.defaultCharset()))
        .thenReturn(new Gson().toJson(this.imageMetaData));

        // Causes isImageMissing to return false, which means file is already present.
        this.validAsv.setImageUrl(".");

        this.exception.expect(VmidcBrokerValidationException.class);
        this.exception.expectMessage("Virtualization Software Version already exists");

        // Act.
        this.service.dispatch(new ImportFileRequest(TEST_TMP_FOLDER));

    }

    @Test
    public void testDispatch_ImportApplianceWithSameURL_ExpectsErrorResponse() throws Exception {
        // Arrange.
        createExistingAppliance();
        this.imageMetaData.setModel(NON_EXISTING_SOFTWARE_MODEL);
        this.imageMetaData.setSoftwareVersion(NON_EXISTING_SOFTWARE_VERSION);
        Mockito.when(FileUtils.readFileToString(this.mockMetaDataFile, Charset.defaultCharset()))
        .thenReturn(new Gson().toJson(this.imageMetaData));

        this.exception.expect(VmidcBrokerValidationException.class);
        this.exception.expectMessage(" Cannot add an image with the same name.");

        // Act.
        this.service.dispatch(new ImportFileRequest(TEST_TMP_FOLDER));
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
        asv.setVirtualizarionSoftwareVersion(OpenstackSoftwareVersion.OS_ICEHOUSE.toString());
        asv.setVirtualizationType(VirtualizationType.OPENSTACK);

        this.em.persist(asv);

        this.em.getTransaction().commit();
    }

    @Test
    public void testDispatch_ImportApplianceWithLowDiskSpace_ExpectsErrorResponse() throws Exception {
        // Arrange.
        spy(ServerUtil.class);
        when(ServerUtil.isEnoughSpace()).thenReturn(Boolean.FALSE);

        this.exception.expect(VmidcException.class);

        // Act.
        this.service.dispatch(new ImportFileRequest(TEST_TMP_FOLDER));

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
        this.service.dispatch(new ImportFileRequest(TEST_TMP_FOLDER));
    }

    @Test
    public void testDispatch_ImportApplianceMissingPayloadFile_ExpectsErrorResponse() throws Exception {
        // Arrange. Make sure input is missing a file
        PowerMockito.when(FileUtil.getFileListFromDirectory(anyString())).thenReturn(new File[] { this.mockMetaDataFile });
        Mockito.when(FileUtils.readFileToString(this.mockMetaDataFile, Charset.defaultCharset()))
        .thenReturn(new Gson().toJson(this.imageMetaData));

        this.exception.expect(VmidcBrokerValidationException.class);
        this.exception.expectMessage("missing in archive");

        // Act.
        this.service.dispatch(new ImportFileRequest(TEST_TMP_FOLDER));
    }

    private void verifySuccessfulImport(BaseResponse response, Long id) throws IOException, Exception {
        // Verify Space check is made
        verifyStatic(Mockito.times(2));
        ServerUtil.isEnoughSpace();

        // validate is called
        Matcher<Boolean> isPolicyMappingSupported = new BaseMatcher<Boolean>() {

            @Override
            public boolean matches(Object arg0) {
                return true;
            }

            @Override
            public void describeTo(Description arg0) {
            }
        };
        verify(this.imageMetaDataValidator).validate(Mockito.argThat(
                new BaseMatcher<ImageMetadata>() {

                    @Override
                    public boolean matches(Object arg0) {
                        Gson gson = new Gson();
                        return gson.toJson(ImportApplianceSoftwareVersionServiceTest.this.imageMetaData)
                                .equals(gson.toJson(arg0));
                    }

                    @Override
                    public void describeTo(Description arg0) {
                        Gson gson = new Gson();
                        arg0.appendText("Did not match " +
                                gson.toJson(ImportApplianceSoftwareVersionServiceTest.this.imageMetaData));
                    }

                }), Mockito.booleanThat(isPolicyMappingSupported));

        // asv ID matches
        assertEquals("Appliance Software Version Id mismatch", id, response.getId());

        FileUtils.copyFileToDirectory(any(File.class), any(File.class), any(Boolean.class));

        verifyStatic();
        FileUtils.deleteDirectory(Mockito.argThat(new BaseMatcher<File>() {
            @Override
            public boolean matches(Object arg0) {
                return TEST_TMP_FOLDER.equals(((File) arg0).getPath());
            }

            @Override
            public void describeTo(Description arg0) {
                arg0.appendText("Did not delete " + TEST_TMP_FOLDER);
            }

        }));
    }

}
