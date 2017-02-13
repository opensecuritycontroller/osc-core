package org.osc.core.broker.rest.server.api;

import java.util.List;
import java.util.Set;

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
import org.osc.core.broker.service.dto.VirtualizationConnectorDto;
import org.osc.core.broker.service.request.BaseDeleteRequest;
import org.osc.core.broker.service.request.BaseIdRequest;
import org.osc.core.broker.service.request.BaseRequest;
import org.osc.core.broker.service.request.DryRunRequest;
import org.osc.core.broker.service.request.GetDtoFromEntityRequest;
import org.osc.core.broker.service.response.BaseJobResponse;
import org.osc.core.broker.service.response.BaseResponse;
import org.osc.core.broker.service.response.ListResponse;
import org.osc.core.broker.service.response.SetResponse;
import org.osc.core.broker.service.securitygroup.AddOrUpdateSecurityGroupRequest;
import org.osc.core.broker.service.securitygroup.AddSecurityGroupService;
import org.osc.core.broker.service.securitygroup.BindSecurityGroupRequest;
import org.osc.core.broker.service.securitygroup.BindSecurityGroupService;
import org.osc.core.broker.service.securitygroup.DeleteSecurityGroupService;
import org.osc.core.broker.service.securitygroup.ListSecurityGroupBindingsBySgService;
import org.osc.core.broker.service.securitygroup.ListSecurityGroupByVcService;
import org.osc.core.broker.service.securitygroup.ListSecurityGroupMembersBySgService;
import org.osc.core.broker.service.securitygroup.SecurityGroupDto;
import org.osc.core.broker.service.securitygroup.SecurityGroupMemberItemDto;
import org.osc.core.broker.service.securitygroup.SyncSecurityGroupService;
import org.osc.core.broker.service.securitygroup.UpdateSecurityGroupMemberRequest;
import org.osc.core.broker.service.securitygroup.UpdateSecurityGroupPropertiesService;
import org.osc.core.broker.service.securitygroup.UpdateSecurityGroupService;
import org.osc.core.broker.service.securitygroup.VirtualSystemPolicyBindingDto;
import org.osc.core.broker.service.vc.AddVirtualizationConnectorService;
import org.osc.core.broker.service.vc.DeleteVirtualizationConnectorService;
import org.osc.core.broker.service.vc.ListVirtualizationConnectorService;
import org.osc.core.broker.service.vc.UpdateVirtualizationConnectorService;
import org.osc.core.broker.service.vc.VirtualizationConnectorRequest;
import org.osc.core.broker.util.SessionUtil;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.Authorization;

@Api(tags = "Operations for Virtualization Connectors", authorizations = { @Authorization(value = "Basic Auth") })
@Path(OscRestServlet.SERVER_API_PATH_PREFIX + "/virtualizationConnectors")
@Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
@OscAuth
public class VirtualizationConnectorApis {

    private static final Logger logger = Logger.getLogger(VirtualizationConnectorApis.class);

