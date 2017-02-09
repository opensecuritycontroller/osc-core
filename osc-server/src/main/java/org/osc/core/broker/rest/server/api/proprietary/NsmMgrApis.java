package org.osc.core.broker.rest.server.api.proprietary;

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

import org.apache.log4j.Logger;
import org.osc.core.broker.di.OSC;
import org.osc.core.broker.rest.server.OscRestServlet;
import org.osc.core.broker.util.api.ApiUtil;
import org.osc.core.broker.util.session.SessionUtil;
import org.osc.core.rest.annotations.OscAuth;
import org.osc.core.broker.rest.server.api.ManagerApis;
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
import org.osc.core.broker.service.request.PropagateVSMgrFileRequest;
import org.osc.core.broker.service.request.UpdateDaiConsolePasswordRequest;

@Path(OscRestServlet.MGR_NSM_API_PATH_PREFIX)
@Consumes({ MediaType.APPLICATION_JSON })
@Produces({ MediaType.APPLICATION_JSON })
@OscAuth
public class NsmMgrApis {

    private static final Logger log = Logger.getLogger(NsmMgrApis.class);

    SessionUtil sessionUtil = OSC.get().sessionUtil();
    ApiUtil apiUtil = OSC.get().apiUtil();

    @Path("/notification")
    @POST
    public Response postNotification(@Context HttpHeaders headers, @Context HttpServletRequest httpRequest,
                                     Notification notification) {
        log.info("postNotification(): " + notification);
        return ManagerApis.triggerMcSync(sessionUtil.getUsername(headers), httpRequest.getRemoteAddr(), notification);
    }

    @Path("/propagateMgrFile/vs/{vsName}")
    @PUT
    public Response propagateMgrFile(@Context HttpHeaders headers, @PathParam("vsName") String vsName,
                                     MgrFile mgrFile) {

        log.info("Propagate MgrFile for vsName: " + vsName);
        sessionUtil.setUser(sessionUtil.getUsername(headers));

        PropagateVSMgrFileRequest request = new PropagateVSMgrFileRequest();
        request.setVsName(vsName);
        request.setDaiList(mgrFile.getApplianceInstances());
        request.setMgrFile(mgrFile.getMgrFile());
        request.setMgrFileName(mgrFile.getMgrFileName());

        return apiUtil.getResponse(new PropagateVSMgrFileService(), request);

    }

    @Path("/updateApplianceConsolePassword/vs/{vsName}")
    @PUT
    public Response updateApplianceConsolePassword(@Context HttpHeaders headers, @PathParam("vsName") String vsName,
                                                   UpdateApplianceConsolePasswordRequest uacpr) {

        log.info("Update appliance(s) console password for vsName: " + vsName);
        sessionUtil.setUser(sessionUtil.getUsername(headers));

        UpdateDaiConsolePasswordRequest request = new UpdateDaiConsolePasswordRequest();
        request.setVsName(vsName);
        request.setDaiList(uacpr.applianceInstance);
        request.setNewPassword(uacpr.newPassword);

        return apiUtil.getResponse(new UpdateApplianceConsolePasswordService(), request);
    }

    @Path("/queryVmInfo")
    @POST
    public Response queryVMInfo(@Context HttpHeaders headers, QueryVmInfoRequest queryVmInfo) {

        log.info("Query VM info request: " + queryVmInfo);
        sessionUtil.setUser(sessionUtil.getUsername(headers));

        return apiUtil.getResponse(new QueryVmInfoService(), queryVmInfo);
    }

    @Path("/tagVm")
    @POST
    public Response tagVm(@Context HttpHeaders headers, TagVmRequest tagVmRequest) {

        log.info("Tag VM info request: " + tagVmRequest);
        sessionUtil.setUser(sessionUtil.getUsername(headers));

        return apiUtil.getResponse(new TagVmService(), tagVmRequest);
    }

    @Path("/untagVm")
    @POST
    public Response unquarantineVm(@Context HttpHeaders headers, TagVmRequest tagVmRequest) {

        log.info("UnTag VM info request: " + tagVmRequest);
        sessionUtil.setUser(sessionUtil.getUsername(headers));

        return apiUtil.getResponse(new UnTagVmService(), tagVmRequest);
    }

}
