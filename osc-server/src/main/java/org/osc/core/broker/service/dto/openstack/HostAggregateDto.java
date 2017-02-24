package org.osc.core.broker.service.dto.openstack;

import org.osc.core.broker.service.dto.BaseDto;

public class HostAggregateDto extends BaseDto {

    private String name;
    private String openstackId;

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getOpenstackId() {
        return this.openstackId;
    }

    public void setOpenstackId(String openstackId) {
        this.openstackId = openstackId;
    }

    @Override
    public String toString() {
        return "HostAggregateDto [name=" + name + ", openstackId=" + openstackId + "]";
    }


}