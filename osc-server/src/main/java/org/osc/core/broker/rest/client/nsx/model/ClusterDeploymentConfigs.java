package org.osc.core.broker.rest.client.nsx.model;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class ClusterDeploymentConfigs {

    public static class ServiceDeploymentConfig {
        public String serviceInstanceId;
        public String dvPortGroup;
        public String ipPool;
        
        public ServiceDeploymentConfig() {
        }
        
        public ServiceDeploymentConfig(String serviceInstanceId, String dvPortGroup, String ipPool) {
            this.serviceInstanceId = serviceInstanceId;
            this.dvPortGroup = dvPortGroup;
            this.ipPool = ipPool;
        }

        @Override
        public String toString() {
            return "ServiceDeploymentConfig [serviceInstanceId=" + serviceInstanceId + ", dvPortGroup=" + dvPortGroup
                    + ", ipPool=" + ipPool + "]";
        }
    }

    public static class Services {

        public List<ServiceDeploymentConfig> serviceDeploymentConfig = new ArrayList<ServiceDeploymentConfig>();

        @Override
        public String toString() {
            return "Services [serviceDeploymentConfig=" + serviceDeploymentConfig + "]";
        }
    }

    public static class ClusterDeploymentConfig {
        public String clusterId;
        public String datastore;
        public Services services = new Services();

        @Override
        public String toString() {
            return "ClusterDeploymentConfig [clusterId=" + clusterId + ", datastore=" + datastore + ", services="
                    + services + "]";
        }

    }

    public List<ClusterDeploymentConfig> clusterDeploymentConfig = new ArrayList<ClusterDeploymentConfig>();

    @Override
    public String toString() {
        return "ClusterDeploymentConfigs [clusterDeploymentConfig=" + clusterDeploymentConfig + "]";
    }
}
