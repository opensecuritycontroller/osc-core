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
package org.osc.core.broker.service.tasks.conformance.virtualsystem;

import java.util.Set;

import javax.persistence.EntityManager;

import org.apache.log4j.Logger;
import org.osc.core.broker.job.TaskInput;
import org.osc.core.broker.job.TaskOutput;
import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.model.entities.appliance.VirtualSystem;
import org.osc.core.broker.model.entities.appliance.VmwareSoftwareVersion;
import org.osc.core.broker.model.plugin.ApiFactoryService;
import org.osc.core.broker.rest.client.nsx.model.VersionedDeploymentSpec;
import org.osc.core.broker.service.persistence.OSCEntityManager;
import org.osc.core.broker.service.tasks.TransactionalTask;
import org.osc.core.server.Server;
import org.osc.core.util.ServerUtil;
import org.osc.sdk.sdn.api.DeploymentSpecApi;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(service=RegisterDeploymentSpecTask.class)
public class RegisterDeploymentSpecTask extends TransactionalTask {
    private static final Logger LOG = Logger.getLogger(RegisterDeploymentSpecTask.class);
    public static final String ALL_MINOR_VERSIONS = ".*";

    @Reference
    private ApiFactoryService apiFactoryService;

    private VirtualSystem vs;
    private VmwareSoftwareVersion version;

    public RegisterDeploymentSpecTask create(VirtualSystem vs,
            VmwareSoftwareVersion version) {
        RegisterDeploymentSpecTask task = new RegisterDeploymentSpecTask();
        task.vs = vs;
        task.version = version;
        task.name = task.getName();
        task.apiFactoryService = this.apiFactoryService;
        task.dbConnectionManager = this.dbConnectionManager;
        task.txBroadcastUtil = this.txBroadcastUtil;

        return task;
    }

    @TaskInput
    public String svcId;
    @TaskOutput
    public String deploymentSpecId;

    @Override
    public void executeTransaction(EntityManager em) throws Exception {
        LOG.debug("Start executing RegisterDeploymentSpec task for svcId: " + this.svcId);

        this.vs = em.find(VirtualSystem.class, this.vs.getId());
        this.deploymentSpecId = createDeploymentSpec(this.version);
        this.vs.getNsxDeploymentSpecIds().put(this.version,
                this.deploymentSpecId);
        OSCEntityManager.update(em, this.vs, this.txBroadcastUtil);
    }

    private String createDeploymentSpec(VmwareSoftwareVersion softwareVersion)
            throws Exception {
        DeploymentSpecApi deploymentSpecApi = this.apiFactoryService.createDeploymentSpecApi(this.vs);
        VersionedDeploymentSpec deploymentSpec = new VersionedDeploymentSpec();
        deploymentSpec.setOvfUrl(generateOvfUrl(this.vs.getApplianceSoftwareVersion().getImageUrl()));
        deploymentSpec.setHostVersion(softwareVersion + RegisterDeploymentSpecTask.ALL_MINOR_VERSIONS);
        return deploymentSpecApi.createDeploymentSpec(this.vs.getNsxServiceId(), deploymentSpec);
    }

    @Override
    public String getName() {
        return "Register Deployment Specification '" + this.version.toString() +
                RegisterDeploymentSpecTask.ALL_MINOR_VERSIONS + "'";
    }

    @Override
    public Set<LockObjectReference> getObjects() {
        return LockObjectReference.getObjectReferences(this.vs);
    }

    public static String generateOvfUrl(String imageName) {
        return "https://" + ServerUtil.getServerIP() + ":" + Server.getApiPort() + "/ovf/" + imageName;
    }
}
