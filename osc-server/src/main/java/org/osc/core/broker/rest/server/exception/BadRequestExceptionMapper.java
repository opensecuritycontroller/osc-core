package org.osc.core.broker.rest.server.exception;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import java.util.Arrays;

@Provider
public class BadRequestExceptionMapper implements ExceptionMapper<BadRequestException>, BaseExceptionMapperUtil {

    @Context
    private HttpHeaders headers;

    @Override
    public Response toResponse(BadRequestException e) {
        return Response
                .status(Response.Status.BAD_REQUEST)
                .type(getMediaType(headers, MediaType.APPLICATION_JSON_TYPE))
                .entity(getErrorCodeDto(e))
                .build();
    }
    private Object getErrorCodeDto(BadRequestException e) {
        if(e instanceof OscBadRequestException){
            return ((OscBadRequestException) e).getErrorCodeDto();
        }
        return new ErrorCodeDto(4000L, Arrays.asList("Bad request"));
    }

}
