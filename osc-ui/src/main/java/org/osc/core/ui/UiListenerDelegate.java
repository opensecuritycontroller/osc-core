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

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

import org.atmosphere.cpr.SessionSupport;
import org.osc.core.broker.service.api.server.ServerTerminationListener;
import org.osc.core.broker.service.broadcast.BroadcastListener;
import org.osc.core.broker.service.broadcast.BroadcastMessage;
import org.osc.core.broker.service.broadcast.EventType;
import org.osc.core.broker.service.dto.UserDto;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;

import com.vaadin.server.VaadinSession;

@Component(property = {
        HTTP_WHITEBOARD_CONTEXT_SELECT + "=(" + HTTP_WHITEBOARD_CONTEXT_NAME + "=" + UiServletContext.OSC_UI_NAME + ")",
        HTTP_WHITEBOARD_TARGET + "=(" + UiServletContext.FELIX_HTTP_NAME + "=" + UiServletContext.OSC_UI_NAME + ")"

})
public class UiListenerDelegate implements HttpSessionListener, ServerTerminationListener,
    BroadcastListener {

    private HttpSessionListener delegate;

    private final List<HttpSession> sessions = new CopyOnWriteArrayList<>();

    @Activate
    void activate() {
        this.delegate = new SessionSupport();
    }

    @Override
    public void sessionCreated(HttpSessionEvent se) {
        this.delegate.sessionCreated(se);
        this.sessions.add(se.getSession());
    }

    @Override
    public void sessionDestroyed(HttpSessionEvent se) {
        this.delegate.sessionDestroyed(se);
        this.sessions.remove(se.getSession());
    }

    @Override
    public void serverStopping() {
        // When the server is stopping we invalidate all sessions
        closeUserVaadinSessions(null);
    }

    // Close vaadin sessions associated to the given loginName,
    // or all sessions if loginName is null.

    private void closeUserVaadinSessions(String loginName) {
        // CopyOnWriteArrayList is thread safe for iteration under update
        for (HttpSession session : this.sessions) {
            for (VaadinSession vaadinSession : VaadinSession.getAllSessions(session)) {
                Object userName = vaadinSession.getAttribute("user");
                if (loginName == null || loginName.equals(userName)) {
                    vaadinSession.close();
                }
            }
        }
    }

    @Override
    public void receiveBroadcast(BroadcastMessage msg) {
        // If a user is deleted then all the sessions associated with
        // that user should be ended.
        if (msg.getEventType() == EventType.DELETED &&
                msg.getDto() instanceof UserDto) {
            closeUserVaadinSessions(((UserDto) msg.getDto()).getLoginName());
        }
    }
}
