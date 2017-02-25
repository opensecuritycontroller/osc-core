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

import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.model.entities.appliance.DistributedAppliance;
import org.osc.core.broker.model.entities.appliance.DistributedApplianceInstance;
import org.osc.core.broker.model.entities.appliance.VirtualSystem;
import org.osc.core.broker.model.entities.virtualization.SecurityGroup;
import org.osc.core.broker.model.entities.virtualization.SecurityGroupInterface;
import org.osc.core.broker.model.entities.virtualization.openstack.DeploymentSpec;
import org.osc.core.broker.service.persistence.DistributedApplianceEntityMgr;
import org.osc.core.broker.service.persistence.EntityManager;
import org.osc.core.broker.service.tasks.TransactionalTask;

public class ForceDeleteDATask extends TransactionalTask {
    private static final Logger log = Logger.getLogger(ForceDeleteDATask.class);

    private DistributedAppliance da;

    public ForceDeleteDATask(DistributedAppliance da) {
        this.da = da;
        this.name = getName();
    }

    @Override
    public String getName() {
        return "Force Delete Distributed Appliance '" + this.da.getName() + "'";
    }

    @Override
    public void executeTransaction(Session session) {
        log.debug("Force Delete Task for DA: " + this.da.getId());
        // load Distributed Appliance from Database
        DistributedAppliance da = DistributedApplianceEntityMgr.findById(session, this.da.getId());

        // remove all virtual systems for this DA
        for (VirtualSystem vs : da.getVirtualSystems()) {

            // remove all DAI(s)
            for (DistributedApplianceInstance dai : vs.getDistributedApplianceInstances()) {
                EntityManager.delete(session, dai);
            }

            // remove all SGI(s) - SG references
            for (SecurityGroupInterface sgi : vs.getSecurityGroupInterfaces()) {
                for (SecurityGroup sg : sgi.getSecurityGroups()) {
                    sgi.removeSecurity(sg);
                    sg.removeSecurityInterface(sgi);
                    EntityManager.update(session, sg);
                    EntityManager.update(session, sgi);
                }
            }

            // remove all Deployment Specs for this virtual system
            for (DeploymentSpec ds : vs.getDeploymentSpecs()) {
                EntityManager.delete(session, ds);
            }

            // remove all SGI for this virtual system
            for (SecurityGroupInterface sgi : vs.getSecurityGroupInterfaces()) {
                EntityManager.delete(session, sgi);
            }

            // delete virtual system from database
            EntityManager.delete(session, vs);
        }

        // delete distributed appliance from database
        EntityManager.delete(session, da);
    }

    @Override
    public Set<LockObjectReference> getObjects() {
        return LockObjectReference.getObjectReferences(this.da);
    }

}
