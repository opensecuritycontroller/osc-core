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
