package org.osc.core.broker.rest.server.exception;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response.ResponseBuilder;
import java.util.Arrays;

@SuppressWarnings("serial")
public class VmidcRestServerException extends WebApplicationException {

    public static final Long REMOTE_EXCEPTION_ERROR_CODE = 6000L;
    public static final Long VMIDC_VALIDATION_EXCEPTION_ERROR_CODE = 4000L;
    public static final Long VMIDC_EXCEPTION_ERROR_CODE = 5000L;

    public VmidcRestServerException(ResponseBuilder response, String message) {
        super(new Exception(message), response.entity("Error: " + message).type(MediaType.TEXT_PLAIN).build());
    }

    public VmidcRestServerException(ResponseBuilder response, String message, Long errorCode) {
        super(new Exception(message),
                response.entity(new ErrorCodeDto(errorCode, Arrays.asList(message))).type(MediaType.APPLICATION_JSON).build());
    }

}
