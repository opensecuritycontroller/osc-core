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

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.Arrays;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.http.context.ServletContextHelper;
import org.osgi.util.tracker.BundleTracker;
import org.osgi.util.tracker.BundleTrackerCustomizer;

@Component(name = "ui.servlet.context", service = ServletContextHelper.class, property = {

        HTTP_WHITEBOARD_CONTEXT_NAME + "=" + UiServletContext.OSC_UI_NAME, HTTP_WHITEBOARD_CONTEXT_PATH + "=/",
        HTTP_WHITEBOARD_RESOURCE_PATTERN + "=/WebHelp/*", HTTP_WHITEBOARD_RESOURCE_PATTERN + "=/SDK/*",
        HTTP_WHITEBOARD_RESOURCE_PREFIX + "=" + UiServletContext.OSC_RESOURCE_PREFIX,
        HTTP_WHITEBOARD_CONTEXT_SELECT + "=(" + HTTP_WHITEBOARD_CONTEXT_NAME + "=" + UiServletContext.OSC_UI_NAME + ")",
        HTTP_WHITEBOARD_TARGET + "=(" + UiServletContext.FELIX_HTTP_NAME + "=" + UiServletContext.OSC_UI_NAME + ")"

})
public class UiServletContext extends ServletContextHelper {
    static final String FELIX_HTTP_NAME = "org.apache.felix.http.name";
    static final String OSC_UI_NAME = "OSC-UI";
    static final String OSC_RESOURCE_PREFIX = "/webapp";
    static final String OSC_VAADIN_PREFIX = "/VAADIN/";

    private static final String[] resources = { "", "/WebHelp", "/SDK" };

    private URI base;

    private BundleTracker<Bundle> vaadinResourceBundles;

    @Activate
    void activate(BundleContext ctx) {
        this.base = new File("").toURI();
        // Find the resolved vaadin bundle
        this.vaadinResourceBundles = new BundleTracker<Bundle>(ctx, Bundle.RESOLVED |
                Bundle.STARTING | Bundle.ACTIVE | Bundle.STOPPING,
                    new BundleTrackerCustomizer<Bundle>() {
                        @Override
                        public Bundle addingBundle(Bundle bundle, BundleEvent event) {
                            return "com.vaadin.server".equals(bundle.getSymbolicName()) ?
                                    bundle : null;
                        }

                        @Override
                        public void modifiedBundle(Bundle bundle, BundleEvent event, Bundle object) {

                        }

                        @Override
                        public void removedBundle(Bundle bundle, BundleEvent event, Bundle object) {
                        }
                });
        this.vaadinResourceBundles.open();
    }

    @Deactivate
    void deactivate() {
        this.vaadinResourceBundles.close();
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

    /*
     * Resource requests have name="/webapp/xxx".
     * Vaadin requests have name="/VAADIN/xxx".
     */
    @Override
    public URL getResource(String name) {
        name = name.replaceFirst(OSC_RESOURCE_PREFIX, "");
        if (name.equals("/")) {
            name += "index.html";
        }

        try {
            for (String resource : resources) {
                URI resolve = this.base.resolve(OSC_RESOURCE_PREFIX.substring(1) + resource + name);
                if (new File(resolve).isFile()) {
                    return resolve.toURL();
                }
            }
        } catch (MalformedURLException e) {
        }

        if(name.startsWith(OSC_VAADIN_PREFIX)) {
            String bundleResourceLocation = name.substring(OSC_VAADIN_PREFIX.length() -1);

            Bundle[] bundlesToCheck = this.vaadinResourceBundles.getBundles();
            if(bundlesToCheck != null) {
                // Sort the list to ensure a consistent check order
                Arrays.sort(bundlesToCheck);
                for (Bundle b : bundlesToCheck) {
                    URL resource = b.getResource(name);
                    if(resource != null) {
                        return resource;
                    }
                }
            }
        }

        return null;
    }

}
