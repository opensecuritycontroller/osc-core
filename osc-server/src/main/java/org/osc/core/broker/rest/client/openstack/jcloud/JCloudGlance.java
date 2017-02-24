package org.osc.core.broker.rest.client.openstack.jcloud;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

import org.apache.log4j.Logger;
import org.jclouds.io.Payload;
import org.jclouds.io.payloads.BaseMutableContentMetadata;
import org.jclouds.io.payloads.InputStreamPayload;
import org.jclouds.openstack.glance.v1_0.GlanceApi;
import org.jclouds.openstack.glance.v1_0.domain.ImageDetails;
import org.jclouds.openstack.glance.v1_0.features.ImageApi;
import org.jclouds.openstack.glance.v1_0.options.CreateImageOptions;
import org.jclouds.rest.ResourceNotFoundException;

public class JCloudGlance extends BaseJCloudApi {

    private static final Logger log = Logger.getLogger(JCloudGlance.class);

    private static final String OPENSTACK_SERVICE_GLANCE = "openstack-glance";

    private GlanceApi glanceApi;

    /**
     * @param endPoint - OpenStack Endpoint
     */
    public JCloudGlance(Endpoint endPoint) {
        super(endPoint);
        this.glanceApi = JCloudUtil.buildApi(GlanceApi.class, OPENSTACK_SERVICE_GLANCE, endPoint);
    }

    public String uploadImage(String region, String imageName, File content, CreateImageOptions... options) throws IOException {
        log.info("Uploading Image " + imageName + " to region " + region);
        ImageApi imageApi = this.glanceApi.getImageApi(region);
        String imageId;
        try (InputStream imageStream = new FileInputStream(content);
             Payload imagePayload = new InputStreamPayload(imageStream)) {
            BaseMutableContentMetadata metaData = new BaseMutableContentMetadata();
            metaData.setContentLength(content.length());
            imagePayload.setContentMetadata(metaData);
            imageId = imageApi.create(imageName, imagePayload, options).getId();
        }
        log.info("Image uploaded with Id: " + imageId);
        return imageId;
    }

    /**
     * Only the name, id and Status values of the image are retrived from openstack.
     */
    public ImageDetails getImageById(String region, String id) throws Exception {
        ImageApi imageApi = this.glanceApi.getImageApi(region);
        return imageApi.get(id);
    }

    public boolean deleteImageById(String region, String id) {
        ImageApi imageApi = this.glanceApi.getImageApi(region);

        try {
            return imageApi.delete(id);
        } catch (ResourceNotFoundException ex) {
            log.warn("Image Id: " + id + " not found.");
        }
        return true;
    }

    @Override
    protected List<? extends Closeable> getApis() {
        return Arrays.asList(this.glanceApi);
    }
}
