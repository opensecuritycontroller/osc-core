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
import org.osc.core.broker.rest.server.ServerRestConstants;
import org.osc.core.broker.rest.server.annotations.OscAuth;
import org.osc.core.broker.service.GetDtoFromEntityService;
import org.osc.core.broker.service.api.AddApplianceManagerConnectorServiceApi;
import org.osc.core.broker.service.api.DeleteApplianceManagerConnectorServiceApi;
import org.osc.core.broker.service.api.GetDtoFromEntityServiceApi;
import org.osc.core.broker.service.api.GetDtoFromEntityServiceFactoryApi;
import org.osc.core.broker.service.api.ListApplianceManagerConnectorServiceApi;
import org.osc.core.broker.service.api.ListDomainsByMcIdServiceApi;
import org.osc.core.broker.service.api.UpdateApplianceManagerConnectorServiceApi;
import org.osc.core.broker.service.dto.ApplianceManagerConnectorDto;
import org.osc.core.broker.service.dto.DomainDto;
import org.osc.core.broker.service.exceptions.ErrorCodeDto;
import org.osc.core.broker.service.request.ApplianceManagerConnectorRequest;
import org.osc.core.broker.service.request.BaseIdRequest;
import org.osc.core.broker.service.request.BaseRequest;
import org.osc.core.broker.service.request.DryRunRequest;
import org.osc.core.broker.service.request.GetDtoFromEntityRequest;
import org.osc.core.broker.service.response.BaseJobResponse;
import org.osc.core.broker.service.response.ListResponse;
import org.osc.core.broker.util.SessionUtil;
import org.osc.core.broker.util.api.ApiUtil;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.Authorization;

@Component(service = ManagerConnectorApis.class)
@Api(tags = "Operations for Manager Connectors", authorizations = { @Authorization(value = "Basic Auth") })
@Path(ServerRestConstants.SERVER_API_PATH_PREFIX + "/applianceManagerConnectors")
@Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
@OscAuth
public class ManagerConnectorApis {

    private static final Logger logger = Logger.getLogger(ManagerConnectorApis.class);

    @Reference
    private AddApplianceManagerConnectorServiceApi addService;

    @Reference
    private UpdateApplianceManagerConnectorServiceApi updateService;

    @Reference
    private DeleteApplianceManagerConnectorServiceApi deleteApplianceManagerConnectorService;

    @Reference
    private ListDomainsByMcIdServiceApi listDomainsByMcIdService;

    @Reference
    private ListApplianceManagerConnectorServiceApi listApplianceManagerConnectorService;

    @Reference
    private ApiUtil apiUtil;

    @Reference
    private GetDtoFromEntityServiceFactoryApi getDtoFromEntityServiceFactory;

