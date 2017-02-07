package org.osc.core.broker.rest.server.api;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.log4j.Logger;
import org.osc.core.broker.rest.server.OscRestServlet;
import org.osc.core.rest.annotations.OscAuth;
import org.osc.core.broker.rest.server.exception.ErrorCodeDto;
import org.osc.core.broker.rest.server.exception.VmidcRestServerException;
import org.osc.core.broker.rest.server.model.MgrFile;
import org.osc.core.broker.rest.server.model.Notification;
import org.osc.core.broker.rest.server.model.QueryVmInfoRequest;
import org.osc.core.broker.rest.server.model.TagVmRequest;
import org.osc.core.broker.rest.server.model.UpdateApplianceConsolePasswordRequest;
import org.osc.core.broker.service.PropagateVSMgrFileService;
import org.osc.core.broker.service.QueryVmInfoService;
import org.osc.core.broker.service.TagVmService;
import org.osc.core.broker.service.UnTagVmService;
import org.osc.core.broker.service.UpdateApplianceConsolePasswordService;
import org.osc.core.broker.service.exceptions.VmidcBrokerValidationException;
import org.osc.core.broker.service.mc.MCChangeNotificationService;
import org.osc.core.broker.service.request.MCChangeNotificationRequest;
import org.osc.core.broker.service.request.PropagateVSMgrFileRequest;
import org.osc.core.broker.service.request.UpdateDaiConsolePasswordRequest;
import org.osc.core.broker.service.response.BaseJobResponse;
import org.osc.core.broker.service.response.QueryVmInfoResponse;
import org.osc.core.broker.util.SessionUtil;
import org.osc.sdk.manager.element.MgrChangeNotification;
import org.osc.sdk.manager.element.MgrChangeNotification.ChangeType;
import org.osc.sdk.manager.element.MgrChangeNotification.MgrObjectType;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.Authorization;

@Api(tags = "Operations for Manager Plugin", authorizations = { @Authorization(value = "Basic Auth") })
@Path(OscRestServlet.MANAGER_API_PATH_PREFIX)
@Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
@OscAuth
public class ManagerApis {

    private static final Logger log = Logger.getLogger(ManagerApis.class);

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
        return ManagerApis.triggerMcSync(SessionUtil.getUsername(headers), httpRequest.getRemoteAddr(), notification);
    }

    @ApiOperation(value = "Propagate a Manager File to Appliance Instances",
            notes = "Provided virtualSystemName must be of an existing Virtual Security System (VSS). <br/> MgrFile request contains "
                    + "Manager File information and list of Appliance Instances to propagate file to. If Appliance "
                    + "Instances is ommited, all instances is assumed.<br/>"
                    + "If successful, returns the File Propagation Job Id. Each Appliance Instance file will be "
                    + "propagated and persisted in the CPA directoy and the process-mgr-file.py will be called to "
                    + "notify the appliance to process the file.",
            response = BaseJobResponse.class)
    @ApiResponses(value = { @ApiResponse(code = 200, message = "Corresponding File Propagation Job started. Id in response is expected to be empty"),
            @ApiResponse(code = 400, message = "In case of any error", response = ErrorCodeDto.class) })
    @Path("/propagateMgrFile/vs/{virtualSystemName}")
    @PUT
    public Response propagateMgrFile(@Context HttpHeaders headers,
                                     @ApiParam(value = "Virtual System name to which file propagation is requested",
                                             required = true) @PathParam("virtualSystemName") String virtualSystemName,
                                     @ApiParam(value = "The File to propogate", required = true) MgrFile mgrFile) {

        log.info("Propagate MgrFile for vsName: " + virtualSystemName);
        SessionUtil.setUser(SessionUtil.getUsername(headers));

        PropagateVSMgrFileRequest request = new PropagateVSMgrFileRequest();
        request.setVsName(virtualSystemName);
        request.setDaiList(mgrFile.getApplianceInstances());
        request.setMgrFile(mgrFile.getMgrFile());
        request.setMgrFileName(mgrFile.getMgrFileName());

        return ApiUtil.getResponse(new PropagateVSMgrFileService(), request);
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

        return ApiUtil.getResponse(new QueryVmInfoService(), queryVmInfo);
    }

    @Path("/updateApplianceConsolePassword/vs/{virtualSystemName}")
    @PUT
    public Response updateApplianceConsolePassword(@Context HttpHeaders headers,
                                                   @PathParam("virtualSystemName") String virtualSystemName,
                                                   UpdateApplianceConsolePasswordRequest uacpr) {

        log.info("Update appliance(s) console password for vsName: " + virtualSystemName);
        SessionUtil.setUser(SessionUtil.getUsername(headers));

        UpdateDaiConsolePasswordRequest request = new UpdateDaiConsolePasswordRequest();
        request.setVsName(virtualSystemName);
        request.setDaiList(uacpr.applianceInstance);
        request.setNewPassword(uacpr.newPassword);

        return ApiUtil.getResponse(new UpdateApplianceConsolePasswordService(), request);
    }

    @Path("/tagVm")
    @POST
    public Response tagVm(@Context HttpHeaders headers, TagVmRequest tagVmRequest) {

        log.info("Tag VM info request: " + tagVmRequest);
        SessionUtil.setUser(SessionUtil.getUsername(headers));

        return ApiUtil.getResponse(new TagVmService(), tagVmRequest);
    }

    @Path("/untagVm")
    @POST
    public Response unquarantineVm(@Context HttpHeaders headers, TagVmRequest tagVmRequest) {

        log.info("UnTag VM info request: " + tagVmRequest);
        SessionUtil.setUser(SessionUtil.getUsername(headers));

        return ApiUtil.getResponse(new UnTagVmService(), tagVmRequest);
    }

    public static Response triggerMcSync(String username, String ipAddress, Notification notification) {
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

    private static Response triggerMcSync(String username, String ipAddress, MgrChangeNotification notification) {
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

    public static BaseJobResponse triggerMcSyncService(String username, String ipAddress,
            MgrChangeNotification notification) throws Exception {
        SessionUtil.setUser(username);

        MCChangeNotificationRequest request = new MCChangeNotificationRequest();
        request.mgrIpAddress = ipAddress;
        request.notification = notification;

        MCChangeNotificationService service = new MCChangeNotificationService();
        BaseJobResponse response = service.dispatch(request);
        return response;
    }
}
