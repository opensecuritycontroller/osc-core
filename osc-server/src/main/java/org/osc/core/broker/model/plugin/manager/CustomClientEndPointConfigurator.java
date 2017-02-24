package org.osc.core.broker.model.plugin.manager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.websocket.ClientEndpointConfig.Configurator;

public class CustomClientEndPointConfigurator extends Configurator {

    private String cookie;

    public CustomClientEndPointConfigurator(String cookie) {
        super();
        this.cookie = cookie;
    }

    @Override
    public void beforeRequest(Map<String, List<String>> headers) {
        super.beforeRequest(headers);
        List<String> parameterList = headers.get("Cookie");
        if (parameterList == null) {
            parameterList = new ArrayList<>();
        }
        if (this.cookie != null) {
            parameterList.add(this.cookie);
            headers.put("Cookie", parameterList);
        }
    }

    public void setCookie(String cookie) {
        this.cookie = cookie;
    }

}
