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

import org.apache.log4j.Logger;
import org.osc.core.broker.model.entities.appliance.DistributedApplianceInstance;
import org.osc.core.broker.model.entities.virtualization.SecurityGroupInterface;
import org.osc.core.broker.model.entities.virtualization.openstack.VMPort;

public class DeallocateDAIOfSGIMembersTask extends UpdateDAIToSGIMembersTask {
    private static final Logger LOG = Logger.getLogger(AllocateDAIWithSGIMembersTask.class);

    public DeallocateDAIOfSGIMembersTask(SecurityGroupInterface sgi, DistributedApplianceInstance dai){
        super(sgi,dai);
    }

    @Override
    public void updatePortProtection(VMPort protectedPort) {
        protectedPort.removeDai(getDai());
        getDai().removeProtectedPort(protectedPort);
        LOG.info(String.format("The DAI %s was unassigned from the port %s.", getDai().getName(), protectedPort.getId()));
    }
}
