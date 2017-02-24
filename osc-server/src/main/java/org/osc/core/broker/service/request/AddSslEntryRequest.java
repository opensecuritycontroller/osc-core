package org.osc.core.broker.service.request;

public class AddSslEntryRequest implements Request {

    private String alias;
    private String certificate;

    public AddSslEntryRequest() {
    }

    public AddSslEntryRequest(String alias, String certificate) {
        this.certificate = certificate;
        this.alias = alias;
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public String getCertificate() {
        return certificate;
    }

    public void setCertificate(String certificate) {
        this.certificate = certificate;
    }
}