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

import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.model.entities.appliance.DistributedApplianceInstance;
import org.osc.core.broker.model.entities.appliance.VirtualSystem;
import org.osc.core.broker.model.entities.virtualization.SecurityGroup;
import org.osc.core.broker.model.entities.virtualization.SecurityGroupInterface;
import org.osc.core.broker.service.tasks.TransactionalTask;
import org.osc.core.broker.service.tasks.conformance.openstack.securitygroup.element.PortGroup;
import org.osc.sdk.controller.DefaultInspectionPort;
import org.osc.sdk.controller.DefaultNetworkPort;
import org.osc.sdk.controller.FailurePolicyType;
import org.osc.sdk.controller.TagEncapsulationType;
import org.osc.sdk.controller.api.SdnControllerApi;
import org.osc.sdk.controller.element.InspectionHookElement;

public class PortGroupHookCreateTask extends TransactionalTask{
    private static final Logger LOG = Logger.getLogger(PortGroupHookCreateTask.class);
    public SecurityGroup sg;
    public SecurityGroupInterface sgi;
    private DistributedApplianceInstance dai;
    private SdnControllerApi controller;
    private VirtualSystem vs;

    public PortGroupHookCreateTask( SecurityGroup sg, SecurityGroupInterface sgi, VirtualSystem vs,
            DistributedApplianceInstance dai){
        this.sg = sg;
        this.sgi = sgi;
        this.vs = vs;
        this.dai = dai;
    }

    @Override
    public void executeTransaction(Session session) throws Exception {
        this.sg = (SecurityGroup) session.get(SecurityGroup.class, this.sg.getId());
        this.sgi = (SecurityGroupInterface) session.get(SecurityGroupInterface.class, this.sgi.getId());
        this.vs = (VirtualSystem) session.get(VirtualSystem.class, this.vs.getId());
        //call install inspectionhook
          String portGroupId = this.sg.getNetworkElementId();
          if (portGroupId != null){
              PortGroup portGroup = new PortGroup();
              portGroup.setPortGroupId(portGroupId);

              DefaultNetworkPort ingressPort = new DefaultNetworkPort(
                      this.dai.getInspectionOsIngressPortId(),
                      this.dai.getInspectionIngressMacAddress());
              DefaultNetworkPort egressPort = new DefaultNetworkPort(
                      this.dai.getInspectionOsEgressPortId(),
                      this.dai.getInspectionEgressMacAddress());
              LOG.info("installInspectionHook called PortGroup : " + portGroup);
              InspectionHookElement inspectionHook = this.controller.installInspectionHook(portGroup, new DefaultInspectionPort(ingressPort, egressPort),
                      this.sgi.getTagValue(), TagEncapsulationType.valueOf(this.vs.getEncapsulationType().name()),
                      this.sgi.getOrder(), FailurePolicyType.valueOf(this.sgi.getFailurePolicyType().name()));
              if (inspectionHook != null){
                  this.sgi.setNetworkElementId(inspectionHook.getHookId());
                  session.update(this.sgi);
              }
          }
    }

    @Override
    public String getName() {
        return String.format(
                  "Creating Inspection Hooks for Port Group Member '%s' ",
                  this.sg.getName());
    }

    @Override
    public Set<LockObjectReference> getObjects() {
        return LockObjectReference.getObjectReferences(this.dai);
    }

}