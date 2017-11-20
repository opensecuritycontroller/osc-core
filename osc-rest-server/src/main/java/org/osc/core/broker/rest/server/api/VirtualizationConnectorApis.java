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

import org.osc.core.broker.rest.server.ApiUtil;
import org.osc.core.broker.rest.server.OscAuthFilter;
import org.osc.core.broker.rest.server.ServerRestConstants;
import org.osc.core.broker.rest.server.annotations.OscAuth;
import org.osc.core.broker.service.api.AddSecurityGroupServiceApi;
import org.osc.core.broker.service.api.AddServiceFunctionChainServiceApi;
import org.osc.core.broker.service.api.AddVirtualizationConnectorServiceApi;
import org.osc.core.broker.service.api.BindSecurityGroupServiceApi;
import org.osc.core.broker.service.api.DeleteSecurityGroupServiceApi;
import org.osc.core.broker.service.api.DeleteServiceFunctionChainServiceApi;
import org.osc.core.broker.service.api.GetDtoFromEntityServiceApi;
import org.osc.core.broker.service.api.GetDtoFromEntityServiceFactoryApi;
import org.osc.core.broker.service.api.ListSecurityGroupBindingsBySgServiceApi;
import org.osc.core.broker.service.api.ListSecurityGroupByVcServiceApi;
import org.osc.core.broker.service.api.ListSecurityGroupMembersBySgServiceApi;
import org.osc.core.broker.service.api.ListServiceFunctionChainByVcServiceApi;
import org.osc.core.broker.service.api.ListVirtualizationConnectorServiceApi;
import org.osc.core.broker.service.api.SyncSecurityGroupServiceApi;
import org.osc.core.broker.service.api.UpdateSecurityGroupPropertiesServiceApi;
import org.osc.core.broker.service.api.UpdateSecurityGroupServiceApi;
import org.osc.core.broker.service.api.UpdateServiceFunctionChainServiceApi;
import org.osc.core.broker.service.api.UpdateVirtualizationConnectorServiceApi;
import org.osc.core.broker.service.api.server.UserContextApi;
import org.osc.core.broker.service.api.vc.DeleteVirtualizationConnectorServiceApi;
import org.osc.core.broker.service.dto.BaseDto;
import org.osc.core.broker.service.dto.BaseVirtualSystemPoliciesDto;
import org.osc.core.broker.service.dto.SecurityGroupDto;
import org.osc.core.broker.service.dto.SecurityGroupMemberItemDto;
import org.osc.core.broker.service.dto.ServiceFunctionChainDto;
import org.osc.core.broker.service.dto.VirtualSystemPolicyBindingDto;
import org.osc.core.broker.service.dto.VirtualizationConnectorDto;
import org.osc.core.broker.service.exceptions.ErrorCodeDto;
import org.osc.core.broker.service.request.AddOrUpdateSecurityGroupRequest;
import org.osc.core.broker.service.request.AddOrUpdateServiceFunctionChainRequest;
import org.osc.core.broker.service.request.BaseDeleteRequest;
import org.osc.core.broker.service.request.BaseIdRequest;
import org.osc.core.broker.service.request.BaseRequest;
import org.osc.core.broker.service.request.BindSecurityGroupRequest;
import org.osc.core.broker.service.request.DryRunRequest;
import org.osc.core.broker.service.request.GetDtoFromEntityRequest;
import org.osc.core.broker.service.request.UpdateSecurityGroupMemberRequest;
import org.osc.core.broker.service.request.VirtualizationConnectorRequest;
import org.osc.core.broker.service.response.BaseJobResponse;
import org.osc.core.broker.service.response.BaseResponse;
import org.osc.core.broker.service.response.BindSecurityGroupResponse;
import org.osc.core.broker.service.response.ListResponse;
import org.osc.core.broker.service.response.SetResponse;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.Authorization;

@Component(service = VirtualizationConnectorApis.class)
@Api(tags = "Operations for Virtualization Connectors", authorizations = { @Authorization(value = "Basic Auth") })
@Path(ServerRestConstants.SERVER_API_PATH_PREFIX + "/virtualizationConnectors")
@Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
@OscAuth
public class VirtualizationConnectorApis {

