package org.osc.core.broker.rest.server.exception;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import io.swagger.annotations.ApiModelProperty;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class ErrorCodeDto {

    public static final Long REMOTE_EXCEPTION_ERROR_CODE = 6000L;
    public static final Long VMIDC_VALIDATION_EXCEPTION_ERROR_CODE = 4000L;
    public static final Long VMIDC_EXCEPTION_ERROR_CODE = 5000L;

    @ApiModelProperty(value="Return an error code based on the type of error.<br/>"
            + "6000 is returned for any Remote calls which failed<br/>"
            + "4000 is returned for OSC validation failures<br/>"
            + "5000 is returned for general errors<br/>")
    private Long errorCode;

    @ApiModelProperty(required = true)
    private String errorMessage;

    @SuppressWarnings("unused")
    private ErrorCodeDto() {
    }

    public ErrorCodeDto(Long errorCode, String errorMessage) {
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
    }

    public Long getErrorCode() {
        return this.errorCode;
    }

    public String getErrorMessage() {
        return this.errorMessage;
    }
}
