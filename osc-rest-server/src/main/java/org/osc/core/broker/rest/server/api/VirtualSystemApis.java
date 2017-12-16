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

import org.osc.core.broker.rest.server.ApiUtil;
import org.osc.core.broker.rest.server.OscAuthFilter;
import org.osc.core.broker.rest.server.ServerRestConstants;
import org.osc.core.broker.rest.server.annotations.OscAuth;
import org.osc.core.broker.service.api.AddDeploymentSpecServiceApi;
import org.osc.core.broker.service.api.AddSecurityGroupInterfaceServiceApi;
import org.osc.core.broker.service.api.DeleteDeploymentSpecServiceApi;
import org.osc.core.broker.service.api.DeleteSecurityGroupInterfaceServiceApi;
import org.osc.core.broker.service.api.ForceDeleteVirtualSystemServiceApi;
import org.osc.core.broker.service.api.GetDtoFromEntityServiceApi;
import org.osc.core.broker.service.api.GetDtoFromEntityServiceFactoryApi;
import org.osc.core.broker.service.api.ListDeploymentSpecServiceByVirtualSystemApi;
import org.osc.core.broker.service.api.ListDistributedApplianceInstanceByVSServiceApi;
import org.osc.core.broker.service.api.ListSecurityGroupInterfaceServiceByVirtualSystemApi;
import org.osc.core.broker.service.api.ListVirtualSystemPolicyServiceApi;
import org.osc.core.broker.service.api.SyncDeploymentSpecServiceApi;
import org.osc.core.broker.service.api.UpdateDeploymentSpecServiceApi;
import org.osc.core.broker.service.api.UpdateSecurityGroupInterfaceServiceApi;
import org.osc.core.broker.service.api.server.UserContextApi;
import org.osc.core.broker.service.dto.ApplianceManagerConnectorDto;
import org.osc.core.broker.service.dto.DistributedApplianceInstanceDto;
import org.osc.core.broker.service.dto.PolicyDto;
import org.osc.core.broker.service.dto.SecurityGroupInterfaceDto;
import org.osc.core.broker.service.dto.openstack.DeploymentSpecDto;
import org.osc.core.broker.service.exceptions.ErrorCodeDto;
import org.osc.core.broker.service.request.BaseDeleteRequest;
import org.osc.core.broker.service.request.BaseIdRequest;
import org.osc.core.broker.service.request.BaseRequest;
import org.osc.core.broker.service.request.GetDtoFromEntityRequest;
import org.osc.core.broker.service.response.BaseJobResponse;
import org.osc.core.broker.service.response.ListResponse;
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

@Component(service = VirtualSystemApis.class)
@Api(tags = "Operations for Virtual Systems", authorizations = { @Authorization(value = "Basic Auth") })
@Path(ServerRestConstants.SERVER_API_PATH_PREFIX + "/virtualSystems")
@Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
@OscAuth
public class VirtualSystemApis {

    private static final Logger logger = LoggerFactory.getLogger(VirtualSystemApis.class);

    @Reference
    private ApiUtil apiUtil;

    @Reference
    private AddDeploymentSpecServiceApi addDeploymentSpecService;

    @Reference
    private SyncDeploymentSpecServiceApi syncDeploymentSpecService;

    @Reference
    private UpdateDeploymentSpecServiceApi updateDeploymentSpecService;

    @Reference
    private DeleteDeploymentSpecServiceApi deleteDeploymentSpecService;

    @Reference
    private ListDeploymentSpecServiceByVirtualSystemApi listDeploymentSpecServiceByVirtualSystem;

    @Reference
    private ListDistributedApplianceInstanceByVSServiceApi listDistributedApplianceInstanceByVSService;

    @Reference
    private ForceDeleteVirtualSystemServiceApi forceDeleteVirtualSystemService;

    @Reference
    private DeleteSecurityGroupInterfaceServiceApi deleteSecurityGroupInterfaceService;

    @Reference
    private ListVirtualSystemPolicyServiceApi listVirtualSystemPolicyService;

    @Reference
    private AddSecurityGroupInterfaceServiceApi addSecurityGroupInterfaceService;

    @Reference
    private ListSecurityGroupInterfaceServiceByVirtualSystemApi listSecurityGroupInterfaceServiceByVirtualSystem;

    @Reference
    private UpdateSecurityGroupInterfaceServiceApi updateSecurityGroupInterfaceService;

    @Reference
    private GetDtoFromEntityServiceFactoryApi getDtoFromEntityServiceFactory;

    @Reference
    private UserContextApi userContext;

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
        this.userContext.setUser(OscAuthFilter.getUsername(headers));

        @SuppressWarnings("unchecked")
        ListResponse<DistributedApplianceInstanceDto> response = (ListResponse<DistributedApplianceInstanceDto>) this.apiUtil
        .getListResponse(this.listDistributedApplianceInstanceByVSService, new BaseIdRequest(vsId));
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
        this.userContext.setUser(OscAuthFilter.getUsername(headers));

