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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.persistence.EntityManager;

import org.apache.log4j.Logger;
import org.jclouds.openstack.neutron.v2.domain.Rule;
import org.jclouds.openstack.neutron.v2.domain.RuleDirection;
import org.jclouds.openstack.neutron.v2.domain.RuleEthertype;
import org.jclouds.openstack.neutron.v2.domain.SecurityGroup;
import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.model.entities.virtualization.openstack.DeploymentSpec;
import org.osc.core.broker.model.entities.virtualization.openstack.OsSecurityGroupReference;
import org.osc.core.broker.rest.client.openstack.jcloud.Endpoint;
import org.osc.core.broker.rest.client.openstack.jcloud.JCloudNeutron;
import org.osc.core.broker.service.persistence.OSCEntityManager;
import org.osc.core.broker.service.tasks.TransactionalTask;
import org.osgi.service.component.annotations.Component;

@Component(service=CreateOsSecurityGroupTask.class)
public class CreateOsSecurityGroupTask extends TransactionalTask {

    private final Logger log = Logger.getLogger(CreateOsSecurityGroupTask.class);

    private DeploymentSpec ds;
    private String sgName;
    private Endpoint osEndPoint;

    public CreateOsSecurityGroupTask create(DeploymentSpec ds, Endpoint osEndPoint) {
        CreateOsSecurityGroupTask task = new CreateOsSecurityGroupTask();
        task.ds = ds;
        task.osEndPoint = osEndPoint;
        task.sgName = ds.getVirtualSystem().getName() + "_" + ds.getRegion() + "_" + ds.getTenantName();
        task.dbConnectionManager = this.dbConnectionManager;
        task.txBroadcastUtil = this.txBroadcastUtil;

        return task;
    }

    @Override
    public void executeTransaction(EntityManager em) throws Exception {

        this.ds = em.find(DeploymentSpec.class, this.ds.getId());

        JCloudNeutron neutron = new JCloudNeutron(this.osEndPoint);
        try {
            this.log.info("Creating Openstack Security Group " + this.sgName + " in tenant " + this.ds.getTenantName()
                    + " for region " + this.ds.getRegion());

            SecurityGroup securityGroup = neutron.createSecurityGroup(this.sgName, this.ds.getRegion());
            neutron.addSecurityGroupRules(securityGroup, this.ds.getRegion(), createSecurityGroupRules());
            OsSecurityGroupReference sgRef = new OsSecurityGroupReference(securityGroup.getId(), this.sgName, this.ds);
            this.ds.setOsSecurityGroupReference(sgRef);

            OSCEntityManager.create(em, sgRef, this.txBroadcastUtil);

        } finally {
            neutron.close();
        }
    }

    private List<Rule> createSecurityGroupRules() {
        List<Rule> expectedList = new ArrayList<>();
        expectedList.add(Rule.createBuilder(RuleDirection.INGRESS, "").ethertype(RuleEthertype.IPV4).protocol(null).build());
        expectedList.add(Rule.createBuilder(RuleDirection.INGRESS, "").ethertype(RuleEthertype.IPV6).protocol(null).build());
        return expectedList;
    }

    @Override
    public String getName() {
        return String.format("Creating Openstack Security Group '%s' in tenant '%s' for region '%s'", this.sgName, this.ds.getTenantName(), this.ds.getRegion());
    };

    @Override
    public Set<LockObjectReference> getObjects() {
        return LockObjectReference.getObjectReferences(this.ds);
    }

}