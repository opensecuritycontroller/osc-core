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

import org.osc.core.broker.model.entities.appliance.DistributedApplianceInstance;
import org.osc.core.broker.model.entities.virtualization.SecurityGroupInterface;
import org.osc.core.broker.model.entities.virtualization.VirtualPort;
import org.osc.core.broker.util.log.LogProvider;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;

/**
 * This task is responsible for assigning the provided DAI
 * to all the protected ports associated with the given SGI.
 * <p>
 * This task is applicable to SGIs whose virtual system refers to an SDN
 * controller that supports port groups.
 */
@Component(service = AllocateDAIWithSGIMembersTask.class)
public final class AllocateDAIWithSGIMembersTask extends UpdateDAIToSGIMembersTask {
    private static final Logger LOG = LogProvider.getLogger(AllocateDAIWithSGIMembersTask.class);

    public AllocateDAIWithSGIMembersTask() {
        super(null, null);
    }

    private AllocateDAIWithSGIMembersTask(SecurityGroupInterface sgi, DistributedApplianceInstance dai) {
        super(sgi, dai);
    }

    @Override
    public AllocateDAIWithSGIMembersTask create(SecurityGroupInterface sgi, DistributedApplianceInstance dai) {
        AllocateDAIWithSGIMembersTask task = new AllocateDAIWithSGIMembersTask(sgi, dai);
        task.dbConnectionManager = this.dbConnectionManager;
        task.txBroadcastUtil = this.txBroadcastUtil;

        return task;
    }

    /**
     * This method assigns the provided port with the {@link #getDai()}
     *
     * @param protectedPort
     *            the port to be attached to the DAI.
     */
    @Override
    public void updatePortProtection(VirtualPort protectedPort) {
        protectedPort.addDai(getDai());
        getDai().addProtectedPort(protectedPort);
        LOG.info(String.format("The port %s is protected with the DAI %s", protectedPort.getId(), getDai().getName()));
    }

    @Override
    public String getName() {
        return String.format("Assigning the DAI %s to all the ports in the SGI %s.", getDai().getName(),
                getSGI().getName());
    }
}
