package org.osc.core.broker.rest.server.exception;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;

/**
 * Created by GER\bsulich on 2/7/17.
 */
public interface BaseExceptionMapperUtil {

    default MediaType getMediaType(HttpHeaders headers, MediaType defaultType){
        if (headers.getMediaType()!=null){
            return headers.getMediaType();
        }
        return headers.getAcceptableMediaTypes()!=null && !headers.getAcceptableMediaTypes().isEmpty()  ? headers.getAcceptableMediaTypes().get(0) : defaultType;
    }
}
