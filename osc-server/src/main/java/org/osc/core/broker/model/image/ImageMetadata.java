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
package org.osc.core.broker.model.image;

import java.util.HashMap;
import java.util.Map;

import org.osc.core.broker.service.request.ImageMetadataRequest;
import org.osc.core.broker.util.ValidateUtil;

public class ImageMetadata extends ImageMetadataRequest {

    public static void checkForNullFields(ImageMetadata dto) throws Exception {
        Map<String, Object> notNullFieldsMap = new HashMap<String, Object>();
        notNullFieldsMap.put("Image Name", dto.getImageName());
        notNullFieldsMap.put("Software Version", dto.getSoftwareVersion());
        notNullFieldsMap.put("Manager Version", dto.getManagerVersion());
        notNullFieldsMap.put("Model", dto.getModel());
        notNullFieldsMap.put("Metadata Version", dto.getMetaDataVersion());
        notNullFieldsMap.put("Virtualization Type", dto.getVirtualizationTypeString());
        notNullFieldsMap.put("Virtualization Version", dto.getVirtualizationVersionString());
        notNullFieldsMap.put("Manager Type", dto.getManagerTypeString());
        notNullFieldsMap.put("Minimum OSC Version", dto.getMinIscVersion());
        notNullFieldsMap.put("Disk Size", dto.getDiskSizeInGb());
        notNullFieldsMap.put("Memory", dto.getMemoryInMb());
        notNullFieldsMap.put("Minimum CPU", dto.getMinCpus());

        ValidateUtil.checkForNullFields(notNullFieldsMap);
    }

}
