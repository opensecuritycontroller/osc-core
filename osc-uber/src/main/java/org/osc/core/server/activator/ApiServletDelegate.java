/*******************************************************************************
 * Copyright (c) 2017 Intel Corporation
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
package org.osc.core.server.activator;

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

import org.osc.core.broker.rest.server.IscRestServlet;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;

import com.sun.jersey.spi.container.servlet.ServletContainer;

@Component(name = "api.servlet", service = Servlet.class, property = {

        HTTP_WHITEBOARD_SERVLET_NAME + "=" + ApiServletContext.OSC_API_NAME, HTTP_WHITEBOARD_SERVLET_PATTERN + "=/*",
        HTTP_WHITEBOARD_CONTEXT_SELECT + "=(" + HTTP_WHITEBOARD_CONTEXT_NAME + "=" + ApiServletContext.OSC_API_NAME
                + ")",
        HTTP_WHITEBOARD_TARGET + "=(" + ApiServletContext.FELIX_HTTP_NAME + "=" + ApiServletContext.OSC_API_NAME + ")"

})
public class ApiServletDelegate implements Servlet {
    /** The Jersey REST container */
    private ServletContainer container;

    @Activate
    void activate() {
        this.container = new ServletContainer(IscRestServlet.class);
    }

    @Override
    public void destroy() {
        this.container.destroy();
    }

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
