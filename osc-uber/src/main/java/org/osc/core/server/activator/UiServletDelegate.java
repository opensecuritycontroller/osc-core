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
import static org.osgi.service.http.whiteboard.HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_ASYNC_SUPPORTED;
import static org.osgi.service.http.whiteboard.HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_NAME;
import static org.osgi.service.http.whiteboard.HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN;
import static org.osgi.service.http.whiteboard.HttpWhiteboardConstants.HTTP_WHITEBOARD_TARGET;

import java.io.IOException;

import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;

@Component(name = "ui.servlet", property = {

		HTTP_WHITEBOARD_SERVLET_NAME + "=" + UiServletContext.OSC_UI_NAME, HTTP_WHITEBOARD_SERVLET_PATTERN + "=/*",
		HTTP_WHITEBOARD_CONTEXT_SELECT + "=(" + HTTP_WHITEBOARD_CONTEXT_NAME + "=" + UiServletContext.OSC_UI_NAME + ")",
		HTTP_WHITEBOARD_TARGET + "=(" + UiServletContext.FELIX_HTTP_NAME + "=" + UiServletContext.OSC_UI_NAME + ")",
		HTTP_WHITEBOARD_SERVLET_ASYNC_SUPPORTED + "=true"

})
public class UiServletDelegate implements Servlet {
	private Servlet delegate;

	@Activate
	void activate() {
		this.delegate = new com.vaadin.server.VaadinServlet();
	}

	@Override
	public void destroy() {
		this.delegate.destroy();
	}

	@Override
	public ServletConfig getServletConfig() {
		return this.delegate.getServletConfig();
	}

	@Override
	public String getServletInfo() {
		return this.delegate.getServletInfo();
	}

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
			this.delegate.init(config);
		} finally {
			Thread.currentThread().setContextClassLoader(oldLoader);
		}
	}

	@Override
	public void service(ServletRequest request, ServletResponse response) throws ServletException, IOException {
		this.delegate.service(request, response);
	}

}
