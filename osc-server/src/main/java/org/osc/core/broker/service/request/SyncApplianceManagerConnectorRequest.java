package org.osc.core.broker.service.request;

public class SyncApplianceManagerConnectorRequest implements Request {
    private Long id;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
}
