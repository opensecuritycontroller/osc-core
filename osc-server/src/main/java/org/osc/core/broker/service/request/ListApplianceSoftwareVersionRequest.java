package org.osc.core.broker.service.request;

public class ListApplianceSoftwareVersionRequest implements Request {
    private long applianceId;

    public ListApplianceSoftwareVersionRequest() {
    }

    public ListApplianceSoftwareVersionRequest(long applianceId) {
        this.applianceId = applianceId;
    }

    public long getApplianceId() {
        return applianceId;
    }

    public void setApplianceId(long applianceId) {
        this.applianceId = applianceId;
    }
}
