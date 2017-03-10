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
import org.osc.core.broker.rest.server.OscRestServlet;
import org.osc.core.rest.annotations.OscAuth;
import org.osc.core.broker.rest.server.exception.ErrorCodeDto;
import org.osc.core.broker.service.AddDistributedApplianceService;
import org.osc.core.broker.service.ConformService;
import org.osc.core.broker.service.DeleteDistributedApplianceService;
import org.osc.core.broker.service.GetDtoFromEntityService;
import org.osc.core.broker.service.ListDistributedApplianceService;
import org.osc.core.broker.service.UpdateDistributedApplianceService;
import org.osc.core.broker.service.dto.BaseDto;
import org.osc.core.broker.service.dto.DistributedApplianceDto;
import org.osc.core.broker.service.request.BaseDeleteRequest;
import org.osc.core.broker.service.request.BaseRequest;
import org.osc.core.broker.service.request.ConformRequest;
import org.osc.core.broker.service.request.GetDtoFromEntityRequest;
import org.osc.core.broker.service.response.AddDistributedApplianceResponse;
import org.osc.core.broker.service.response.BaseJobResponse;
import org.osc.core.broker.service.response.ListResponse;
import org.osc.core.broker.util.SessionUtil;
import org.osgi.service.component.annotations.Component;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.Authorization;

@Component(service = DistributedApplianceApis.class)
@Api(tags = "Operations for Distributed Appliances", authorizations = { @Authorization(value = "Basic Auth") })
@Path(OscRestServlet.SERVER_API_PATH_PREFIX + "/distributedAppliances")
@Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
@OscAuth
public class DistributedApplianceApis {

    private static final Logger logger = Logger.getLogger(DistributedApplianceApis.class);

