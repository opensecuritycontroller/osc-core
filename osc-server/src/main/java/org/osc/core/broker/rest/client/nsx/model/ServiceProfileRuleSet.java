package org.osc.core.broker.rest.client.nsx.model;

public class ServiceProfileRuleSet {
    private String serviceProfileRuleSetId;
    private ServiceProfileRuleSetRule serviceProfileRuleSetRule = new ServiceProfileRuleSetRule();

    public String getServiceProfileRuleSetId() {
        return serviceProfileRuleSetId;
    }

    public void setServiceProfileRuleSetId(String serviceProfileRuleSetId) {
        this.serviceProfileRuleSetId = serviceProfileRuleSetId;
    }

    public ServiceProfileRuleSetRule getServiceProfileRuleSetRule() {
        return serviceProfileRuleSetRule;
    }

    public void setServiceProfileRuleSetRule(ServiceProfileRuleSetRule serviceProfileRuleSetRule) {
        this.serviceProfileRuleSetRule = serviceProfileRuleSetRule;
    }

}
