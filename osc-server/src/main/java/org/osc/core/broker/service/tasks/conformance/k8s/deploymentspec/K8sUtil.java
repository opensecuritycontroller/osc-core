package org.osc.core.broker.service.tasks.conformance.k8s.deploymentspec;

import org.osc.core.broker.model.entities.virtualization.openstack.DeploymentSpec;

class K8sUtil {

    static String getK8sName(DeploymentSpec ds) {
        return ds.getName() + "_" + ds.getId();
    }
}
