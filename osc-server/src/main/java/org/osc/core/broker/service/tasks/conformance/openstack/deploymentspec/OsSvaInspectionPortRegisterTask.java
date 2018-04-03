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

import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.model.entities.appliance.DistributedApplianceInstance;
import org.osc.core.broker.model.entities.appliance.VirtualSystem;
import org.osc.core.broker.model.entities.virtualization.VirtualizationConnector;
import org.osc.core.broker.model.entities.virtualization.openstack.DeploymentSpec;
import org.osc.core.broker.model.plugin.ApiFactoryService;
import org.osc.core.broker.service.persistence.DistributedApplianceInstanceEntityMgr;
import org.osc.core.broker.service.persistence.OSCEntityManager;
import org.osc.core.broker.service.tasks.TransactionalTask;
import org.osc.sdk.controller.DefaultInspectionPort;
import org.osc.sdk.controller.DefaultNetworkPort;
import org.osc.sdk.controller.api.SdnRedirectionApi;
import org.osc.sdk.controller.element.Element;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Register inspection port for the given dai via RedirectionApi.
 */
@Component(service = OsSvaInspectionPortRegisterTask.class)
public class OsSvaInspectionPortRegisterTask extends TransactionalTask {

    private final Logger log = LoggerFactory.getLogger(OsSvaInspectionPortRegisterTask.class);

    @Reference
    private ApiFactoryService apiFactoryService;

    private DistributedApplianceInstance dai;

    public OsSvaInspectionPortRegisterTask create(DistributedApplianceInstance dai) {
        OsSvaInspectionPortRegisterTask task = new OsSvaInspectionPortRegisterTask();
        task.apiFactoryService = this.apiFactoryService;
        task.dai = dai;
        task.dbConnectionManager = this.dbConnectionManager;
        task.txBroadcastUtil = this.txBroadcastUtil;

        return task;
    }

    @Override
    public void executeTransaction(EntityManager em) throws Exception {
        this.dai = DistributedApplianceInstanceEntityMgr.findById(em, this.dai.getId());
        DeploymentSpec ds = this.dai.getDeploymentSpec();

        VirtualSystem vs = ds.getVirtualSystem();

        VirtualizationConnector vc = vs.getVirtualizationConnector();

        if (vc.isControllerDefined()) {
            try (SdnRedirectionApi controller = this.apiFactoryService.createNetworkRedirectionApi(this.dai);) {
                DefaultNetworkPort ingressPort = new DefaultNetworkPort(this.dai.getInspectionOsIngressPortId(),
                        this.dai.getInspectionIngressMacAddress());
                DefaultNetworkPort egressPort = new DefaultNetworkPort(this.dai.getInspectionOsIngressPortId(),
                        this.dai.getInspectionIngressMacAddress());

                if (this.apiFactoryService.supportsNeutronSFC(this.dai.getVirtualSystem())) {
                    // In case of neutron SFC, port group id needs to be used when registering and updated in the DS
                    String portGroupId = ds.getPortGroupId();
                    boolean pgAlreadyCreatedByOther = (portGroupId != null);

                    Element element = controller
                            .registerInspectionPort(new DefaultInspectionPort(ingressPort, egressPort, null, portGroupId));

                    portGroupId = element.getParentId();

                    this.log.info(String.format("Setting port_group_id to %s on DAI %s (id %d) for Deployment Spec %s (id: %d)",
                            portGroupId, this.dai.getName(), this.dai.getId(), ds.getName(), ds.getId()));

                    if (!pgAlreadyCreatedByOther) {
                        ds = em.find(DeploymentSpec.class, ds.getId());
                        ds.setPortGroupId(portGroupId);
                        OSCEntityManager.update(em, ds, this.txBroadcastUtil);
                    }

                } else if (this.apiFactoryService.supportsPortGroup(this.dai.getVirtualSystem())) {
                    String domainId = OpenstackUtil.extractDomainId(ds.getProjectId(),
                                                    ds.getProjectName(),
                                                    ds.getVirtualSystem().getVirtualizationConnector(),
                                                    Arrays.asList(ingressPort));

                    ingressPort.setParentId(domainId);
                    egressPort.setParentId(domainId);

                  //Element object in DefaultInspectionport is not used at this point, hence null
                    controller.registerInspectionPort(
                            new DefaultInspectionPort(ingressPort, egressPort, null));
                } else {
                    controller.registerInspectionPort(
                            new DefaultInspectionPort(ingressPort, egressPort, null));
                }
            }
        }

        this.log.info("Dai: " + this.dai + " Server Id set to: " + this.dai.getExternalId());

        OSCEntityManager.update(em, this.dai, this.txBroadcastUtil);
    }

    @Override
    public String getName() {
        return String.format("Registering Inspection Port for Distributed Appliance Instance '%s'", this.dai.getName());
    }

    @Override
    public Set<LockObjectReference> getObjects() {
        return LockObjectReference.getObjectReferences(this.dai);
    }
}
