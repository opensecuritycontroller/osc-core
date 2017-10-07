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

import java.util.Arrays;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.osc.core.broker.service.exceptions.ErrorCodeDto;
import org.osc.core.broker.service.exceptions.OscBadRequestException;

@Provider
public class BadRequestExceptionMapper implements ExceptionMapper<BadRequestException>, BaseExceptionMapperUtil {

    @Context
    private HttpHeaders headers;

    @Override
    public Response toResponse(BadRequestException e) {
        return Response
                .status(Response.Status.BAD_REQUEST)
                .type(getMediaType(headers, MediaType.APPLICATION_JSON_TYPE))
                .entity(getErrorCodeDto(e))
                .build();
    }

    private Object getErrorCodeDto(BadRequestException e) {
        if(e instanceof OscBadRequestException){
            return ((OscBadRequestException) e).getErrorCodeDto();
        }
        return new ErrorCodeDto(4000L, Arrays.asList("Bad request"));
    }

}
