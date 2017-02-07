package org.osc.core.broker.rest.server.exception;

import javax.validation.ConstraintViolationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Exception handler for invalid input
 */
@Provider
public class ConstraintViolationExceptionMapper implements ExceptionMapper<ConstraintViolationException> {


    @Context
    private HttpHeaders headers;

    @Override
    public Response toResponse(final ConstraintViolationException exception) {
        return Response
                .status(400)
                .type(headers.getAcceptableMediaTypes()!=null && !headers.getAcceptableMediaTypes().isEmpty()  ? headers.getAcceptableMediaTypes().get(0) : MediaType.APPLICATION_JSON_TYPE)
                .entity(new ErrorCodeDto(ErrorCodeDto.VMIDC_VALIDATION_EXCEPTION_ERROR_CODE,mapErrorCode(exception)))
                .build();
    }

    private List<String> mapErrorCode(ConstraintViolationException exception) {
        return exception.getConstraintViolations().stream().map(t -> t.getMessage()).collect(Collectors.toList());
    }
}
