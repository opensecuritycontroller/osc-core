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
import org.osc.core.broker.model.plugin.ApiFactoryService;
import org.osc.core.broker.service.exceptions.VmidcException;
import org.osc.core.broker.service.persistence.OSCEntityManager;
import org.osc.sdk.controller.DefaultInspectionPort;
import org.osc.sdk.controller.api.SdnRedirectionApi;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * This task is responsible for creating a inspection hook for a given
 * security group interface (SGI) using the provided distributed appliance
 * instance. This task will also update the SGI with the identifier
 * of the created inspection hook.
 * <p>
 * This task is applicable to SGIs whose virtual system refers to an SDN
 * controller that supports port groups.
 */
@Component(service=CreatePortGroupHookTask.class)
public final class CreatePortGroupHookTask extends BasePortGroupHookTask {
    private static final Logger LOG = Logger.getLogger(CreatePortGroupHookTask.class);

    @Reference
    private ApiFactoryService apiFactoryService;

    public CreatePortGroupHookTask() {
        super(null, null);
    }

    private CreatePortGroupHookTask(SecurityGroupInterface sgi, DistributedApplianceInstance dai){
        super(sgi, dai);
    }

    public CreatePortGroupHookTask create(SecurityGroupInterface sgi, DistributedApplianceInstance dai){
        CreatePortGroupHookTask task = new CreatePortGroupHookTask(sgi, dai);

        task.apiFactoryService = this.apiFactoryService;
        task.dbConnectionManager = this.dbConnectionManager;
        task.txBroadcastUtil = this.txBroadcastUtil;

        return task;
    }

    @Override
    public void executeTransaction(EntityManager em) throws Exception {
        super.executeTransaction(em);
        String inspectionHookId = null;

        try (SdnRedirectionApi redirection = this.apiFactoryService.createNetworkRedirectionApi(getSGI().getVirtualSystem())) {
            inspectionHookId = redirection.installInspectionHook(getPortGroup(),
                    new DefaultInspectionPort(getIngressPort(), getEgressPort()),
                    getSGI().getTagValue(), null,
                    getSGI().getOrder(), null);
        }

        LOG.info(String.format("Created inspection hook %s for the security group interface %s",  inspectionHookId, getSGI().getName()));

        if (inspectionHookId == null){
            throw new VmidcException(String.format("The creation of the inspection hook for the security group interface %s."
                    + "succeeded but the returned identifier was null.", getSGI().getName()));
        }

        getSGI().setNetworkElementId(inspectionHookId);
        OSCEntityManager.update(em, getSGI(), this.txBroadcastUtil);
    }

    @Override
    public String getName() {
        return String.format("Create the inspection hook for the security group interface '%s' ", getSGI().getName());
    }
}
