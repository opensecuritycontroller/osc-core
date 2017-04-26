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
package org.osc.core.server.activator;

import static org.osgi.service.http.whiteboard.HttpWhiteboardConstants.*;

import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;

import org.osc.core.broker.view.MainUIProvider;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.vaadin.server.DeploymentConfiguration;
import com.vaadin.server.ServiceException;
import com.vaadin.server.VaadinServlet;
import com.vaadin.server.VaadinServletService;

@Component(name = "ui.servlet", property = {

		HTTP_WHITEBOARD_SERVLET_NAME + "=" + UiServletContext.OSC_UI_NAME, HTTP_WHITEBOARD_SERVLET_PATTERN + "=/*",
		HTTP_WHITEBOARD_CONTEXT_SELECT + "=(" + HTTP_WHITEBOARD_CONTEXT_NAME + "=" + UiServletContext.OSC_UI_NAME + ")",
		HTTP_WHITEBOARD_TARGET + "=(" + UiServletContext.FELIX_HTTP_NAME + "=" + UiServletContext.OSC_UI_NAME + ")",
		HTTP_WHITEBOARD_SERVLET_ASYNC_SUPPORTED + "=true"

})
public class UiServlet extends VaadinServlet implements Servlet {

    /**
     *
     */
    private static final long serialVersionUID = 222397637292917068L;

    @Reference
    MainUIProvider uiProvider;


	/*
	 * Atmosphere tries to load websocket support using the ContextClassLoader.
	 * So set ContextClassLoader to our uber-bundle classloader.
	 * The uber-bundle also needs to import the Jetty websocket packages for this to work.
	 *
	 * @see {@DefaultAsyncSupportResolver.newCometSupport()}
	 */
	@Override
	public void init(ServletConfig config) throws ServletException {
		final ClassLoader oldLoader = Thread.currentThread().getContextClassLoader();
		try {
			Thread.currentThread().setContextClassLoader(this.getClass().getClassLoader());
			super.init(config);
		} finally {
			Thread.currentThread().setContextClassLoader(oldLoader);
		}
	}

    @Override
    protected VaadinServletService createServletService(DeploymentConfiguration deploymentConfiguration)
            throws ServiceException {
        VaadinServletService servletService = super.createServletService(deploymentConfiguration);
        servletService.addSessionInitListener(e -> e.getSession().addUIProvider(this.uiProvider));
        return servletService;
    }
}
