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
import static org.osgi.service.http.whiteboard.HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_PATH;
import static org.osgi.service.http.whiteboard.HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT;
import static org.osgi.service.http.whiteboard.HttpWhiteboardConstants.HTTP_WHITEBOARD_RESOURCE_PATTERN;
import static org.osgi.service.http.whiteboard.HttpWhiteboardConstants.HTTP_WHITEBOARD_RESOURCE_PREFIX;
import static org.osgi.service.http.whiteboard.HttpWhiteboardConstants.HTTP_WHITEBOARD_TARGET;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.http.context.ServletContextHelper;

@Component(name = "api.servlet.context", service = ServletContextHelper.class, property = {

        HTTP_WHITEBOARD_CONTEXT_NAME + "=" + ApiServletContext.OSC_API_NAME, HTTP_WHITEBOARD_CONTEXT_PATH + "=/",
        HTTP_WHITEBOARD_RESOURCE_PATTERN + "=/api-doc/*", HTTP_WHITEBOARD_RESOURCE_PATTERN + "=/ovf/*",
        HTTP_WHITEBOARD_RESOURCE_PREFIX + "=" + ApiServletContext.OSC_RESOURCE_PREFIX,
        HTTP_WHITEBOARD_CONTEXT_SELECT + "=(" + HTTP_WHITEBOARD_CONTEXT_NAME + "=" + ApiServletContext.OSC_API_NAME
                + ")",
        HTTP_WHITEBOARD_TARGET + "=(" + ApiServletContext.FELIX_HTTP_NAME + "=" + ApiServletContext.OSC_API_NAME + ")"

})
public class ApiServletContext extends ServletContextHelper {
    static final String FELIX_HTTP_NAME = "org.apache.felix.http.name";
    static final String OSC_API_NAME = "OSC-API";
    static final String OSC_RESOURCE_PREFIX = "/webapp";

    private static final String[] resources = { "/api-doc", "/ovf" };

    private URI base;

    @Activate
    void activate() {
        this.base = new File("").toURI();
    }

    @Override
    public boolean handleSecurity(final HttpServletRequest request, final HttpServletResponse response)
            throws IOException {
        final String resource = request.getRequestURI();
        if (!resource.endsWith("/")) {
            URI resolve = this.base.resolve(OSC_RESOURCE_PREFIX.substring(1) + resource);
            if (new File(resolve).isDirectory()) {
                response.sendRedirect(resource + "/");
                return false;
            }
        }
        return true;
    }

    @Override
    public URL getResource(String name) {
        name = name.replaceFirst(OSC_RESOURCE_PREFIX, "");
        if (name.equals("/")) {
            name += "index.html";
        }

        try {
            for (String resource : resources) {
                URI resolve = this.base.resolve(OSC_RESOURCE_PREFIX.substring(1) + resource + name);
                //                trace("resolve: %s", resolve);
                if (new File(resolve).isFile()) {
                    return resolve.toURL();
                }
            }
        } catch (MalformedURLException e) {
        }

        return null;
    }

}
