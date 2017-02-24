package org.osc.core.broker.service.request;

public class DeleteSslEntryRequest implements Request {

    private String alias;

    public DeleteSslEntryRequest() {
    }

    public DeleteSslEntryRequest(String alias) {
        this.alias = alias;
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }
}
