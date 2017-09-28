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

import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.osc.core.broker.service.exceptions.ErrorCodeDto;
import org.osc.core.broker.service.exceptions.OscNotFoundException;

@Provider
public class NotFoundExceptionMapper implements ExceptionMapper<NotFoundException>, BaseExceptionMapperUtil {

    @Context
    private HttpHeaders headers;

    @Override
    public Response toResponse(NotFoundException e) {
        return Response
                .status(Response.Status.NOT_FOUND)
                .type(getMediaType(headers, MediaType.APPLICATION_JSON_TYPE))
                .entity(getErrorCodeDto(e))
                .build();
    }

    private Object getErrorCodeDto(NotFoundException e) {
        if(e instanceof OscNotFoundException){
            return ((OscNotFoundException) e).getErrorCodeDto();
        }
        return new ErrorCodeDto(4000L, Arrays.asList("Not found"));
    }
}
