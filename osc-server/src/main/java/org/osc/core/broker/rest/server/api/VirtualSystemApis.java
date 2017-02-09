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
import org.osc.core.broker.di.OSC;
import org.osc.core.broker.rest.server.OscRestServlet;
import org.osc.core.broker.util.api.ApiUtil;
import org.osc.core.broker.util.session.SessionUtil;
import org.osc.core.rest.annotations.OscAuth;
import org.osc.core.broker.rest.server.exception.ErrorCodeDto;
import org.osc.core.broker.service.AddDeploymentSpecService;
import org.osc.core.broker.service.DeleteDeploymentSpecService;
import org.osc.core.broker.service.ForceDeleteVirtualSystemService;
import org.osc.core.broker.service.GetDtoFromEntityService;
import org.osc.core.broker.service.ListDeploymentSpecServiceByVirtualSystem;
import org.osc.core.broker.service.ListDistributedApplianceInstanceByVSService;
import org.osc.core.broker.service.UpdateDeploymentSpecService;
import org.osc.core.broker.service.dto.ApplianceManagerConnectorDto;
import org.osc.core.broker.service.dto.DistributedApplianceInstanceDto;
import org.osc.core.broker.service.dto.openstack.DeploymentSpecDto;
import org.osc.core.broker.service.policy.ListVirtualSystemPolicyService;
import org.osc.core.broker.service.policy.PolicyDto;
import org.osc.core.broker.service.request.BaseDeleteRequest;
import org.osc.core.broker.service.request.BaseIdRequest;
import org.osc.core.broker.service.request.BaseRequest;
import org.osc.core.broker.service.request.GetDtoFromEntityRequest;
import org.osc.core.broker.service.response.BaseJobResponse;
import org.osc.core.broker.service.response.ListResponse;
import org.osc.core.broker.service.securityinterface.AddSecurityGroupInterfaceService;
import org.osc.core.broker.service.securityinterface.DeleteSecurityGroupInterfaceService;
import org.osc.core.broker.service.securityinterface.ListSecurityGroupInterfaceServiceByVirtualSystem;
import org.osc.core.broker.service.securityinterface.SecurityGroupInterfaceDto;
import org.osc.core.broker.service.securityinterface.UpdateSecurityGroupInterfaceService;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.Authorization;

@Api(tags = "Operations for Virtual Systems", authorizations = { @Authorization(value = "Basic Auth") })
@Path(OscRestServlet.SERVER_API_PATH_PREFIX + "/virtualSystems")
@Consumes({ MediaType.APPLICATION_JSON })
@Produces({ MediaType.APPLICATION_JSON })
@OscAuth
public class VirtualSystemApis {

    private static final Logger logger = Logger.getLogger(VirtualSystemApis.class);

    SessionUtil sessionUtil = OSC.get().sessionUtil();
    ApiUtil apiUtil = OSC.get().apiUtil();

