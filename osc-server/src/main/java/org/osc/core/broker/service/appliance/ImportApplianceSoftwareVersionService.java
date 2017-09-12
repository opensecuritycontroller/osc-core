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
import java.util.stream.Collectors;

import javax.persistence.EntityManager;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.log4j.Logger;
import org.osc.core.broker.model.entities.appliance.Appliance;
import org.osc.core.broker.model.entities.appliance.ApplianceSoftwareVersion;
import org.osc.core.broker.model.entities.appliance.TagEncapsulationType;
import org.osc.core.broker.model.image.ImageMetadata;
import org.osc.core.broker.model.plugin.ApiFactoryService;
import org.osc.core.broker.service.ServiceDispatcher;
import org.osc.core.broker.service.api.AddApplianceServiceApi;
import org.osc.core.broker.service.api.AddApplianceSoftwareVersionServiceApi;
import org.osc.core.broker.service.api.ImportApplianceSoftwareVersionServiceApi;
import org.osc.core.broker.service.common.VmidcMessages;
import org.osc.core.broker.service.common.VmidcMessages_;
import org.osc.core.broker.service.dto.ApplianceDto;
import org.osc.core.broker.service.dto.ApplianceSoftwareVersionDto;
import org.osc.core.broker.service.exceptions.VmidcBrokerValidationException;
import org.osc.core.broker.service.exceptions.VmidcException;
import org.osc.core.broker.service.persistence.ApplianceEntityMgr;
import org.osc.core.broker.service.persistence.ApplianceSoftwareVersionEntityMgr;
import org.osc.core.broker.service.persistence.OSCEntityManager;
import org.osc.core.broker.service.request.BaseRequest;
import org.osc.core.broker.service.request.ImportFileRequest;
import org.osc.core.broker.service.response.BaseResponse;
import org.osc.core.broker.util.FileUtil;
import org.osc.core.broker.util.ServerUtil;
import org.osc.core.common.virtualization.VirtualizationType;
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
    private static final Logger LOG = Logger.getLogger(ImportApplianceSoftwareVersionService.class);

    @Reference
    private ApiFactoryService apiFactoryService;

    @Reference
    private AddApplianceServiceApi addApplianceService;

    @Reference
    private AddApplianceSoftwareVersionServiceApi addApplianceSoftwareVersionService;

    private ImageMetadataValidator imageMetadataValidator;

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
            ImageMetadata imageMetadata = validateAndLoad(tmpUploadFolder);

            Appliance appliance = ApplianceEntityMgr.findByModel(em, imageMetadata.getModel());
            Long applianceId = null;
            if (appliance == null) {
                ApplianceDto applianceDto = new ApplianceDto(imageMetadata.getModel(),
                        imageMetadata.getManagerType(),
                        imageMetadata.getManagerVersion());

                BaseRequest<ApplianceDto> addApplianceRequest = new BaseRequest<>(applianceDto);

                BaseResponse addApplianceResponse = this.addApplianceService.dispatch(addApplianceRequest);
                applianceId = addApplianceResponse.getId();
            } else {
                if (!appliance.getManagerType().equals(imageMetadata.getManagerType())) {
                    throw new VmidcBrokerValidationException("Invalid manager type for the appliance. Expected: "
                            + appliance.getManagerType().toString() + " Received:"
                            + imageMetadata.getManagerType().toString());
                }
                if (!appliance.getManagerSoftwareVersion().equals(imageMetadata.getManagerVersion())) {
                    throw new VmidcBrokerValidationException("Invalid manager version for the appliance. Expected: "
                            + appliance.getManagerSoftwareVersion() + " Received:"
                            + imageMetadata.getManagerVersion());
                }

                applianceId = appliance.getId();
            }
            VirtualizationType virtualizationType = imageMetadata.getVirtualizationType();
            String virtualizationVersion = "";
            if (virtualizationType.isOpenstack()) {
                virtualizationVersion = imageMetadata.getOpenstackVirtualizationVersion().toString();
            }

            String softwareVersion = imageMetadata.getSoftwareVersion();

            /*
             * Query database to see if a record with the same composite key exists.
             * If it comes back not null there could still a valid case where the
             * record exists in DB but image does not in the file system.
             */
            ApplianceSoftwareVersion av = ApplianceSoftwareVersionEntityMgr.findByApplianceVersionVirtTypeAndVersion(
                    em,
                    applianceId,
                    softwareVersion,
                    virtualizationType,
                    virtualizationVersion);

            boolean isPolicyMappingSupported = this.apiFactoryService.syncsPolicyMapping(imageMetadata.getManagerType());
            Long asvId = null;
            if (av == null) {
                ApplianceSoftwareVersionDto asvDto = new ApplianceSoftwareVersionDto();
                asvDto.setParentId(applianceId);
                asvDto.setSwVersion(softwareVersion);
                asvDto.setVirtualizationType(virtualizationType);
                asvDto.setVirtualizationVersion(virtualizationVersion);
                asvDto.setImageUrl(imageMetadata.getImageName());
                if (isPolicyMappingSupported){
                    asvDto.setEncapsulationTypes(imageMetadata.getEncapsulationTypes());
                }
                asvDto.setMinCpus(imageMetadata.getMinCpus());
                asvDto.setMemoryInMb(imageMetadata.getMemoryInMb());
                asvDto.setDiskSizeInGb(imageMetadata.getDiskSizeInGb());
                asvDto.getImageProperties().putAll(imageMetadata.getImageProperties());
                asvDto.getConfigProperties().putAll(imageMetadata.getConfigProperties());
                asvDto.setAdditionalNicForInspection(imageMetadata.hasAdditionalNicForInspection());

                BaseRequest<ApplianceSoftwareVersionDto> addAsvRequest = new BaseRequest<>(asvDto);

                BaseResponse addAsvResponse = this.addApplianceSoftwareVersionService.dispatch(addAsvRequest);
                asvId = addAsvResponse.getId();
            } else {
                // We allow re-importing of the image to support the use case of backing up database and restore to a new VM
                if (isImageMissing(av.getImageUrl())) {
                    if (isPolicyMappingSupported){
                        av.setEncapsulationTypes(
                                imageMetadata.getEncapsulationTypes()
                                .stream()
                                .map(t -> TagEncapsulationType.valueOf(t.name()))
                                .collect(Collectors.toList()));
                    }
                    av.setMinCpus(imageMetadata.getMinCpus());
                    av.setMemoryInMb(imageMetadata.getMemoryInMb());
                    av.setDiskSizeInGb(imageMetadata.getDiskSizeInGb());
                    av.getImageProperties().clear();
                    av.getConfigProperties().clear();
                    OSCEntityManager.update(em, av, this.txBroadcastUtil);
                    em.flush();

                    av.getImageProperties().putAll(imageMetadata.getImageProperties());
                    av.getConfigProperties().putAll(imageMetadata.getConfigProperties());

                    OSCEntityManager.update(em, av, this.txBroadcastUtil);
                    asvId = av.getId();
                } else {
                    throw new VmidcBrokerValidationException(
                            "The composite key of Appliance Software Version, Virtualization Type, and Virtualization Software Version already exists.");
                }
            }

            response.setId(asvId);

            File imageFolder = new File(this.uploadPath);

            File[] tmpFolderList = FileUtil.getFileListFromDirectory(tmpUploadFolder.getPath());
            for (File tmpFolderFile : tmpFolderList) {
                if (tmpFolderFile.getName().equals(ImageMetadata.META_FILE_NAME)) {
                    continue;
                }
                FileUtils.copyFileToDirectory(tmpFolderFile, imageFolder, true);
                LOG.info("Moving file: " + tmpFolderFile.getName() + " to Images folder: " + imageFolder.getPath());
            }

        } finally {
            LOG.info("Cleaning temp folder: " + tmpUploadFolder.getPath());
            try {
                FileUtils.deleteDirectory(tmpUploadFolder);
            } catch (Exception e) {
                LOG.error("Failed to cleaning temp folder: " + tmpUploadFolder.getPath(), e);
                // Not throwing exception since AddApplianceSoftwareVersionService succeeded
            }
        }

        return response;
    }

    ImageMetadata validateAndLoad(File tmpUploadFolder) throws Exception {

        if (!ServerUtil.isEnoughSpace()) {
            throw new VmidcException(VmidcMessages.getString(VmidcMessages_.UPLOAD_APPLIANCE_NOSPACE));
        }

        File[] tmpFolderList = FileUtil.getFileListFromDirectory(tmpUploadFolder.getPath());

        File metaDataFile = loadMetadataFile(tmpFolderList);

        ImageMetadata imageMetadata = getFromJson(metaDataFile);

        if (this.imageMetadataValidator == null) {
            this.imageMetadataValidator = new ImageMetadataValidator();
        }

        this.imageMetadataValidator.validate(imageMetadata, this.apiFactoryService);

        boolean isImageFileMissing = true;

        for (File tmpFolderFile : tmpFolderList) {
            String fileName = FilenameUtils.getName(tmpFolderFile.getName());
            if (fileName.equals(imageMetadata.getImageName())) {
                isImageFileMissing = false;
                break;
            }
        }

        if (isImageFileMissing) {
            LOG.error("Image file: " + imageMetadata.getImageName() + " missing in archive");
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

    private ImageMetadata getFromJson(File metaDataFile) throws VmidcBrokerValidationException {
        ImageMetadata tempImageMetadata;
        try {
            tempImageMetadata = new Gson().fromJson(FileUtils.readFileToString(metaDataFile, Charset.defaultCharset()),
                    ImageMetadata.class);
        } catch (JsonSyntaxException | IOException exception) {
            LOG.error("Error reading meta data file", exception);
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
