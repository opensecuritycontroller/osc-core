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
import org.osc.core.broker.model.entities.appliance.DistributedApplianceInstance;
import org.osc.core.broker.model.entities.appliance.VirtualSystem;
import org.osc.core.broker.model.entities.virtualization.SecurityGroup;
import org.osc.core.broker.model.entities.virtualization.SecurityGroupInterface;
import org.osc.core.broker.model.entities.virtualization.openstack.DeploymentSpec;
import org.osc.core.broker.service.persistence.OSCEntityManager;
import org.osc.core.broker.service.tasks.TransactionalTask;
import org.osgi.service.component.annotations.Component;

@Component(service=ForceDeleteVirtualSystemTask.class)
public class ForceDeleteVirtualSystemTask extends TransactionalTask {

    private VirtualSystem vs;

    public ForceDeleteVirtualSystemTask create(VirtualSystem vs) {
        ForceDeleteVirtualSystemTask task = new ForceDeleteVirtualSystemTask();
        task.vs = vs;
        task.name = task.getName();
        task.dbConnectionManager = this.dbConnectionManager;
        task.txBroadcastUtil = this.txBroadcastUtil;

        return task;
    }

    @Override
    public String getName() {
        return "Force Delete Virtual System '" + this.vs.getName() + "'";
    }

    @Override
    public void executeTransaction(EntityManager em) {
        // load Distributed Appliance from Database
        this.vs = em.find(VirtualSystem.class, this.vs.getId());

        // remove all DAI(s)
        for (DistributedApplianceInstance dai : this.vs.getDistributedApplianceInstances()) {
            OSCEntityManager.delete(em, dai, this.txBroadcastUtil);
        }

        // remove all SGI(s) - SG references
        for (SecurityGroupInterface sgi : this.vs.getSecurityGroupInterfaces()) {
            for (SecurityGroup sg : sgi.getSecurityGroups()) {
                sgi.removeSecurity(sg);
                sg.removeSecurityInterface(sgi);
                OSCEntityManager.update(em, sg, this.txBroadcastUtil);
                OSCEntityManager.update(em, sgi, this.txBroadcastUtil);
            }
        }

        // remove all Deployment Specs for this virtual system
        for (DeploymentSpec ds : this.vs.getDeploymentSpecs()) {
            OSCEntityManager.delete(em, ds, this.txBroadcastUtil);
        }

        // remove all SGI for this virtual system
        for (SecurityGroupInterface sgi : this.vs.getSecurityGroupInterfaces()) {
            OSCEntityManager.delete(em, sgi, this.txBroadcastUtil);
        }

        //TODO: Delete OsFlavorReference and OsImageReferences too

        // delete virtual system from database
        OSCEntityManager.delete(em, this.vs, this.txBroadcastUtil);
    }

    @Override
    public Set<LockObjectReference> getObjects() {
        return LockObjectReference.getObjectReferences(this.vs);
    }

}