    @ApiOperation(value = "Lists All Distributed Appliances",
            notes = "Lists all the Distributed Appliances",
            response = DistributedApplianceDto.class,
            responseContainer = "Set")
    @ApiResponses(value = { @ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 400, message = "In case of any error", response = ErrorCodeDto.class) })
    @GET
    public List<DistributedApplianceDto> getDistributedAppliance(@Context HttpHeaders headers) {

        logger.info("Listing Distributed Appliances");
        SessionUtil.setUser(SessionUtil.getUsername(headers));

        @SuppressWarnings("unchecked")
        ListResponse<DistributedApplianceDto> response = (ListResponse<DistributedApplianceDto>) ApiUtil
                .getListResponse(new ListDistributedApplianceService(), new BaseRequest<BaseDto>(true));
        return response.getList();
    }

    @ApiOperation(value = "Retrieves the Distributed Appliance",
            notes = "Retrieves a Distributed Appliance specified by the Id",
            response = DistributedApplianceDto.class)
    @ApiResponses(value = { @ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 400, message = "In case of any error", response = ErrorCodeDto.class) })
    @Path("/{distributedApplianceId}")
    @GET
    public DistributedApplianceDto getDistributedAppliance(@Context HttpHeaders headers,
                                                           @ApiParam(value = "The Id of the Distributed Appliance",
                                                                   required = true) @PathParam("distributedApplianceId") Long distributedApplianceId) {

        logger.info("Getting Distributed Appliance " + distributedApplianceId);
        SessionUtil.setUser(SessionUtil.getUsername(headers));

        GetDtoFromEntityRequest getDtoRequest = new GetDtoFromEntityRequest();
        getDtoRequest.setEntityId(distributedApplianceId);
        getDtoRequest.setEntityName("DistributedAppliance");
        GetDtoFromEntityService<DistributedApplianceDto> getDtoService = new GetDtoFromEntityService<DistributedApplianceDto>();
        return ApiUtil.submitBaseRequestToService(getDtoService, getDtoRequest).getDto();
    }

    @ApiOperation(value = "Creates an Distributed Appliance",
            notes = "Creates an Distributed Appliance and sync's it immediately.",
            response = AddDistributedApplianceResponse.class)
    @ApiResponses(value = { @ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 400, message = "In case of any error", response = ErrorCodeDto.class) })
    @POST
    public Response createAppliance(@Context HttpHeaders headers,
                                    @ApiParam(required = true) DistributedApplianceDto daDto) {
        logger.info("Creating Distributed Appliance...");
        SessionUtil.setUser(SessionUtil.getUsername(headers));
        return ApiUtil.getResponseForBaseRequest(new AddDistributedApplianceService(),
                new BaseRequest<DistributedApplianceDto>(daDto));
    }

    @ApiOperation(value = "Updates a Distributed Appliance",
            notes = "Updates a Distributed Appliance and sync's it immediately.",
            response = BaseJobResponse.class)
    @ApiResponses(value = { @ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 400, message = "In case of any error", response = ErrorCodeDto.class) })
    @Path("/{distributedApplianceId}")
    @PUT
    public Response updateDistributedAppliance(@Context HttpHeaders headers,
                                               @ApiParam(value = "The Id of the Distributed Appliance",
                                                       required = true) @PathParam("distributedApplianceId") Long distributedApplianceId,
                                               @ApiParam(required = true) DistributedApplianceDto daDto) {
        logger.info("Updating Distributed Appliance " + distributedApplianceId);
        SessionUtil.setUser(SessionUtil.getUsername(headers));
        ApiUtil.setIdOrThrow(daDto, distributedApplianceId, "DistributedAppliance");
        return ApiUtil.getResponseForBaseRequest(new UpdateDistributedApplianceService(),
                new BaseRequest<DistributedApplianceDto>(daDto));
    }

    @ApiOperation(value = "Deletes a Distributed Appliance",
            notes = "Triggers a Job to clean up all artifacts by Distributed Appliance references objects.",
            response = BaseJobResponse.class)
    @ApiResponses(value = { @ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 400, message = "In case of any error", response = ErrorCodeDto.class) })
    @Path("/{distributedApplianceId}")
    @DELETE
    public Response deleteDistributedAppliance(@Context HttpHeaders headers,
                                               @ApiParam(value = "The Id of the Distributed Appliance Appliance",
                                                       required = true) @PathParam("distributedApplianceId") Long distributedApplianceId) {
        logger.info("Deleting Distributed Appliance " + distributedApplianceId);
        SessionUtil.setUser(SessionUtil.getUsername(headers));
        return ApiUtil.getResponseForBaseRequest(new DeleteDistributedApplianceService(),
                new BaseDeleteRequest(distributedApplianceId, false)); // false as this is not force delete
    }

    @ApiOperation(value = "Force Delete a Distributed Appliance",
            notes = "This will not trigger a Job and will not attempt to clean up any artifacts.")
    @ApiResponses(value = { @ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 400, message = "In case of any error", response = ErrorCodeDto.class) })
    @Path("/{distributedApplianceId}/force")
    @DELETE
    public Response forceDeleteDistributedAppliance(@Context HttpHeaders headers,
                                                    @ApiParam(value = "The Id of the Distributed Appliance",
                                                            required = true) @PathParam("distributedApplianceId") Long distributedApplianceId) {
        logger.info("Deleting Distributed Appliance " + distributedApplianceId);
        SessionUtil.setUser(SessionUtil.getUsername(headers));
        return ApiUtil.getResponseForBaseRequest(new DeleteDistributedApplianceService(),
                new BaseDeleteRequest(distributedApplianceId, true));
    }

    @ApiOperation(value = "Trigger Synchronization Job for a Distributed Appliance",
            notes = "Trigger Synchronization Job for a Distributed Appliance which will attempt to bring all "
                    + "related objects into conformance.",
            response = BaseJobResponse.class)
    @ApiResponses(value = { @ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 400, message = "In case of any error", response = ErrorCodeDto.class) })
    @Path("/{distributedApplianceId}/sync")
    @PUT
    public Response syncDistributedAppliance(@Context HttpHeaders headers,
                                             @ApiParam(value = "The Id of the Distributed Appliance",
                                                     required = true) @PathParam("distributedApplianceId") Long distributedApplianceId) {
        logger.info("Sync Distributed Appliance " + distributedApplianceId);
        SessionUtil.setUser(SessionUtil.getUsername(headers));
        return ApiUtil.getResponseForBaseRequest(new ConformService(), new ConformRequest(distributedApplianceId));
    }

}
