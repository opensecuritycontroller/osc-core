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

import org.hibernate.Session;
import org.osc.core.broker.model.entities.appliance.DistributedApplianceInstance;
import org.osc.core.broker.model.entities.virtualization.SecurityGroupInterface;
import org.osc.core.broker.model.entities.virtualization.openstack.VMPort;
import org.osc.core.broker.model.plugin.sdncontroller.SdnControllerApiFactory;
import org.osc.core.broker.service.sdn.NetworkElementImpl;
import org.osc.core.broker.service.tasks.TransactionalTask;
import org.osc.sdk.controller.DefaultInspectionPort;
import org.osc.sdk.controller.DefaultNetworkPort;
import org.osc.sdk.controller.api.SdnControllerApi;

class VmPortHookTagUpdateTask extends TransactionalTask {

    //private final Logger log = Logger.getLogger(SecurityGroupMemberVmHookTagUpdateTask.class);

    private final String vmName;
    private final String serviceName;
    private VMPort vmPort;
    private DistributedApplianceInstance dai;
    private SecurityGroupInterface securityGroupInterface;

    public VmPortHookTagUpdateTask(VMPort vmPort, SecurityGroupInterface securityGroupInterface,
            DistributedApplianceInstance daiToRedirectTo) {
        this.vmPort = vmPort;
        this.dai = daiToRedirectTo;
        this.securityGroupInterface = securityGroupInterface;
        this.serviceName = this.securityGroupInterface.getVirtualSystem().getDistributedAppliance().getName();
        this.vmName = vmPort.getVm().getName();
    }

    @Override
    public void executeTransaction(Session session) throws Exception {
        this.vmPort = (VMPort) session.get(VMPort.class, this.vmPort.getId());
        this.dai = (DistributedApplianceInstance) session.get(DistributedApplianceInstance.class, this.dai.getId());
        this.securityGroupInterface = (SecurityGroupInterface) session.get(SecurityGroupInterface.class,
                this.securityGroupInterface.getId());

        SdnControllerApi controller = SdnControllerApiFactory.createNetworkControllerApi(this.dai);
        try {
            DefaultNetworkPort ingressPort = new DefaultNetworkPort(this.dai.getInspectionOsIngressPortId(),
                    this.dai.getInspectionIngressMacAddress());
            DefaultNetworkPort egressPort = new DefaultNetworkPort(this.dai.getInspectionOsEgressPortId(),
                    this.dai.getInspectionEgressMacAddress());

            controller.setInspectionHookTag(new NetworkElementImpl(this.vmPort), new DefaultInspectionPort(ingressPort, egressPort),
                    this.securityGroupInterface.getTagValue());
        } finally {
            controller.close();
        }

    }

    @Override
    public String getName() {
        return String.format("Updating Inspection Hook Tag (%d) for Security Group Member '%s' for service '%s'",
                this.securityGroupInterface.getTagValue(), this.vmName, this.serviceName);
    }

}
