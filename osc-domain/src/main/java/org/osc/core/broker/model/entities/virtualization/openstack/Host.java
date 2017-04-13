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
package org.osc.core.broker.model.entities.virtualization.openstack;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ForeignKey;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.osc.core.broker.model.entities.BaseEntity;

@Entity
@Table(name = "HOST")
public class Host extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @Column(name = "openstack_id", nullable = false)
    private String openstackId;

    @Column(name = "name", nullable = false)
    private String name;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "ds_host_fk", nullable = false,
            foreignKey = @ForeignKey(name = "FK_HOST_DS"))
    private DeploymentSpec deploymentSpec;

    public Host(DeploymentSpec deploymentSpec, String openstackId) {
        this.deploymentSpec = deploymentSpec;
        this.openstackId = openstackId;
    }

    public Host() {

    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
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

    public void setOpenstackId(String openstackId) {
        this.openstackId = openstackId;
    }

}
