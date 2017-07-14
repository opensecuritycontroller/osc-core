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

import java.util.Arrays;

import javax.ws.rs.BadRequestException;

public class OscBadRequestException extends BadRequestException {

    /**
     *
     */
    private static final long serialVersionUID = -9176801533010370069L;
    private ErrorCodeDto errorCodeDto;

    public OscBadRequestException(String errorMessege, Long errorCode){
        this.errorCodeDto = new ErrorCodeDto(errorCode, Arrays.asList(errorMessege));
    }

    public ErrorCodeDto getErrorCodeDto() {
        return this.errorCodeDto;
    }
}
