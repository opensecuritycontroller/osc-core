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

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;

import javax.persistence.EntityManager;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.log4j.Logger;
import org.osc.core.broker.model.image.ImageMetadata;
import org.osc.core.broker.model.plugin.ApiFactoryService;
import org.osc.core.broker.service.ServiceDispatcher;
import org.osc.core.broker.service.api.ImportApplianceSoftwareVersionServiceApi;
import org.osc.core.broker.service.common.VmidcMessages;
import org.osc.core.broker.service.common.VmidcMessages_;
import org.osc.core.broker.service.exceptions.VmidcBrokerValidationException;
import org.osc.core.broker.service.exceptions.VmidcException;
import org.osc.core.broker.service.request.ImageMetadataRequest;
import org.osc.core.broker.service.request.ImportFileRequest;
import org.osc.core.broker.service.response.BaseResponse;
import org.osc.core.broker.util.FileUtil;
import org.osc.core.broker.util.ServerUtil;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

@Component(configurationPid="org.osc.core.broker.upload",
configurationPolicy=ConfigurationPolicy.REQUIRE)
public class ImportApplianceSoftwareVersionService extends ServiceDispatcher<ImportFileRequest, BaseResponse>
implements ImportApplianceSoftwareVersionServiceApi {

    private static final Logger log = Logger.getLogger(ImportApplianceSoftwareVersionService.class);

    @Reference
    private ApiFactoryService apiFactoryService;

    @Reference
    private AddApplianceService addApplianceService;

    private String uploadPath;

    @Activate
    void start(UploadConfig config) {
        this.uploadPath = config.upload_path();
    }

    @Override
    public BaseResponse exec(ImportFileRequest request, EntityManager em) throws Exception {
        BaseResponse response = new BaseResponse();

        File tmpUploadFolder = new File(request.getUploadPath());

        try {
            ImageMetadataRequest imageMetadata = loadRequest(tmpUploadFolder);

            Long id = this.addApplianceService.addAppliance(imageMetadata, em);
            response.setId(id);

            File imageFolder = new File(this.uploadPath);

            File[] tmpFolderList = FileUtil.getFileListFromDirectory(tmpUploadFolder.getPath());
            for (File tmpFolderFile : tmpFolderList) {
                if (tmpFolderFile.getName().equals(ImageMetadata.META_FILE_NAME)) {
                    continue;
                }
                FileUtils.copyFileToDirectory(tmpFolderFile, imageFolder, true);
                log.info("Moving file: " + tmpFolderFile.getName() + " to Images folder: " + imageFolder.getPath());
            }

        } finally {
            log.info("Cleaning temp folder: " + tmpUploadFolder.getPath());
            try {
                FileUtils.deleteDirectory(tmpUploadFolder);
            } catch (Exception e) {
                log.error("Failed to cleaning temp folder: " + tmpUploadFolder.getPath(), e);
                // Not throwing exception since AddApplianceSoftwareVersionService succeeded
            }
        }

        return response;
    }

    ImageMetadataRequest loadRequest(File tmpUploadFolder) throws Exception {

        if (!ServerUtil.isEnoughSpace()) {
            throw new VmidcException(VmidcMessages.getString(VmidcMessages_.UPLOAD_APPLIANCE_NOSPACE));
        }

        File[] tmpFolderList = FileUtil.getFileListFromDirectory(tmpUploadFolder.getPath());

        File metaDataFile = loadMetadataFile(tmpFolderList);

        ImageMetadataRequest imageMetadata = getFromJson(metaDataFile);

        boolean isImageFileMissing = true;

        for (File tmpFolderFile : tmpFolderList) {
            String fileName = FilenameUtils.getName(tmpFolderFile.getName());
            if (fileName.equals(imageMetadata.getImageName())) {
                isImageFileMissing = false;
                break;
            }
        }

        if (isImageFileMissing) {
            log.error("Image file: " + imageMetadata.getImageName() + " missing in archive");
            throw new VmidcBrokerValidationException("Invalid file format. Image file: " + imageMetadata.getImageName() + " missing in archive.");
        }

        return imageMetadata;
    }

    private File loadMetadataFile(File[] tmpFolderList) throws VmidcBrokerValidationException {
        File metaDataFile = null;
        for (File tmpFolderFile : tmpFolderList) {
            String fileName = FilenameUtils.getName(tmpFolderFile.getName());
            if (fileName.equals(ImageMetadata.META_FILE_NAME)) {
                metaDataFile = tmpFolderFile;
                break;
            }
        }

        if (metaDataFile != null) {
            return metaDataFile;
        } else {
            throw new VmidcBrokerValidationException(
                    VmidcMessages.getString(VmidcMessages_.UPLOAD_APPLIANCE_INVALID_METAFILE, ImageMetadata.META_FILE_NAME)
                    );
        }
    }

    private ImageMetadataRequest getFromJson(File metaDataFile) throws VmidcBrokerValidationException {
        ImageMetadataRequest tempImageMetadata;
        try {
            tempImageMetadata = new Gson().fromJson(FileUtils.readFileToString(metaDataFile, Charset.defaultCharset()),
                    ImageMetadataRequest.class);
        } catch (JsonSyntaxException | IOException exception) {
            log.error("Error reading meta data file", exception);
            throw new VmidcBrokerValidationException(
                    VmidcMessages.getString(VmidcMessages_.UPLOAD_APPLIANCE_INVALID_METAFILE_SYNTAX, exception.getMessage())
                    );
        }

        if (tempImageMetadata == null) {
            throw new VmidcBrokerValidationException("Cannot process metadata from json content");
        } else {
            return tempImageMetadata;
        }
    }

    private boolean isImageMissing(String imageUrl) {
        return imageUrl == null || !new File(this.uploadPath, imageUrl).exists();
    }
}
