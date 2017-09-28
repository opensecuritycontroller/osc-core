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
import org.slf4j.LoggerFactory;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;

/**
 * This task is responsible for detaching the provided DAI
 * from all the protected ports associated with the given SGI.
 * <p>
 * This task is applicable to SGIs whose virtual system refers to an SDN
 * controller that supports port groups.
 */
@Component(service = DeallocateDAIOfSGIMembersTask.class)
public class DeallocateDAIOfSGIMembersTask extends UpdateDAIToSGIMembersTask {
    private static final Logger LOG = LoggerFactory.getLogger(AllocateDAIWithSGIMembersTask.class);

    public DeallocateDAIOfSGIMembersTask() {
        super(null, null);
    }

    private DeallocateDAIOfSGIMembersTask(SecurityGroupInterface sgi, DistributedApplianceInstance dai) {
        super(sgi, dai);
    }

    @Override
    public DeallocateDAIOfSGIMembersTask create(SecurityGroupInterface sgi, DistributedApplianceInstance dai) {
        DeallocateDAIOfSGIMembersTask task = new DeallocateDAIOfSGIMembersTask(sgi, dai);
        task.dbConnectionManager = this.dbConnectionManager;
        task.txBroadcastUtil = this.txBroadcastUtil;

        return task;
    }

    /**
     * This method detaches the provided port from the {@link #getDai()}
     *
     * @param protectedPort
     *            the port to be detached from the DAI.
     */
    @Override
    public void updatePortProtection(VirtualPort protectedPort) {
        protectedPort.removeDai(getDai());
        getDai().removeProtectedPort(protectedPort);
        LOG.info(String.format("The DAI %s was unassigned from the port %s.", getDai().getName(),
                protectedPort.getId()));
    }

    @Override
    public String getName() {
        return String.format("Detaching the DAI %s from all the ports in the SGI %s.", getDai().getName(),
                getSGI().getName());
    }
}