    @ApiOperation(value = "Lists All Virtualization Connectors",
            notes = "Password information is not returned as it is sensitive information",
            response = VirtualizationConnectorDto.class,
            responseContainer = "Set")
    @ApiResponses(value = { @ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 400, message = "In case of any error", response = ErrorCodeDto.class) })
    @GET
    public List<VirtualizationConnectorDto> getVirtualizationConnectors(@Context HttpHeaders headers) {

        logger.info("Listing Virtualization Connectors");
        SessionUtil.setUser(SessionUtil.getUsername(headers));

        @SuppressWarnings("unchecked")
        ListResponse<VirtualizationConnectorDto> response = (ListResponse<VirtualizationConnectorDto>) ApiUtil
                .getListResponse(new ListVirtualizationConnectorService(), new BaseRequest<>(true));

        return response.getList();
    }

    @ApiOperation(value = "Retrieves the Virtualization Connector by Id",
            notes = "Password information is not returned as it is sensitive information",
            response = VirtualizationConnectorDto.class)
    @ApiResponses(value = { @ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 400, message = "In case of any error", response = ErrorCodeDto.class) })
    @Path("/{vcId}")
    @GET
    public VirtualizationConnectorDto getVirtualizationConnector(@Context HttpHeaders headers,
                                                                 @ApiParam(value = "The Virtualization Connector Id") @PathParam("vcId") Long vcId) {

        logger.info("getting Virtualization Connector " + vcId);
        SessionUtil.setUser(SessionUtil.getUsername(headers));

        GetDtoFromEntityRequest getDtoRequest = new GetDtoFromEntityRequest();
        getDtoRequest.setEntityId(vcId);
        getDtoRequest.setEntityName("VirtualizationConnector");

        return ApiUtil
                .submitBaseRequestToService(new GetDtoFromEntityService<VirtualizationConnectorDto>(), getDtoRequest)
                .getDto();
    }

    /**
     * Creates a Virtualization connector
     *
     * @param vcRequest
     * @return
     */
    // Virtualization Connector APIS
    @ApiOperation(value = "Creates a Virtualization Connector",
            notes = "Creates a Virtualization Connector<br/>"
                    + "If we are unable to connect to the endpoint using the credentials provided, this call will fail.<br/>"
                    + "To skip validation of IP and credentials 'skipRemoteValidation' flag can be used.",
            response = BaseResponse.class)
    @ApiResponses(value = { @ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 400, message = "In case of any error", response = ErrorCodeDto.class) })
    @POST
    public Response createVirtualizationConnector(@Context HttpHeaders headers,
                                                  @ApiParam(required = true) VirtualizationConnectorRequest vcRequest) {

        logger.info("Creating Virtualization Connector...");
        SessionUtil.setUser(SessionUtil.getUsername(headers));
        return ApiUtil.getResponseForBaseRequest(new AddVirtualizationConnectorService(),
                new DryRunRequest<VirtualizationConnectorDto>(vcRequest, vcRequest.isSkipRemoteValidation()));
    }

    /**
     * Updates a Virtualization connector
     *
     * @return the Id of the updated virtualization connector
     */
    @ApiOperation(value = "Updates a Virtualization Connector.",
            notes = "Updates a Virtualization Connector.<br/>"
                    + "If we are unable to connect to the endpoint using the credentials provided, this call will fail.<br/>"
                    + "To skip validation of IP and credentials 'skipRemoteValidation' flag can be used.<br/>"
                    + "Controller type changes from NONE->new-type is allowed unconditionally. "
                    + "For all other cases (current-type->NONE, current-type->new-type), there should not be any"
                    + "virtual systems using it.<br/> Password information is Optional for update requests as OSC will use "
                    + "the current password information.",
            response = BaseResponse.class)
    @ApiResponses(value = { @ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 400, message = "In case of any error", response = ErrorCodeDto.class) })
    @Path("/{vcId}")
    @PUT
    public Response updateVirtualizationConnector(@Context HttpHeaders headers,
                                                  @ApiParam(value = "The Virtualization Connector Id") @PathParam("vcId") Long vcId,
                                                  @ApiParam(required = true) VirtualizationConnectorRequest vcRequest) {

        logger.info("Updating Virtualization Connector " + vcId);
        SessionUtil.setUser(SessionUtil.getUsername(headers));
        ApiUtil.setIdOrThrow(vcRequest, vcId, "Virtualization Connector");
        return ApiUtil.getResponseForBaseRequest(new UpdateVirtualizationConnectorService(),
                new DryRunRequest<VirtualizationConnectorDto>(vcRequest, vcRequest.isSkipRemoteValidation()));
    }

    /**
     * Delete a Virtualization connector
     *
     * @param vcId
     * @return
     */
    @ApiOperation(value = "Deletes a Virtualization Connector",
            notes = "Deletes a Virtualization Connector if not referenced by any Virtual Systems")
    @ApiResponses(value = { @ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 400, message = "In case of any error", response = ErrorCodeDto.class) })
    @Path("/{vcId}")
    @DELETE
    public Response deleteVirtualizationConnector(@Context HttpHeaders headers,
                                                  @ApiParam(value = "The Virtualization Connector Id") @PathParam("vcId") Long vcId) {

        logger.info("Deleting Virtualization Connector " + vcId);
        SessionUtil.setUser(SessionUtil.getUsername(headers));

        return ApiUtil.getResponseForBaseRequest(new DeleteVirtualizationConnectorService(), new BaseIdRequest(vcId));
    }

    // Security Group APIs
    @ApiOperation(value = "Lists Security Groups",
            notes = "Lists Security Groups owned by the Virtualization Connector",
            response = SecurityGroupDto.class,
            responseContainer = "Set")
    @ApiResponses(value = { @ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 400, message = "In case of any error", response = ErrorCodeDto.class) })
    @Path("/{vcId}/securityGroups")
    @GET
    public List<SecurityGroupDto> getSecurityGroupByVirtualiazationConnector(@Context HttpHeaders headers,
                                                                             @ApiParam(value = "The Virtualization Connector Id") @PathParam("vcId") Long vcId) {
        logger.info("Listing Security groups");
        SessionUtil.setUser(SessionUtil.getUsername(headers));
        @SuppressWarnings("unchecked")
        ListResponse<SecurityGroupDto> response = (ListResponse<SecurityGroupDto>) ApiUtil
                .getListResponse(new ListSecurityGroupByVcService(), new BaseIdRequest(vcId));
        return response.getList();
    }

    @ApiOperation(value = "Retrieves a Security Group",
            notes = "Retrieves the Security Group owned by Virtualization Connector provided and by the specified Security Group Id",
            response = SecurityGroupDto.class)
    @ApiResponses(value = { @ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 400, message = "In case of any error", response = ErrorCodeDto.class) })
    @Path("/{vcId}/securityGroups/{sgId}")
    @GET
    public SecurityGroupDto getSecurityGroup(@Context HttpHeaders headers,
                                             @ApiParam(value = "The Virtualization Connector Id") @PathParam("vcId") Long vcId,
                                             @ApiParam(value = "The Security Group Id") @PathParam("sgId") Long sgId) {
        logger.info("getting Security Group " + sgId);
        SessionUtil.setUser(SessionUtil.getUsername(headers));
        GetDtoFromEntityRequest getDtoRequest = new GetDtoFromEntityRequest();
        getDtoRequest.setEntityId(sgId);
        getDtoRequest.setEntityName("SecurityGroup");
        GetDtoFromEntityService<SecurityGroupDto> getDtoService = new GetDtoFromEntityService<SecurityGroupDto>();
        SecurityGroupDto dto = ApiUtil.submitBaseRequestToService(getDtoService, getDtoRequest).getDto();

        ApiUtil.validateParentIdMatches(dto, vcId, "SecurityGroup");

        return dto;
    }

    @ApiOperation(value = "Creates a Security Group (Openstack Only)",
            notes = "Creates a Security Group owned by Virtualization Connector provided and kicks off a " + "sync job",
            response = BaseJobResponse.class)
    @ApiResponses(value = { @ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 400, message = "In case of any error", response = ErrorCodeDto.class) })
    @Path("/{vcId}/securityGroups")
    @POST
    public Response createSecurityGroup(@Context HttpHeaders headers,
                                        @ApiParam(value = "The Virtualization Connector Id") @PathParam("vcId") Long vcId,
                                        @ApiParam(required = true) SecurityGroupDto sgDto) {
        logger.info("Creating Security Group ...");
        SessionUtil.setUser(SessionUtil.getUsername(headers));
        ApiUtil.setIdAndParentIdOrThrow(sgDto, null, vcId, "Security Group");
        AddOrUpdateSecurityGroupRequest request = new AddOrUpdateSecurityGroupRequest();
        request.setDto(sgDto);
        return ApiUtil.getResponseForBaseRequest(new AddSecurityGroupService(), request);
    }

    @ApiOperation(value = "Updates a Security Group (Openstack Only)",
            notes = "Updates the Security Group owned by Virtualization Connector provided and kicks off a sync job",
            response = BaseJobResponse.class)
    @ApiResponses(value = { @ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 400, message = "In case of any error", response = ErrorCodeDto.class) })
    @Path("/{vcId}/securityGroups/{sgId}")
    @PUT
    public Response updateSecurityGroup(@Context HttpHeaders headers,
                                        @ApiParam(value = "The Virtualization Connector Id") @PathParam("vcId") Long vcId,
                                        @ApiParam(value = "The Security Group Id") @PathParam("sgId") Long sgId,
                                        @ApiParam(required = true) SecurityGroupDto sgDto) {
        logger.info("Updating Security Group " + sgId);
        SessionUtil.setUser(SessionUtil.getUsername(headers));
        ApiUtil.setIdAndParentIdOrThrow(sgDto, sgId, vcId, "Security Group");
        AddOrUpdateSecurityGroupRequest request = new AddOrUpdateSecurityGroupRequest();
        request.setDto(sgDto);
        return ApiUtil.getResponseForBaseRequest(new UpdateSecurityGroupPropertiesService(), request);
    }

    @ApiOperation(value = "Deletes a Security Group (Openstack Only)",
            notes = "Deletes the Security Group owned by Virtualization Connector provided and kicks off a sync job",
            response = BaseJobResponse.class)
    @ApiResponses(value = { @ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 400, message = "In case of any error", response = ErrorCodeDto.class) })
    @Path("/{vcId}/securityGroups/{sgId}")
    @DELETE
    public Response deleteSecurityGroup(@Context HttpHeaders headers,
                                        @ApiParam(value = "The Virtualization Connector Id") @PathParam("vcId") Long vcId,
                                        @ApiParam(value = "The Security Group Id") @PathParam("sgId") Long sgId) {
        logger.info("Deleting Security Group.. " + sgId);
        SessionUtil.setUser(SessionUtil.getUsername(headers));
        return ApiUtil.getResponseForBaseRequest(new DeleteSecurityGroupService(),
                new BaseDeleteRequest(sgId, vcId, false)); // false as this is not force delete
    }

    @ApiOperation(value = "Force Delete a Security Group (Openstack Only)",
            notes = "Force Deletes a Security Group owned by Virtualization Connector provided and kicks off a sync job.<br/>"
                    + "Warning: Force delete just deletes the entity from OSC, please make sure to clean the related entities outside of OSC.",
            response = BaseJobResponse.class)
    @ApiResponses(value = { @ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 400, message = "In case of any error", response = ErrorCodeDto.class) })
    @Path("/{vcId}/securityGroups/{sgId}/force")
    @DELETE
    public Response forceDeleteSecurityGroup(@Context HttpHeaders headers,
                                             @ApiParam(value = "The Virtualization Connector Id") @PathParam("vcId") Long vcId,
                                             @ApiParam(value = "The Security Group Id") @PathParam("sgId") Long sgId) {
        logger.info("Deleting Security Group.. " + sgId);
        SessionUtil.setUser(SessionUtil.getUsername(headers));
        return ApiUtil.getResponseForBaseRequest(new DeleteSecurityGroupService(),
                new BaseDeleteRequest(sgId, vcId, true));
    }

    @ApiOperation(value = "Sync a Security Group (Openstack Only)",
            notes = "Sync a Security Group Object",
            response = BaseJobResponse.class)
    @ApiResponses(value = { @ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 400, message = "In case of any error", response = ErrorCodeDto.class) })
    @Path("/{vcId}/securityGroups/{sgId}/sync")
    @PUT
    public Response syncSecurityGroup(@Context HttpHeaders headers,
                                      @ApiParam(value = "The Virtualization Connector Id") @PathParam("vcId") Long vcId,
                                      @ApiParam(value = "The Security Group Id") @PathParam("sgId") Long sgId) {
        logger.info("Sync Security Group" + sgId);
        SessionUtil.setUser(SessionUtil.getUsername(headers));
        return ApiUtil.getResponseForBaseRequest(new SyncSecurityGroupService(), new BaseIdRequest(sgId, vcId));
    }

    // Security Group member APIs
    @ApiOperation(value = "Lists Security Group Members",
            notes = "Lists Security Group Member owned by Security Group and Virtualization Connector provided.",
            response = SecurityGroupMemberItemDto.class,
            responseContainer = "Set")
    @ApiResponses(value = { @ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 400, message = "In case of any error", response = ErrorCodeDto.class) })
    @Path("/{vcId}/securityGroups/{sgId}/members")
    @GET
    public Set<SecurityGroupMemberItemDto> getSecurityGroupMembers(@Context HttpHeaders headers,
                                                                   @ApiParam(value = "The Virtualization Connector Id") @PathParam("vcId") Long vcId,
                                                                   @ApiParam(value = "The Security Group Id") @PathParam("sgId") Long sgId) {
        logger.info("Listing Members for Security Group - " + sgId);
        SessionUtil.setUser(SessionUtil.getUsername(headers));
        GetDtoFromEntityRequest getDtoRequest = new GetDtoFromEntityRequest();
        getDtoRequest.setEntityId(sgId);
        getDtoRequest.setEntityName("SecurityGroup");
        GetDtoFromEntityService<SecurityGroupDto> getDtoService = new GetDtoFromEntityService<SecurityGroupDto>();
        SecurityGroupDto dto = ApiUtil.submitBaseRequestToService(getDtoService, getDtoRequest).getDto();

        ApiUtil.validateParentIdMatches(dto, vcId, "SecurityGroup");

        @SuppressWarnings("unchecked")
        SetResponse<SecurityGroupMemberItemDto> memberList = (SetResponse<SecurityGroupMemberItemDto>) ApiUtil
                .getSetResponse(new ListSecurityGroupMembersBySgService(), new BaseIdRequest(sgId));

        return memberList.getSet();
    }

    @ApiOperation(value = "Updates the Security Group Members (Openstack Only)",
            notes = "Updates the member list in a Security Group.",
            response = BaseJobResponse.class)
    @ApiResponses(value = { @ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 400, message = "In case of any error", response = ErrorCodeDto.class) })
    @Path("/{vcId}/securityGroups/{sgId}/members")
    @PUT
    public Response updateSecurityGroupMembers(@Context HttpHeaders headers,
                                               @ApiParam(value = "The Virtualization Connector Id") @PathParam("vcId") Long vcId,
                                               @ApiParam(value = "The Security Group Id") @PathParam("sgId") Long sgId,
                                               @ApiParam(required = true) UpdateSecurityGroupMemberRequest sgUpdateRequest) {
        logger.info("Updating Security Group " + sgId);
        SessionUtil.setUser(SessionUtil.getUsername(headers));

        if (!sgId.equals(sgUpdateRequest.getId())) {
            throw ApiUtil.createIdMismatchException(sgUpdateRequest.getId(), "Security Group");
        } else if (!vcId.equals(sgUpdateRequest.getParentId())) {
            throw ApiUtil.createParentChildMismatchException(sgUpdateRequest.getParentId(), "Security Group");
        }
        AddOrUpdateSecurityGroupRequest request = new AddOrUpdateSecurityGroupRequest();
        request.setMembers(sgUpdateRequest.getMembers());
        request.setDto(new SecurityGroupDto());
        request.getDto().setId(sgId);
        request.getDto().setParentId(vcId);

        return ApiUtil.getResponseForBaseRequest(new UpdateSecurityGroupService(), request);
    }

    // SG Interface APIS
    @ApiOperation(value = "Retrieves the Security Group Bindings",
            notes = "Retrieves the all available Security Group Bindings to Security Function Service(Distributed Appliance).<br/>"
                    + "The isBinded flag indicates whether the binding is active.",
            response = VirtualSystemPolicyBindingDto.class,
            responseContainer = "Set")
    @ApiResponses(value = { @ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 400, message = "In case of any error", response = ErrorCodeDto.class) })
    @Path("/{vcId}/securityGroups/{sgId}/bindings")
    @GET
    public List<VirtualSystemPolicyBindingDto> getVirtualSecurityPolicyBindings(@Context HttpHeaders headers,
                                                                                @ApiParam(value = "The Virtualization Connector Id") @PathParam("vcId") Long vcId,
                                                                                @ApiParam(value = "The Security Group Id") @PathParam("sgId") Long sgId) {
        logger.info("Listing Bindings for Security Group - " + sgId);
        SessionUtil.setUser(SessionUtil.getUsername(headers));

        GetDtoFromEntityRequest getDtoRequest = new GetDtoFromEntityRequest();
        getDtoRequest.setEntityId(sgId);
        getDtoRequest.setEntityName("SecurityGroup");
        SecurityGroupDto dto = ApiUtil
                .submitBaseRequestToService(new GetDtoFromEntityService<SecurityGroupDto>(), getDtoRequest).getDto();

        ApiUtil.validateParentIdMatches(dto, vcId, "SecurityGroup");

        @SuppressWarnings("unchecked")
        ListResponse<VirtualSystemPolicyBindingDto> memberList = (ListResponse<VirtualSystemPolicyBindingDto>) ApiUtil
                .getListResponse(new ListSecurityGroupBindingsBySgService(), new BaseIdRequest(sgId));

        return memberList.getList();
    }

    @ApiOperation(value = "Set Security Group Bindings (Openstack Only)",
            notes = "Adds/Update/Remove Security Group Bindings to Security Function Services.<br/>"
                    + "To Remove all services, pass in empty json.<br/>"
                    + "To update services binded to, pass in the updated list of services.<br/>",
            response = BaseJobResponse.class)
    @ApiResponses(value = { @ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 400, message = "In case of any error", response = ErrorCodeDto.class) })
    @Path("/{vcId}/securityGroups/{sgId}/bindings")
    @PUT
    public Response updateVirtualSecurityPolicyBindings(@Context HttpHeaders headers,
                                                        @ApiParam(value = "The Virtualization Connector Id") @PathParam("vcId") Long vcId,
                                                        @ApiParam(value = "The Security Group Id") @PathParam("sgId") Long sgId,
                                                        @ApiParam(value = "List of Bindings", required = true) Set<VirtualSystemPolicyBindingDto> bindings) {
        logger.info("Update Bindings for Security Group - " + sgId);
        SessionUtil.setUser(SessionUtil.getUsername(headers));
        BindSecurityGroupService bindService = new BindSecurityGroupService();
        BindSecurityGroupRequest bindRequest = new BindSecurityGroupRequest();
        bindRequest.setVcId(vcId);
        bindRequest.setSecurityGroupId(sgId);
        for (VirtualSystemPolicyBindingDto vsBinding : bindings) {
            bindRequest.addServiceToBindTo(vsBinding);
        }
        return ApiUtil.getResponseForBaseRequest(bindService, bindRequest);
    }

}
