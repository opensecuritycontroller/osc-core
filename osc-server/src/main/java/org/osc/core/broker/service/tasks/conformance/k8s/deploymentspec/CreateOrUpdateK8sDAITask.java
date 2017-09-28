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
package org.osc.core.broker.service.tasks.conformance.k8s.deploymentspec;

import java.util.Optional;
import java.util.Set;

import javax.persistence.EntityManager;

import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.model.entities.appliance.DistributedApplianceInstance;
import org.osc.core.broker.model.entities.virtualization.openstack.DeploymentSpec;
import org.osc.core.broker.model.plugin.ApiFactoryService;
import org.osc.core.broker.rest.client.k8s.KubernetesDeploymentApi;
import org.osc.core.broker.rest.client.k8s.KubernetesPod;
import org.osc.core.broker.service.exceptions.VmidcException;
import org.osc.core.broker.service.persistence.OSCEntityManager;
import org.osc.core.broker.service.tasks.TransactionalTask;
import org.osc.core.broker.util.log.LogProvider;
import org.osc.sdk.controller.api.SdnRedirectionApi;
import org.osc.sdk.controller.element.NetworkElement;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;

/**
 * This task is responsible for persisting a DAI in the OSC database for a newly found pod VNF.
 */
@Component(service = CreateOrUpdateK8sDAITask.class)
public class CreateOrUpdateK8sDAITask extends TransactionalTask {
    private static final Logger LOG = LogProvider.getLogger(CreateOrUpdateK8sDAITask.class);

    private DeploymentSpec ds;

    private KubernetesPod k8sPod;

    @Reference
    private ApiFactoryService apiFactoryService;

    public CreateOrUpdateK8sDAITask create(DeploymentSpec ds, KubernetesPod k8sPod) {
        return create(ds, k8sPod, null);
    }

    CreateOrUpdateK8sDAITask create(DeploymentSpec ds, KubernetesPod k8sPod, KubernetesDeploymentApi k8sDeploymentApi) {
        CreateOrUpdateK8sDAITask task = new CreateOrUpdateK8sDAITask();
        task.dbConnectionManager = this.dbConnectionManager;
        task.txBroadcastUtil = this.txBroadcastUtil;
        task.apiFactoryService = this.apiFactoryService;
        task.ds = ds;
        task.k8sPod = k8sPod;

        return task;
    }

    @Override
    public void executeTransaction(EntityManager em) throws Exception {
        OSCEntityManager<DeploymentSpec> dsEmgr = new OSCEntityManager<DeploymentSpec>(DeploymentSpec.class, em, this.txBroadcastUtil);
        this.ds = dsEmgr.findByPrimaryKey(this.ds.getId());

        NetworkElement portElement;

        try (SdnRedirectionApi redirection = this.apiFactoryService.createNetworkRedirectionApi(this.ds.getVirtualSystem())) {
            // TODO emanoel: Replace this id workaround with the pod UID once Nuage supports the look up by PodUID.
            String deviceOwnerId = this.k8sPod.getNamespace() + ":" + this.k8sPod.getName();
            portElement = redirection.getNetworkElementByDeviceOwnerId(deviceOwnerId);
            if (portElement == null) {
                throw new VmidcException(String.format("The SDN controller did not return a network element for the device id %s" , deviceOwnerId));
            }
        }

        // Trying to retrieve a dai that is assigned to an inspection element on the SDN
        // but does not have an associated port (previously deleted pod)
        Optional<DistributedApplianceInstance> orphanDai =
                this.ds.getDistributedApplianceInstances()
                .stream()
                .filter(dai -> (dai.getInspectionElementId() != null && dai.getInspectionOsIngressPortId() == null)).findFirst();

        if (orphanDai.isPresent()) {
            LOG.info(String.format("Found orphan dai with id %s and name %s and inspection element %s",
                    orphanDai.get().getId(),
                    orphanDai.get().getName(),
                    orphanDai.get().getInspectionElementId()));
        }

        DistributedApplianceInstance dai = new DistributedApplianceInstance(this.ds.getVirtualSystem());
        dai.setOsHostName(this.k8sPod.getNode());
        dai.setDeploymentSpec(this.ds);
        dai.setName(this.k8sPod.getNamespace() + "-" + this.k8sPod.getName());
        dai.setExternalId(this.k8sPod.getUid());
        // Creating the new DAI with the existing/orphan DAI inspection element id
        dai.setInspectionElementId(orphanDai.isPresent() ? orphanDai.get().getInspectionElementId() : null);

        // The DAI inspection element parent ID (domain id) is the same as the port element parent id
        dai.setInspectionElementParentId(orphanDai.isPresent() ? orphanDai.get().getInspectionElementParentId() : portElement.getParentId());
        dai.setInspectionOsIngressPortId(portElement.getElementId());
        dai.setInspectionIngressMacAddress(portElement.getMacAddresses().get(0));

        dai.setInspectionOsEgressPortId(portElement.getElementId());
        dai.setInspectionEgressMacAddress(portElement.getMacAddresses().get(0));

        dai.setIpAddress(portElement.getPortIPs().get(0));
        dai = OSCEntityManager.create(em, dai, this.txBroadcastUtil);
        LOG.info(String.format("Created dai %s.", dai.getName()));

        if (orphanDai.isPresent()) {
            OSCEntityManager.delete(em, orphanDai.get(), this.txBroadcastUtil);
            LOG.info(String.format("Deleted orphan dai with id %s and name %s", orphanDai.get().getId(), orphanDai.get().getName()));
        }
    }

    @Override
    public String getName() {
        return String.format("Creating the K8s dai for deployment spec %s and pod name %s, pod id %s", this.ds.getName(), this.k8sPod.getName(), this.k8sPod.getUid());
    }

    @Override
    public Set<LockObjectReference> getObjects() {
        return LockObjectReference.getObjectReferences(this.ds);
    }
}
