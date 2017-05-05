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
package org.osc.core.broker.rest.server.api.proprietary;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.log4j.Logger;
import org.osc.core.broker.rest.server.ApiUtil;
import org.osc.core.broker.rest.server.ServerRestConstants;
import org.osc.core.broker.rest.server.annotations.OscAuth;
import org.osc.core.broker.rest.server.api.ManagerApis;
import org.osc.core.broker.rest.server.model.Notification;
import org.osc.core.broker.service.api.QueryVmInfoServiceApi;
import org.osc.core.broker.service.api.TagVmServiceApi;
import org.osc.core.broker.service.api.UnTagVmServiceApi;
import org.osc.core.broker.service.request.QueryVmInfoRequest;
import org.osc.core.broker.service.request.TagVmRequest;
import org.osc.core.broker.util.SessionUtil;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(service = NsmMgrApis.class)
@Path(ServerRestConstants.MGR_NSM_API_PATH_PREFIX)
@Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
@OscAuth
public class NsmMgrApis {

    private static final Logger log = Logger.getLogger(NsmMgrApis.class);

    @Reference
    private ManagerApis managerApis;

    @Reference
    private ApiUtil apiUtil;

    @Reference
    private QueryVmInfoServiceApi queryVmInfoService;

    @Reference
    private TagVmServiceApi tagVmService;

    @Reference
    private UnTagVmServiceApi untagVmService;

    @Path("/notification")
    @POST
    public Response postNotification(@Context HttpHeaders headers, @Context HttpServletRequest httpRequest,
            Notification notification) {
        log.info("postNotification(): " + notification);
        return this.managerApis.triggerMcSync(SessionUtil.getUsername(headers), httpRequest.getRemoteAddr(), notification);
    }

    @Path("/queryVmInfo")
    @POST
    public Response queryVMInfo(@Context HttpHeaders headers, QueryVmInfoRequest queryVmInfo) {

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

}