    @ApiOperation(value = "Lists All Manager Connectors",
            notes = "Password/API Key information is not returned as it is sensitive information",
            response = ApplianceManagerConnectorDto.class,
            responseContainer = "Set")
    @ApiResponses(value = { @ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 400, message = "In case of any error", response = ErrorCodeDto.class) })
    @GET
    public List<ApplianceManagerConnectorDto> getApplianceManagerConnectors(@Context HttpHeaders headers) {

        logger.info("Listing Appliance Manager Connectors");
        SessionUtil.setUser(SessionUtil.getUsername(headers));

        @SuppressWarnings("unchecked")
        ListResponse<ApplianceManagerConnectorDto> response = (ListResponse<ApplianceManagerConnectorDto>) this.apiUtil
                .getListResponse(this.listApplianceManagerConnectorService, new BaseRequest<>(true));

        return response.getList();
    }

    @ApiOperation(value = "Retrieves the Manager Connector by Id",
            notes = "Password/API Key information is not returned as it is sensitive information",
            response = ApplianceManagerConnectorDto.class)
    @Path("/{applianceManagerConnectorId}")
    @ApiResponses(value = { @ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 400, message = "In case of any error", response = ErrorCodeDto.class) })
    @GET
    public ApplianceManagerConnectorDto getApplianceManagerConnector(@Context HttpHeaders headers,
                                                                     @ApiParam(value = "Id of the Appliance Manager Connector",
                                                                             required = true) @PathParam("applianceManagerConnectorId") Long amcId) {

        logger.info("getting Appliance Manager Connector " + amcId);
        SessionUtil.setUser(SessionUtil.getUsername(headers));

        GetDtoFromEntityRequest getDtoRequest = new GetDtoFromEntityRequest();
        getDtoRequest.setEntityId(amcId);
        getDtoRequest.setEntityName("ApplianceManagerConnector");
        GetDtoFromEntityServiceApi<ApplianceManagerConnectorDto> getDtoService = this.getDtoFromEntityServiceFactory.getService(ApplianceManagerConnectorDto.class);
        return this.apiUtil.submitBaseRequestToService(getDtoService, getDtoRequest).getDto();
    }

    @ApiOperation(value = "Creates an Manager Connector",
            notes = "Creates an Manager Connector and sync's it immediately.<br/> "
                    + "If we are unable to connect to the manager using the credentials provided, this call will fail.<br/>"
                    + "To skip validation of IP and credentials 'skipRemoteValidation' flag can be used.",
            response = BaseJobResponse.class)
    @ApiResponses(value = { @ApiResponse(code = 200, message = "Successful operation"), @ApiResponse(code = 400,
            message = "In case of any error validating the information",
            response = ErrorCodeDto.class) })
    @POST
    public Response createApplianceManagerConnector(@Context HttpHeaders headers,
                                                    @ApiParam(required = true) ApplianceManagerConnectorRequest amcRequest) {

        logger.info("Creating Appliance Manager Connector...");
        SessionUtil.setUser(SessionUtil.getUsername(headers));
        Response responseForBaseRequest = this.apiUtil.getResponseForBaseRequest(this.addService,
                    new DryRunRequest<>(amcRequest, amcRequest.isSkipRemoteValidation()));
        return responseForBaseRequest;
    }

    @ApiOperation(
            value = "Updates an Manager Connector.  If we are unable to connect to the endpoint(IP) using the credentials provided, this call will fail.",
            notes = "Creates an Manager Connector and sync's it immediately. "
                    + "If we are unable to connect to the manager using the credentials provided, this call will fail."
                    + "To skip validation of IP and credentials 'skipRemoteValidation' flag can be used.",
            response = BaseJobResponse.class)
    @ApiResponses(value = { @ApiResponse(code = 200, message = "Successful operation"), @ApiResponse(code = 400,
            message = "In case of any error validating the information",
            response = ErrorCodeDto.class) })
    @Path("/{applianceManagerConnectorId}")
    @PUT
    public Response updateApplianceManagerConnector(@Context HttpHeaders headers,
                                                    @ApiParam(value = "Id of the Appliance Manager Connector",
                                                            required = true) @PathParam("applianceManagerConnectorId") Long amcId,
                                                    @ApiParam(required = true) ApplianceManagerConnectorRequest amcRequest) {

        logger.info("Updating Appliance Manager Connector " + amcId);
        SessionUtil.setUser(SessionUtil.getUsername(headers));

        this.apiUtil.setIdOrThrow(amcRequest, amcId, "Appliance Manager Connector");

        Response responseForBaseRequest = this.apiUtil.getResponseForBaseRequest(this.updateService,
                    new DryRunRequest<>(amcRequest, amcRequest.isSkipRemoteValidation()));
        return responseForBaseRequest;
    }

    @ApiOperation(value = "Deletes an Manager Connector",
            notes = "Deletes an Appliance Manager Connector if not referenced by any Distributed Appliances",
            response = BaseJobResponse.class)
    @ApiResponses(value = { @ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 400,
                    message = "In case of any error or if the Manager connector is referenced by a Distributed Appliance",
                    response = ErrorCodeDto.class) })
    @Path("/{applianceManagerConnectorId}")
    @DELETE
    public Response deleteApplianceManagerConnector(@Context HttpHeaders headers,
                                                    @ApiParam(value = "Id of the Appliance Manager Connector",
                                                            required = true) @PathParam("applianceManagerConnectorId") Long amcId) {

        logger.info("Deleting Appliance Manager Connector " + amcId);
        SessionUtil.setUser(SessionUtil.getUsername(headers));

        return this.apiUtil.getResponseForBaseRequest(this.deleteApplianceManagerConnectorService,
                new BaseIdRequest(amcId));
    }

    @ApiOperation(value = "Retrieves the Manager Connector's Domains",
            notes = "Retrieves domains for Appliance Manager Connector specified by the Id",
            response = DomainDto.class)
    @ApiResponses(value = { @ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 400, message = "In case of any error", response = ErrorCodeDto.class) })
    @Path("/{applianceManagerConnectorId}/domains")
    @GET
    public List<DomainDto> getApplianceManagerConnectorDomains(@Context HttpHeaders headers,
                                                               @ApiParam(value = "Id of the Appliance Manager Connector",
                                                                       required = true) @PathParam("applianceManagerConnectorId") Long amcId) {

        logger.info("Listing domains for Appliance Manager Connector " + amcId);
        SessionUtil.setUser(SessionUtil.getUsername(headers));

        @SuppressWarnings("unchecked")
        ListResponse<DomainDto> response = (ListResponse<DomainDto>) this.apiUtil
                .getListResponse(this.listDomainsByMcIdService, new BaseIdRequest(amcId));
        return response.getList();
    }

}
