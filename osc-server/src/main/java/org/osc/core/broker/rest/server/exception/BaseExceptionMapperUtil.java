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
package org.osc.core.broker.rest.server.exception;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;

public interface BaseExceptionMapperUtil {

    default MediaType getMediaType(HttpHeaders headers, MediaType defaultType){
        MediaType mediaType =null;
        if (headers.getMediaType()!=null){
            if(MediaType.APPLICATION_JSON_TYPE.equals(headers.getMediaType()) || MediaType.APPLICATION_XML_TYPE.equals(headers.getMediaType())){
                return headers.getMediaType();
            }
        } else if (headers.getAcceptableMediaTypes()!=null && !headers.getAcceptableMediaTypes().isEmpty()){
            if(MediaType.APPLICATION_JSON_TYPE.equals(headers.getMediaType()) || MediaType.APPLICATION_XML_TYPE.equals(headers.getMediaType())) {
                return headers.getAcceptableMediaTypes().get(0);
            }
        }
        return defaultType;
    }
}
