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