    private static final Logger logger = LoggerFactory.getLogger(VirtualizationConnectorApis.class);

    @Reference
    private ApiUtil apiUtil;

    @Reference
    private UpdateVirtualizationConnectorServiceApi updateVirtualizationConnectorService;

    @Reference
    private AddVirtualizationConnectorServiceApi addVirtualizationConnectorService;

    @Reference
    private AddSecurityGroupServiceApi addSecurityGroupService;

    @Reference
    private UpdateSecurityGroupServiceApi updateSecurityGroupService;

    @Reference
    private UpdateSecurityGroupPropertiesServiceApi updateSecurityGroupPropertiesService;

    @Reference
    private DeleteSecurityGroupServiceApi deleteSecurityGroupService;

    @Reference
    private ListSecurityGroupBindingsBySgServiceApi listSecurityGroupBindingsBySgService;

    @Reference
    private BindSecurityGroupServiceApi bindSecurityGroupService;

    @Reference
    private ListSecurityGroupByVcServiceApi listSecurityGroupByVcService;

    @Reference
    private ListSecurityGroupMembersBySgServiceApi listSecurityGroupMembersBySgService;

    @Reference
    private SyncSecurityGroupServiceApi syncSecurityGroupService;

    @Reference
    private DeleteVirtualizationConnectorServiceApi deleteVirtualizationConnectorService;

    @Reference
    private ListVirtualizationConnectorServiceApi listVirtualizationConnectorService;

    @Reference
    private GetDtoFromEntityServiceFactoryApi getDtoFromEntityServiceFactory;

    @Reference
    private AddServiceFunctionChainServiceApi addServiceFunctionChainService;

    @Reference
    private ListServiceFunctionChainByVcServiceApi listServiceFunctionChainByVcService;

    @Reference
    DeleteServiceFunctionChainServiceApi  deleteServiceFunctionChainService;

    @Reference
    UpdateServiceFunctionChainServiceApi  updateServiceFunctionChainService;

    @Reference
    private UserContextApi userContext;

