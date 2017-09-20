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
package org.osc.core.broker.rest.client.k8s;

public class KubernetesPod extends KubernetesEntity {
    private String namespace;
    private String node;

    public KubernetesPod(String name, String namespace, String uid, String node) {
        super(name, uid);
        this.node = node;
        this.namespace = namespace;
    }

    public String getNode() {
        return this.node;
    }

    public String getNamespace() {
        return this.namespace;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((this.namespace == null) ? 0 : this.namespace.hashCode());
        result = prime * result + ((this.node == null) ? 0 : this.node.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!super.equals(obj)) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        KubernetesPod other = (KubernetesPod) obj;
        if (this.namespace == null) {
            if (other.namespace != null) {
                return false;
            }
        } else if (!this.namespace.equals(other.namespace)) {
            return false;
        }
        if (this.node == null) {
            if (other.node != null) {
                return false;
            }
        } else if (!this.node.equals(other.node)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "KubernetesPod [namespace=" + this.namespace + ", node=" + this.node + ", getName()=" + getName() + ", getUid()="
                + getUid() + "]";
    }
}


