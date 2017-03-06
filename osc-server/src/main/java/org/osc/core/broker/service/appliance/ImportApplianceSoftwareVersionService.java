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
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.osc.core.broker.model.entities.appliance.Appliance;
import org.osc.core.broker.model.entities.appliance.ApplianceSoftwareVersion;
import org.osc.core.broker.model.entities.appliance.TagEncapsulationType;
import org.osc.core.broker.model.image.ImageMetadata;
import org.osc.core.broker.model.plugin.manager.ManagerApiFactory;
import org.osc.core.broker.model.virtualization.VirtualizationType;
import org.osc.core.broker.service.ServiceDispatcher;
import org.osc.core.broker.service.dto.ApplianceSoftwareVersionDto;
import org.osc.core.broker.service.exceptions.VmidcBrokerValidationException;
import org.osc.core.broker.service.exceptions.VmidcException;
import org.osc.core.broker.service.persistence.ApplianceEntityMgr;
import org.osc.core.broker.service.persistence.ApplianceSoftwareVersionEntityMgr;
import org.osc.core.broker.service.persistence.EntityManager;
import org.osc.core.broker.service.response.BaseResponse;
import org.osc.core.broker.view.common.VmidcMessages;
import org.osc.core.broker.view.common.VmidcMessages_;
import org.osc.core.broker.view.maintenance.ApplianceUploader;
import org.osc.core.util.FileUtil;
import org.osc.core.util.ServerUtil;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

public class ImportApplianceSoftwareVersionService extends ServiceDispatcher<ImportFileRequest, BaseResponse> {

    private static final Logger log = Logger.getLogger(ImportApplianceSoftwareVersionService.class);

    private File tmpUploadFolder;

    private ImageMetadata imageMetadata;
    private ImageMetadataValidator imageMetadataValidator;

    @Override
    public BaseResponse exec(ImportFileRequest request, Session session) throws Exception {
        BaseResponse response = new BaseResponse();

        if (this.tmpUploadFolder == null) {
            this.tmpUploadFolder = new File(request.getUploadPath());
        }

        try {

            validateAndLoad();

            Appliance appliance = ApplianceEntityMgr.findByModel(session, this.imageMetadata.getModel());

            if (appliance == null) {
                appliance = new Appliance();
                appliance.setManagerType(this.imageMetadata.getManagerType().getValue());
                appliance.setModel(this.imageMetadata.getModel());
                appliance.setManagerSoftwareVersion(this.imageMetadata.getManagerVersion());

                EntityManager<Appliance> applianceEntityManager = new EntityManager<Appliance>(Appliance.class, session);

                appliance = applianceEntityManager.create(appliance);
            } else {
                if (!appliance.getManagerType().equals(this.imageMetadata.getManagerType().getValue())) {
                    throw new VmidcBrokerValidationException("Invalid manager type for the appliance. Expected: "
                            + appliance.getManagerType().toString() + " Received:"
                            + this.imageMetadata.getManagerType().toString());
                }
                if (!appliance.getManagerSoftwareVersion().equals(this.imageMetadata.getManagerVersion())) {
                    throw new VmidcBrokerValidationException("Invalid manager version for the appliance. Expected: "
                            + appliance.getManagerSoftwareVersion() + " Received:"
                            + this.imageMetadata.getManagerVersion());
                }
            }
            VirtualizationType virtualizationType = this.imageMetadata.getVirtualizationType();
            String virtualizationVersion = "";
            if (virtualizationType.isOpenstack()) {
                virtualizationVersion = this.imageMetadata.getOpenstackVirtualizationVersion().toString();
            } else if (virtualizationType.isVmware()) {
                virtualizationVersion = this.imageMetadata.getVmwareVirtualizationVersion().toString();
            }

            String softwareVersion = this.imageMetadata.getSoftwareVersion();

            /*
             * Query database to see if a record with the same composite key exists.
             * If it comes back not null there could still a valid case where the
             * record exists in DB but image does not in the file system.
             */
            ApplianceSoftwareVersion av = ApplianceSoftwareVersionEntityMgr.findByApplianceVersionVirtTypeAndVersion(session,
                    appliance.getId(), softwareVersion,
                    org.osc.core.broker.model.entities.appliance.VirtualizationType.valueOf(
                            virtualizationType.name()), virtualizationVersion);

            boolean isPolicyMappingSupported = ManagerApiFactory.createApplianceManagerApi(
                    this.imageMetadata.getManagerType()).isPolicyMappingSupported();
            if (av == null) {

                ApplianceSoftwareVersion asv = ApplianceSoftwareVersionEntityMgr.findByImageUrl(session,
                        this.imageMetadata.getImageName());
                if (asv != null) {
                    throw new VmidcBrokerValidationException("Image file: " + this.imageMetadata.getImageName()
                            + " already exists. Cannot add an image with the same name.");
                }

                ApplianceSoftwareVersionDto asvDto = new ApplianceSoftwareVersionDto();
                asvDto.setParentId(appliance.getId());
                asvDto.setSwVersion(softwareVersion);
                asvDto.setVirtualizationType(virtualizationType);
                asvDto.setVirtualizationVersion(virtualizationVersion);
                asvDto.setImageUrl(this.imageMetadata.getImageName());
                if (isPolicyMappingSupported){
                    asvDto.setEncapsulationTypes(this.imageMetadata.getEncapsulationTypes());
                }
                asvDto.setMinCpus(this.imageMetadata.getMinCpus());
                asvDto.setMemoryInMb(this.imageMetadata.getMemoryInMb());
                asvDto.setDiskSizeInGb(this.imageMetadata.getDiskSizeInGb());
                asvDto.getImageProperties().putAll(this.imageMetadata.getImageProperties());
                asvDto.getConfigProperties().putAll(this.imageMetadata.getConfigProperties());
                asvDto.setAdditionalNicForInspection(this.imageMetadata.hasAdditionalNicForInspection());

                EntityManager<ApplianceSoftwareVersion> emgr = new EntityManager<ApplianceSoftwareVersion>(
                        ApplianceSoftwareVersion.class, session);

                // creating new entry in the db using entity manager object
                av = ApplianceSoftwareVersionEntityMgr.createEntity(session, asvDto, appliance);

                av = emgr.create(av);
            } else {
                // We allow re-importing of the image to support the use case of backing up database and restore to a new VM
                if (isImageMissing(av.getImageUrl())) {
                    if (isPolicyMappingSupported){
                        av.setEncapsulationTypes(
                                this.imageMetadata.getEncapsulationTypes()
                                .stream()
                                .map(t -> TagEncapsulationType.valueOf(t.name()))
                                .collect(Collectors.toList()));
                    }
                    av.setMinCpus(this.imageMetadata.getMinCpus());
                    av.setMemoryInMb(this.imageMetadata.getMemoryInMb());
                    av.setDiskSizeInGb(this.imageMetadata.getDiskSizeInGb());
                    av.getImageProperties().clear();
                    av.getConfigProperties().clear();
                    EntityManager.update(session, av);
                    session.flush();

                    av.getImageProperties().putAll(this.imageMetadata.getImageProperties());
                    av.getConfigProperties().putAll(this.imageMetadata.getConfigProperties());

                    EntityManager.update(session, av);
                } else {
                    throw new VmidcBrokerValidationException(
                            "The composite key of Appliance Software Version, Virtualization Type, and Virtualization Software Version already exists.");
                }
            }

            response.setId(av.getId());

            File imageFolder = new File(ApplianceUploader.getImageFolderPath());

            File[] tmpFolderList = FileUtil.getFileListFromDirectory(this.tmpUploadFolder.getPath());
            for (File tmpFolderFile : tmpFolderList) {
                if (tmpFolderFile.getName().equals(ImageMetadata.META_FILE_NAME)) {
                    continue;
                }
                FileUtils.copyFileToDirectory(tmpFolderFile, imageFolder, true);
                log.info("Moving file: " + tmpFolderFile.getName() + " to Images folder: " + imageFolder.getPath());
            }

        } finally {
            log.info("Cleaning temp folder: " + this.tmpUploadFolder.getPath());
            try {
                FileUtils.deleteDirectory(this.tmpUploadFolder);
            } catch (Exception e) {
                log.error("Failed to cleaning temp folder: " + this.tmpUploadFolder.getPath(), e);
                // Not throwing exception since AddApplianceSoftwareVersionService succeeded
            }
        }

        return response;
    }

