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
package org.osc.core.broker.service.tasks.conformance.openstack.deploymentspec;

import java.util.Set;

import javax.persistence.EntityManager;

import org.osc.core.broker.job.TaskGraph;
import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.model.entities.appliance.ApplianceSoftwareVersion;
import org.osc.core.broker.model.entities.appliance.DistributedApplianceInstance;
import org.osc.core.broker.model.entities.virtualization.openstack.DeploymentSpec;
import org.osc.core.broker.service.SecurityGroupConformJobFactory;
import org.osc.core.broker.service.persistence.OSCEntityManager;
import org.osc.core.broker.service.tasks.TransactionalMetaTask;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * Deletes existing SVA corresponding to the DAI and recreates the SVA with the new version. This task also schedules
 * security group syncs for security groups related to this DAI.
 */
@Component(service = OsDAIUpgradeMetaTask.class)
public class OsDAIUpgradeMetaTask extends TransactionalMetaTask {

    @Reference
    private OsSvaCreateMetaTask osSvaCreateMetaTask;

    @Reference
    private SecurityGroupConformJobFactory sgConformJobFactory;
    @Reference
    private DeleteSvaServerTask deleteSvaServerTask;

    private DistributedApplianceInstance dai;
    private ApplianceSoftwareVersion upgradedSoftwareVersion;
    private TaskGraph tg;

    public OsDAIUpgradeMetaTask create(DistributedApplianceInstance dai,
            ApplianceSoftwareVersion upgradedSoftwareVersion) {

        OsDAIUpgradeMetaTask task = new OsDAIUpgradeMetaTask();
        task.dai = dai;
        task.upgradedSoftwareVersion = upgradedSoftwareVersion;
        task.dbConnectionManager = this.dbConnectionManager;
        task.txBroadcastUtil = this.txBroadcastUtil;
        task.deleteSvaServerTask = this.deleteSvaServerTask;
        task.osSvaCreateMetaTask = this.osSvaCreateMetaTask;
        task.sgConformJobFactory = this.sgConformJobFactory;

        return task;
    }

    @Override
    public void executeTransaction(EntityManager em) throws Exception {

        this.tg = new TaskGraph();

        OSCEntityManager<DistributedApplianceInstance> daiEntityMgr = new OSCEntityManager<DistributedApplianceInstance>(
                DistributedApplianceInstance.class, em, this.txBroadcastUtil);
        this.dai = daiEntityMgr.findByPrimaryKey(this.dai.getId());

        DeploymentSpec ds = this.dai.getDeploymentSpec();

        this.tg.appendTask(this.deleteSvaServerTask.create(ds.getRegion(), this.dai));
        this.tg.appendTask(this.osSvaCreateMetaTask.create(this.dai));

        OpenstackUtil.scheduleSecurityGroupJobsRelatedToDai(em, this.dai, this, this.sgConformJobFactory);
    }

    @Override
    public String getName() {
        return String.format("Upgrading Distributed Appliance Instance '%s' to '%s'", this.dai.getName(),
                this.upgradedSoftwareVersion.getApplianceSoftwareVersion());
    }

    @Override
    public TaskGraph getTaskGraph() {
        return this.tg;
    }

    @Override
    public Set<LockObjectReference> getObjects() {
        return LockObjectReference.getObjectReferences(this.dai);
    }

}
