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

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;

import org.apache.commons.collections4.CollectionUtils;
import org.osc.core.broker.job.TaskGraph;
import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.model.entities.appliance.DistributedApplianceInstance;
import org.osc.core.broker.model.entities.virtualization.openstack.DeploymentSpec;
import org.osc.core.broker.rest.client.k8s.KubernetesClient;
import org.osc.core.broker.rest.client.k8s.KubernetesDeploymentApi;
import org.osc.core.broker.rest.client.k8s.KubernetesPod;
import org.osc.core.broker.rest.client.k8s.KubernetesPodApi;
import org.osc.core.broker.service.persistence.OSCEntityManager;
import org.osc.core.broker.service.tasks.TransactionalMetaTask;
import org.osc.core.broker.service.tasks.conformance.manager.MgrCheckDevicesMetaTask;
import org.osc.core.common.job.TaskGuard;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * Conforms the deployment pods for a Kubernetes deployment.
 */
@Component(service = ConformK8sDeploymentPodsMetaTask.class)
public class ConformK8sDeploymentPodsMetaTask extends TransactionalMetaTask {
    @Reference
    CleanK8sDAITask deleteOrCleanK8sDAITask;

    @Reference
    CreateOrUpdateK8sDAITask createOrUpdateK8sDAITask;

    @Reference
    ConformK8sDeploymentSpecInspectionPortsMetaTask conformK8sInspectionPortMetaTask;

    @Reference
    MgrCheckDevicesMetaTask managerCheckDevicesMetaTask;

    private KubernetesPodApi k8sPodApi;

    private DeploymentSpec ds;
    private TaskGraph tg;

    public ConformK8sDeploymentPodsMetaTask create(DeploymentSpec ds, KubernetesPodApi k8sPodApi) {
        ConformK8sDeploymentPodsMetaTask task = new ConformK8sDeploymentPodsMetaTask();
        task.deleteOrCleanK8sDAITask = this.deleteOrCleanK8sDAITask;
        task.createOrUpdateK8sDAITask = this.createOrUpdateK8sDAITask;
        task.conformK8sInspectionPortMetaTask = this.conformK8sInspectionPortMetaTask;
        task.managerCheckDevicesMetaTask = this.managerCheckDevicesMetaTask;

        task.ds = ds;
        task.k8sPodApi = this.k8sPodApi;

        task.dbConnectionManager = this.dbConnectionManager;
        task.txBroadcastUtil = this.txBroadcastUtil;

        return task;
    }

    public ConformK8sDeploymentPodsMetaTask create(DeploymentSpec ds) {
        return create(ds, null);
    }

    @Override
    public void executeTransaction(EntityManager em) throws Exception {
        this.tg = new TaskGraph();
        OSCEntityManager<DeploymentSpec> dsEmgr = new OSCEntityManager<DeploymentSpec>(DeploymentSpec.class, em, this.txBroadcastUtil);
        this.ds = dsEmgr.findByPrimaryKey(this.ds.getId());

        List<KubernetesPod> k8sPods = null;

        if (this.ds.getExternalId() != null) {
            try (KubernetesClient client = new KubernetesClient(this.ds.getVirtualSystem().getVirtualizationConnector())) {
                if (this.k8sPodApi == null) {
                    this.k8sPodApi = new KubernetesPodApi(client);
                } else {
                    this.k8sPodApi.setKubernetesClient(client);
                }

                k8sPods = this.k8sPodApi.getPodsByLabel(KubernetesDeploymentApi.OSC_DEPLOYMENT_LABEL_NAME + "=" + K8sUtil.getK8sName(this.ds));
            }
        }

        final List<String> existingPodIdsInOSC =
                CollectionUtils.emptyIfNull(this.ds.getDistributedApplianceInstances())
                .stream().map(DistributedApplianceInstance::getExternalId).collect(Collectors.toList());

        final List<String> existingPodIdsInK8s =
                CollectionUtils.emptyIfNull(k8sPods)
                .stream().map(KubernetesPod::getUid).collect(Collectors.toList());

        List<KubernetesPod> podsForDAICreation = CollectionUtils.emptyIfNull(k8sPods).stream().filter(pod -> !existingPodIdsInOSC.contains(pod.getUid())).collect(Collectors.toList());

        List<DistributedApplianceInstance> daisForDeletion =
                CollectionUtils.emptyIfNull(this.ds.getDistributedApplianceInstances()).stream().filter(dai -> !existingPodIdsInK8s.contains(dai.getExternalId())).collect(Collectors.toList());

        for (KubernetesPod podForDaiCreation : podsForDAICreation) {
            this.tg.addTask(this.createOrUpdateK8sDAITask.create(this.ds, podForDaiCreation));
        }

        for (DistributedApplianceInstance daiForDeletion : daisForDeletion) {
            this.tg.addTask(this.deleteOrCleanK8sDAITask.create(daiForDeletion));
        }

        this.tg.appendTask(this.conformK8sInspectionPortMetaTask.create(this.ds), TaskGuard.ALL_PREDECESSORS_COMPLETED);
        this.tg.appendTask(this.managerCheckDevicesMetaTask.create(this.ds.getVirtualSystem()), TaskGuard.ALL_PREDECESSORS_COMPLETED);
    }


    @Override
    public String getName() {
        return String.format("Conforming the K8s pods for the deployment spec %s", this.ds.getName());
    }

    @Override
    public TaskGraph getTaskGraph() {
        return this.tg;
    }

    @Override
    public Set<LockObjectReference> getObjects() {
        return LockObjectReference.getObjectReferences(this.ds);
    }
}