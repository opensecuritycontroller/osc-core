package org.osc.core.broker.service.request;

import java.util.Map;

public class UpdateConnectorPluginPropertiesRequest implements Request {
    private final String pluginName;
    private final Map<String, Object> properties;

    public UpdateConnectorPluginPropertiesRequest(String pluginName, Map<String, Object> properties) {
        this.pluginName = pluginName;
        this.properties = properties;
    }

    public String getPluginName() {
        return this.pluginName;
    }

    public Map<String, Object> getProperties() {
        return this.properties;
    }
}
