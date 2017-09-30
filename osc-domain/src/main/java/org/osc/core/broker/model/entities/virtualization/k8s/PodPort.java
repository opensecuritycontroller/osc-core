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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ForeignKey;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.osc.core.broker.model.entities.BaseEntity;
import org.osc.core.broker.model.entities.appliance.DistributedApplianceInstance;
import org.osc.core.broker.model.entities.virtualization.VirtualPort;

@SuppressWarnings("serial")
@Entity
@Table(name = "POD_PORT")
public class PodPort extends BaseEntity implements VirtualPort {
    public PodPort(String externalId, String macAddress, String ipAddress, String parentId) {
        this.externalId = externalId;
        this.macAddress = macAddress;
        this.ipAddresses.add(ipAddress);
        this.parentId = parentId;
    }

    PodPort() {
    }

    @Column(name = "external_id", nullable = false, unique = true)
    private String externalId;

    @Column(name = "mac_address", nullable = false, unique = true)
    private String macAddress;

    @ElementCollection(fetch = FetchType.LAZY)
    @Column(name = "ip_address")
    @CollectionTable(name = "POD_PORT_IP_ADDRESS", joinColumns = @JoinColumn(name = "pod_port_fk"),
    foreignKey=@ForeignKey(name = "FK_POD_PORT_IP_ADDRESS"))
    private List<String> ipAddresses = new ArrayList<String>();

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "pod_fk", foreignKey = @ForeignKey(name = "FK_PODP_POD"))
    private Pod pod;

    @ManyToMany(fetch = FetchType.LAZY, mappedBy = "protectedPodPorts")
    private Set<DistributedApplianceInstance> dais = new HashSet<DistributedApplianceInstance>();

    @Column(name = "parent_id", nullable = true, unique = false)
    private String parentId;

    public String getExternalId() {
        return this.externalId;
    }

    public String getParentId() {
        return this.parentId;
    }

    public String getMacAddress() {
        return this.macAddress;
    }

    public List<String> getIpAddresses() {
        return this.ipAddresses;
    }

    public Pod getPod() {
        return this.pod;
    }

    public void setPod(Pod pod) {
        this.pod = pod;
    }

    public void setParentId(String parentId) {
        this.parentId = parentId;
    }

    @Override
    public Set<DistributedApplianceInstance> getDais() {
        return this.dais;
    }

    @Override
    public void addDai(DistributedApplianceInstance dai) {
        this.dais.add(dai);
    }

    @Override
    public void removeDai(DistributedApplianceInstance dai) {
        dai.removeProtectedPodPort(this);
        this.dais.remove(dai);
    }

    @Override
    public void removeAllDais() {
        for (DistributedApplianceInstance dai : this.dais) {
            dai.removeProtectedPodPort(this);
        }
        this.dais.clear();
    }
}