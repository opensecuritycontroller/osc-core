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

import static org.osgi.service.http.whiteboard.HttpWhiteboardConstants.*;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.http.context.ServletContextHelper;
import org.osgi.service.http.runtime.HttpServiceRuntime;
import org.osgi.service.http.runtime.HttpServiceRuntimeConstants;

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

        String[] endpointStrings = getEndpointArray(endpoints);

        try {
            for (String endpoint : endpointStrings) {
                if (endpoint.startsWith("https:")) {
                    this.redirect = endpoint;
                    trace("redirect endpoint=%s", this.redirect);
                }
            }
        } catch (Exception e) {
        }
    }

    private String[] getEndpointArray(Object endpoints) {
        String[] endpointStrings;

        if(endpoints instanceof String) {
            endpointStrings = new String[] {endpoints.toString()};
        } else if (endpoints instanceof String[]) {
            endpointStrings = (String[]) endpoints;
        } else if (endpoints instanceof Collection) {
            endpointStrings = ((Collection<?>) endpoints).stream()
                    .map(String::valueOf)
                    .toArray(String[]::new);
        } else {
            endpointStrings = new String[0];
        }
        return endpointStrings;
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
