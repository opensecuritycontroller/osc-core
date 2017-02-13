package org.osc.core.broker.rest.server.api;

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
import org.osc.core.broker.rest.server.OscRestServlet;
import org.osc.core.rest.annotations.OscAuth;
import org.osc.core.broker.rest.server.exception.ErrorCodeDto;
import org.osc.core.broker.service.GetDtoFromEntityService;
import org.osc.core.broker.service.alarm.AddAlarmService;
import org.osc.core.broker.service.alarm.AlarmDto;
import org.osc.core.broker.service.alarm.DeleteAlarmService;
import org.osc.core.broker.service.alarm.ListAlarmService;
import org.osc.core.broker.service.alarm.UpdateAlarmService;
import org.osc.core.broker.service.dto.BaseDto;
import org.osc.core.broker.service.request.BaseIdRequest;
import org.osc.core.broker.service.request.BaseRequest;
import org.osc.core.broker.service.request.GetDtoFromEntityRequest;
import org.osc.core.broker.service.response.BaseResponse;
import org.osc.core.broker.service.response.ListResponse;
import org.osc.core.broker.util.SessionUtil;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.Authorization;

import java.util.List;

@Api(tags = "Operations for Alarms", authorizations = { @Authorization(value = "Basic Auth") })
@Path(OscRestServlet.SERVER_API_PATH_PREFIX + "/alarms")
@Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@OscAuth
public class AlarmApis {

    private static final Logger logger = Logger.getLogger(AlarmApis.class);

    @ApiOperation(value = "Lists all configured Alarms", response = AlarmDto.class, responseContainer = "Set")
    @ApiResponses(value = { @ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 400, message = "In case of any error", response = ErrorCodeDto.class) })
    @GET
    public List<AlarmDto> getAlarms(@Context HttpHeaders headers) {

        logger.info("Listing Alarms");
        SessionUtil.setUser(SessionUtil.getUsername(headers));

        @SuppressWarnings("unchecked")
        ListResponse<AlarmDto> response = (ListResponse<AlarmDto>) ApiUtil.getListResponse(new ListAlarmService(),
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
        SessionUtil.setUser(SessionUtil.getUsername(headers));

        GetDtoFromEntityRequest getDtoRequest = new GetDtoFromEntityRequest();
        getDtoRequest.setEntityId(alarmId);
        getDtoRequest.setEntityName("Alarm");

        GetDtoFromEntityService<AlarmDto> getDtoService = new GetDtoFromEntityService<AlarmDto>();
        return ApiUtil.submitBaseRequestToService(getDtoService, getDtoRequest).getDto();
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
        SessionUtil.setUser(SessionUtil.getUsername(headers));
        return ApiUtil.getResponseForBaseRequest(new AddAlarmService(), new BaseRequest<AlarmDto>(alarmDto));
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
        SessionUtil.setUser(SessionUtil.getUsername(headers));
        ApiUtil.setIdOrThrow(alarmDto, alarmId, "Alarm");
        return ApiUtil.getResponseForBaseRequest(new UpdateAlarmService(), new BaseRequest<AlarmDto>(alarmDto));
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
        SessionUtil.setUser(SessionUtil.getUsername(headers));

        return ApiUtil.getResponseForBaseRequest(new DeleteAlarmService(), new BaseIdRequest(alarmId));
    }
}
