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
package org.osc.core.broker.rest.server.api;

import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.log4j.Logger;
import org.osc.core.broker.rest.server.ApiUtil;
import org.osc.core.broker.rest.server.OscAuthFilter;
import org.osc.core.broker.rest.server.ServerRestConstants;
import org.osc.core.broker.rest.server.annotations.OscAuth;
import org.osc.core.broker.service.api.AddAlarmServiceApi;
import org.osc.core.broker.service.api.DeleteAlarmServiceApi;
import org.osc.core.broker.service.api.GetDtoFromEntityServiceApi;
import org.osc.core.broker.service.api.GetDtoFromEntityServiceFactoryApi;
import org.osc.core.broker.service.api.ListAlarmServiceApi;
import org.osc.core.broker.service.api.UpdateAlarmServiceApi;
import org.osc.core.broker.service.api.server.UserContextApi;
import org.osc.core.broker.service.dto.AlarmDto;
import org.osc.core.broker.service.dto.BaseDto;
import org.osc.core.broker.service.exceptions.ErrorCodeDto;
import org.osc.core.broker.service.request.BaseIdRequest;
import org.osc.core.broker.service.request.BaseRequest;
import org.osc.core.broker.service.request.GetDtoFromEntityRequest;
import org.osc.core.broker.service.response.BaseResponse;
import org.osc.core.broker.service.response.ListResponse;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.Authorization;


@Component(service = AlarmApis.class)
@Api(tags = "Operations for Alarms", authorizations = { @Authorization(value = "Basic Auth") })
@Path(ServerRestConstants.SERVER_API_PATH_PREFIX + "/alarms")
@Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@OscAuth
public class AlarmApis {

    private static final Logger logger = Logger.getLogger(AlarmApis.class);

    @Reference
    private ApiUtil apiUtil;

    @Reference
    private AddAlarmServiceApi addAlarmService;

    @Reference
    private UpdateAlarmServiceApi updateAlarmService;

    @Reference
    private DeleteAlarmServiceApi deleteAlarmService;

    @Reference
    private ListAlarmServiceApi listAlarmService;

    @Reference
    private GetDtoFromEntityServiceFactoryApi getDtoFromEntityServiceFactory;

    @Reference
    private UserContextApi userContext;

    @ApiOperation(value = "Lists all configured Alarms", response = AlarmDto.class, responseContainer = "Set")
    @ApiResponses(value = { @ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 400, message = "In case of any error", response = ErrorCodeDto.class) })
    @GET
    public List<AlarmDto> getAlarms(@Context HttpHeaders headers) {

        logger.info("Listing Alarms");
        this.userContext.setUser(OscAuthFilter.getUsername(headers));

        @SuppressWarnings("unchecked")
        ListResponse<AlarmDto> response = (ListResponse<AlarmDto>) this.apiUtil.getListResponse(this.listAlarmService,
                new BaseRequest<BaseDto>(true));

        return response.getList();
    }

    @ApiOperation(value = "Retrieves an Alarm by Id", response = AlarmDto.class)
    @ApiResponses(value = { @ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 400, message = "In case of any error", response = ErrorCodeDto.class) })
    @Path("/{alarmId}")
    @GET
    public AlarmDto getAlarm(@Context HttpHeaders headers, @PathParam("alarmId") Long alarmId) {

        logger.info("getting Alarm " + alarmId);
        this.userContext.setUser(OscAuthFilter.getUsername(headers));

        GetDtoFromEntityRequest getDtoRequest = new GetDtoFromEntityRequest();
        getDtoRequest.setEntityId(alarmId);
        getDtoRequest.setEntityName("Alarm");

        GetDtoFromEntityServiceApi<AlarmDto> getDtoService = this.getDtoFromEntityServiceFactory.getService(AlarmDto.class);
        return this.apiUtil.submitBaseRequestToService(getDtoService, getDtoRequest).getDto();
    }

    /**
     * Creates an Alarm
     *
     * @param alarmDto
     * @return
     */
    @ApiOperation(value = "Creates an Alarm", response = AlarmDto.class)
    @ApiResponses(value = { @ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 400, message = "In case of any error", response = ErrorCodeDto.class) })
    @POST
    public Response createAlarm(@Context HttpHeaders headers, @ApiParam(required = true) AlarmDto alarmDto) {

        logger.info("Creating Alarm...");
        this.userContext.setUser(OscAuthFilter.getUsername(headers));
        return this.apiUtil.getResponseForBaseRequest(this.addAlarmService, new BaseRequest<AlarmDto>(alarmDto));
    }

    /**
     * Updates an Alarm
     *
     * @return the Id of the updated alarm
     */
    @ApiOperation(value = "Updates an Alarm", response = BaseResponse.class)
    @ApiResponses(value = { @ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 400, message = "In case of any error", response = ErrorCodeDto.class) })
    @Path("/{alarmId}")
    @PUT
    public Response updateAlarm(@Context HttpHeaders headers, @PathParam("alarmId") Long alarmId,
                                @ApiParam(required = true) AlarmDto alarmDto) {

        logger.info("Updating Alarm " + alarmId);
        this.userContext.setUser(OscAuthFilter.getUsername(headers));
        this.apiUtil.setIdOrThrow(alarmDto, alarmId, "Alarm");
        return this.apiUtil.getResponseForBaseRequest(this.updateAlarmService, new BaseRequest<AlarmDto>(alarmDto));
    }

    /**
     * Delete an Alarm
     *
     * @param alarmId
     * @return
     */
    @ApiOperation(value = "Deletes an Alarm by Id")
    @ApiResponses(value = { @ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 400, message = "In case of any error", response = ErrorCodeDto.class) })
    @Path("/{alarmId}")
    @DELETE
    public Response deleteAlarm(@Context HttpHeaders headers, @PathParam("alarmId") Long alarmId) {

        logger.info("Deleting the Alarm " + alarmId);
        this.userContext.setUser(OscAuthFilter.getUsername(headers));

        return this.apiUtil.getResponseForBaseRequest(this.deleteAlarmService, new BaseIdRequest(alarmId));
    }
}
