package org.osc.core.broker.model.entities.virtualization.openstack;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.hibernate.annotations.ForeignKey;
import org.osc.core.broker.model.entities.BaseEntity;

@Entity
@Table(name = "AVAILABILITY_ZONE")
public class AvailabilityZone extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @Column(name = "region", nullable = false)
    private String region;

    @Column(name = "zone", nullable = false)
    private String zone;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "ds_fk", nullable = false)
    @ForeignKey(name = "FK_DS")
    private DeploymentSpec deploymentSpec;

    public AvailabilityZone(DeploymentSpec deploymentSpec, String region, String zone) {
        this.deploymentSpec = deploymentSpec;
        this.region = region;
        this.zone = zone;
    }

    public AvailabilityZone() {

    }

    public String getRegion() {
        return this.region;
    }

    void setRegion(String region) {
        this.region = region;
    }

    public String getZone() {
        return this.zone;
    }

    void setZone(String zone) {
        this.zone = zone;
    }

    public DeploymentSpec getDeploymentSpec() {
        return this.deploymentSpec;
    }

    void setDeploymentSpec(DeploymentSpec deploymentSpec) {
        this.deploymentSpec = deploymentSpec;
    }

}
