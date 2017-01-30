package org.osc.core.broker.service.openstack.request;

import org.osc.core.broker.service.request.BaseIdRequest;

public class BaseOpenStackRequest extends BaseIdRequest {

    private String tenantName;
    private String tenantId;
    private String region;

    public String getTenantName() {
        return this.tenantName;
    }

    public void setTenantName(String tenant) {
        this.tenantName = tenant;
    }

    public String getRegion() {
        return this.region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getTenantId() {
        return this.tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

}