        @SuppressWarnings("unchecked")
        ListResponse<PolicyDto> response = (ListResponse<PolicyDto>) this.apiUtil.getListResponse(this.listVirtualSystemPolicyService,
                new BaseIdRequest(vsId));
        return response.getList();
    }

    // DS APIS
    @ApiOperation(value = "Lists Deployment Specifications",
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
        this.userContext.setUser(OscAuthFilter.getUsername(headers));

        @SuppressWarnings("unchecked")
        ListResponse<DeploymentSpecDto> response = (ListResponse<DeploymentSpecDto>) this.apiUtil
        .getListResponse(this.listDeploymentSpecServiceByVirtualSystem, new BaseIdRequest(vsId));
        return response.getList();
    }

    @ApiOperation(value = "Retrieves the Deployment Specification",
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
        this.userContext.setUser(OscAuthFilter.getUsername(headers));
        GetDtoFromEntityRequest getDtoRequest = new GetDtoFromEntityRequest();
        getDtoRequest.setEntityId(dsId);
        getDtoRequest.setEntityName("DeploymentSpec");
        GetDtoFromEntityServiceApi<DeploymentSpecDto> getDtoService = this.getDtoFromEntityServiceFactory.getService(DeploymentSpecDto.class);
        DeploymentSpecDto dto = this.apiUtil.submitBaseRequestToService(getDtoService, getDtoRequest).getDto();

        this.apiUtil.validateParentIdMatches(dto, vsId, "SecurityGroup");

        return dto;
    }

    @ApiOperation(value = "Creates a Deployment Specification",
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
        this.userContext.setUser(OscAuthFilter.getUsername(headers));
        this.apiUtil.setParentIdOrThrow(dsDto, vsId, "Deployment Specification");
        return this.apiUtil.getResponseForBaseRequest(this.addDeploymentSpecService,
                new BaseRequest<DeploymentSpecDto>(dsDto));
    }

    @ApiOperation(value = "Syncs a deployment spec",
            notes = "Syncs a deployment spec",
            response = BaseJobResponse.class)
    @ApiResponses(value = { @ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 400, message = "In case of any error", response = ErrorCodeDto.class) })
    @Path("/{vsId}/deploymentSpecs/{dsId}/sync")
    @PUT
    public Response syncDeploymentSpec(@Context HttpHeaders headers,
            @ApiParam(value = "The Virtual System Id") @PathParam("vsId") Long vsId,
            @ApiParam(value = "The Deployment Specification Id") @PathParam("dsId") Long dsId) {
        logger.info("Sync deployment spec" + dsId);
        this.userContext.setUser(OscAuthFilter.getUsername(headers));
        DeploymentSpecDto dsDto = new DeploymentSpecDto();
        dsDto.setId(dsId);
        return this.apiUtil.getResponseForBaseRequest(this.syncDeploymentSpecService, new BaseRequest<DeploymentSpecDto>(dsDto));
    }

    @ApiOperation(value = "Updates a Deployment Specification",
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
        this.userContext.setUser(OscAuthFilter.getUsername(headers));
        this.apiUtil.setIdAndParentIdOrThrow(dsDto, dsId, vsId, "Deployment Spec");
        return this.apiUtil.getResponseForBaseRequest(this.updateDeploymentSpecService,
                new BaseRequest<DeploymentSpecDto>(dsDto));
    }

    @ApiOperation(value = "Deletes a Deployment Specification",
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
        this.userContext.setUser(OscAuthFilter.getUsername(headers));
        return this.apiUtil.getResponseForBaseRequest(this.deleteDeploymentSpecService,
                new BaseDeleteRequest(dsId, vsId, false));// false as this is not force delete
    }

    @ApiOperation(value = "Force Delete a Deployment Specification",
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
        this.userContext.setUser(OscAuthFilter.getUsername(headers));
        return this.apiUtil.getResponseForBaseRequest(this.deleteDeploymentSpecService,
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
        this.userContext.setUser(OscAuthFilter.getUsername(headers));
        @SuppressWarnings("unchecked")
        ListResponse<SecurityGroupInterfaceDto> response = (ListResponse<SecurityGroupInterfaceDto>) this.apiUtil
        .getListResponse(this.listSecurityGroupInterfaceServiceByVirtualSystem, new BaseIdRequest(vsId));
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
        this.userContext.setUser(OscAuthFilter.getUsername(headers));
        GetDtoFromEntityRequest getDtoRequest = new GetDtoFromEntityRequest();
        getDtoRequest.setEntityId(sgiId);
        getDtoRequest.setEntityName("SecurityGroupInterface");
        GetDtoFromEntityServiceApi<SecurityGroupInterfaceDto> getDtoService = this.getDtoFromEntityServiceFactory.getService(SecurityGroupInterfaceDto.class);
        SecurityGroupInterfaceDto dto = this.apiUtil.submitBaseRequestToService(getDtoService, getDtoRequest).getDto();

        this.apiUtil.validateParentIdMatches(dto, vsId, "SecurityGroupInterface");

        return dto;
    }

    @ApiOperation(value = "Creates a Traffic Policy Mapping",
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
        this.userContext.setUser(OscAuthFilter.getUsername(headers));
        this.apiUtil.setParentIdOrThrow(sgiDto, vsId, "Traffic Policy Mapping");
        return this.apiUtil.getResponseForBaseRequest(this.addSecurityGroupInterfaceService,
                new BaseRequest<SecurityGroupInterfaceDto>(sgiDto));
    }

    @ApiOperation(value = "Updates a Traffic Policy Mapping",
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
        this.userContext.setUser(OscAuthFilter.getUsername(headers));
        this.apiUtil.setIdAndParentIdOrThrow(sgiDto, sgiId, vsId, "Traffic Policy Mapping");
        return this.apiUtil.getResponseForBaseRequest(this.updateSecurityGroupInterfaceService,
                new BaseRequest<SecurityGroupInterfaceDto>(sgiDto));
    }

    @ApiOperation(value = "Deletes a Traffic Policy Mapping",
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
        this.userContext.setUser(OscAuthFilter.getUsername(headers));
        return this.apiUtil.getResponseForBaseRequest(this.deleteSecurityGroupInterfaceService,
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
        this.userContext.setUser(OscAuthFilter.getUsername(headers));
        return this.apiUtil.getResponseForBaseRequest(this.forceDeleteVirtualSystemService,
                new BaseDeleteRequest(vsId, true));
    }
}
