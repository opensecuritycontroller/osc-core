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
package org.osc.core.broker.service.request;

import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class DeploymentScope {
    protected Long id;
    protected Long revision;
    protected List<Cluster> clusters;
    protected List<String> dataNetworks;

    public Long getId() {
        return id;
    }

    public void setId(Long value) {
        this.id = value;
    }

    public Long getRevision() {
        return revision;
    }

    public void setRevision(Long value) {
        this.revision = value;
    }

    public List<Cluster> getClusters() {
        return clusters;
    }

    public void setClusters(List<Cluster> value) {
        this.clusters = value;
    }

    public List<String> getDataNetworks() {
        return dataNetworks;
    }

    public void setDataNetworks(List<String> value) {
        this.dataNetworks = value;
    }

    @XmlRootElement
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class Cluster {
        protected String string;

        public String getString() {
            return string;
        }

        public void setString(String value) {
            this.string = value;
        }

        @Override
        public String toString() {
            return "Cluster [string=" + string + "]";
        }
    }

    @XmlRootElement
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class DataNetwork {
        protected String string;

        public String getString() {
            return string;
        }

        public void setString(String value) {
            this.string = value;
        }

        @Override
        public String toString() {
            return "DataNetwork [string=" + string + "]";
        }
    }

    @Override
    public String toString() {
        return "DeploymentScope [id=" + id + ", revision=" + revision + ", clusters=" + clusters + ", dataNetworks="
                + dataNetworks + "]";
    }

}
