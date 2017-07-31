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
package org.osc.core.ui;

import static org.osgi.service.http.whiteboard.HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME;
import static org.osgi.service.http.whiteboard.HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT;
import static org.osgi.service.http.whiteboard.HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_ASYNC_SUPPORTED;
import static org.osgi.service.http.whiteboard.HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_NAME;
import static org.osgi.service.http.whiteboard.HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN;
import static org.osgi.service.http.whiteboard.HttpWhiteboardConstants.HTTP_WHITEBOARD_TARGET;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

import org.osc.core.broker.service.api.server.UserContextApi;
import org.osc.core.broker.view.MainUIProvider;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

import com.vaadin.server.DeploymentConfiguration;
import com.vaadin.server.ServiceException;
import com.vaadin.server.VaadinServlet;
import com.vaadin.server.VaadinServletRequest;
import com.vaadin.server.VaadinServletService;
import com.vaadin.server.VaadinSession;
import com.vaadin.ui.UI;

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

    @Reference
    UserContextApi userContext;
    
    private final Set<VaadinSession> sessions = new CopyOnWriteArraySet<>();


	/*
	 * Atmosphere tries to load websocket support using the ContextClassLoader.
	 * So set ContextClassLoader to the Http Service implementation classloader.
	 *
	 * @see {@DefaultAsyncSupportResolver.newCometSupport()}
	 */
	@Override
	public void init(ServletConfig config) throws ServletException {
		final ClassLoader oldLoader = Thread.currentThread().getContextClassLoader();
		try {
			Thread.currentThread().setContextClassLoader(config.getClass().getClassLoader());
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
        servletService.addSessionInitListener(e -> sessions.add(e.getSession()));
        servletService.addSessionDestroyListener(e -> sessions.remove(e.getSession()));
        return servletService;
    }

    @Deactivate
    void stop() {
        // Terminate Vaadin UI Application
        destroy();
        
        for (VaadinSession vaadinSession : sessions) {
        	vaadinSession.close();

        	// Redirect all UIs to force the close
        	for (UI ui : vaadinSession.getUIs()) {
        		ui.access(() -> ui.getPage().setLocation("/"));
			}
        }
    }

    /**
     * Every incoming request should set the current user context
     */
    @Override
    protected VaadinServletRequest createVaadinRequest(HttpServletRequest request) {
        VaadinServletRequest vaadinRequest = super.createVaadinRequest(request);

        VaadinSession vaadinSession;
        try {
            vaadinSession = getService().findVaadinSession(vaadinRequest);
        } catch (Exception e) {
            // This exception will be handled later when we try to service
            // the request
            vaadinSession = null;
        }

        if(vaadinSession != null) {
            this.userContext.setUser((String) vaadinSession.getAttribute("user"));
        } else {
            this.userContext.setUser(null);
        }

        return vaadinRequest;
    }
}
