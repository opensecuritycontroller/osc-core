package org.osc.core.broker.rest.client.nsx.model;

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
