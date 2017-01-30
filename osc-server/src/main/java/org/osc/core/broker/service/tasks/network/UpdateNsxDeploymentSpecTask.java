package org.osc.core.broker.service.tasks.network;

import java.util.List;
import java.util.Set;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.model.entities.appliance.VirtualSystem;
import org.osc.core.broker.model.plugin.sdncontroller.VMwareSdnApiFactory;
import org.osc.core.broker.rest.client.nsx.model.VersionedDeploymentSpec;
import org.osc.core.broker.service.tasks.TransactionalTask;
import org.osc.core.broker.service.tasks.conformance.virtualsystem.RegisterDeploymentSpecTask;
import org.osc.sdk.sdn.api.DeploymentSpecApi;
import org.osc.sdk.sdn.element.DeploymentSpecElement;

public class UpdateNsxDeploymentSpecTask extends TransactionalTask {

    final Logger log = Logger.getLogger(UpdateNsxDeploymentSpecTask.class);

    private final VirtualSystem vs;
    private VersionedDeploymentSpec deploySpec;

    public UpdateNsxDeploymentSpecTask(VirtualSystem vs) {
        this.vs = vs;
        this.name = getName();
    }

    public UpdateNsxDeploymentSpecTask(VirtualSystem vs, VersionedDeploymentSpec deploySpec) {
        this(vs);
        this.deploySpec = deploySpec;
    }

    @Override
    public void executeTransaction(Session session) throws Exception {
        DeploymentSpecApi deploymentSpecApi = VMwareSdnApiFactory.createDeploymentSpecApi(this.vs);
        String imageName = this.vs.getApplianceSoftwareVersion().getImageUrl();
        if (this.deploySpec != null){
            this.deploySpec.setOvfUrl(RegisterDeploymentSpecTask.generateOvfUrl(imageName));
            deploymentSpecApi.updateDeploymentSpec(this.vs.getNsxServiceId(), this.deploySpec);
        } else {
            List<DeploymentSpecElement> deploymentSpecs = deploymentSpecApi.getDeploymentSpecs(this.vs.getNsxServiceId());
            for (DeploymentSpecElement deploymentSpec: CollectionUtils.emptyIfNull(deploymentSpecs)){
                VersionedDeploymentSpec ds = new VersionedDeploymentSpec(deploymentSpec);
                ds.setOvfUrl(RegisterDeploymentSpecTask.generateOvfUrl(imageName));
                deploymentSpecApi.updateDeploymentSpec(this.vs.getNsxServiceId(), ds);
            }
        }
    }

    @Override
    public String getName() {
        if (this.deploySpec != null) {
            return "Updating Deployment Specification of NSX Manager '" + this.vs.getVirtualizationConnector().getName()
                    + " for host version " + this.deploySpec.getHostVersion().toString() + "'";
        } else {
            return "Updating All Deployment Specifications of NSX Manager '" + this.vs.getVirtualizationConnector().getName()
                    + "'";
        }
    }

    @Override
    public Set<LockObjectReference> getObjects() {
        return LockObjectReference.getObjectReferences(this.vs);
    }

}
