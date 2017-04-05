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

import org.apache.log4j.Logger;
import org.osc.core.broker.model.entities.virtualization.SecurityGroup;
import org.osc.core.broker.model.plugin.sdncontroller.SdnControllerApiFactory;
import org.osc.core.broker.service.tasks.TransactionalTask;
import org.osc.core.broker.service.tasks.conformance.openstack.securitygroup.element.PortGroup;
import org.osc.sdk.controller.api.SdnRedirectionApi;

class DeletePortGroupTask extends TransactionalTask {
    private static final Logger LOG = Logger.getLogger(DeletePortGroupTask.class);
    private PortGroup portGroup;
    private SecurityGroup securityGroup;

    public DeletePortGroupTask(SecurityGroup securityGroup, PortGroup portGroup){
        this.securityGroup = securityGroup;
        this.portGroup = portGroup;
    }

    @Override
    public void executeTransaction(EntityManager em) throws Exception {
        SdnRedirectionApi controller = SdnControllerApiFactory.createNetworkRedirectionApi(
                this.securityGroup.getVirtualizationConnector());
        controller.deleteNetworkElement(this.portGroup);
    }

    @Override
    public String getName() {
        return String.format("Delete Port Group ID : %s", this.portGroup.getElementId()) ;
    }
}