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
import org.openstack4j.api.Builders;
import org.openstack4j.model.network.SecurityGroup;
import org.openstack4j.model.network.SecurityGroupRule;
import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.model.entities.virtualization.openstack.DeploymentSpec;
import org.osc.core.broker.model.entities.virtualization.openstack.OsSecurityGroupReference;
import org.osc.core.broker.rest.client.openstack.openstack4j.Endpoint;
import org.osc.core.broker.rest.client.openstack.openstack4j.Openstack4JNeutron;
import org.osc.core.broker.service.persistence.OSCEntityManager;
import org.osc.core.broker.service.tasks.TransactionalTask;
import org.osgi.service.component.annotations.Component;

@Component(service = CreateOsSecurityGroupTask.class)
public class CreateOsSecurityGroupTask extends TransactionalTask {

    private final Logger log = Logger.getLogger(CreateOsSecurityGroupTask.class);
    final static String INGRESS = "ingress";
    final static String EGRESS = "egress";
    final static String IPV4 = "IPv4";
    final static String IPV6 = "IPv6";

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

        this.log.info("Creating Openstack Security Group " + this.sgName + " in tenant " + this.ds.getTenantName()
                + " for region " + this.ds.getRegion());
        try (Openstack4JNeutron neutron = new Openstack4JNeutron(this.osEndPoint)) {
            SecurityGroup securityGroup = neutron.createSecurityGroup(this.sgName, this.ds.getRegion());
            neutron.addSecurityGroupRules(securityGroup, this.ds.getRegion(), createSecurityGroupRules());
            OsSecurityGroupReference sgRef = new OsSecurityGroupReference(securityGroup.getId(), this.sgName, this.ds);
            this.ds.setOsSecurityGroupReference(sgRef);
            OSCEntityManager.create(em, sgRef, this.txBroadcastUtil);
        }
    }

    private List<SecurityGroupRule> createSecurityGroupRules() {
        List<SecurityGroupRule> expectedList = new ArrayList<>();
        expectedList.add(Builders.securityGroupRule().protocol(null).ethertype(IPV4).direction(INGRESS).build());
        expectedList.add(Builders.securityGroupRule().protocol(null).ethertype(IPV6).direction(INGRESS).build());
        return expectedList;
    }

    @Override
    public String getName() {
        return String.format("Creating Openstack Security Group '%s' in tenant '%s' for region '%s'", this.sgName, this.ds.getTenantName(), this.ds.getRegion());
    }

    @Override
    public Set<LockObjectReference> getObjects() {
        return LockObjectReference.getObjectReferences(this.ds);
    }

}