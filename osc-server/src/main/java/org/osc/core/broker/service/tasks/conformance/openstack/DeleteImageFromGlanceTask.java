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

import org.apache.log4j.Logger;
import org.osc.core.broker.model.entities.virtualization.openstack.OsImageReference;
import org.osc.core.broker.rest.client.openstack.openstack4j.Endpoint;
import org.osc.core.broker.rest.client.openstack.openstack4j.Openstack4jGlance;
import org.osc.core.broker.service.persistence.OSCEntityManager;
import org.osc.core.broker.service.tasks.TransactionalTask;
import org.osgi.service.component.annotations.Component;

import javax.persistence.EntityManager;

@Component(service = DeleteImageFromGlanceTask.class)
public class DeleteImageFromGlanceTask extends TransactionalTask {
    private final Logger log = Logger.getLogger(DeleteImageFromGlanceTask.class);

    private String region;
    private OsImageReference imageReference;
    private Endpoint osEndPoint;

    public DeleteImageFromGlanceTask create(String region, OsImageReference imageReference, Endpoint osEndPoint) {

        DeleteImageFromGlanceTask task = new DeleteImageFromGlanceTask();
        task.region = region;
        task.osEndPoint = osEndPoint;
        task.imageReference = imageReference;
        task.name = task.getName();
        task.dbConnectionManager = this.dbConnectionManager;
        task.txBroadcastUtil = this.txBroadcastUtil;

        return task;
    }

    @Override
    public void executeTransaction(EntityManager em) throws Exception {
        this.imageReference = em.find(OsImageReference.class, this.imageReference.getId());
        this.log.info("Deleting image " + this.imageReference.getImageRefId() + " from region " + this.region);
        try (Openstack4jGlance glance = new Openstack4jGlance(this.osEndPoint)) {
            glance.deleteImageById(this.region, this.imageReference.getImageRefId());
        }
        OSCEntityManager.delete(em, this.imageReference, this.txBroadcastUtil);
    }

    @Override
    public String getName() {
        return String.format("Deleting image with id: '%s' from region '%s'", this.imageReference.getImageRefId(), this.region);
    }

}
