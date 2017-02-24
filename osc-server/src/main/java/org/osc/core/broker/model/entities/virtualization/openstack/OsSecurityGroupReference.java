/*******************************************************************************
 * Copyright (c) 2017 Intel Corporation
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
package org.osc.core.broker.model.entities.virtualization.openstack;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import org.osc.core.broker.model.entities.BaseEntity;

@Entity
@Table(name = "OS_SECURITY_GROUP_REFERENCE")
public class OsSecurityGroupReference extends BaseEntity {

	private static final long serialVersionUID = 1L;

	@Column(name = "sg_ref_id", nullable = false, unique = true)
	private String sgRefId;

	@Column(name = "sg_ref_name", nullable = false)
	private String sgRefName;

    @OneToMany(mappedBy = "osSecurityGroupReference", fetch = FetchType.LAZY)
	private Set<DeploymentSpec> deploymentSpecs = new HashSet<DeploymentSpec>();

	OsSecurityGroupReference() {
	}

	public OsSecurityGroupReference(String sgRefId, String sgRefName, DeploymentSpec ds) {
		this.sgRefId = sgRefId;
		this.sgRefName = sgRefName;
		this.deploymentSpecs.add(ds);
	}

	public String getSgRefId() {
		return this.sgRefId;
	}

	public void setSgRefId(String sgRefId) {
		this.sgRefId = sgRefId;
	}

	public String getSgRefName() {
		return this.sgRefName;
	}

	public void setSgRefName(String sgRefName) {
		this.sgRefName = sgRefName;
	}

	public Set<DeploymentSpec> getDeploymentSpecs() {
		return this.deploymentSpecs;
	}

	public void setDeploymentSpecs(Set<DeploymentSpec> deploymentSpecs) {
		this.deploymentSpecs = deploymentSpecs;
	}

}