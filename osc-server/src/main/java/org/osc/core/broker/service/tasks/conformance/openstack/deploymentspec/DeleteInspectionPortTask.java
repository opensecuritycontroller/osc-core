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

import java.util.Arrays;
import java.util.Set;

import javax.persistence.EntityManager;

import org.apache.log4j.Logger;
import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.model.entities.appliance.DistributedApplianceInstance;
import org.osc.core.broker.model.entities.virtualization.openstack.DeploymentSpec;
import org.osc.core.broker.model.plugin.ApiFactoryService;
import org.osc.core.broker.service.exceptions.VmidcBrokerValidationException;
import org.osc.core.broker.service.persistence.DistributedApplianceInstanceEntityMgr;
import org.osc.core.broker.service.tasks.TransactionalTask;
import org.osc.sdk.controller.DefaultInspectionPort;
import org.osc.sdk.controller.DefaultNetworkPort;
import org.osc.sdk.controller.api.SdnRedirectionApi;
import org.osc.sdk.controller.element.InspectionPortElement;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * This Task is invoked only if SDN Controller supports Port Group
 */
@Component(service = DeleteInspectionPortTask.class)
public class DeleteInspectionPortTask extends TransactionalTask {

    private static final Logger LOG = Logger.getLogger(DeleteInspectionPortTask.class);

    @Reference
    private ApiFactoryService apiFactoryService;

    private DistributedApplianceInstance dai;
    private String region;


    public DeleteInspectionPortTask create(String region, DistributedApplianceInstance dai) {
        DeleteInspectionPortTask task = new DeleteInspectionPortTask();
        // TODO emanoel: This does not seem to be used anywhere, remove it on master.
        task.region = region;
        task.dai = dai;
        task.apiFactoryService = this.apiFactoryService;
        task.dbConnectionManager = this.dbConnectionManager;
        task.txBroadcastUtil = this.txBroadcastUtil;

        return task;
    }

    @Override
    public void executeTransaction(EntityManager em) throws Exception {
        this.dai = DistributedApplianceInstanceEntityMgr.findById(em, this.dai.getId());

        try (SdnRedirectionApi controller = this.apiFactoryService.createNetworkRedirectionApi(this.dai);) {
        DefaultNetworkPort ingressPort = new DefaultNetworkPort(this.dai.getInspectionOsIngressPortId(),
                this.dai.getInspectionIngressMacAddress());
        DefaultNetworkPort egressPort = new DefaultNetworkPort(this.dai.getInspectionOsEgressPortId(),
                this.dai.getInspectionEgressMacAddress());
        DeploymentSpec ds = this.dai.getDeploymentSpec();
        String domainId = OpenstackUtil.extractDomainId(
                ds.getProjectId(),
                ds.getProjectName(),
                ds.getVirtualSystem().getVirtualizationConnector(),
                Arrays.asList(ingressPort));

        if (domainId == null) {
            throw new VmidcBrokerValidationException(String.format("A domain was not found for the ingress port %s.", ingressPort.getElementId()));
        }

        ingressPort.setParentId(domainId);
        egressPort.setParentId(domainId);

        //Element Object in DefaultInspectionPort is not used for now, hence null
        InspectionPortElement portEl = new DefaultInspectionPort(ingressPort, egressPort, null);
        LOG.info(String.format("Deleting Inspection port(s): '%s' from region '%s' and Server : '%s' ",
                portEl, this.region, this.dai));


            controller.removeInspectionPort(portEl);
        }
    }

    @Override
    public String getName() {
        return String.format("Deleting Inspection Port of Server '%s' using SDN Controller plugin", this.dai.getName());
    }

    @Override
    public Set<LockObjectReference> getObjects() {
        return LockObjectReference.getObjectReferences(this.dai);
    }

}
