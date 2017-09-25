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

import static org.apache.commons.collections4.CollectionUtils.emptyIfNull;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;

import org.osc.core.broker.job.TaskGraph;
import org.osc.core.broker.model.entities.virtualization.SecurityGroupMember;
import org.osc.core.broker.model.entities.virtualization.k8s.Label;
import org.osc.core.broker.model.entities.virtualization.k8s.Pod;
import org.osc.core.broker.rest.client.k8s.KubernetesClient;
import org.osc.core.broker.rest.client.k8s.KubernetesPod;
import org.osc.core.broker.rest.client.k8s.KubernetesPodApi;
import org.osc.core.broker.service.tasks.TransactionalMetaTask;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(service = UpdateK8sSecurityGroupMemberLabelMetaTask.class)
public class UpdateK8sSecurityGroupMemberLabelMetaTask extends TransactionalMetaTask {

    private SecurityGroupMember sgm;

    @Reference
    LabelPodCreateTask labelPodCreateTask;

    @Reference
    LabelPodDeleteTask labelPodDeleteTask;

    private TaskGraph tg;

    private KubernetesPodApi k8sPodApi;

    @Override
    public TaskGraph getTaskGraph() {
        return this.tg;
    }

    @Override
    public String getName() {
        return String.format("Update Kubernetes Security Group Member Label %s", this.sgm.getLabel().getName());
    }

    @Override
    public void executeTransaction(EntityManager em) throws Exception {

        this.tg = new TaskGraph();
        Label label = this.sgm.getLabel();

        List<KubernetesPod> k8sPods = Collections.emptyList();
        try (KubernetesClient client = new KubernetesClient(this.sgm.getSecurityGroup().getVirtualizationConnector())) {
            if (this.k8sPodApi == null) {
                this.k8sPodApi = new KubernetesPodApi(client);
            } else {
                this.k8sPodApi.setKubernetesClient(client);
            }
            k8sPods = this.k8sPodApi.getPodsByLabel(this.sgm.getLabel().getValue());
        }

        if (label.getId() != null) {
            label = em.find(Label.class, label.getId());
        }

        Set<String> existingPodIdsInOSC = emptyIfNull(label.getPods()).stream().map(Pod::getExternalId)
                .collect(Collectors.toSet());

        Set<String> existingPodIdsInK8s = emptyIfNull(k8sPods).stream().map(KubernetesPod::getUid)
                .collect(Collectors.toSet());

        Set<KubernetesPod> k8sPodsToCreate = emptyIfNull(k8sPods).stream()
                .filter(p -> !existingPodIdsInOSC.contains(p.getUid())).collect(Collectors.toSet());

        Set<Pod> dbPodsToDelete = emptyIfNull(label.getPods()).stream()
                .filter(p -> !existingPodIdsInK8s.contains(p.getExternalId())).collect(Collectors.toSet());

        k8sPodsToCreate.forEach(p -> this.tg.addTask(this.labelPodCreateTask.create(p, this.sgm.getLabel(), this.k8sPodApi)));
        dbPodsToDelete.forEach(p -> this.tg.addTask(this.labelPodDeleteTask.create(p)));
    }

    UpdateK8sSecurityGroupMemberLabelMetaTask create(SecurityGroupMember sgm, KubernetesPodApi k8sPodApi) {
        UpdateK8sSecurityGroupMemberLabelMetaTask task = new UpdateK8sSecurityGroupMemberLabelMetaTask();
        task.sgm = sgm;
        task.labelPodCreateTask = this.labelPodCreateTask;
        task.labelPodDeleteTask = this.labelPodDeleteTask;
        task.dbConnectionManager = this.dbConnectionManager;
        task.txBroadcastUtil = this.txBroadcastUtil;
        task.k8sPodApi = this.k8sPodApi;

        return task;
    }
}
