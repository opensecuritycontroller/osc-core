package org.osc.core.broker.rest.server.exception;

import javax.ws.rs.NotFoundException;
import java.util.Arrays;

/**
 * Created by GER\bsulich on 2/7/17.
 */
public class OscNotFoundException extends NotFoundException {

    private ErrorCodeDto errorCodeDto;

    public OscNotFoundException(String errorMessege, Long errorCode){
        this.errorCodeDto = new ErrorCodeDto(errorCode, Arrays.asList(errorMessege));
    }

    public ErrorCodeDto getErrorCodeDto() {
        return errorCodeDto;
    }
}
