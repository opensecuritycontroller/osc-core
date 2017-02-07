package org.osc.core.broker.rest.server.api;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.log4j.Logger;
import org.osc.core.broker.rest.server.OscRestServlet;
import org.osc.core.rest.annotations.AgentAuth;
import org.osc.core.broker.rest.server.exception.ErrorCodeDto;
import org.osc.core.broker.service.AgentRegisterService;
import org.osc.core.broker.service.request.AgentRegisterServiceRequest;
import org.osc.core.broker.service.response.AgentRegisterServiceResponse;
import org.osc.core.broker.util.SessionUtil;
import org.osc.core.rest.client.agent.model.input.AgentRegisterRequest;
import org.osc.core.rest.client.agent.model.output.AgentRegisterResponse;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.Authorization;

@Api(tags = "Operations for Agents", authorizations = { @Authorization(value = "Basic Auth") })
@Path(OscRestServlet.AGENT_API_PATH_PREFIX)
@Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
@AgentAuth
public class AgentApis {

    private static final Logger log = Logger.getLogger(AgentApis.class);

    @ApiOperation(value = "Agent register callback. Agents are expected to callback every 3 minutes to report back"
            + " health status information and ability to inspect traffic.", response = AgentRegisterResponse.class)
    @ApiResponses(value = { @ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 400, message = "In case of any error", response = ErrorCodeDto.class) })
    @Path("/agentregister")
    @POST
    public Response registerAgent(@Context HttpHeaders headers, @ApiParam(required = true) AgentRegisterRequest request) {

        log.info("registerAgent: " + request);
        SessionUtil.setUser(SessionUtil.getUsername(headers));

        AgentRegisterServiceResponse serviceResponse = ApiUtil.submitRequestToService(new AgentRegisterService(),
                new AgentRegisterServiceRequest(request));

        AgentRegisterResponse response = new AgentRegisterResponse(serviceResponse.getApplianceName(),
                serviceResponse.getMgrIp(), serviceResponse.getSharedSecretKey(), serviceResponse.getApplianceConfig2(),
                serviceResponse.getApplianceConfig1());

        return Response.status(Status.OK).entity(response).build();
    }

}
