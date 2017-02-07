package org.osc.core.broker.rest.server.exception;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;

import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import java.util.Arrays;

/**
 * Exception handler for invalid json deserialization
 */
@Provider
public class JsonProcessingExceptionMapper implements ExceptionMapper<JsonProcessingException> {

    @Context
    private HttpHeaders headers;

    @Override
    public Response toResponse(final JsonProcessingException exception) {
        return Response
                .status(Response.Status.BAD_REQUEST)
                .type(headers.getAcceptableMediaTypes()!=null && !headers.getAcceptableMediaTypes().isEmpty()  ? headers.getAcceptableMediaTypes().get(0) : MediaType.APPLICATION_JSON_TYPE)
                .entity(new ErrorCodeDto(ErrorCodeDto.REMOTE_EXCEPTION_ERROR_CODE, Arrays.asList(
                    "Value "+((InvalidFormatException) exception).getValue() + " is invalid"
                )))
                .build();
    }

}
