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
package org.osc.core.broker.service.tasks.conformance.openstack;

import javax.persistence.EntityManager;

import org.apache.log4j.Logger;
import org.osc.core.broker.model.entities.virtualization.openstack.OsFlavorReference;
import org.osc.core.broker.rest.client.openstack.openstack4j.Endpoint;
import org.osc.core.broker.rest.client.openstack.openstack4j.Openstack4JNova;
import org.osc.core.broker.service.persistence.OSCEntityManager;
import org.osc.core.broker.service.tasks.TransactionalTask;
import org.osgi.service.component.annotations.Component;

@Component(service = DeleteFlavorTask.class)
public class DeleteFlavorTask extends TransactionalTask {

    private final Logger log = Logger.getLogger(DeleteFlavorTask.class);

    private String region;
    private OsFlavorReference flavorReference;
    private Endpoint osEndPoint;

    public DeleteFlavorTask create(String region, OsFlavorReference flavorReference, Endpoint osEndPoint) {

        DeleteFlavorTask task = new DeleteFlavorTask();
        task.region = region;
        task.osEndPoint = osEndPoint;
        task.flavorReference = flavorReference;
        task.dbConnectionManager = this.dbConnectionManager;
        task.txBroadcastUtil = this.txBroadcastUtil;

        return task;
    }

    @Override
    public void executeTransaction(EntityManager em) throws Exception {
        this.log.info("Deleting flavor " + this.flavorReference.getFlavorRefId() + " from region " + this.region);

        Openstack4JNova nova = new Openstack4JNova(this.osEndPoint);
        nova.deleteFlavorById(this.region, this.flavorReference.getFlavorRefId());

        // We have to find the entity again as the one reference by
        // this.flavorReference is detached.
        OSCEntityManager.delete(em, em.find(OsFlavorReference.class,
                this.flavorReference.getId()), this.txBroadcastUtil);

    }

    @Override
    public String getName() {
        return String.format("Deleting Flavor with id '%s' from region '%s'", this.flavorReference.getFlavorRefId(),
                this.region);

    };

}
