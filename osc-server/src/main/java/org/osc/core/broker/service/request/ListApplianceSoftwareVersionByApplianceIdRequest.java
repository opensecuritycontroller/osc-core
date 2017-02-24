package org.osc.core.broker.service.request;



public class ListApplianceSoftwareVersionByApplianceIdRequest implements Request {
    private long applianceId;

    /**
     * @return the applianceId
     */
    public long getApplianceId() {
        return applianceId;
    }

    /**
     * @param applianceId
     *            the applianceId to set
     */
    public void setApplianceId(long applianceId) {
        this.applianceId = applianceId;
    }

}
