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

import org.glassfish.jersey.servlet.ServletContainer;
import org.osc.core.broker.rest.server.OscRestServlet;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;


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
        this.container = new ServletContainer(new OscRestServlet());
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