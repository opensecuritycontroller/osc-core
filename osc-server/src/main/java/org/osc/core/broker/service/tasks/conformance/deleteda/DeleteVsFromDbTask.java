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
package org.osc.core.broker.service.tasks.conformance.deleteda;

import java.util.Set;

import javax.persistence.EntityManager;

import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.model.entities.appliance.VirtualSystem;
import org.osc.core.broker.service.exceptions.VmidcBrokerInvalidRequestException;
import org.osc.core.broker.service.persistence.OSCEntityManager;
import org.osc.core.broker.service.tasks.TransactionalTask;
import org.osgi.service.component.annotations.Component;

@Component(service = DeleteVsFromDbTask.class)
public class DeleteVsFromDbTask extends TransactionalTask {
    //private static final Logger log = LoggerFactory.getLogger(DeleteVsFromDbTask.class);

    private VirtualSystem vs;

    public DeleteVsFromDbTask create(VirtualSystem vs) {
        DeleteVsFromDbTask task = new DeleteVsFromDbTask();
        task.vs = vs;
        task.name = task.getName();
        task.dbConnectionManager = this.dbConnectionManager;
        task.txBroadcastUtil = this.txBroadcastUtil;

        return task;
    }

    @Override
    public void executeTransaction(EntityManager em) throws Exception {
        this.vs = em.find(VirtualSystem.class, this.vs.getId());
        if (this.vs.getSecurityGroupInterfaces().size() > 0) {
            throw new VmidcBrokerInvalidRequestException(String.format(
                    "Virtual System '%s' has Traffic Policy Mappings which"
                            + " need to be unbinded before the Virtual system can be deleted", this.vs
                            .getVirtualizationConnector().getName()));
        }
        OSCEntityManager.delete(em, this.vs, this.txBroadcastUtil);
    }

    @Override
    public String getName() {
        return "Delete Virtualization System '" + this.vs.getVirtualizationConnector().getName() + "'";
    }

    @Override
    public Set<LockObjectReference> getObjects() {
        return LockObjectReference.getObjectReferences(this.vs);
    }

}
