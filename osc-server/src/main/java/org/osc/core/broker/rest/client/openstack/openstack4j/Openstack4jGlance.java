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
package org.osc.core.broker.rest.client.openstack.openstack4j;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import org.apache.commons.io.FilenameUtils;
import org.apache.log4j.Logger;
import org.openstack4j.api.Builders;
import org.openstack4j.api.exceptions.ResponseException;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.common.Payload;
import org.openstack4j.model.common.Payloads;
import org.openstack4j.model.image.v2.ContainerFormat;
import org.openstack4j.model.image.v2.DiskFormat;
import org.openstack4j.model.image.v2.Image;
import org.openstack4j.model.image.v2.builder.ImageBuilder;
import org.osc.core.broker.service.exceptions.VmidcBrokerValidationException;


public class Openstack4jGlance extends BaseOpenstack4jApi {

    private static final Logger log = Logger.getLogger(Openstack4jGlance.class);

    public Openstack4jGlance(Endpoint endPoint) {
        super(endPoint);
    }

    public String uploadImage(String region, String imageName, File imageFile, Map<String, String> imageProperties)
            throws IOException, VmidcBrokerValidationException {
        log.info("Uploading Image " + imageName + " to region " + region);
        getOs().useRegion(region);

        String fileExtension = FilenameUtils.getExtension(imageFile.getName());

        ImageBuilder imageBuilder = Builders.imageV2()
                .name(imageName)
                .containerFormat(ContainerFormat.BARE)
                .visibility(Image.ImageVisibility.PRIVATE)
                .diskFormat(getDiskFormat(fileExtension));

        if (imageProperties != null) {
            imageProperties.forEach(imageBuilder::additionalProperty);
        }

        Image createdImage = getOs().imagesV2().create(imageBuilder.build());
        String imageId = createdImage.getId();

        Payload<File> payload = Payloads.create(imageFile);

        ActionResponse actionResponse = getOs().imagesV2().upload(imageId, payload, createdImage);
        if (actionResponse.isSuccess()) {
            log.info("Image uploaded with Id: " + imageId);
        } else {
            String message = String.format("Failed to upload image: %s to region: %s. error: %s", imageName, region,
                    actionResponse.getFault());
            log.warn(message);
            throw new ResponseException(message, actionResponse.getCode());
        }

        return imageId;
    }

    /**
     * Only the name, id and Status values of the image are retrieved from openstack.
     */
    public Image getImageById(String region, String id) throws Exception {
        getOs().useRegion(region);
        return getOs().imagesV2().get(id);
    }

    public boolean deleteImageById(String region, String id) {
        getOs().useRegion(region);
        ActionResponse actionResponse = getOs().imagesV2().delete(id);
        if (!actionResponse.isSuccess()) {
            String message = String.format("Image Id: %s in region: %s cannot be removed. Error: %s", id, region,
                    actionResponse.getFault());
            log.warn(message);
            throw new ResponseException(message, actionResponse.getCode());
        }
        return actionResponse.isSuccess();
    }

    private DiskFormat getDiskFormat(String fileExtension) throws VmidcBrokerValidationException {
        if (fileExtension != null && fileExtension.toUpperCase().equals("QCOW")) {
            return DiskFormat.QCOW2;
        } else {
            DiskFormat diskFormat = DiskFormat.value(fileExtension);
            if (diskFormat == DiskFormat.UNRECOGNIZED) {
                throw new VmidcBrokerValidationException("Unsupported Disk Image format: '" + fileExtension + "'.");
            }
            return diskFormat;
        }
    }

    @Override
    public void close() throws IOException {
        if (getOs() != null) {
            getOs().removeRegion();
        }
        super.close();
    }
}
