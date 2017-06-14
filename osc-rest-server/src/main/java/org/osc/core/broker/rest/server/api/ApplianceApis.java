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

import java.io.InputStream;
import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.log4j.Logger;
import org.osc.core.broker.rest.server.ApiUtil;
import org.osc.core.broker.rest.server.OscAuthFilter;
import org.osc.core.broker.rest.server.ServerRestConstants;
import org.osc.core.broker.rest.server.annotations.OscAuth;
import org.osc.core.broker.rest.server.exception.VmidcRestServerException;
import org.osc.core.broker.service.api.DeleteApplianceServiceApi;
import org.osc.core.broker.service.api.DeleteApplianceSoftwareVersionServiceApi;
import org.osc.core.broker.service.api.GetDtoFromEntityServiceApi;
import org.osc.core.broker.service.api.GetDtoFromEntityServiceFactoryApi;
import org.osc.core.broker.service.api.ListApplianceServiceApi;
import org.osc.core.broker.service.api.ListApplianceSoftwareVersionServiceApi;
import org.osc.core.broker.service.api.UploadApplianceVersionFileServiceApi;
import org.osc.core.broker.service.api.server.UserContextApi;
import org.osc.core.broker.service.dto.ApplianceDto;
import org.osc.core.broker.service.dto.ApplianceSoftwareVersionDto;
import org.osc.core.broker.service.dto.BaseDto;
import org.osc.core.broker.service.exceptions.ErrorCodeDto;
import org.osc.core.broker.service.request.BaseIdRequest;
import org.osc.core.broker.service.request.BaseRequest;
import org.osc.core.broker.service.request.GetDtoFromEntityRequest;
import org.osc.core.broker.service.request.ListApplianceSoftwareVersionRequest;
import org.osc.core.broker.service.request.UploadRequest;
import org.osc.core.broker.service.response.BaseResponse;
import org.osc.core.broker.service.response.ListResponse;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.Authorization;

@Component(service = ApplianceApis.class)
@Api(tags = "Operations for Security Functions Catalog", authorizations = { @Authorization(value = "Basic Auth") })
@Path(ServerRestConstants.SERVER_API_PATH_PREFIX + "/catalog")
@Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
@OscAuth
public class ApplianceApis {

    private static final Logger logger = Logger.getLogger(ApplianceApis.class);

    @Reference
    private ApiUtil apiUtil;

    @Reference
    ListApplianceServiceApi listApplianceService;

    @Reference
    ListApplianceSoftwareVersionServiceApi listApplianceSoftwareVersionService;

    @Reference
    DeleteApplianceServiceApi deleteApplianceService;

    @Reference
    DeleteApplianceSoftwareVersionServiceApi deleteApplianceSoftwareVersionService;

    @Reference
    UploadApplianceVersionFileServiceApi uploadApplianceVersionFileService;

    @Reference
    private GetDtoFromEntityServiceFactoryApi getDtoFromEntityServiceFactory;

    @Reference
    UserContextApi userContext;