    // DAI APIs
    @ApiOperation(value = "Lists Appliance Instances",
            notes = "Lists the Appliance Instances owned by the Virtual System",
            response = DistributedApplianceInstanceDto.class,
            responseContainer = "Set")
    @ApiResponses(value = { @ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 400, message = "In case of any error", response = ErrorCodeDto.class) })
    @Path("/{vsId}/distributedApplianceInstances")
    @GET
    public List<DistributedApplianceInstanceDto> getDistributedApplianceInstances(
            @Context HttpHeaders headers, @ApiParam(value = "The Virtual System Id") @PathParam("vsId") Long vsId) {

        logger.info("Listing Distributed Appliance Instances based on given VS id");
        sessionUtil.setUser(sessionUtil.getUsername(headers));

        @SuppressWarnings("unchecked")
        ListResponse<DistributedApplianceInstanceDto> response = (ListResponse<DistributedApplianceInstanceDto>) apiUtil
                .getListResponse(new ListDistributedApplianceInstanceByVSService(), new BaseIdRequest(vsId));
        return response.getList();
    }

    @ApiOperation(value = "Lists Policies",
            notes = "Lists the Policies owned by the Virtual System",
            response = PolicyDto.class,
            responseContainer = "Set")
    @ApiResponses(value = { @ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 400, message = "In case of any error", response = ErrorCodeDto.class) })
    @Path("/{vsId}/policies")
    @GET
    public List<PolicyDto> getPolicies(@Context HttpHeaders headers,
                                       @ApiParam(value = "The Virtual System Id") @PathParam("vsId") Long vsId) {

        logger.info("Listing Policies based on given VS id");
        sessionUtil.setUser(sessionUtil.getUsername(headers));

        @SuppressWarnings("unchecked")
        ListResponse<PolicyDto> response = (ListResponse<PolicyDto>) apiUtil.getListResponse(new ListVirtualSystemPolicyService(),
                new BaseIdRequest(vsId));
        return response.getList();
    }

    // DS APIS
    @ApiOperation(value = "Lists Deployment Specifications (Openstack Only)",
            notes = "Lists the Deployment Specifications owned by the Virtual System",
            response = DeploymentSpecDto.class,
            responseContainer = "Set")
    @ApiResponses(value = { @ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 400, message = "In case of any error", response = ErrorCodeDto.class) })
    @Path("/{vsId}/deploymentSpecs")
    @GET
    public List<DeploymentSpecDto> getDeploymentSpecsByVirtualSystem(@Context HttpHeaders headers,
                                                                     @ApiParam(value = "The Virtual System Id") @PathParam("vsId") Long vsId) {
        logger.info("Listing Deployment Spces");
        sessionUtil.setUser(sessionUtil.getUsername(headers));

        @SuppressWarnings("unchecked")
        ListResponse<DeploymentSpecDto> response = (ListResponse<DeploymentSpecDto>) apiUtil
                .getListResponse(new ListDeploymentSpecServiceByVirtualSystem(), new BaseIdRequest(vsId));
        return response.getList();
    }

    @ApiOperation(value = "Retrieves the Deployment Specification (Openstack Only)",
            notes = "Retrieves a Deployment Specification specified by its owning Virtual System and Deployment Spec Id",
            response = ApplianceManagerConnectorDto.class)
    @ApiResponses(value = { @ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 400, message = "In case of any error", response = ErrorCodeDto.class) })
    @Path("/{vsId}/deploymentSpecs/{dsId}")
    @GET
    public DeploymentSpecDto getDeploymentSpec(@Context HttpHeaders headers,
                                               @ApiParam(value = "The Virtual System Id") @PathParam("vsId") Long vsId,
                                               @ApiParam(value = "The Deployment Specification Id") @PathParam("dsId") Long dsId) {
        logger.info("getting Deployment Spec " + dsId);
        sessionUtil.setUser(sessionUtil.getUsername(headers));
        GetDtoFromEntityRequest getDtoRequest = new GetDtoFromEntityRequest();
        getDtoRequest.setEntityId(dsId);
        getDtoRequest.setEntityName("DeploymentSpec");
        GetDtoFromEntityService<DeploymentSpecDto> getDtoService = new GetDtoFromEntityService<DeploymentSpecDto>();
        DeploymentSpecDto dto = apiUtil.submitBaseRequestToService(getDtoService, getDtoRequest).getDto();

        apiUtil.validateParentIdMatches(dto, vsId, "SecurityGroup");

        return dto;
    }

    @ApiOperation(value = "Creates a Deployment Specification (Openstack Only)",
            notes = "Creates a Deployment Specification Object owned by Virtual System provided",
            response = BaseJobResponse.class)
    @ApiResponses(value = { @ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 400, message = "In case of any error", response = ErrorCodeDto.class) })
    @Path("/{vsId}/deploymentSpecs")
    @POST
    public Response createDeploymentSpec(@Context HttpHeaders headers,
                                         @ApiParam(value = "The Virtual System Id") @PathParam("vsId") Long vsId,
                                         @ApiParam(required = true) DeploymentSpecDto dsDto) {
        logger.info("Creating Deployment Spec ...");
        sessionUtil.setUser(sessionUtil.getUsername(headers));
        apiUtil.setParentIdOrThrow(dsDto, vsId, "Deployment Specification");
        return apiUtil.getResponseForBaseRequest(new AddDeploymentSpecService(),
                new BaseRequest<>(dsDto));
    }

    @ApiOperation(value = "Updates a Deployment Specification (Openstack Only)",
            notes = "Updates a Deployment Specification",
            response = BaseJobResponse.class)
    @ApiResponses(value = { @ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 400, message = "In case of any error", response = ErrorCodeDto.class) })
    @Path("/{vsId}/deploymentSpecs/{dsId}")
    @PUT
    public Response updateDeploymentSpec(@Context HttpHeaders headers,
                                         @ApiParam(value = "The Virtual System Id") @PathParam("vsId") Long vsId,
                                         @ApiParam(value = "The Deployment Specification Id") @PathParam("dsId") Long dsId,
                                         @ApiParam(required = true) DeploymentSpecDto dsDto) {
        logger.info("Updating Deployment Spec " + dsId);
        sessionUtil.setUser(sessionUtil.getUsername(headers));
        apiUtil.setIdAndParentIdOrThrow(dsDto, dsId, vsId, "Deployment Spec");
        return apiUtil.getResponseForBaseRequest(new UpdateDeploymentSpecService(),
                new BaseRequest<>(dsDto));
    }

    @ApiOperation(value = "Deletes a Deployment Specification (Openstack Only)",
            notes = "Deletes a Deployment Specification",
            response = BaseJobResponse.class)
    @ApiResponses(value = { @ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 400, message = "In case of any error", response = ErrorCodeDto.class) })
    @Path("/{vsId}/deploymentSpecs/{dsId}")
    @DELETE
    public Response deleteDeploymentSpec(@Context HttpHeaders headers,
                                         @ApiParam(value = "The Virtual System Id") @PathParam("vsId") Long vsId,
                                         @ApiParam(value = "The Deployment Specification Id") @PathParam("dsId") Long dsId) {
        logger.info("Deleting Deployment Spec " + dsId);
        sessionUtil.setUser(sessionUtil.getUsername(headers));
        return apiUtil.getResponseForBaseRequest(new DeleteDeploymentSpecService(),
                new BaseDeleteRequest(dsId, vsId, false));// false as this is not force delete
    }

    @ApiOperation(value = "Force Delete a Deployment Specification (Openstack Only)",
            notes = "Deletes a Deployment Specification.<br/>"
                    + "Warning: Force delete just deletes the entity from OSC, please make sure to clean the related entities outside of OSC.")
    @ApiResponses(value = { @ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 400, message = "In case of any error", response = ErrorCodeDto.class) })
    @Path("/{vsId}/deploymentSpecs/{dsId}/force")
    @DELETE
    public Response forceDeleteDeploymentSpec(@Context HttpHeaders headers,
                                              @ApiParam(value = "The Virtual System Id") @PathParam("vsId") Long vsId,
                                              @ApiParam(value = "The Deployment Specification Id") @PathParam("dsId") Long dsId) {
        logger.info("Deleting Deployment Spec " + dsId);
        sessionUtil.setUser(sessionUtil.getUsername(headers));
        return apiUtil.getResponseForBaseRequest(new DeleteDeploymentSpecService(),
                new BaseDeleteRequest(dsId, vsId, true));
    }

    // SGI APIS
    @ApiOperation(value = "Lists Traffic Policy Mappings",
            notes = "Lists the Traffic Policy Mappings owned by the Virtual System",
            response = SecurityGroupInterfaceDto.class,
            responseContainer = "Set")
    @ApiResponses(value = { @ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 400, message = "In case of any error", response = ErrorCodeDto.class) })
    @Path("/{vsId}/securityGroupInterfaces")
    @GET
    public List<SecurityGroupInterfaceDto> getSecurityGroupInterfacesByVirtualSystem(
            @Context HttpHeaders headers, @ApiParam(value = "The Virtual System Id") @PathParam("vsId") Long vsId) {
        logger.info("Listing Traffic Policy Mappings");
        sessionUtil.setUser(sessionUtil.getUsername(headers));
        @SuppressWarnings("unchecked")
        ListResponse<SecurityGroupInterfaceDto> response = (ListResponse<SecurityGroupInterfaceDto>) apiUtil
                .getListResponse(new ListSecurityGroupInterfaceServiceByVirtualSystem(), new BaseIdRequest(vsId));
        return response.getList();
    }

    @ApiOperation(value = "Retrieves the Traffic Policy Mapping",
            notes = "Retrieves a Traffic Policy Mappings specified by its owning Virtual System and Traffic Policy Mapping Id",
            response = SecurityGroupInterfaceDto.class)
    @ApiResponses(value = { @ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 400, message = "In case of any error", response = ErrorCodeDto.class) })
    @Path("/{vsId}/securityGroupInterfaces/{sgiId}")
    @GET
    public SecurityGroupInterfaceDto getSecurityGroupInterface(@Context HttpHeaders headers,
                                                               @ApiParam(value = "The Virtual System Id") @PathParam("vsId") Long vsId,
                                                               @ApiParam(value = "The Traffic Policy Mapping Id") @PathParam("sgiId") Long sgiId) {
        logger.info("Getting Security Group Interface " + sgiId);
        sessionUtil.setUser(sessionUtil.getUsername(headers));
        GetDtoFromEntityRequest getDtoRequest = new GetDtoFromEntityRequest();
        getDtoRequest.setEntityId(sgiId);
        getDtoRequest.setEntityName("SecurityGroupInterface");
        GetDtoFromEntityService<SecurityGroupInterfaceDto> getDtoService = new GetDtoFromEntityService<>();
        SecurityGroupInterfaceDto dto = apiUtil.submitBaseRequestToService(getDtoService, getDtoRequest).getDto();

        apiUtil.validateParentIdMatches(dto, vsId, "SecurityGroupInterface");

        return dto;
    }

    @ApiOperation(value = "Creates a Traffic Policy Mapping (Openstack Only)",
            notes = "Creates a Traffic Policy Mapping owned by Virtual System provided",
            response = BaseJobResponse.class)
    @ApiResponses(value = { @ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 400, message = "In case of any error", response = ErrorCodeDto.class) })
    @Path("/{vsId}/securityGroupInterfaces")
    @POST
    public Response createSecutiryGroupInterface(@Context HttpHeaders headers,
                                                 @ApiParam(value = "The Virtual System Id") @PathParam("vsId") Long vsId,
                                                 @ApiParam(required = true) SecurityGroupInterfaceDto sgiDto) {
        logger.info("Creating Security Group Interface ...");
        sessionUtil.setUser(sessionUtil.getUsername(headers));
        apiUtil.setParentIdOrThrow(sgiDto, vsId, "Traffic Policy Mapping");
        return apiUtil.getResponseForBaseRequest(new AddSecurityGroupInterfaceService(),
                new BaseRequest<>(sgiDto));
    }

    @ApiOperation(value = "Updates a Traffic Policy Mapping (Openstack Only)",
            notes = "Updates a Traffic Policy Mapping Object",
            response = BaseJobResponse.class)
    @ApiResponses(value = { @ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 400, message = "In case of any error", response = ErrorCodeDto.class) })
    @Path("/{vsId}/securityGroupInterfaces/{sgiId}")
    @PUT
    public Response updateSecurityGroupInterface(@Context HttpHeaders headers,
                                                 @ApiParam(value = "The Virtual System Id") @PathParam("vsId") Long vsId,
                                                 @ApiParam(value = "The Traffic Policy Mapping Id") @PathParam("sgiId") Long sgiId,
                                                 @ApiParam(required = true) SecurityGroupInterfaceDto sgiDto) {
        logger.info("Updating Security Group Interface " + sgiId);
        sessionUtil.setUser(sessionUtil.getUsername(headers));
        apiUtil.setIdAndParentIdOrThrow(sgiDto, sgiId, vsId, "Traffic Policy Mapping");
        return apiUtil.getResponseForBaseRequest(new UpdateSecurityGroupInterfaceService(),
                new BaseRequest<SecurityGroupInterfaceDto>(sgiDto));
    }

    @ApiOperation(value = "Deletes a Traffic Policy Mapping (Openstack Only)",
            notes = "Deletes a Traffic Policy Mapping, Trigger Sync Job and return Job Id",
            response = BaseJobResponse.class)
    @ApiResponses(value = { @ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 400, message = "In case of any error", response = ErrorCodeDto.class) })
    @Path("/{vsId}/securityGroupInterfaces/{sgiId}")
    @DELETE
    public Response deleteSecurityGroupInterface(@Context HttpHeaders headers,
                                                 @ApiParam(value = "The Virtual System Id") @PathParam("vsId") Long vsId,
                                                 @ApiParam(value = "The Traffic Policy Mapping Id") @PathParam("sgiId") Long sgiId) {
        logger.info("Deleting Security Group Interface.. " + sgiId);
        sessionUtil.setUser(sessionUtil.getUsername(headers));
        return apiUtil.getResponseForBaseRequest(new DeleteSecurityGroupInterfaceService(),
                new BaseIdRequest(sgiId, vsId));
    }

    @ApiOperation(value = "Force Delete a Virtual System",
            notes = "Force Deletes a Virtual System provided.<br/>"
                    + "Warning: Force delete just deletes the entity from OSC, please make sure to clean the related entities outside of OSC.",
            response = BaseJobResponse.class)
    @ApiResponses(value = { @ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 400, message = "In case of any error", response = ErrorCodeDto.class) })
    @Path("/{vsId}/force")
    @DELETE
    public Response forceDeleteVirtualSystem(@Context HttpHeaders headers,
                                             @ApiParam(value = "The Virtual System Id") @PathParam("vsId") Long vsId) {
        logger.info("Deleting Virtual System " + vsId);
        sessionUtil.setUser(sessionUtil.getUsername(headers));
        return apiUtil.getResponseForBaseRequest(new ForceDeleteVirtualSystemService(),
                new BaseDeleteRequest(vsId, true));
    }
}
