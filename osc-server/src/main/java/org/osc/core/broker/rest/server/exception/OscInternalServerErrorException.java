package org.osc.core.broker.rest.server.exception;

import javax.ws.rs.InternalServerErrorException;
import java.util.Arrays;

/**
 * Created by GER\bsulich on 2/7/17.
 */
public class OscInternalServerErrorException extends InternalServerErrorException {

    private ErrorCodeDto errorCodeDto;

    public OscInternalServerErrorException(String errorMessege, Long errorCode){
        this.errorCodeDto = new ErrorCodeDto(errorCode, Arrays.asList(errorMessege));
    }

    public OscInternalServerErrorException(Long errorCode){
        this.errorCodeDto = new ErrorCodeDto(errorCode, Arrays.asList("Something went wrong"));
    }

    public ErrorCodeDto getErrorCodeDto() {
        return errorCodeDto;
    }
}
