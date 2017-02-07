package org.osc.core.broker.rest.server.api;

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.log4j.Logger;
import org.osc.core.broker.model.entities.events.AcknowledgementStatus;
import org.osc.core.broker.rest.server.OscRestServlet;
import org.osc.core.rest.annotations.OscAuth;
import org.osc.core.broker.rest.server.exception.ErrorCodeDto;
import org.osc.core.broker.service.GetDtoFromEntityService;
import org.osc.core.broker.service.alert.AcknowledgeAlertService;
import org.osc.core.broker.service.alert.AlertDto;
import org.osc.core.broker.service.alert.AlertRequest;
import org.osc.core.broker.service.alert.DeleteAlertService;
import org.osc.core.broker.service.alert.ListAlertService;
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

@Api(tags = "Operations for Alerts", authorizations = { @Authorization(value = "Basic Auth") })
@Path(OscRestServlet.SERVER_API_PATH_PREFIX + "/alerts")
@Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
@OscAuth
public class AlertApis {

    private static final Logger logger = Logger.getLogger(AlertApis.class);

    @ApiOperation(value = "Lists all Alerts", response = AlertDto.class, responseContainer = "Set")
    @ApiResponses(value = { @ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 400, message = "In case of any error", response = ErrorCodeDto.class) })
    @GET
    public List<AlertDto> getAlerts(@Context HttpHeaders headers) {

        logger.info("Listing Alerts");
        SessionUtil.setUser(SessionUtil.getUsername(headers));

        @SuppressWarnings("unchecked")
        ListResponse<AlertDto> response = (ListResponse<AlertDto>) ApiUtil.getListResponse(new ListAlertService(),
                new BaseRequest<>(true));

        return response.getList();
    }

    @ApiOperation(value = "Retrieves the Alert by Id", response = AlertDto.class)
    @ApiResponses(value = { @ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 400, message = "In case of any error", response = ErrorCodeDto.class) })
    @Path("/{alertId}")
    @GET
    public AlertDto getAlert(@Context HttpHeaders headers, @PathParam("alertId") Long alertId) {

        logger.info("getting Alert " + alertId);
        SessionUtil.setUser(SessionUtil.getUsername(headers));

        GetDtoFromEntityRequest getDtoRequest = new GetDtoFromEntityRequest();
        getDtoRequest.setEntityId(alertId);
        getDtoRequest.setEntityName("Alert");

        GetDtoFromEntityService<AlertDto> getDtoService = new GetDtoFromEntityService<AlertDto>();
        return ApiUtil.submitBaseRequestToService(getDtoService, getDtoRequest).getDto();
    }

    //TODO: Future. Allow multi update/delete of alerts

    /**
     * Updates the acknowledgment status of an Alert
     *
     * @return the Id of the Acknowledged/Unacknowledged alert
     */
    @ApiOperation(value = "Updates the acknowledgment status of an Alert", response = BaseResponse.class)
    @ApiResponses(value = { @ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 400, message = "In case of any error", response = ErrorCodeDto.class) })
    @Path("/{alertId}")
    @PUT
    public Response updateAlert(@Context HttpHeaders headers, @PathParam("alertId") Long alertId,
                                @ApiParam(required = true) AlertDto alertDto) {

        logger.info("Updating Alert " + alertId);
        SessionUtil.setUser(SessionUtil.getUsername(headers));
        ApiUtil.setIdOrThrow(alertDto, alertId, "Alert");

        AlertRequest alertRequest = createAcknowledgeRequest(alertId, alertDto);
        AcknowledgeAlertService service = new AcknowledgeAlertService();

        return ApiUtil.getResponseForBaseRequest(service, alertRequest);
    }

    /**
     * Delete an Alert
     *
     * @param alertId
     * @return
     */
    @ApiOperation(value = "Deletes an Alert by Id")
    @ApiResponses(value = { @ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 400, message = "In case of any error", response = ErrorCodeDto.class) })
    @Path("/{alertId}")
    @DELETE
    public Response deleteAlert(@Context HttpHeaders headers, @PathParam("alertId") Long alertId) {

        logger.info("Deleting the Alert " + alertId);
        SessionUtil.setUser(SessionUtil.getUsername(headers));

        AlertRequest alertRequest = createDeleteRequest(alertId);

        DeleteAlertService deleteService = new DeleteAlertService();

        return ApiUtil.getResponseForBaseRequest(deleteService, alertRequest);
    }

    private AlertRequest createDeleteRequest(Long alertId) {
        AlertRequest alertRequest = new AlertRequest();
        List<AlertDto> alertList = new ArrayList<AlertDto>();
        AlertDto alertDto = new AlertDto();
        alertDto.setId(alertId);
        alertList.add(alertDto);
        alertRequest.setDtoList(alertList);
        return alertRequest;
    }

    private AlertRequest createAcknowledgeRequest(Long alertId, AlertDto alertDto) {
        AlertRequest alertRequest = new AlertRequest();
        List<AlertDto> alertList = new ArrayList<AlertDto>();
        alertList.add(alertDto);
        alertRequest.setDtoList(alertList);
        if (alertDto.getStatus().equals(AcknowledgementStatus.ACKNOWLEDGED)) {
            alertRequest.setAcknowledge(true);
        } else {
            alertRequest.setAcknowledge(false);
        }
        return alertRequest;
    }
}
