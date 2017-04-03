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
import org.osc.core.broker.model.entities.appliance.DistributedApplianceInstance;
import org.osc.core.broker.model.entities.virtualization.SecurityGroupInterface;
import org.osc.core.broker.model.plugin.sdncontroller.SdnControllerApiFactory;
import org.osc.sdk.controller.DefaultInspectionPort;
import org.osc.sdk.controller.FailurePolicyType;
import org.osc.sdk.controller.TagEncapsulationType;
import org.osc.sdk.controller.api.SdnRedirectionApi;

/**
 * This task is responsible for updating a inspection hook for a given
 * security group interface (SGI) using the given distributed
 * appliance instance.
 * <p>
 * This task is applicable to SGIs whose virtual system refers to an SDN
 * controller that supports port groups.
 */
public final class UpdatePortGroupHookTask extends BasePortGroupHookTask {
    private static final Logger LOG = Logger.getLogger(UpdatePortGroupHookTask.class);

    public UpdatePortGroupHookTask(SecurityGroupInterface sgi, DistributedApplianceInstance dai){
        super(sgi, dai);
    }

    @Override
    public void executeTransaction(EntityManager em) throws Exception {
        super.executeTransaction(em);

        String inspectionHookId = getSGI().getNetworkElementId();

        try (SdnRedirectionApi redirection = SdnControllerApiFactory.createNetworkRedirectionApi(getSGI().getVirtualSystem())) {
            redirection.updateInspectionHook(inspectionHookId, getPortGroup(),
                    new DefaultInspectionPort(getIngressPort(), getEgressPort()),
                    getSGI().getTagValue(), TagEncapsulationType.valueOf(getSGI().getVirtualSystem().getEncapsulationType().name()),
                    getSGI().getOrder(), FailurePolicyType.valueOf(getSGI().getFailurePolicyType().name()));
        }

        LOG.info(String.format("Updated inspection hook %s for the security group interface %s",  inspectionHookId, getSGI().getName()));
    }

    @Override
    public String getName() {
        return String.format("Update the inspection hook for the security group interface '%s' ", getSGI().getName());
    }
}
