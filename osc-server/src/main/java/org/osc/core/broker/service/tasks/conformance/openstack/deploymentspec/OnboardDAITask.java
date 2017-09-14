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
import org.osc.core.broker.model.plugin.ApiFactoryService;
import org.osc.core.broker.service.tasks.TransactionalTask;
import org.osc.sdk.controller.DefaultInspectionPort;
import org.osc.sdk.controller.DefaultNetworkPort;
import org.osc.sdk.controller.api.SdnRedirectionApi;
import org.osc.sdk.controller.element.Element;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(service=OnboardDAITask.class)
public class OnboardDAITask extends TransactionalTask {
    private static final Logger log = Logger.getLogger(OnboardDAITask.class);
    private DistributedApplianceInstance dai;

    @Reference
    private ApiFactoryService apiFactoryService;

    public OnboardDAITask create(DistributedApplianceInstance dai) {
        OnboardDAITask task = new OnboardDAITask();
        task.dai = dai;
        task.name = task.getName();
        task.apiFactoryService = this.apiFactoryService;
        task.dbConnectionManager = this.dbConnectionManager;
        task.txBroadcastUtil = this.txBroadcastUtil;

        return task;
    }

    @Override
    public String getName() {
        return "Onboarding Distributed Appliance Instance '" + this.dai.getName() + "'";
    }

    @Override
    public void executeTransaction(EntityManager em) throws Exception {
        this.dai = em.find(DistributedApplianceInstance.class, this.dai.getId());
        SdnRedirectionApi controller = this.apiFactoryService.createNetworkRedirectionApi(this.dai);
        try {
            DefaultNetworkPort ingressPort = new DefaultNetworkPort(this.dai.getInspectionOsIngressPortId(),
                    this.dai.getInspectionIngressMacAddress());
            DefaultNetworkPort egressPort = new DefaultNetworkPort(this.dai.getInspectionOsEgressPortId(),
                    this.dai.getInspectionEgressMacAddress());

            DeploymentSpec ds = this.dai.getDeploymentSpec();
            String portGroupId = null;

            if (this.apiFactoryService.supportsPortGroup(this.dai.getVirtualSystem())){

                String domainId = OpenstackUtil.extractDomainId(ds.getProjectId(), ds.getProjectName(),
                        ds.getVirtualSystem().getVirtualizationConnector(), new ArrayList<>(
                                Arrays.asList(ingressPort)));
                ingressPort.setParentId(domainId);
                egressPort.setParentId(domainId);
                if (domainId != null){
                	//Element Object is not used in DefaultInstepctionPort for now, hence null
                    portGroupId = ds.getPortGroupId();
                    Element element = controller.registerInspectionPort(new DefaultInspectionPort(ingressPort, egressPort,
                                                                        null, portGroupId));

                    portGroupId = element.getParentId();
                } else {
                    log.warn("DomainId is missing, cannot be null");
                }

            } else {
                controller.registerInspectionPort(new DefaultInspectionPort(ingressPort, egressPort, null));
            }

            ds.setPortGroupId(portGroupId);
        } finally {
            controller.close();
        }
    }

    @Override
    public Set<LockObjectReference> getObjects() {
        return LockObjectReference.getObjectReferences(this.dai);
    }

}
