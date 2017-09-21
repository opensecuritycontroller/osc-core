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
package org.osc.core.broker.model.entities.virtualization.k8s;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ManyToMany;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import org.osc.core.broker.model.entities.BaseEntity;

@SuppressWarnings("serial")
@Entity
@Table(name = "POD")
public class Pod extends BaseEntity {
    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "namespace", nullable = false)
    private String namespace;

    @Column(name = "external_id", nullable = false, unique = true)
    private String externalId;

    @Column(name = "node")
    private String node;

    @OneToMany(mappedBy = "pod", fetch = FetchType.LAZY)
    private Set<PodPort> ports = new HashSet<PodPort>();

    @ManyToMany(fetch = FetchType.LAZY, mappedBy = "pods")
    private Set<Label> labels = new HashSet<Label>();

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getNamespace() {
        return this.namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public String getNode() {
        return this.node;
    }

    public void setNode(String node) {
        this.node = node;
    }

    public String getExternalId() {
        return this.externalId;
    }

    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }

    public Set<PodPort> getPorts() {
        return this.ports;
    }

    public Set<Label> getLabels() {
        return this.labels;
    }
}
