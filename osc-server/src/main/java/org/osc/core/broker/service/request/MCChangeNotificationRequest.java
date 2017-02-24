package org.osc.core.broker.service.request;

import org.osc.sdk.manager.element.MgrChangeNotification;

public class MCChangeNotificationRequest implements Request {

    public String mgrIpAddress;
    public MgrChangeNotification notification;
}
