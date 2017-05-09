/*******************************************************************************
 * Copyright (c) Intel Corporation
 * Copyright (c) 2017
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package org.osc.core.broker.service.exceptions;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import io.swagger.annotations.ApiModelProperty;

import java.util.ArrayList;
import java.util.List;

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
    private List<String> errorMessages = new ArrayList<>();
    @SuppressWarnings("unused")
    private ErrorCodeDto() {
    }

    public ErrorCodeDto(Long errorCode, List<String> errorMessages) {
        this.errorCode = errorCode;
        this.errorMessages = errorMessages;
    }

    public List<String> getErrorMessages() {
        return errorMessages;
    }

    public void setErrorMessages(List<String> errorMessages) {
        this.errorMessages = errorMessages;
    }
}
