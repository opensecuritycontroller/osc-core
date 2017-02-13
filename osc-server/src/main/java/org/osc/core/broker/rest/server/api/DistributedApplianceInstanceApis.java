package org.osc.core.broker.rest.server.api;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

import org.apache.log4j.Logger;
import org.osc.core.broker.di.OSC;
import org.osc.core.broker.rest.server.OscRestServlet;
import org.osc.core.broker.util.api.ApiUtil;
import org.osc.core.broker.util.session.SessionUtil;
import org.osc.core.rest.annotations.OscAuth;
import org.osc.core.broker.rest.server.exception.ErrorCodeDto;
import org.osc.core.broker.service.DownloadAgentLogService;
import org.osc.core.broker.service.GetAgentStatusService;
import org.osc.core.broker.service.GetDtoFromEntityService;
import org.osc.core.broker.service.ListDistributedApplianceInstanceService;
import org.osc.core.broker.service.RegisterAgentService;
import org.osc.core.broker.service.SyncAgentService;
import org.osc.core.broker.service.dto.DistributedApplianceDto;
import org.osc.core.broker.service.dto.DistributedApplianceInstanceDto;
import org.osc.core.broker.service.request.BaseIdRequest;
import org.osc.core.broker.service.request.BaseRequest;
import org.osc.core.broker.service.request.DistributedApplianceInstancesRequest;
import org.osc.core.broker.service.request.GetDtoFromEntityRequest;
import org.osc.core.broker.service.response.BaseJobResponse;
import org.osc.core.broker.service.response.DownloadAgentLogResponse;
import org.osc.core.broker.service.response.GetAgentStatusResponseDto;
import org.osc.core.broker.service.response.ListResponse;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.Authorization;

import java.util.List;

@Api(tags = "Operations for Distributed Appliance Instances", authorizations = { @Authorization(value = "Basic Auth") })
@Path(OscRestServlet.SERVER_API_PATH_PREFIX + "/distributedApplianceInstances")
@Consumes({ MediaType.APPLICATION_JSON })
@Produces({ MediaType.APPLICATION_JSON })
@OscAuth
public class DistributedApplianceInstanceApis {

    private static final Logger logger = Logger.getLogger(DistributedApplianceInstanceApis.class);

    SessionUtil sessionUtil = OSC.get().sessionUtil();
    ApiUtil apiUtil = OSC.get().apiUtil();

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
        sessionUtil.setUser(sessionUtil.getUsername(headers));

        ListDistributedApplianceInstanceService service = OSC.get().listDistributedApplianceInstanceService();
        @SuppressWarnings("unchecked")
        ListResponse<DistributedApplianceInstanceDto> response = (ListResponse<DistributedApplianceInstanceDto>) apiUtil
                .getListResponse(service, new BaseRequest<>(true));

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
        sessionUtil.setUser(sessionUtil.getUsername(headers));

        GetDtoFromEntityRequest getDtoRequest = new GetDtoFromEntityRequest();
        getDtoRequest.setEntityId(distributedApplianceInstanceId);
        getDtoRequest.setEntityName("DistributedApplianceInstance");

        GetDtoFromEntityService<DistributedApplianceInstanceDto> getDtoService = OSC.get().dtoFromEntityService();

        return apiUtil.submitBaseRequestToService(getDtoService, getDtoRequest).getDto();
    }

    @ApiOperation(value = "Retrieves the Distributed Appliance Instance agent log",
            notes = "Retrieves the agent logs for the Distributed Appliance Instance specified by the Id")
    @ApiResponses(value = { @ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 400, message = "In case of any error", response = ErrorCodeDto.class) })
    @Path("/{distributedApplianceInstanceId}/log")
    @GET
    @Produces({ MediaType.APPLICATION_OCTET_STREAM })
    public Response downloadLog(@Context HttpHeaders headers,
                                @ApiParam(value = "The Id of the Distributed Appliance Instance",
                                        required = true) @PathParam("distributedApplianceInstanceId") Long distributedApplianceInstanceId) {

        logger.info("Getting Distributed Appliance Instance log " + distributedApplianceInstanceId);
        sessionUtil.setUser(sessionUtil.getUsername(headers));

        BaseIdRequest request = new BaseIdRequest(distributedApplianceInstanceId);
        request.setApi(true);

        DownloadAgentLogService service = new DownloadAgentLogService();

        DownloadAgentLogResponse response = apiUtil.submitBaseRequestToService(service, request);
        ResponseBuilder responseBuilder = Response.ok(response.getSupportBundle());
        responseBuilder.header("Content-Disposition", "attachment; filename=AgentSupportBundle.zip");

        return responseBuilder.build();
    }

    @ApiOperation(value = "Retrieves the Distributed Appliance Instances status",
            notes = "Retrieves the Distributed Appliance Instances statuses specified by the Ids",
            response = GetAgentStatusResponseDto.class)
    @ApiResponses(value = { @ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 400, message = "In case of any error", response = ErrorCodeDto.class) })
    @Path("/status")
    @PUT
    public GetAgentStatusResponseDto getDistributedApplianceInstanceStatus(@Context HttpHeaders headers,
                                                                           @ApiParam(value = "The Ids of the Distributed Appliance Instances to get status for",
                                                                                   required = true) DistributedApplianceInstancesRequest req) {

        logger.info("Getting Distributed Appliance Instance Status " + req);
        sessionUtil.setUser(sessionUtil.getUsername(headers));

        return apiUtil.submitRequestToService(new GetAgentStatusService(), req);
    }

    @ApiOperation(value = "Trigger Synchronization Job for Distributed Appliance Instances",
            notes = "Trigger Synchronization Job for the Distributed Appliance Instance specified, which will attempt to bring all "
                    + "related objects into conformance.<br/>"
                    + " Since this operation applies for multiple objects, only job Id is applicable in the response. The id field is expected to be null.",
            response = BaseJobResponse.class)
    @ApiResponses(value = { @ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 400, message = "In case of any error", response = ErrorCodeDto.class) })
    @Path("/sync")
    @PUT
    public Response syncDistributedApplianceInstance(@Context HttpHeaders headers,
                                                     @ApiParam(value = "The Ids of the Distributed Appliance Instances to sync",
                                                             required = true) DistributedApplianceInstancesRequest req) {
        logger.info("Sync Distributed Appliance Instances" + req);
        sessionUtil.setUser(sessionUtil.getUsername(headers));

        return apiUtil.getResponse(new SyncAgentService(), req);
    }

    @ApiOperation(value = "Trigger Appliance Re-authentication Job for Distributed Appliance Instances",
            notes = "Trigger Synchronization Job for a Distributed Appliance which will attempt to bring all "
                    + "related objects into conformance<br/>"
                    + " Since this operation applies for multiple objects, only job Id is applicable in the response. The id field is expected to be null.",
            response = BaseJobResponse.class)
    @ApiResponses(value = { @ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 400, message = "In case of any error", response = ErrorCodeDto.class) })
    @Path("/authenticate")
    @PUT
    public Response authenticateDistributedApplianceInstance(@Context HttpHeaders headers,
                                                             @ApiParam(value = "The Ids of the Distributed Appliance Instances to trigger re-authentication for",
                                                                     required = true) DistributedApplianceInstancesRequest req) {
        logger.info("Re-Authenticate Distributed Appliance Instance" + req);
        sessionUtil.setUser(sessionUtil.getUsername(headers));

        return apiUtil.getResponse(new RegisterAgentService(), req);
    }
}
