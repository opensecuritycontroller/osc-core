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
package org.osc.core.broker.service.tasks.conformance.openstack.deploymentspec;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Set;

import javax.persistence.EntityManager;

import org.apache.log4j.Logger;
import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.model.entities.appliance.DistributedApplianceInstance;
import org.osc.core.broker.model.entities.virtualization.openstack.DeploymentSpec;
import org.osc.core.broker.model.plugin.sdncontroller.SdnControllerApiFactory;
import org.osc.core.broker.service.tasks.TransactionalTask;
import org.osc.sdk.controller.DefaultInspectionPort;
import org.osc.sdk.controller.DefaultNetworkPort;
import org.osc.sdk.controller.api.SdnControllerApi;
import org.osc.sdk.controller.element.NetworkElement;

public class OnboardDAITask extends TransactionalTask {
    private static final Logger log = Logger.getLogger(OnboardDAITask.class);
    private DistributedApplianceInstance dai;

    public OnboardDAITask(DistributedApplianceInstance dai) {
        this.dai = dai;
        this.name = getName();
    }

    @Override
    public String getName() {
        return "Onboarding Distributed Appliance Instance '" + this.dai.getName() + "'";
    }

    @Override
    public void executeTransaction(EntityManager em) throws Exception {
        this.dai = em.find(DistributedApplianceInstance.class, this.dai.getId());
        SdnControllerApi controller = SdnControllerApiFactory.createNetworkControllerApi(this.dai);
        try {
            DefaultNetworkPort ingressPort = new DefaultNetworkPort(this.dai.getInspectionOsIngressPortId(),
                    this.dai.getInspectionIngressMacAddress());
            DefaultNetworkPort egressPort = new DefaultNetworkPort(this.dai.getInspectionOsEgressPortId(),
                    this.dai.getInspectionEgressMacAddress());

            if (SdnControllerApiFactory.supportsPortGroup(this.dai.getVirtualSystem())){
                DeploymentSpec ds = this.dai.getDeploymentSpec();
                String domainId = OpenstackUtil.extractDomainId(ds.getTenantId(), ds.getTenantName(),
                        ds.getVirtualSystem().getVirtualizationConnector(), new ArrayList<NetworkElement>(
                                Arrays.asList(ingressPort)));
                ingressPort.setParentId(domainId);
                egressPort.setParentId(domainId);
                if (domainId != null){
                    controller.registerInspectionPort(new DefaultInspectionPort(ingressPort, egressPort));
                } else {
                    log.warn("DomainId is missing, cannot be null");
                }

            } else {
                controller.registerInspectionPort(new DefaultInspectionPort(ingressPort, egressPort));
            }


        } finally {
            controller.close();
        }
    }

    @Override
    public Set<LockObjectReference> getObjects() {
        return LockObjectReference.getObjectReferences(this.dai);
    }

}
