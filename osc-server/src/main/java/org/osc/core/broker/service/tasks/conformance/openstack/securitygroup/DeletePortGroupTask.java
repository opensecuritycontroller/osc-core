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

import javax.persistence.EntityManager;

import org.osc.core.broker.model.entities.virtualization.SecurityGroup;
import org.osc.core.broker.model.plugin.ApiFactoryService;
import org.osc.core.broker.service.tasks.TransactionalTask;
import org.osc.core.broker.service.tasks.conformance.openstack.securitygroup.element.PortGroup;
import org.osc.sdk.controller.api.SdnRedirectionApi;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(service = DeletePortGroupTask.class)
public class DeletePortGroupTask extends TransactionalTask {

    @Reference
    private ApiFactoryService apiFactoryService;

    private PortGroup portGroup;
    private SecurityGroup securityGroup;

    public DeletePortGroupTask create(SecurityGroup securityGroup, PortGroup portGroup){
        DeletePortGroupTask task = new DeletePortGroupTask();
        task.securityGroup = securityGroup;
        task.portGroup = portGroup;
        task.apiFactoryService = this.apiFactoryService;
        task.dbConnectionManager = this.dbConnectionManager;
        task.txBroadcastUtil = this.txBroadcastUtil;

        return task;
    }

    @Override
    public void executeTransaction(EntityManager em) throws Exception {
        this.securityGroup = em.find(SecurityGroup.class, this.securityGroup.getId());

        try(SdnRedirectionApi controller = this.apiFactoryService.createNetworkRedirectionApi(
                this.securityGroup.getVirtualizationConnector())) {
            controller.deleteNetworkElement(this.portGroup);
        }
    }

    @Override
    public String getName() {
        return String.format("Delete Port Group ID : %s", this.portGroup.getElementId());
    }
}