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
import java.util.UUID;

import javax.persistence.EntityManager;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.osc.core.broker.job.TaskGraph;
import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.model.entities.appliance.DistributedApplianceInstance;
import org.osc.core.broker.model.entities.virtualization.openstack.DeploymentSpec;
import org.osc.core.broker.model.plugin.manager.ManagerApiFactory;
import org.osc.core.broker.service.persistence.OSCEntityManager;
import org.osc.core.broker.service.tasks.TransactionalMetaTask;
import org.osc.core.broker.service.tasks.conformance.manager.MgrCreateMemberDeviceTask;
import org.osc.sdk.manager.api.ManagerDeviceApi;

/**
 * Creates an SVA on openstack
 */
class OsSvaCreateMetaTask extends TransactionalMetaTask {

    final Logger log = Logger.getLogger(OsSvaCreateMetaTask.class);

    private TaskGraph tg;

    private DeploymentSpec ds;
    private DistributedApplianceInstance dai;
    private String availabilityZone;
    private final String hypervisorHostName;

    /**
     * Constructor. All arguments are required(unless specified)
     *
     * @param ds
     *            deployment spec
     * @param hypervisorName
     *            the hypervisor to deploy to
     * @param availabilityZone
     *            the availability zone to deploy to
     */
    public OsSvaCreateMetaTask(DeploymentSpec ds, String hypervisorHostName, String availabilityZone) {
        this.availabilityZone = availabilityZone;
        this.ds = ds;
        this.hypervisorHostName = hypervisorHostName;
        this.dai = null;
    }

    public OsSvaCreateMetaTask(DistributedApplianceInstance dai) {
        this.dai = dai;
        this.ds = dai.getDeploymentSpec();
        this.availabilityZone = dai.getOsAvailabilityZone();
        this.hypervisorHostName = this.dai.getOsHostName();
    }

    @Override
    public void executeTransaction(EntityManager em) throws Exception {
        this.tg = new TaskGraph();

        this.ds = em.find(DeploymentSpec.class, this.ds.getId());

        this.dai = getDAI(em, this.ds, this.dai);

        // Reset all discovered attributes
        this.dai.resetAllDiscoveredAttributes();
        // Recalculate AZ for this host. It is possible our DB contains a staled AZ.
        this.dai.setOsAvailabilityZone(OpenstackUtil.getHostAvailibilityZone(this.ds, this.ds.getRegion(),
                this.dai.getOsHostName()));
        this.availabilityZone = this.dai.getOsAvailabilityZone();

        OSCEntityManager.update(em, this.dai);

        this.tg.addTask(new OsSvaServerCreateTask(this.dai, this.hypervisorHostName, this.availabilityZone));
        this.tg.appendTask(new OsSvaEnsureActiveTask(this.dai));
        if (!StringUtils.isBlank(this.ds.getFloatingIpPoolName())) {
            this.tg.appendTask(new OsSvaCheckFloatingIpTask(this.dai));
        }

        this.tg.appendTask(new OsSvaCheckNetworkInfoTask(this.dai));

        try (ManagerDeviceApi mgrApi = ManagerApiFactory.createManagerDeviceApi(this.dai.getVirtualSystem())) {
            if (mgrApi.isDeviceGroupSupported()) {
                this.tg.appendTask(new MgrCreateMemberDeviceTask(this.dai));
            }
        }

        OpenstackUtil.scheduleSecurityGroupJobsRelatedToDai(em, this.dai, this);
    }

    /**
     * Gets a DAI based on the dai id passed in. If the passed in id is null, creates a new DAI.
     *
     * @param session
     *            the session
     * @param vs
     *            the vs to associate the DAI(newly created) with
     * @param ds
     *            the ds to associate the dai(newly created) with
     * @param daiIdToLoad
     *            the dai to load
     * @return
     * @throws Exception
     */
    private DistributedApplianceInstance getDAI(EntityManager em, DeploymentSpec ds,
            DistributedApplianceInstance daiToLoad) throws Exception {
        DistributedApplianceInstance dai = null;

        if (daiToLoad != null) {
            dai = em.find(DistributedApplianceInstance.class, daiToLoad.getId());
        } else {
            String daiName;
            dai = new DistributedApplianceInstance(ds.getVirtualSystem());
            dai.setOsHostName(this.hypervisorHostName);
            dai.setOsAvailabilityZone(this.availabilityZone);
            dai.setDeploymentSpec(ds);
            // setting temporary name since it is mandatory field
            dai.setName("Temporary" + UUID.randomUUID().toString());
            dai = OSCEntityManager.create(em, dai);

            // Generate a unique, intuitive and immutable name
            daiName = ds.getVirtualSystem().getName() + "-" + dai.getId().toString();
            this.log.info("Creating DAI using name '" + daiName + "'");

            dai.setName(daiName);

            OSCEntityManager.update(em, dai);
            this.log.info("Creating new DAI " + dai);
        }
        return dai;
    }

    @Override
    public String getName() {
        return String.format("Create SVA for '%s' in Availability zone '%s' in Region '%s'", this.hypervisorHostName,
                this.availabilityZone, this.ds.getRegion());
    }

    @Override
    public TaskGraph getTaskGraph() {
        return this.tg;
    }

    @Override
    public Set<LockObjectReference> getObjects() {
        return LockObjectReference.getObjectReferences(this.ds);
    }

}