    @ApiOperation(value = "Lists All Virtualization Connectors",
            notes = "Password information is not returned as it is sensitive information",
            response = VirtualizationConnectorDto.class,
            responseContainer = "Set")
    @ApiResponses(value = { @ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 400, message = "In case of any error", response = ErrorCodeDto.class) })
    @GET
    public List<VirtualizationConnectorDto> getVirtualizationConnectors(@Context HttpHeaders headers) {

        logger.info("Listing Virtualization Connectors");
        this.userContext.setUser(OscAuthFilter.getUsername(headers));

        @SuppressWarnings("unchecked")
        ListResponse<VirtualizationConnectorDto> response = (ListResponse<VirtualizationConnectorDto>) this.apiUtil
        .getListResponse(this.listVirtualizationConnectorService, new BaseRequest<>(true));

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
        this.userContext.setUser(OscAuthFilter.getUsername(headers));

        GetDtoFromEntityRequest getDtoRequest = new GetDtoFromEntityRequest();
        getDtoRequest.setEntityId(vcId);
        getDtoRequest.setEntityName("VirtualizationConnector");

        return this.apiUtil
                .submitBaseRequestToService(this.getDtoFromEntityServiceFactory.getService(VirtualizationConnectorDto.class), getDtoRequest)
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
        this.userContext.setUser(OscAuthFilter.getUsername(headers));
        return this.apiUtil.getResponseForBaseRequest(this.addVirtualizationConnectorService,
                new DryRunRequest<>(vcRequest, vcRequest.isSkipRemoteValidation()));
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
        this.userContext.setUser(OscAuthFilter.getUsername(headers));
        this.apiUtil.setIdOrThrow(vcRequest, vcId, "Virtualization Connector");
        return this.apiUtil.getResponseForBaseRequest(this.updateVirtualizationConnectorService,
                new DryRunRequest<>(vcRequest, vcRequest.isSkipRemoteValidation()));
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
        this.userContext.setUser(OscAuthFilter.getUsername(headers));

        return this.apiUtil.getResponseForBaseRequest(this.deleteVirtualizationConnectorService, new BaseIdRequest(vcId));
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
        this.userContext.setUser(OscAuthFilter.getUsername(headers));
        @SuppressWarnings("unchecked")
        ListResponse<SecurityGroupDto> response = (ListResponse<SecurityGroupDto>) this.apiUtil
        .getListResponse(this.listSecurityGroupByVcService, new BaseIdRequest(vcId));
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
        this.userContext.setUser(OscAuthFilter.getUsername(headers));
        GetDtoFromEntityRequest getDtoRequest = new GetDtoFromEntityRequest();
        getDtoRequest.setEntityId(sgId);
        getDtoRequest.setEntityName("SecurityGroup");
        GetDtoFromEntityServiceApi<SecurityGroupDto> getDtoService = this.getDtoFromEntityServiceFactory.getService(SecurityGroupDto.class);
        SecurityGroupDto dto = this.apiUtil.submitBaseRequestToService(getDtoService, getDtoRequest).getDto();

        this.apiUtil.validateParentIdMatches(dto, vcId, "SecurityGroup");

        return dto;
    }

    @ApiOperation(value = "Creates a Security Group",
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
        this.userContext.setUser(OscAuthFilter.getUsername(headers));
        this.apiUtil.setIdAndParentIdOrThrow(sgDto, null, vcId, "Security Group");
        AddOrUpdateSecurityGroupRequest request = new AddOrUpdateSecurityGroupRequest();
        request.setDto(sgDto);
        return this.apiUtil.getResponseForBaseRequest(this.addSecurityGroupService, request);
    }

    @ApiOperation(value = "Updates a Security Group",
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
        this.userContext.setUser(OscAuthFilter.getUsername(headers));
        this.apiUtil.setIdAndParentIdOrThrow(sgDto, sgId, vcId, "Security Group");
        AddOrUpdateSecurityGroupRequest request = new AddOrUpdateSecurityGroupRequest();
        request.setDto(sgDto);
        return this.apiUtil.getResponseForBaseRequest(this.updateSecurityGroupPropertiesService, request);
    }

    @ApiOperation(value = "Deletes a Security Group",
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
        this.userContext.setUser(OscAuthFilter.getUsername(headers));
        return this.apiUtil.getResponseForBaseRequest(this.deleteSecurityGroupService,
                new BaseDeleteRequest(sgId, vcId, false)); // false as this is not force delete
    }

    @ApiOperation(value = "Force Delete a Security Group",
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
        this.userContext.setUser(OscAuthFilter.getUsername(headers));
        return this.apiUtil.getResponseForBaseRequest(this.deleteSecurityGroupService,
                new BaseDeleteRequest(sgId, vcId, true));
    }

    @ApiOperation(value = "Sync a Security Group",
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
        this.userContext.setUser(OscAuthFilter.getUsername(headers));
        return this.apiUtil.getResponseForBaseRequest(this.syncSecurityGroupService, new BaseIdRequest(sgId, vcId));
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
        this.userContext.setUser(OscAuthFilter.getUsername(headers));
        GetDtoFromEntityRequest getDtoRequest = new GetDtoFromEntityRequest();
        getDtoRequest.setEntityId(sgId);
        getDtoRequest.setEntityName("SecurityGroup");
        GetDtoFromEntityServiceApi<SecurityGroupDto> getDtoService = this.getDtoFromEntityServiceFactory.getService(SecurityGroupDto.class);
        SecurityGroupDto dto = this.apiUtil.submitBaseRequestToService(getDtoService, getDtoRequest).getDto();

        this.apiUtil.validateParentIdMatches(dto, vcId, "SecurityGroup");

        @SuppressWarnings("unchecked")
        SetResponse<SecurityGroupMemberItemDto> memberList = (SetResponse<SecurityGroupMemberItemDto>) this.apiUtil
        .getSetResponse(this.listSecurityGroupMembersBySgService, new BaseIdRequest(sgId));

        return memberList.getSet();
    }

    @ApiOperation(value = "Updates the Security Group Members",
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
        this.userContext.setUser(OscAuthFilter.getUsername(headers));

        if (!sgId.equals(sgUpdateRequest.getId())) {
            throw this.apiUtil.createIdMismatchException(sgUpdateRequest.getId(), "Security Group");
        } else if (!vcId.equals(sgUpdateRequest.getParentId())) {
            throw this.apiUtil.createParentChildMismatchException(sgUpdateRequest.getParentId(), "Security Group");
        }
        AddOrUpdateSecurityGroupRequest request = new AddOrUpdateSecurityGroupRequest();
        request.setMembers(sgUpdateRequest.getMembers());
        request.setDto(new SecurityGroupDto());
        request.getDto().setId(sgId);
        request.getDto().setParentId(vcId);

        return this.apiUtil.getResponseForBaseRequest(this.updateSecurityGroupService, request);
    }

 // SG Interface APIS
    @ApiOperation(value = "Retrieves the Security Group Bindings",
            notes = "Retrieves the all available Security Group Bindings to Security Function Service(Distributed Appliance).<br/>"
                    + "The binded flag indicates whether the binding is active.",
                    response = VirtualSystemPolicyBindingDto.class,
                    responseContainer = "Set")
    @ApiResponses(value = { @ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 400, message = "In case of any error", response = ErrorCodeDto.class) })
    @Path("/{vcId}/securityGroups/{sgId}/bindings")
    @GET
    public BindSecurityGroupResponse getVirtualSecurityPolicyBindings(@Context HttpHeaders headers,
            @ApiParam(value = "The Virtualization Connector Id") @PathParam("vcId") Long vcId,
            @ApiParam(value = "The Security Group Id") @PathParam("sgId") Long sgId) {
        logger.info("Listing Bindings for Security Group - " + sgId);
        this.userContext.setUser(OscAuthFilter.getUsername(headers));

        GetDtoFromEntityRequest getDtoRequest = new GetDtoFromEntityRequest();
        getDtoRequest.setEntityId(sgId);
        getDtoRequest.setEntityName("SecurityGroup");
        SecurityGroupDto dto = this.apiUtil
                .submitBaseRequestToService(this.getDtoFromEntityServiceFactory.getService(SecurityGroupDto.class), getDtoRequest).getDto();

        this.apiUtil.validateParentIdMatches(dto, vcId, "SecurityGroup");

        return this.apiUtil.submitBaseRequestToService(this.listSecurityGroupBindingsBySgService, new BaseIdRequest(sgId));
    }

    @ApiOperation(value = "Set Security Group Bindings",
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
        this.userContext.setUser(OscAuthFilter.getUsername(headers));
        BindSecurityGroupRequest bindRequest = new BindSecurityGroupRequest();
        bindRequest.setVcId(vcId);
        bindRequest.setSecurityGroupId(sgId);
        bindRequest.setBindSfc(false);
        for (VirtualSystemPolicyBindingDto vsBinding : bindings) {
            bindRequest.addServiceToBindTo(vsBinding);
        }
        return this.apiUtil.getResponseForBaseRequest(this.bindSecurityGroupService, bindRequest);
    }

    @ApiOperation(value = "Set Security Group Bindings with Service Function Chain  (Openstack Only)",
            notes = "Adds/Updates Security Group Bindings to Security Function Services.<br/>"
                    + "To Remove all policies, pass in empty json.<br/>"
                    + "To update services binded to, pass in the updated list of services.<br/>",
            response = BaseJobResponse.class)
    @ApiResponses(value = { @ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 400, message = "In case of any error", response = ErrorCodeDto.class) })
    @Path("/{vcId}/securityGroups/{sgId}/sfc/{sfcId}/bindings")
    @PUT
    public Response updateVirtualSecurityPolicyBindingsWithSfc(@Context HttpHeaders headers,
                                                        @ApiParam(value = "The Virtualization Connector Id") @PathParam("vcId") Long vcId,
                                                        @ApiParam(value = "The Security Group Id") @PathParam("sgId") Long sgId,
                                                        @ApiParam(value = "Service Function Chain Id, required = false") @PathParam("sfcId") Long sfcId,
                                                        @ApiParam(value = "List of Policies", required = true) Set<BaseVirtualSystemPoliciesDto> policies) {
        logger.info("Update Binding SFC and Policies for Security Group - " + sgId);
        this.userContext.setUser(OscAuthFilter.getUsername(headers));
        BindSecurityGroupRequest bindRequest = new BindSecurityGroupRequest();
        bindRequest.setVcId(vcId);
        bindRequest.setSecurityGroupId(sgId);
        bindRequest.setSfcId(sfcId);
        bindRequest.setBindSfc(true);
        // stub SFC virtual system polices into VirtualSystemPolicyBindingDto
        for(BaseVirtualSystemPoliciesDto policy : policies) {
        	VirtualSystemPolicyBindingDto bindDto = new VirtualSystemPolicyBindingDto(policy.getVirtualSystemId(),
        																	policy.getName(), policy.getPolicyIds(), policy.getPolicies());
        	bindRequest.addServiceToBindTo(bindDto);
        }
        return this.apiUtil.getResponseForBaseRequest(this.bindSecurityGroupService, bindRequest);
    }

    @ApiOperation(value = "Deletes a Service Function Chain binding with Security Group",
            notes = "Unbind a Serice Function Chain from a Given Security group and Virtualization Connector")
    @ApiResponses(value = { @ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 400, message = "In case of any error", response = ErrorCodeDto.class) })
    @Path("/{vcId}/securityGroups/{sgId}/sfc")
    @DELETE
    public Response unbindSecurityGroupWithServiceFunctionChain(@Context HttpHeaders headers,
												            @ApiParam(value = "The Virtualization Connector Id") @PathParam("vcId") Long vcId,
												            @ApiParam(value = "Security Group Id") @PathParam("sgId") Long sgId) {
        logger.info("Unbind Security Group Id " + sgId + "with Service Function Chain  ");
        this.userContext.setUser(OscAuthFilter.getUsername(headers));
        BindSecurityGroupRequest bindRequest = new BindSecurityGroupRequest();
        bindRequest.setVcId(vcId);
        bindRequest.setSecurityGroupId(sgId);
        bindRequest.setBindSfc(true);
        return this.apiUtil.getResponseForBaseRequest(this.bindSecurityGroupService, bindRequest);
    }

    // Service Function Chain APIs
    @ApiOperation(value = "Creates a Service Function Chain",
            notes = "Creates a Service Fucntion Chain owned by Virtualization Connector provided and kicks off a " + "sync job",
            response = BaseJobResponse.class)
    @ApiResponses(value = { @ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 400, message = "In case of any error", response = ErrorCodeDto.class) })
    @Path("/{vcId}/serviceFunctionChain")
    @POST
    public Response createServiceFunctionChain(@Context HttpHeaders headers,
            @ApiParam(value = "The Virtualization Connector Id") @PathParam("vcId") Long vcId,
            @ApiParam(required = true) AddOrUpdateServiceFunctionChainRequest sfcAddRequest) {
        logger.info("Creating Service Function Chain ...");
        this.userContext.setUser(OscAuthFilter.getUsername(headers));
        if (sfcAddRequest.getDto() == null) {
            sfcAddRequest.setDto(new BaseDto());
        }
        this.apiUtil.setIdAndParentIdOrThrow(sfcAddRequest.getDto(), null, vcId, "Service Function Chain");
        return this.apiUtil.getResponseForBaseRequest(this.addServiceFunctionChainService, sfcAddRequest);
    }


    @ApiOperation(value = "Lists Service Function Chains",
            notes = "Lists Service Function Chains owned by the Virtualization Connector",
            response = ServiceFunctionChainDto.class,
            responseContainer = "Set")
    @ApiResponses(value = { @ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 400, message = "In case of any error", response = ErrorCodeDto.class) })
    @Path("/{vcId}/serviceFunctionChain")
    @GET
    public List<ServiceFunctionChainDto> getServiceFunctionChainByVirtualiazationConnector(@Context HttpHeaders headers,
            @ApiParam(value = "The Virtualization Connector Id") @PathParam("vcId") Long vcId) {
        logger.info("Listing Service Function Chains");
        this.userContext.setUser(OscAuthFilter.getUsername(headers));
        @SuppressWarnings("unchecked")
        ListResponse<ServiceFunctionChainDto> response = (ListResponse<ServiceFunctionChainDto>) this.apiUtil
        .getListResponse(this.listServiceFunctionChainByVcService, new BaseIdRequest(vcId));
        return response.getList();
    }

    @ApiOperation(value = "Retrieves a Service Function Chain",
            notes = "Retrieves the Service Function Chain owned by Virtualization Connector provided and by the specified Service Function Chain Id",
            response = ServiceFunctionChainDto.class)
    @ApiResponses(value = { @ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 400, message = "In case of any error", response = ErrorCodeDto.class) })
    @Path("/{vcId}/serviceFunctionChain/{sfcId}")
    @GET
    public ServiceFunctionChainDto getServiceFunctionChain(@Context HttpHeaders headers,
            @ApiParam(value = "The Virtualization Connector Id") @PathParam("vcId") Long vcId,
            @ApiParam(value = "The Service Function Chain Id") @PathParam("sfcId") Long sfcId) {
        logger.info("getting Service Function Chain " + sfcId);
        this.userContext.setUser(OscAuthFilter.getUsername(headers));
        GetDtoFromEntityRequest getDtoRequest = new GetDtoFromEntityRequest();
        getDtoRequest.setEntityId(sfcId);
        getDtoRequest.setEntityName("ServiceFunctionChain");
        GetDtoFromEntityServiceApi<ServiceFunctionChainDto> getDtoService = this.getDtoFromEntityServiceFactory.getService(ServiceFunctionChainDto.class);
        ServiceFunctionChainDto dto = this.apiUtil.submitBaseRequestToService(getDtoService, getDtoRequest).getDto();
        this.apiUtil.validateParentIdMatches(dto, vcId, "ServiceFunctionChain");
        return dto;
    }

    @ApiOperation(value = "Deletes a Service Function Chain",
            notes = "Delete a Service Function Chain owned by a Virutlaization Connector")
    @ApiResponses(value = { @ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 400, message = "In case of any error", response = ErrorCodeDto.class) })
    @Path("/{vcId}/serviceFunctionChain/{sfcId}")
    @DELETE
    public Response deleteServiceFunctionChain(@Context HttpHeaders headers,
            @ApiParam(value = "The Virtualization Connector Id") @PathParam("vcId") Long vcId,
            @ApiParam(value = "The Service Function Chain Id") @PathParam("sfcId") Long sfcId) {
        logger.info("Deleting Service Function Chain  " + sfcId);
        this.userContext.setUser(OscAuthFilter.getUsername(headers));
        return this.apiUtil.getResponseForBaseRequest(this.deleteServiceFunctionChainService, new BaseIdRequest(sfcId, vcId));
    }

    @ApiOperation(value = "Update a Service Function Chain",
            notes = "Update a Service Fucntion Chain owned by Virtualization Connector provided and kicks off a " + "sync job",
            response = BaseJobResponse.class)
    @ApiResponses(value = { @ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 400, message = "In case of any error", response = ErrorCodeDto.class) })
    @Path("/{vcId}/serviceFunctionChain/{sfcId}")
    @PUT
    public Response updateServiceFunctionChain(@Context HttpHeaders headers,
            @ApiParam(value = "The Virtualization Connector Id") @PathParam("vcId") Long vcId,
            @ApiParam(value = "The Service Function Chain Id") @PathParam("sfcId") Long sfcId,
            @ApiParam(required = true) AddOrUpdateServiceFunctionChainRequest sfcUpdateRequest) {
        logger.info("Update Service Function Chain ...");
        this.userContext.setUser(OscAuthFilter.getUsername(headers));
        if (sfcUpdateRequest.getDto() == null) {
            sfcUpdateRequest.setDto(new BaseDto());
        }
        this.apiUtil.setIdAndParentIdOrThrow(sfcUpdateRequest.getDto(), sfcId, vcId, "Service Function Chain");
        return this.apiUtil.getResponseForBaseRequest(this.updateServiceFunctionChainService, sfcUpdateRequest);
    }
}
