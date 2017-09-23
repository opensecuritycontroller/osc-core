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

import org.osc.core.broker.rest.client.k8s.KubernetesPod;
import org.osc.core.broker.service.tasks.TransactionalTask;

public class LabelPodCreateTask extends TransactionalTask{

    private KubernetesPod pod;

    @Override
    public void executeTransaction(EntityManager em) throws Exception {
        // TODO Auto-generated method stub
    }

    public LabelPodCreateTask create(KubernetesPod pod) {
        LabelPodCreateTask task = new LabelPodCreateTask();
        task.pod = pod;
        return task;
    }

    @Override
    public String getName() {
        return "Label pod create task for pod " + this.pod.getUid();
    }
}
