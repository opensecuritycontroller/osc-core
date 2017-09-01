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
package org.osc.core.broker.model.entities.virtualization;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import org.osc.core.broker.model.entities.BaseEntity;
import org.osc.core.broker.model.entities.appliance.VirtualSystem;

@SuppressWarnings("serial")
@Entity
@Table(name = "SERVICE_FUNCTION_CHAIN", uniqueConstraints = { @UniqueConstraint(columnNames = { "name" }) })
public class ServiceFunctionChain extends BaseEntity {

	@Column(name = "name", nullable = false)
	private String name;

	@ManyToMany(fetch = FetchType.LAZY)
	@JoinTable(name = "SERVICE_FUNCTION_CHAIN_VIRTUAL_SYSTEM", joinColumns = @JoinColumn(name = "sfc_fk", referencedColumnName = "id"), inverseJoinColumns = @JoinColumn(name = "virtual_system_fk", referencedColumnName = "id"))
	private Set<VirtualSystem> virtualSystems = new HashSet<>();

	public ServiceFunctionChain(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Set<VirtualSystem> getVirtualSystems() {
		return virtualSystems;
	}

	public void setVirtualSystems(Set<VirtualSystem> virtualSystems) {
		this.virtualSystems = virtualSystems;
	}

	public void addVirtualSystems(VirtualSystem virtualSystem) {
        this.virtualSystems.add(virtualSystem);
    }

    public void removeSfcVs(VirtualSystem virtualSystem) {
        this.virtualSystems.remove(virtualSystem);
    }

}
