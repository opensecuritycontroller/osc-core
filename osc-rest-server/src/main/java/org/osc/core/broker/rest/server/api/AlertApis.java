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
import org.osc.core.broker.rest.server.ApiUtil;
import org.osc.core.broker.rest.server.ServerRestConstants;
import org.osc.core.broker.rest.server.annotations.OscAuth;
import org.osc.core.broker.service.GetDtoFromEntityService;
import org.osc.core.broker.service.api.AcknowledgeAlertServiceApi;
import org.osc.core.broker.service.api.DeleteAlertServiceApi;
import org.osc.core.broker.service.api.GetDtoFromEntityServiceApi;
import org.osc.core.broker.service.api.GetDtoFromEntityServiceFactoryApi;
import org.osc.core.broker.service.api.ListAlertServiceApi;
import org.osc.core.broker.service.dto.AlertDto;
import org.osc.core.broker.service.exceptions.ErrorCodeDto;
import org.osc.core.broker.service.request.AlertRequest;
import org.osc.core.broker.service.request.BaseRequest;
import org.osc.core.broker.service.request.GetDtoFromEntityRequest;
import org.osc.core.broker.service.response.BaseResponse;
import org.osc.core.broker.service.response.ListResponse;
import org.osc.core.broker.util.SessionUtil;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.Authorization;

@Component(service = AlertApis.class)
@Api(tags = "Operations for Alerts", authorizations = { @Authorization(value = "Basic Auth") })
@Path(ServerRestConstants.SERVER_API_PATH_PREFIX + "/alerts")
@Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
@OscAuth
public class AlertApis {

    private static final Logger logger = Logger.getLogger(AlertApis.class);

    @Reference
    private ApiUtil apiUtil;

    @Reference
    private AcknowledgeAlertServiceApi acknowledgeAlertService;

    @Reference
    private DeleteAlertServiceApi deleteAlertService;

    @Reference
    private ListAlertServiceApi listAlertService;

    @Reference
    private GetDtoFromEntityServiceFactoryApi getDtoFromEntityServiceFactory;

    @ApiOperation(value = "Lists all Alerts", response = AlertDto.class, responseContainer = "Set")
    @ApiResponses(value = { @ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 400, message = "In case of any error", response = ErrorCodeDto.class) })
    @GET
    public List<AlertDto> getAlerts(@Context HttpHeaders headers) {

        logger.info("Listing Alerts");
        SessionUtil.setUser(SessionUtil.getUsername(headers));

        @SuppressWarnings("unchecked")
        ListResponse<AlertDto> response = (ListResponse<AlertDto>) this.apiUtil.getListResponse(this.listAlertService,
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

        GetDtoFromEntityServiceApi<AlertDto> getDtoService = this.getDtoFromEntityServiceFactory.getService(AlertDto.class);
        return this.apiUtil.submitBaseRequestToService(getDtoService, getDtoRequest).getDto();
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
        this.apiUtil.setIdOrThrow(alertDto, alertId, "Alert");

        AlertRequest alertRequest = createAcknowledgeRequest(alertId, alertDto);

        return this.apiUtil.getResponseForBaseRequest(this.acknowledgeAlertService, alertRequest);
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

        return this.apiUtil.getResponseForBaseRequest(this.deleteAlertService, alertRequest);
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
