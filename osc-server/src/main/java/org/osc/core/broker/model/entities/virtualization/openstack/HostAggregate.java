package org.osc.core.broker.model.entities.virtualization.openstack;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.hibernate.annotations.ForeignKey;
import org.osc.core.broker.model.entities.BaseEntity;

@SuppressWarnings("serial")
@Entity
@Table(name = "HOST_AGGREGATE")
public class HostAggregate extends BaseEntity {

    @Column(name = "openstack_id", nullable = false)
    private String openstackId;

    @Column(name = "name", nullable = false)
    private String name;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "ds_fk", nullable = false)
    @ForeignKey(name = "FK_HOST_AGGREGATE_DS")
    private DeploymentSpec deploymentSpec;

    public HostAggregate(DeploymentSpec deploymentSpec, String openstackId) {
        this.deploymentSpec = deploymentSpec;
        this.openstackId = openstackId;
    }

    public HostAggregate() {

    }

    public DeploymentSpec getDeploymentSpec() {
        return this.deploymentSpec;
    }

    void setDeploymentSpec(DeploymentSpec deploymentSpec) {
        this.deploymentSpec = deploymentSpec;
    }

    public String getOpenstackId() {
        return this.openstackId;
    }

    void setOpenstackId(String openstackId) {
        this.openstackId = openstackId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

}