    @ApiOperation(value = "Lists All Software Function Models",
            notes = "Lists all the Software Function Models",
            response = ApplianceDto.class,
            responseContainer = "Set")
    @ApiResponses(value = { @ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 400, message = "In case of any error", response = ErrorCodeDto.class) })
    @GET
    public List<ApplianceDto> getAppliance(@Context HttpHeaders headers) {

        logger.info("Listing Appliances");
        this.userContext.setUser(OscAuthFilter.getUsername(headers));

        @SuppressWarnings("unchecked")
        ListResponse<ApplianceDto> response = (ListResponse<ApplianceDto>) this.apiUtil
                .getListResponse(this.listApplianceService, new BaseRequest<BaseDto>(true));

        return response.getList();
    }

    @ApiOperation(value = "Retrieves a Software Function Model",
            notes = "Retrieves a Software Function Model specified by the Id",
            response = ApplianceDto.class)
    @ApiResponses(value = { @ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 400, message = "In case of any error", response = ErrorCodeDto.class) })
    @Path("/{applianceId}")
    @GET
    public ApplianceDto getAppliance(@Context HttpHeaders headers,
                                     @ApiParam(value = "Id of the Appliance Model",
                                             required = true) @PathParam("applianceId") Long applianceId) {

        logger.info("Getting Appliance " + applianceId);
        this.userContext.setUser(OscAuthFilter.getUsername(headers));

        GetDtoFromEntityRequest getDtoRequest = new GetDtoFromEntityRequest();
        getDtoRequest.setEntityId(applianceId);
        getDtoRequest.setEntityName("Appliance");
        GetDtoFromEntityServiceApi<ApplianceDto> getDtoService = this.getDtoFromEntityServiceFactory.getService(ApplianceDto.class);
        return this.apiUtil.submitBaseRequestToService(getDtoService, getDtoRequest).getDto();
    }

    @ApiOperation(value = "Deletes a Security Function Model",
            notes = "Deletes a Security Function Model if no Software Versions exist")
    @ApiResponses(value = { @ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 400, message = "In case of any error", response = ErrorCodeDto.class) })
    @Path("/{applianceId}")
    @DELETE
    public Response deleteAppliance(@Context HttpHeaders headers, @ApiParam(value = "Id of the Appliance Model",
            required = true) @PathParam("applianceId") Long applianceId) {
        logger.info("Deleting Appliance " + applianceId);
        this.userContext.setUser(OscAuthFilter.getUsername(headers));
        return this.apiUtil.getResponseForBaseRequest(this.deleteApplianceService, new BaseIdRequest(applianceId));
    }

    @ApiOperation(value = "Lists Software Function Software Versions",
            notes = "Lists the Appliance Software Versions by its owning Appliance Model Id",
            response = ApplianceSoftwareVersionDto.class,
            responseContainer = "Set")
    @ApiResponses(value = { @ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 400, message = "In case of any error", response = ErrorCodeDto.class) })
    @Path("/{applianceId}/versions")
    @GET
    public List<ApplianceSoftwareVersionDto> getApplianceSoftwareVersions(@Context HttpHeaders headers,
                                                                          @ApiParam(value = "Id of the Appliance Model",
                                                                                  required = true) @PathParam("applianceId") Long applianceId) {

        logger.info("Listing Appliance Software Versions for appliance " + applianceId);
        this.userContext.setUser(OscAuthFilter.getUsername(headers));

        try {
            ListResponse<ApplianceSoftwareVersionDto> response = this.listApplianceSoftwareVersionService
                    .dispatch(new ListApplianceSoftwareVersionRequest(applianceId));

            return response.getList();
        } catch (Exception e) {
            throw new VmidcRestServerException(Response.status(Status.INTERNAL_SERVER_ERROR), e.getMessage());
        }
    }

    @ApiOperation(value = "Retrieves a Software Function Software Version",
            notes = "Retrieves a Software Function Software Version specified by the Id",
            response = ApplianceSoftwareVersionDto.class)
    @ApiResponses(value = { @ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 400, message = "In case of any error", response = ErrorCodeDto.class) })
    @Path("/{applianceId}/versions/{ApplianceSoftwareVersionId}")
    @GET
    public ApplianceSoftwareVersionDto getApplianceSoftwareVersion(@Context HttpHeaders headers,
                                                                   @ApiParam(value = "Id of the Appliance Model", required = true) @PathParam("applianceId") Long applianceId,
                                                                   @ApiParam(value = "Id of the Appliance Software Version",
                                                                           required = true) @PathParam("ApplianceSoftwareVersionId") Long applianceSoftwareVersionId) {

        logger.info(
                "getting Appliance Software Version " + applianceSoftwareVersionId + " from appliance " + applianceId);
        this.userContext.setUser(OscAuthFilter.getUsername(headers));

        GetDtoFromEntityRequest getDtoRequest = new GetDtoFromEntityRequest();
        getDtoRequest.setEntityName("ApplianceSoftwareVersion");
        getDtoRequest.setEntityId(applianceSoftwareVersionId);
        getDtoRequest.setParentId(applianceId);

        GetDtoFromEntityServiceApi<ApplianceSoftwareVersionDto> getDtoService = this.getDtoFromEntityServiceFactory.getService(ApplianceSoftwareVersionDto.class);
        return this.apiUtil.submitBaseRequestToService(getDtoService, getDtoRequest).getDto();
    }

    @ApiOperation(value = "Deletes a Security Function Software Version",
            notes = "Deletes a Security Function Software Version if not referenced by any Distributed Appliances")
    @ApiResponses(value = { @ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 400, message = "In case of any error", response = ErrorCodeDto.class) })
    @Path("/{applianceId}/versions/{ApplianceSoftwareVersionId}")
    @DELETE
    public Response deleteApplianceSoftwareVersion(@Context HttpHeaders headers,
                                                   @ApiParam(value = "Id of the Appliance Model", required = true) @PathParam("applianceId") Long applianceId,
                                                   @ApiParam(value = "Id of the Appliance Software Version",
                                                           required = true) @PathParam("ApplianceSoftwareVersionId") Long applianceSoftwareVersionId) {
        logger.info(
                "Deleting Appliance Software Version " + applianceSoftwareVersionId + " from appliance " + applianceId);
        this.userContext.setUser(OscAuthFilter.getUsername(headers));
        return this.apiUtil.getResponseForBaseRequest(this.deleteApplianceSoftwareVersionService,
                new BaseIdRequest(applianceSoftwareVersionId, applianceId));
    }

    @ApiOperation(value = "Import a Security Function Software Version",
            notes = "Creates a Security Function Appliance Software Version Image.",
            response = BaseResponse.class)
    @ApiResponses(value = { @ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 400, message = "In case of any error", response = ErrorCodeDto.class) })
    @Path("/import/{fileName}")
    @POST
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    public Response uploadAsvFile(@Context HttpHeaders headers,
                                  @ApiParam(value = "The imported Appliance Image file name",
                                          required = true) @PathParam("fileName") String fileName,
                                  @ApiParam(required = true) InputStream uploadedInputStream) {
        logger.info("Started uploading file " + fileName);
        this.userContext.setUser(OscAuthFilter.getUsername(headers));
        return this.apiUtil.getResponseForBaseRequest(this.uploadApplianceVersionFileService,
                new UploadRequest(fileName, uploadedInputStream));
    }

}
