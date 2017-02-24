package org.osc.core.broker.service.appliance;

import org.apache.log4j.Logger;
import org.osc.core.broker.model.image.ImageMetadata;
import org.osc.core.broker.model.plugin.manager.ManagerApiFactory;
import org.osc.core.broker.model.virtualization.VirtualizationType;
import org.osc.core.broker.service.exceptions.VmidcBrokerValidationException;
import org.osc.core.util.VersionUtil;
import org.osc.core.util.VersionUtil.Version;

public class ImageMetadataValidator {

    private static final Logger log = Logger.getLogger(ImageMetadataValidator.class);

    public void validate(ImageMetadata imageMetadata) throws Exception {
        ImageMetadata.checkForNullFields(imageMetadata);

        Version minIscVersion = imageMetadata.getMinIscVersion();
        int compareValue = VersionUtil.getVersion().compareTo(minIscVersion);
        if (compareValue != 0) {
            if (compareValue < 1) {
                throw new VmidcBrokerValidationException("This Appliance is compatible with OSC server version "
                        + minIscVersion.getShortVersionStrWithBuild() + " or higher");
            } else if (compareValue > 1) {
                // TODO: Future. We fulfill minimum software version. Check if iSC can support this CPA in
                // case protocol changes
            }
        }

        try {
            imageMetadata.getManagerType();
            VirtualizationType virtualizationType = imageMetadata.getVirtualizationType();
            imageMetadata.getEncapsulationTypes();
            if (virtualizationType == null) {
                throw new IllegalArgumentException();
            } else if (virtualizationType.isOpenstack()) {
                imageMetadata.getOpenstackVirtualizationVersion();
            } else if (virtualizationType.isVmware()) {
                imageMetadata.getVmwareVirtualizationVersion();
            }
        } catch (IllegalArgumentException iae) {
            log.error("Invalid manager type/virtualization type/virtualization version/encapsulation type", iae);
            throw new VmidcBrokerValidationException(
                    "Invalid File Format. Invalid Manager Type and/or Virtualization Type and/or Virtualization Version and/or Encapsulation Type.");
        }
        boolean isPolicyMappingSupported = ManagerApiFactory.createApplianceManagerApi(imageMetadata.getManagerType())
                .isPolicyMappingSupported();

        if (!imageMetadata.getEncapsulationTypes().isEmpty() && imageMetadata.getVirtualizationType().isVmware()) {
            throw new VmidcBrokerValidationException(
                    "Invalid File Format. Encapsulation Types is not supported by VMware Virtualization Type.");
        } else if (isPolicyMappingSupported && imageMetadata.getVirtualizationType().isOpenstack()
                && imageMetadata.getEncapsulationTypes().isEmpty()
                ) {
            throw new VmidcBrokerValidationException(
                    "Invalid File Format. Encapsulation Types cannot be empty for Openstack Virtualization Type.");
        }



    }
}
