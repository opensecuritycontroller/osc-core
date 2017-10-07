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
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;

import org.osc.core.broker.rest.server.ApiUtil;
import org.slf4j.LoggerFactory;
import org.osc.core.broker.rest.server.OscAuthFilter;
import org.osc.core.broker.rest.server.ServerRestConstants;
import org.osc.core.broker.rest.server.annotations.OscAuth;
import org.osc.core.broker.service.api.GetAgentStatusServiceApi;
import org.osc.core.broker.service.api.GetDtoFromEntityServiceApi;
import org.osc.core.broker.service.api.GetDtoFromEntityServiceFactoryApi;
import org.osc.core.broker.service.api.ListDistributedApplianceInstanceServiceApi;
import org.osc.core.broker.service.api.server.UserContextApi;
import org.osc.core.broker.service.dto.DistributedApplianceDto;
import org.osc.core.broker.service.dto.DistributedApplianceInstanceDto;
import org.osc.core.broker.service.exceptions.ErrorCodeDto;
import org.osc.core.broker.service.request.BaseRequest;
import org.osc.core.broker.service.request.DistributedApplianceInstancesRequest;
import org.osc.core.broker.service.request.GetDtoFromEntityRequest;
import org.osc.core.broker.service.response.GetAgentStatusResponse;
import org.osc.core.broker.service.response.ListResponse;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.Authorization;

@Component(service = DistributedApplianceInstanceApis.class)
@Api(tags = "Operations for Distributed Appliance Instances", authorizations = { @Authorization(value = "Basic Auth") })
@Path(ServerRestConstants.SERVER_API_PATH_PREFIX + "/distributedApplianceInstances")
@Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
@OscAuth
public class DistributedApplianceInstanceApis {

    private static final Logger logger = LoggerFactory.getLogger(DistributedApplianceInstanceApis.class);

    @Reference
    private ApiUtil apiUtil;

    @Reference
    private GetAgentStatusServiceApi getAgentStatusService;

    @Reference
    private ListDistributedApplianceInstanceServiceApi listDAIService;

    @Reference
    private GetDtoFromEntityServiceFactoryApi getDtoFromEntityServiceFactory;

    @Reference
    private UserContextApi userContext;

    @ApiOperation(value = "Lists All Distributed Appliance Instances",
            notes = "Lists all the Distributed Appliance Instances",
            response = DistributedApplianceInstanceDto.class,
            responseContainer = "Set")
    @ApiResponses(value = { @ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 400, message = "In case of any error", response = ErrorCodeDto.class) })
    @GET
    public List<DistributedApplianceInstanceDto> listDistributedApplianceInstances(
            @Context HttpHeaders headers) {

        logger.info("Listing Distributed Appliance Instances");
        this.userContext.setUser(OscAuthFilter.getUsername(headers));

        @SuppressWarnings("unchecked")
        ListResponse<DistributedApplianceInstanceDto> response = (ListResponse<DistributedApplianceInstanceDto>) this.apiUtil
        .getListResponse(this.listDAIService, new BaseRequest<>(true));

        return response.getList();
    }

    @ApiOperation(value = "Retrieves the Distributed Appliance Instance",
            notes = "Retrieves a Distributed Appliance Instance specified by the Id",
            response = DistributedApplianceDto.class)
    @ApiResponses(value = { @ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 400, message = "In case of any error", response = ErrorCodeDto.class) })
    @Path("/{distributedApplianceInstanceId}")
    @GET
    public DistributedApplianceInstanceDto getDistributedApplianceInstance(@Context HttpHeaders headers,
                                                                           @ApiParam(value = "The Id of the Distributed Appliance Instance",
                                                                                   required = true) @PathParam("distributedApplianceInstanceId") Long distributedApplianceInstanceId) {

        logger.info("Getting Distributed Appliance Instance " + distributedApplianceInstanceId);
        this.userContext.setUser(OscAuthFilter.getUsername(headers));

        GetDtoFromEntityRequest getDtoRequest = new GetDtoFromEntityRequest();
        getDtoRequest.setEntityId(distributedApplianceInstanceId);
        getDtoRequest.setEntityName("DistributedApplianceInstance");
        GetDtoFromEntityServiceApi<DistributedApplianceInstanceDto> getDtoService = this.getDtoFromEntityServiceFactory.getService(DistributedApplianceInstanceDto.class);

        return this.apiUtil.submitBaseRequestToService(getDtoService, getDtoRequest).getDto();
    }

    @ApiOperation(value = "Retrieves the Distributed Appliance Instances status",
            notes = "Retrieves the Distributed Appliance Instances statuses specified by the Ids",
            response = GetAgentStatusResponse.class)
    @ApiResponses(value = { @ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 400, message = "In case of any error", response = ErrorCodeDto.class) })
    @Path("/status")
    @PUT
    public GetAgentStatusResponse getDistributedApplianceInstanceStatus(@Context HttpHeaders headers,
                                                                           @ApiParam(value = "The Ids of the Distributed Appliance Instances to get status for",
                                                                                   required = true) DistributedApplianceInstancesRequest req) {

        logger.info("Getting Distributed Appliance Instance Status " + req);
        this.userContext.setUser(OscAuthFilter.getUsername(headers));

        return this.apiUtil.submitRequestToService(this.getAgentStatusService, req);
    }
}
