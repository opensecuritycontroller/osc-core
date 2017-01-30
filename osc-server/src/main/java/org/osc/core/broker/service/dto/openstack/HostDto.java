package org.osc.core.broker.service.dto.openstack;

import org.osc.core.broker.service.dto.BaseDto;

public class HostDto extends BaseDto {

    private String openstackId;
    private String name;

    public HostDto() {
    }

    public HostDto(String openstackId, String name) {
        setOpenstackId(openstackId);
        this.name = name;
    }

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
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((this.openstackId == null) ? 0 : this.openstackId.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        HostDto other = (HostDto) obj;
        if (this.openstackId == null) {
            if (other.openstackId != null) {
                return false;
            }
        } else if (!this.openstackId.equals(other.openstackId)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "HostDto [openstackId=" + this.openstackId + ", name=" + this.name + "]";
    }

}
