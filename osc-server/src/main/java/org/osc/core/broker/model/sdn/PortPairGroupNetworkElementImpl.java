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
package org.osc.core.broker.model.sdn;

import java.util.List;

import org.osc.sdk.controller.element.NetworkElement;
/**
 * PortPairGroupNetworkElementImpl contains only element id which is used to compare the manager sfc port group element id.
 */
public class PortPairGroupNetworkElementImpl implements NetworkElement {

    private String elementId;

    public PortPairGroupNetworkElementImpl(String elementId) {
        this.elementId = elementId;
    }

	@Override
	public String getElementId() {
		return this.elementId;
	}

	@Override
	public String getParentId() {
		return null;
	}

	@Override
	public List<String> getMacAddresses() {
		return null;
	}

	@Override
	public List<String> getPortIPs() {
		return null;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((this.elementId == null) ? 0 : this.elementId.hashCode());
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
		PortPairGroupNetworkElementImpl other = (PortPairGroupNetworkElementImpl) obj;
		if (this.elementId == null) {
			if (other.elementId != null) {
				return false;
			}
		} else if (!this.elementId.equals(other.elementId)) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		return "PortPairGroupNetworkElementImpl [elementId=" + this.elementId + "]";
	}
}
