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
package org.osc.core.broker.rest.server;

import static org.osgi.service.http.whiteboard.HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME;
import static org.osgi.service.http.whiteboard.HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT;
import static org.osgi.service.http.whiteboard.HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_NAME;
import static org.osgi.service.http.whiteboard.HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN;
import static org.osgi.service.http.whiteboard.HttpWhiteboardConstants.HTTP_WHITEBOARD_TARGET;

import java.io.IOException;

import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.ServerProperties;
import org.glassfish.jersey.servlet.ServletContainer;
import org.osc.core.broker.rest.server.api.AlarmApis;
import org.osc.core.broker.rest.server.api.AlertApis;
import org.osc.core.broker.rest.server.api.ApplianceApis;
import org.osc.core.broker.rest.server.api.DistributedApplianceApis;
import org.osc.core.broker.rest.server.api.DistributedApplianceInstanceApis;
import org.osc.core.broker.rest.server.api.JobApis;
import org.osc.core.broker.rest.server.api.ManagerApis;
import org.osc.core.broker.rest.server.api.ManagerConnectorApis;
import org.osc.core.broker.rest.server.api.ServerDebugApis;
import org.osc.core.broker.rest.server.api.ServerMgmtApis;
import org.osc.core.broker.rest.server.api.VirtualSystemApis;
import org.osc.core.broker.rest.server.api.VirtualizationConnectorApis;
import org.osc.core.broker.rest.server.api.proprietary.NsmMgrApis;
import org.osc.core.broker.rest.server.api.proprietary.NsxApis;
import org.osc.core.broker.rest.server.exception.BadRequestExceptionMapper;
import org.osc.core.broker.rest.server.exception.InternalServerErrorExceptionMapper;
import org.osc.core.broker.rest.server.exception.JsonProcessingExceptionMapper;
import org.osc.core.broker.rest.server.exception.NotFoundExceptionMapper;
import org.osc.core.broker.rest.server.exception.PathParamExceptionMapper;
import org.osc.core.broker.rest.server.exception.XMLParseExceptionMapper;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.fasterxml.jackson.jaxrs.json.JacksonJaxbJsonProvider;

@Component(name = "api.servlet", service = Servlet.class, property = {

        HTTP_WHITEBOARD_SERVLET_NAME + "=" + ApiServletContext.OSC_API_NAME, HTTP_WHITEBOARD_SERVLET_PATTERN + "=/*",
        HTTP_WHITEBOARD_CONTEXT_SELECT + "=(" + HTTP_WHITEBOARD_CONTEXT_NAME + "=" + ApiServletContext.OSC_API_NAME
                + ")",
        HTTP_WHITEBOARD_TARGET + "=(" + ApiServletContext.FELIX_HTTP_NAME + "=" + ApiServletContext.OSC_API_NAME + ")"

})
public class ApiServletDelegate extends ResourceConfig implements Servlet {
    static final long serialVersionUID = 1L;

    @Reference
    private AlarmApis alarmApis;
    @Reference
    private AlertApis alertApis;
    @Reference
    private ApplianceApis applianceApis;
    @Reference
    private DistributedApplianceApis distributedApplianceApis;
    @Reference
    private DistributedApplianceInstanceApis distributedApplianceInstanceApis;
    @Reference
    private JobApis jobApis;
    @Reference
    private ManagerApis managerApis;
    @Reference
    private ManagerConnectorApis managerConnectorApis;
    @Reference
    private NsmMgrApis nsmMgrApis;
    @Reference
    private NsxApis nsxApis;
    @Reference
    private ServerDebugApis serverDebugApis;
    @Reference
    private ServerMgmtApis serverMgmtApis;
    @Reference
    private VirtualSystemApis virtualSystemApis;
    @Reference
    private VirtualizationConnectorApis virtualizationConnectorApis;

    @Reference
    private NsxAuthFilter nsxAuthFilter;

    @Reference
    private OscAuthFilter oscAuthFilter;

    @Reference
    private LocalHostAuthFilter localHostAuthFilter;

    /** The Jersey REST container */
    private ServletContainer container;

    @Activate
    void activate() {
        //Json feature
        super.register(JacksonJaxbJsonProvider.class);

        //Auth Filters
        super.registerInstances(this.nsxAuthFilter, this.oscAuthFilter, this.localHostAuthFilter);

        //Exception mappers
        super.register(BadRequestExceptionMapper.class);
        super.register(InternalServerErrorExceptionMapper.class);
        super.register(JsonProcessingExceptionMapper.class);
        super.register(NotFoundExceptionMapper.class);
        super.register(PathParamExceptionMapper.class);
        super.register(XMLParseExceptionMapper.class);

        //Properties
        super.property(ServerProperties.FEATURE_AUTO_DISCOVERY_DISABLE, true);

        // agent & server apis
        super.registerInstances(this.alarmApis, this.alertApis, this.applianceApis, this.distributedApplianceApis,
                this.distributedApplianceInstanceApis, this.jobApis, this.managerApis, this.managerConnectorApis,
                this.nsmMgrApis, this.nsxApis, this.serverDebugApis, this.serverMgmtApis, this.virtualSystemApis,
                this.virtualizationConnectorApis);

        this.container = new ServletContainer(this);
    }

    @Override
    public void destroy() {
        this.container.destroy();
    }

    // Servlet interface methods

    @Override
    public ServletConfig getServletConfig() {
        return this.container.getServletConfig();
    }

    @Override
    public String getServletInfo() {
        return this.container.getServletInfo();
    }

    @Override
    public void init(ServletConfig config) throws ServletException {
        this.container.init(config);
    }

    @Override
    public void service(ServletRequest request, ServletResponse response) throws ServletException, IOException {
        this.container.service(request, response);
    }

}
