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
    public Response toResponse(NotFoundException notFoundException) {
        return Response
                .status(Response.Status.NOT_FOUND)
                .type(getMediaType(headers, MediaType.APPLICATION_JSON_TYPE))
                .entity(new ErrorCodeDto(ErrorCodeDto.VMIDC_VALIDATION_EXCEPTION_ERROR_CODE, Arrays.asList("Not found")))
                .build();
    }
}
