package org.osc.core.server.activator;

import static org.osgi.service.http.whiteboard.HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME;
import static org.osgi.service.http.whiteboard.HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT;
import static org.osgi.service.http.whiteboard.HttpWhiteboardConstants.HTTP_WHITEBOARD_TARGET;

import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

import org.atmosphere.cpr.SessionSupport;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;

import com.mcafee.vmidc.server.Server;

@Component(property = {
        HTTP_WHITEBOARD_CONTEXT_SELECT + "=(" + HTTP_WHITEBOARD_CONTEXT_NAME + "=" + UiServletContext.OSC_UI_NAME + ")",
        HTTP_WHITEBOARD_TARGET + "=(" + UiServletContext.FELIX_HTTP_NAME + "=" + UiServletContext.OSC_UI_NAME + ")"

})
public class UiListenerDelegate implements HttpSessionListener {

    private HttpSessionListener delegate;

    @Activate
    void activate() {
        this.delegate = new SessionSupport();
    }

    @Override
    public void sessionCreated(HttpSessionEvent se) {
        this.delegate.sessionCreated(se);
        Server.addSession(se.getSession());
    }

    @Override
    public void sessionDestroyed(HttpSessionEvent se) {
        this.delegate.sessionDestroyed(se);
        Server.removeSession(se.getSession());
    }

}