    void validateAndLoad() throws Exception {

        if (!ServerUtil.isEnoughSpace()) {
            throw new VmidcException(VmidcMessages.getString(VmidcMessages_.UPLOAD_APPLIANCE_NOSPACE));
        }

        File[] tmpFolderList = FileUtil.getFileListFromDirectory(this.tmpUploadFolder.getPath());

        File metaDataFile = loadMetadataFile(tmpFolderList);

        if (this.imageMetadata == null) {
            this.imageMetadata = getFromJson(metaDataFile);
        }

        if (this.imageMetadataValidator == null) {
            this.imageMetadataValidator = new ImageMetadataValidator();
        }

        this.imageMetadataValidator.validate(this.imageMetadata);

        boolean isImageFileMissing = true;
        boolean checkOvfExists = this.imageMetadata.getVirtualizationType().isVmware();

        for (File tmpFolderFile : tmpFolderList) {
            String fileName = FilenameUtils.getName(tmpFolderFile.getName());
            if (fileName.equals(this.imageMetadata.getImageName())) {
                if (checkOvfExists) {
                    String extension = FilenameUtils.getExtension(tmpFolderFile.getName());
                    if (extension.equals("ovf")) {
                        isImageFileMissing = false;
                        break;
                    } else {
                        break;
                    }
                } else {
                    isImageFileMissing = false;
                    break;
                }
            }
        }

        if (isImageFileMissing) {
            log.error("Image file: " + this.imageMetadata.getImageName() + " missing in archive");
            throw new VmidcBrokerValidationException("Invalid file format. Image file: " + this.imageMetadata.getImageName() + " missing in archive.");
        }

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
            tempImageMetadata = new Gson().fromJson(FileUtils.readFileToString(metaDataFile), ImageMetadata.class);
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
        return imageUrl == null || !new File(ApplianceUploader.getImageFolderPath() + imageUrl).exists();
    }

}
