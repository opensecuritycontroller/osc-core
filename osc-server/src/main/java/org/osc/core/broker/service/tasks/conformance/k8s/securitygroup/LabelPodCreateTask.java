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

import javax.persistence.EntityManager;

import org.osc.core.broker.model.entities.virtualization.k8s.Label;
import org.osc.core.broker.rest.client.k8s.KubernetesPod;
import org.osc.core.broker.rest.client.k8s.KubernetesPodApi;
import org.osc.core.broker.service.tasks.TransactionalTask;

public class LabelPodCreateTask extends TransactionalTask{

    private KubernetesPod pod;
    private KubernetesPodApi k8sPodApi;
    private Label label;

    @Override
    public void executeTransaction(EntityManager em) throws Exception {
        // TODO Auto-generated method stub
    }

    public LabelPodCreateTask create(KubernetesPod pod, Label label, KubernetesPodApi k8sPodApi) {
        LabelPodCreateTask task = new LabelPodCreateTask();
        task.pod = pod;
        task.label = label;
        task.dbConnectionManager = this.dbConnectionManager;
        task.txBroadcastUtil = this.txBroadcastUtil;
        this.k8sPodApi = k8sPodApi;
        return task;
    }

    @Override
    public String getName() {
        return "Label pod create task for pod " + this.pod.getUid();
    }
}
