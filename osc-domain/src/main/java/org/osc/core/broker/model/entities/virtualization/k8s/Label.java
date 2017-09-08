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
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import org.osc.core.broker.model.entities.BaseEntity;
import org.osc.core.broker.model.entities.virtualization.ProtectionEntity;
import org.osc.core.broker.model.entities.virtualization.SecurityGroupMember;
import org.osc.core.broker.model.entities.virtualization.SecurityGroupMemberType;

@SuppressWarnings("serial")
@Entity
@Table(name = "LABEL")
public class Label extends BaseEntity implements ProtectionEntity {
    public Label() {
    }

    @Column(name = "value", nullable = false, unique = true)
    private String value;

    @Column(name = "name", nullable = false, unique = true)
    private String name;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "POD_LABEL",
    joinColumns = @JoinColumn(name = "label_fk", referencedColumnName = "id"),
    inverseJoinColumns = @JoinColumn(name = "pod_fk", referencedColumnName = "id"))
    private Set<Pod> pods = new HashSet<Pod>();

    @OneToMany(mappedBy = "label", fetch = FetchType.LAZY)
    private Set<SecurityGroupMember> securityGroupMembers = new HashSet<>();

    public Label(String name, String labelValue) {
        this.name = name;
        this.value = labelValue;
    }

    @Override
    public SecurityGroupMemberType getType() {
        return SecurityGroupMemberType.LABEL;
    }

    @Override
    public String getName() {
        return this.name;
    }

    public String getValue() {
        return this.value;
    }

    public Set<Pod> getPods() {
        return this.pods;
    }

    @Override
    public Set<SecurityGroupMember> getSecurityGroupMembers() {
        return this.securityGroupMembers;
    }
}
