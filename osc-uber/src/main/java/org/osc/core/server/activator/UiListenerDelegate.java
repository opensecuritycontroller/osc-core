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

import static org.osgi.service.http.whiteboard.HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME;
import static org.osgi.service.http.whiteboard.HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT;
import static org.osgi.service.http.whiteboard.HttpWhiteboardConstants.HTTP_WHITEBOARD_TARGET;

import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

import org.atmosphere.cpr.SessionSupport;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.mcafee.vmidc.server.Server;

@Component(property = {
        HTTP_WHITEBOARD_CONTEXT_SELECT + "=(" + HTTP_WHITEBOARD_CONTEXT_NAME + "=" + UiServletContext.OSC_UI_NAME + ")",
        HTTP_WHITEBOARD_TARGET + "=(" + UiServletContext.FELIX_HTTP_NAME + "=" + UiServletContext.OSC_UI_NAME + ")"

})
public class UiListenerDelegate implements HttpSessionListener {

    private HttpSessionListener delegate;

    @Reference
    Server server;

    @Activate
    void activate() {
        this.delegate = new SessionSupport();
    }

    @Override
    public void sessionCreated(HttpSessionEvent se) {
        this.delegate.sessionCreated(se);
        this.server.addSession(se.getSession());
    }

    @Override
    public void sessionDestroyed(HttpSessionEvent se) {
        this.delegate.sessionDestroyed(se);
        this.server.removeSession(se.getSession());
    }

}
