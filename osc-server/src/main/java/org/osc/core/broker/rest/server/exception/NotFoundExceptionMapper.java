package org.osc.core.broker.rest.server.exception;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import java.util.Arrays;

/**
 * Created by GER\bsulich on 2/6/17.
 */
@Provider
public class NotFoundExceptionMapper implements ExceptionMapper<NotFoundException>, BaseExceptionMapperUtil {

    @Context
    private HttpHeaders headers;

    @Override
    public Response toResponse(NotFoundException e) {
        return Response
                .status(Response.Status.NOT_FOUND)
                .type(getMediaType(headers, MediaType.APPLICATION_JSON_TYPE))
                .entity(getErrorCodeDto(e))
                .build();
    }

    private Object getErrorCodeDto(NotFoundException e) {
        if(e instanceof OscNotFoundException){
            return ((OscNotFoundException) e).getErrorCodeDto();
        }
        return new ErrorCodeDto(4000L, Arrays.asList("Not found"));
    }
}
