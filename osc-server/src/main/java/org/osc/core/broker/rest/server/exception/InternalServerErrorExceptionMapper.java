package org.osc.core.broker.rest.server.exception;

import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class InternalServerErrorExceptionMapper implements ExceptionMapper<InternalServerErrorException>, BaseExceptionMapperUtil {

    @Context
    private HttpHeaders headers;

    @Override
    public Response toResponse(InternalServerErrorException e) {
        return Response
                .status(Response.Status.BAD_REQUEST)
                .type(getMediaType(headers, MediaType.APPLICATION_JSON_TYPE))
                .entity(getErrorCodeDto(e))
                .build();
    }

    private Object getErrorCodeDto(InternalServerErrorException e) {
        if(e instanceof OscInternalServerErrorException){
            return ((OscInternalServerErrorException) e).getErrorCodeDto();
        }
        return new ErrorCodeDto(5000L, "Something went wrong");
    }
}
