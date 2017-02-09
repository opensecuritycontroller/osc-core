package org.osc.core.broker.rest.server.exception;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;

/**
 * Created by GER\bsulich on 2/7/17.
 */
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
