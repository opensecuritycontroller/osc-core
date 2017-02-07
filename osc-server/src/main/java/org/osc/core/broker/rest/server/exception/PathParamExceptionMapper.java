package org.osc.core.broker.rest.server.exception;

import org.glassfish.jersey.server.ParamException;

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
public class PathParamExceptionMapper implements ExceptionMapper<ParamException> {

    @Context
    private HttpHeaders headers;

    @Override
    public Response toResponse(ParamException notFoundException) {
        return Response
                .status(Response.Status.BAD_REQUEST)
                .type(headers.getAcceptableMediaTypes()!=null && !headers.getAcceptableMediaTypes().isEmpty()  ? headers.getAcceptableMediaTypes().get(0) : MediaType.APPLICATION_JSON_TYPE)
                .entity(new ErrorCodeDto(ErrorCodeDto.VMIDC_VALIDATION_EXCEPTION_ERROR_CODE, Arrays.asList("Not found")))
                .build();
    }
}
