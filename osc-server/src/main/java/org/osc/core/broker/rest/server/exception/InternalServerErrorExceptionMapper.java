package org.osc.core.broker.rest.server.exception;

import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import java.util.Arrays;

@Provider
public class InternalServerErrorExceptionMapper implements ExceptionMapper<InternalServerErrorException>, BaseExceptionMapperUtil {

    @Context
    private HttpHeaders headers;

    @Override
    public Response toResponse(InternalServerErrorException e) {
        return Response
                .status(Response.Status.INTERNAL_SERVER_ERROR)
                .type(getMediaType(headers, MediaType.APPLICATION_JSON_TYPE))
                .entity(getErrorCodeDto(e))
                .build();
    }

    private Object getErrorCodeDto(InternalServerErrorException e) {
        if(e instanceof OscInternalServerErrorException){
            return ((OscInternalServerErrorException) e).getErrorCodeDto();
        }
        return new ErrorCodeDto(ErrorCodeDto.VMIDC_EXCEPTION_ERROR_CODE, Arrays.asList("Something went wrong"));
    }
}
