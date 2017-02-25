/*******************************************************************************
 * Copyright (c) 2017 Intel Corporation
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
import org.hibernate.Session;
import org.osc.core.broker.model.entities.virtualization.openstack.OsFlavorReference;
import org.osc.core.broker.rest.client.openstack.jcloud.Endpoint;
import org.osc.core.broker.rest.client.openstack.jcloud.JCloudNova;
import org.osc.core.broker.service.persistence.EntityManager;
import org.osc.core.broker.service.tasks.TransactionalTask;

public class DeleteFlavorTask extends TransactionalTask {

    private final Logger log = Logger.getLogger(DeleteFlavorTask.class);

    private String region;
    private OsFlavorReference flavorReference;
    private Endpoint osEndPoint;

    public DeleteFlavorTask(String region, OsFlavorReference flavorReference, Endpoint osEndPoint) {
        this.region = region;
        this.osEndPoint = osEndPoint;
        this.flavorReference = flavorReference;
    }

    @Override
    public void executeTransaction(Session session) throws Exception {
        this.log.info("Deleting flavor " + this.flavorReference.getFlavorRefId() + " from region " + this.region);

        JCloudNova nova = new JCloudNova(this.osEndPoint);
        try {
            nova.deleteFlavorById(this.region, this.flavorReference.getFlavorRefId());
        } finally {
            nova.close();
        }
        EntityManager.delete(session, this.flavorReference);

    }

    @Override
    public String getName() {
        return String.format("Deleting Flavor with id '%s' from region '%s'", this.flavorReference.getFlavorRefId(),
                this.region);

    };

}
