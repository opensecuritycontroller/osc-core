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

import javax.servlet.http.HttpServletRequest;
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
import org.osc.core.broker.rest.server.ServerRestConstants;
import org.osc.core.broker.rest.server.annotations.OscAuth;
import org.osc.core.broker.rest.server.exception.VmidcRestServerException;
import org.osc.core.broker.rest.server.model.Notification;
import org.osc.core.broker.service.api.MCChangeNotificationServiceApi;
import org.osc.core.broker.service.api.ManagerApi;
import org.osc.core.broker.service.api.QueryVmInfoServiceApi;
import org.osc.core.broker.service.api.TagVmServiceApi;
import org.osc.core.broker.service.api.UnTagVmServiceApi;
import org.osc.core.broker.service.exceptions.ErrorCodeDto;
import org.osc.core.broker.service.exceptions.VmidcBrokerValidationException;
import org.osc.core.broker.service.request.MCChangeNotificationRequest;
import org.osc.core.broker.service.request.QueryVmInfoRequest;
import org.osc.core.broker.service.request.TagVmRequest;
import org.osc.core.broker.service.response.BaseJobResponse;
import org.osc.core.broker.service.response.QueryVmInfoResponse;
import org.osc.core.broker.util.SessionUtil;
import org.osc.core.broker.util.api.ApiUtil;
import org.osc.sdk.manager.element.MgrChangeNotification;
import org.osc.sdk.manager.element.MgrChangeNotification.ChangeType;
import org.osc.sdk.manager.element.MgrChangeNotification.MgrObjectType;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.Authorization;

/**
 * This service is published as {@link ManagerApi} for the WebSocketClient in osc-server
 * and {@link ManagerApis} for access from within osc-rest-server.
 */
@Component(service = { ManagerApis.class, ManagerApi.class })
@Api(tags = "Operations for Manager Plugin", authorizations = { @Authorization(value = "Basic Auth") })
@Path(ServerRestConstants.MANAGER_API_PATH_PREFIX)
@Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
@OscAuth
public class ManagerApis implements ManagerApi {

    private static final Logger log = Logger.getLogger(ManagerApis.class);

    @Reference
    private ApiUtil apiUtil;

    @Reference
    private QueryVmInfoServiceApi queryVmInfoService;

    @Reference
    private TagVmServiceApi tagVmService;

    @Reference
    private UnTagVmServiceApi untagVmService;

    @Reference
    private MCChangeNotificationServiceApi mCChangeNotificationService;

    @ApiOperation(value = "Notfies OSC about registered changes in Manager",
            notes = "The relevant manager connector is derived from the IP address of the HTTP client the notification "
                    + "request is reported by and responds to the notification accordingly",
            response = BaseJobResponse.class)
    @ApiResponses(value = { @ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 400, message = "In case of any error", response = ErrorCodeDto.class) })
    @Path("/notification")
    @POST
    public Response postNotification(@Context HttpHeaders headers, @Context HttpServletRequest httpRequest,
            @ApiParam(required = true) Notification notification) {
        log.info("postNotification(): " + notification);
        return triggerMcSync(SessionUtil.getUsername(headers), httpRequest.getRemoteAddr(), notification);
    }

    @ApiOperation(value = "Query Virtual Machine information",
            notes = "Query VM information based on VM UUID, IP, MAC or Flow 6-field-tuple. Request can include all search "
                    + "criteria. If found, the respond will include the VM "
                    + "information based on the information provided for query. For example, if IP is provided, "
                    + "response will include a map entry where the key is the IP and the value is the VM information.<br>",
            response = QueryVmInfoResponse.class)
    @ApiResponses(value = { @ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 400, message = "In case of any error", response = ErrorCodeDto.class) })
    @Path("/queryVmInfo")
    @POST
    public Response queryVMInfo(@Context HttpHeaders headers,
            @ApiParam(required = true) QueryVmInfoRequest queryVmInfo) {

        log.info("Query VM info request: " + queryVmInfo);
        SessionUtil.setUser(SessionUtil.getUsername(headers));

        return this.apiUtil.getResponse(this.queryVmInfoService, queryVmInfo);
    }

    @Path("/tagVm")
    @POST
    public Response tagVm(@Context HttpHeaders headers, TagVmRequest tagVmRequest) {

        log.info("Tag VM info request: " + tagVmRequest);
        SessionUtil.setUser(SessionUtil.getUsername(headers));

        return this.apiUtil.getResponse(this.tagVmService, tagVmRequest);
    }

    @Path("/untagVm")
    @POST
    public Response unquarantineVm(@Context HttpHeaders headers, TagVmRequest tagVmRequest) {

        log.info("UnTag VM info request: " + tagVmRequest);
        SessionUtil.setUser(SessionUtil.getUsername(headers));

        return this.apiUtil.getResponse(this.untagVmService, tagVmRequest);
    }

    public Response triggerMcSync(String username, String ipAddress, Notification notification) {
        ChangeType ct = ChangeType.UPDATED;
        MgrObjectType ot;
        if (notification.eventNotification.eventObject.equals("Admin Domain")) {
            ot = MgrObjectType.DOMAIN;
        } else if (notification.eventNotification.eventObject.equals("PolicyGroup")) {
            ot = MgrObjectType.POLICY;
        } else {
            throw new VmidcRestServerException(Response.status(Status.BAD_REQUEST),
                    "Invalid request. Object type not specified.");
        }

        MgrChangeNotification mgrNotification = new MgrChangeNotification(ct, ot, null);

        return triggerMcSync(username, ipAddress, mgrNotification);
    }

    private Response triggerMcSync(String username, String ipAddress, MgrChangeNotification notification) {
        try {
            BaseJobResponse response = triggerMcSyncService(username, ipAddress, notification);
            return Response.status(Status.OK).entity(response).build();

        } catch (VmidcBrokerValidationException ex) {

            log.error("MC change notification validation error", ex);
            throw new VmidcRestServerException(Response.status(Status.BAD_REQUEST), ex.getMessage());

        } catch (Exception ex) {

            log.error("Error while processing MC change notification", ex);
            throw new VmidcRestServerException(Response.status(Status.INTERNAL_SERVER_ERROR), ex.getMessage());
        }
    }

    @Override
    public BaseJobResponse triggerMcSyncService(String username, String ipAddress, MgrChangeNotification notification)
            throws Exception {
        SessionUtil.setUser(username);

        MCChangeNotificationRequest request = new MCChangeNotificationRequest();
        request.mgrIpAddress = ipAddress;
        request.notification = notification;

        BaseJobResponse response = this.mCChangeNotificationService.dispatch(request);
        return response;
    }
}
