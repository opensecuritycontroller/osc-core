package org.osc.core.server.activator;

import static org.osgi.service.http.whiteboard.HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME;
import static org.osgi.service.http.whiteboard.HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_PATH;
import static org.osgi.service.http.whiteboard.HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT;
import static org.osgi.service.http.whiteboard.HttpWhiteboardConstants.HTTP_WHITEBOARD_RESOURCE_PATTERN;
import static org.osgi.service.http.whiteboard.HttpWhiteboardConstants.HTTP_WHITEBOARD_RESOURCE_PREFIX;
import static org.osgi.service.http.whiteboard.HttpWhiteboardConstants.HTTP_WHITEBOARD_TARGET;

import java.io.IOException;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.http.context.ServletContextHelper;
import org.osgi.service.http.runtime.HttpServiceRuntime;
import org.osgi.service.http.runtime.HttpServiceRuntimeConstants;

import aQute.lib.converter.Converter;

@Component(name = "redirect.servlet.context", service = ServletContextHelper.class, property = {

        HTTP_WHITEBOARD_CONTEXT_NAME + "=" + RedirectServletContext.OSC_REDIRECT_NAME,
        HTTP_WHITEBOARD_CONTEXT_PATH + "=/", HTTP_WHITEBOARD_RESOURCE_PATTERN + "=/*",
        HTTP_WHITEBOARD_RESOURCE_PREFIX + "=/",
        HTTP_WHITEBOARD_CONTEXT_SELECT + "=(" + HTTP_WHITEBOARD_CONTEXT_NAME + "="
                + RedirectServletContext.OSC_REDIRECT_NAME + ")",
        HTTP_WHITEBOARD_TARGET + "=(" + RedirectServletContext.FELIX_HTTP_NAME + "="
                + RedirectServletContext.OSC_REDIRECT_NAME + ")"

})
public class RedirectServletContext extends ServletContextHelper {
    static final String FELIX_HTTP_NAME = "org.apache.felix.http.name";
    static final String OSC_REDIRECT_NAME = "OSC-REDIRECT";

    private String redirect = null;

    @Reference(target = "(" + FELIX_HTTP_NAME + "=" + UiServletContext.OSC_UI_NAME + ")")
    void setHttpRuntime(HttpServiceRuntime runtime, Map<String, Object> config) {
        Object endpoints = config.get(HttpServiceRuntimeConstants.HTTP_SERVICE_ENDPOINT);

        try {
            for (String endpoint : Converter.cnv(String[].class, endpoints)) {
                if (endpoint.startsWith("https:")) {
                    this.redirect = endpoint;
                    trace("redirect endpoint=%s", this.redirect);
                }
            }
        } catch (Exception e) {
        }
    }

    private String getRedirect(String hostPort) {
        if (this.redirect != null && hostPort != null) {
            String host = hostPort.replaceFirst(":.*", "");
            // https://1.2.3.4:8888/
            String loc = this.redirect.replaceFirst("://[^:/]+", "://" + host);
            trace("host redirect=%s", loc);
            return loc;
        }
        return this.redirect;
    }

    @Override
    public boolean handleSecurity(final HttpServletRequest request, final HttpServletResponse response)
            throws IOException {
        String location = getRedirect(request.getHeader("Host"));
        if (location != null) {
            response.sendRedirect(location);
        } else {
            response.sendError(500, "redirect configuration failed.");
        }
        return false;
    }

    private static void trace(String format, Object... args) {
        System.err.printf("TRACE " + RedirectServletContext.class.getSimpleName() + ": " + format + "\n", args);
    }
}
