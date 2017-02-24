/*******************************************************************************
 * Copyright (c) 2017 Intel Corporation
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
package org.osc.core.broker.service.tasks.passwordchange;

import java.util.Set;

import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.model.entities.appliance.VirtualSystem;
import org.osc.core.broker.model.plugin.sdncontroller.VMwareSdnApiFactory;
import org.osc.core.broker.rest.server.AgentAuthFilter;
import org.osc.core.broker.service.tasks.TransactionalTask;
import org.osc.core.util.EncryptionUtil;
import org.osc.core.util.ServerUtil;
import org.osc.sdk.sdn.api.ServiceApi;

public class UpdateNsxServiceAttributesTask extends TransactionalTask {
    private static final Logger LOG = Logger.getLogger(UpdateNsxServiceAttributesTask.class);

    private VirtualSystem vs;

    public UpdateNsxServiceAttributesTask(VirtualSystem vs) {
        super();
        this.vs = vs;
        this.name = getName();
    }

    @Override
    public String getName() {
        return "Updating service attribute in NSX Manager '" + this.vs.getVirtualizationConnector().getName() + "'";
    }

    @Override
    public void executeTransaction(Session session) throws Exception {
        LOG.info("Start executing UpdateNsxServiceAttributesTask");
        ServiceApi serviceApi = VMwareSdnApiFactory.createServiceApi(this.vs);
        serviceApi.updateService(
                this.vs.getNsxServiceId(),
                this.vs.getDistributedAppliance().getName(),
                this.vs.getDistributedAppliance().getAppliance().getModel(),
                this.vs.getDistributedAppliance().getApplianceVersion(),
                ServerUtil.getServerIP(),
                EncryptionUtil.encryptAESCTR(AgentAuthFilter.VMIDC_AGENT_PASS),
                AgentAuthFilter.VMIDC_AGENT_LOGIN,
                this.vs.getDistributedAppliance().getName()
                );
    }

    @Override
    public Set<LockObjectReference> getObjects() {
        return LockObjectReference.getObjectReferences(this.vs);
    }

}
