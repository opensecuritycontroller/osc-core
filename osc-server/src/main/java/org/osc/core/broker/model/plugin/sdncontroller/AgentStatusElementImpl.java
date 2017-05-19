/*******************************************************************************
 * Copyright (c) Intel Corporation
 * Copyright (c) 2017
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package org.osc.core.broker.model.plugin.sdncontroller;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.osc.sdk.sdn.element.AgentStatusElement;

public final class AgentStatusElementImpl implements AgentStatusElement {
    private String status;
    private String errorDescription;

    public AgentStatusElementImpl(String status, String errorDescription) {
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

        AgentStatusElementImpl other = (AgentStatusElementImpl) object;

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