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
package org.osc.core.broker.service.tasks.conformance.k8s.securitygroup;

import java.util.Set;

import javax.persistence.EntityManager;

import org.apache.log4j.Logger;
import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.model.entities.virtualization.SecurityGroup;
import org.osc.core.broker.model.entities.virtualization.SecurityGroupMember;
import org.osc.core.broker.model.entities.virtualization.k8s.Label;
import org.osc.core.broker.model.entities.virtualization.k8s.Pod;
import org.osc.core.broker.model.entities.virtualization.k8s.PodPort;
import org.osc.core.broker.model.plugin.ApiFactoryService;
import org.osc.core.broker.rest.client.k8s.KubernetesPod;
import org.osc.core.broker.service.exceptions.VmidcException;
import org.osc.core.broker.service.persistence.OSCEntityManager;
import org.osc.core.broker.service.persistence.PodEntityMgr;
import org.osc.core.broker.service.tasks.TransactionalTask;
import org.osc.sdk.controller.api.SdnRedirectionApi;
import org.osc.sdk.controller.element.NetworkElement;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(service = CreateK8sLabelPodTask.class)
public class CreateK8sLabelPodTask extends TransactionalTask{
    private static final Logger LOG = Logger.getLogger(CreateK8sLabelPodTask.class);

    private KubernetesPod k8sPod;
    private Label label;

    @Reference
    private ApiFactoryService apiFactoryService;

    @Override
    public void executeTransaction(EntityManager em) throws Exception {
        OSCEntityManager<Label> dsEmgr = new OSCEntityManager<Label>(Label.class, em, this.txBroadcastUtil);
        this.label = dsEmgr.findByPrimaryKey(this.label.getId());

        Pod existingPod = PodEntityMgr.findExternalId(em, this.k8sPod.getUid());
        SecurityGroup thisSG = this.label.getSecurityGroupMembers().iterator().next().getSecurityGroup();

        if (existingPod != null) {
            for (Label existingPodLabel : existingPod.getLabels()) {
                if (existingPodLabel.getMarkedForDeletion()) {
                    continue;
                }

                for (SecurityGroupMember sgm : existingPodLabel.getSecurityGroupMembers()) {
                    if (sgm.getMarkedForDeletion()) {
                        continue;
                    }

                    if (!sgm.getSecurityGroup().getId().equals(thisSG.getId())) {
                        throw new VmidcException(String.format("The pod id %s, name %s is already part of the security group %s", this.k8sPod.getUid(), this.k8sPod.getName(), sgm.getSecurityGroup().getName()));
                    }
                }
            }
        }

        NetworkElement portElement;

        try (SdnRedirectionApi redirection = this.apiFactoryService.createNetworkRedirectionApi(thisSG.getVirtualizationConnector())) {
            // TODO emanoel: Replace this id workaround with the pod UID once Nuage supports the look up by PodUID.
            String deviceOwnerId = this.k8sPod.getNamespace() + ":" + this.k8sPod.getName();
            portElement = redirection.getNetworkElementByDeviceOwnerId(deviceOwnerId);
            if (portElement == null) {
                throw new VmidcException(String.format("The SDN controller did not return a network element for the device id %s" , deviceOwnerId));
            }
        }

        PodPort podPort = new PodPort(
                portElement.getElementId(),
                portElement.getMacAddresses().get(0),
                portElement.getPortIPs().get(0),
                portElement.getParentId());

        Pod newPod = new Pod(
                this.k8sPod.getName(),
                this.k8sPod.getNamespace(),
                this.k8sPod.getNode(),
                this.k8sPod.getUid());

        newPod.getPorts().add(podPort);
        podPort.setPod(newPod);

        OSCEntityManager.create(em, podPort, this.txBroadcastUtil);
        OSCEntityManager.create(em, newPod, this.txBroadcastUtil);

        this.label.getPods().add(newPod);

        OSCEntityManager.update(em, this.label, this.txBroadcastUtil);

        LOG.info(String.format("Created pod entity for %s", this.k8sPod.getName()));
    }

    public CreateK8sLabelPodTask create(KubernetesPod k8sPod, Label label) {
        CreateK8sLabelPodTask task = new CreateK8sLabelPodTask();
        task.k8sPod = k8sPod;
        task.label = label;
        task.apiFactoryService = this.apiFactoryService;
        task.dbConnectionManager = this.dbConnectionManager;
        task.txBroadcastUtil = this.txBroadcastUtil;
        return task;
    }

    @Override
    public String getName() {
        return String.format("Creating the pod id %s, name %s", this.k8sPod.getUid(), this.k8sPod.getName());
    }

    @Override
    public Set<LockObjectReference> getObjects() {
        return LockObjectReference.getObjectReferences(this.label);
    }
}
