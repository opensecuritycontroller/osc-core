package org.osc.core.broker.rest.server.exception;

import javax.management.modelmbean.XMLParseException;
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
public class XMLParseExceptionMapper implements ExceptionMapper<XMLParseException>, BaseExceptionMapperUtil {

    @Context
    private HttpHeaders headers;

    @Override
    public Response toResponse(final XMLParseException exception) {
        return Response
                .status(Response.Status.BAD_REQUEST)
                .type(getMediaType(headers,MediaType.APPLICATION_XML_TYPE))
                .entity(new ErrorCodeDto(ErrorCodeDto.VMIDC_VALIDATION_EXCEPTION_ERROR_CODE, Arrays.asList(
                    "Value "+ exception.getMessage() + " is invalid"
                )))
                .build();
    }

}
