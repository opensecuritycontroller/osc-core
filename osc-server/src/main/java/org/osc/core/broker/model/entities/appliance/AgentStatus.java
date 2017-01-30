package org.osc.core.broker.model.entities.appliance;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.osc.sdk.sdn.element.AgentStatusElement;

public final class AgentStatus implements AgentStatusElement {
    private String status;
    private String errorDescription;

    public AgentStatus(String status, String errorDescription) {
        this.status = status;
        this.errorDescription = errorDescription;
    }

    @Override
    public String getStatus() {
        return this.status;
    }

    @Override
    public String getErrorDescription() {
        return this.errorDescription;
    }

    @Override
    public boolean equals(Object object) {
        if (object == null) {
            return false;
        }
        if (getClass() != object.getClass()) {
            return false;
        }
        if (this == object) {
            return true;
        }

        AgentStatus other = (AgentStatus) object;

        return new EqualsBuilder()
                .append(getStatus(), other.getStatus())
                .append(getErrorDescription(), other.getErrorDescription())
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(getStatus())
                .append(getErrorDescription())
                .toHashCode();
    }
}
