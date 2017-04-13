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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;

import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import java.util.Arrays;

@Provider
public class JsonProcessingExceptionMapper implements ExceptionMapper<JsonProcessingException>, BaseExceptionMapperUtil {

    @Context
    private HttpHeaders headers;

    @Override
    public Response toResponse(final JsonProcessingException exception) {
        return Response
                .status(Response.Status.BAD_REQUEST)
                .type(getMediaType(headers, MediaType.APPLICATION_JSON_TYPE))
                .entity(getErrorCodeDto(exception))
                .build();
    }

    private ErrorCodeDto getErrorCodeDto(Exception e) {
        if(e instanceof InvalidFormatException) {
            return new ErrorCodeDto(ErrorCodeDto.VMIDC_VALIDATION_EXCEPTION_ERROR_CODE, Arrays.asList(
                    "Value " + ((InvalidFormatException) e).getValue() + " is invalid"
            ));
        }
        return new ErrorCodeDto(ErrorCodeDto.VMIDC_VALIDATION_EXCEPTION_ERROR_CODE, Arrays.asList(
                "Parse exception. Invalid request."
        ));
    }

}
