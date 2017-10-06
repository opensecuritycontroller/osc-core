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
package org.osc.core.broker.service.tasks.conformance.openstack.securitygroup;

import java.util.Set;

import javax.persistence.EntityManager;

import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.model.entities.virtualization.SecurityGroupInterface;
import org.osc.core.broker.model.plugin.ApiFactoryService;
import org.osc.core.broker.service.persistence.OSCEntityManager;
import org.osc.core.broker.service.tasks.TransactionalTask;
import org.osc.core.broker.util.log.LogProvider;
import org.osc.sdk.controller.api.SdnRedirectionApi;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;

/**
 * This task is responsible for removing a inspection hook
 * from a given security group interface (SGI) using the inspection hook
 * identifier found in the SGI.
 * This task will also update the SGI removing the existing
 * inspection hook identifier.
 * <p>
 * This task is applicable to SGIs whose virtual system refers to an SDN
 * controller that supports port groups.
 */
@Component(service=RemovePortGroupHookTask.class)
public class RemovePortGroupHookTask extends TransactionalTask {
    public SecurityGroupInterface sgi;
    private static final Logger LOG = LogProvider.getLogger(RemovePortGroupHookTask.class);

    @Reference
    private ApiFactoryService apiFactoryService;

    public RemovePortGroupHookTask create(SecurityGroupInterface sgi){
        RemovePortGroupHookTask task = new RemovePortGroupHookTask();
        task.sgi = sgi;
        task.apiFactoryService = this.apiFactoryService;
        task.dbConnectionManager = this.dbConnectionManager;
        task.txBroadcastUtil = this.txBroadcastUtil;

        return task;
    }

    @Override
    public void executeTransaction(EntityManager em) throws Exception {
        this.sgi = em.find(SecurityGroupInterface.class, this.sgi.getId());

        if (this.sgi.getNetworkElementId() == null) {
            LOG.warn(String.format("The security group interface %s does not have network element (port group hook) assigned.", this.sgi.getName()));
            return;
        }

        try (SdnRedirectionApi redirection = this.apiFactoryService.createNetworkRedirectionApi(this.sgi.getVirtualSystem())) {
            redirection.removeInspectionHook(this.sgi.getNetworkElementId());
            LOG.info(String.format("The port group hook %s was removed.", this.sgi.getNetworkElementId()));
        }

        this.sgi.setNetworkElementId(null);
        OSCEntityManager.update(em, this.sgi, this.txBroadcastUtil);
    }

    @Override
    public String getName() {
        return String.format("Deleting the inspection hook for the security group interface %s.", this.sgi.getName());
    }

    @Override
    public Set<LockObjectReference> getObjects() {
        return LockObjectReference.getObjectReferences(this.sgi);
    }
}
