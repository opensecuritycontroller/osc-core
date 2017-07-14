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
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.LockModeType;

import org.apache.log4j.Logger;
import org.openstack4j.api.Builders;
import org.openstack4j.model.network.SecurityGroup;
import org.openstack4j.model.network.SecurityGroupRule;
import org.osc.core.broker.job.TaskGraph;
import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.model.entities.appliance.VirtualSystem;
import org.osc.core.broker.model.entities.virtualization.openstack.DeploymentSpec;
import org.osc.core.broker.model.entities.virtualization.openstack.OsSecurityGroupReference;
import org.osc.core.broker.model.plugin.ApiFactoryService;
import org.osc.core.broker.rest.client.openstack.openstack4j.Endpoint;
import org.osc.core.broker.rest.client.openstack.openstack4j.Openstack4JNeutron;
import org.osc.core.broker.service.persistence.DeploymentSpecEntityMgr;
import org.osc.core.broker.service.persistence.OSCEntityManager;
import org.osc.core.broker.service.tasks.TransactionalMetaTask;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableList;

@Component(service = OsSecurityGroupCheckMetaTask.class)
public class OsSecurityGroupCheckMetaTask extends TransactionalMetaTask {

    private static final Logger log = Logger.getLogger(OsSecurityGroupCheckMetaTask.class);

    @Reference
    CreateOsSecurityGroupTask createOsSecurityGroupTask;

    @Reference
    private ApiFactoryService apiFactoryService;

    private DeploymentSpec ds;
    private TaskGraph tg;

    public OsSecurityGroupCheckMetaTask create(DeploymentSpec ds) {
        OsSecurityGroupCheckMetaTask task = new OsSecurityGroupCheckMetaTask();
        task.ds = ds;
        task.name = task.getName();
        task.createOsSecurityGroupTask = this.createOsSecurityGroupTask;
        task.apiFactoryService = this.apiFactoryService;
        task.dbConnectionManager = this.dbConnectionManager;
        task.txBroadcastUtil = this.txBroadcastUtil;

        return task;
    }

    @Override
    public void executeTransaction(EntityManager em) throws Exception {
        this.tg = new TaskGraph();
        DeploymentSpec ds = em.find(DeploymentSpec.class, this.ds.getId(), LockModeType.PESSIMISTIC_WRITE);
        VirtualSystem vs = ds.getVirtualSystem();
        log.info("Checking if VS" + vs.getName() + " has the corresponding Openstack Security Group");

        Endpoint endPoint = new Endpoint(ds);
        // Check if the VS have ds or dds with os security group reference
        OsSecurityGroupReference sgReference = null;
        List<DeploymentSpec> dss = DeploymentSpecEntityMgr.findDeploymentSpecsByVirtualSystemProjectAndRegion(
                em, ds.getVirtualSystem(), ds.getProjectId(), ds.getRegion());
        for (DeploymentSpec depSpec : dss) {
            if (depSpec.getOsSecurityGroupReference() != null) {
                sgReference = depSpec.getOsSecurityGroupReference();
                break;
            }
        }

        // The only SDN controller that currently returns true for supportsPortGroup is Nuage.
        boolean skipSecurityGroupCreation = vs.getVirtualizationConnector().isControllerDefined()
                ? this.apiFactoryService.supportsPortGroup(vs) : false;
        // If DS or DDS both have no os security group reference, create OS SG
        if (sgReference == null) {
            //TODO: sjallapx Hack to workaround Nuage SimpleDateFormat parse errors due to JCloud
            if (!skipSecurityGroupCreation) {
                this.tg.appendTask(this.createOsSecurityGroupTask.create(ds, endPoint));
            }
        } else {
            DeploymentSpec existingDs;
            try (Openstack4JNeutron neutron = new Openstack4JNeutron(endPoint)) {
                for (Iterator<DeploymentSpec> iterator = sgReference.getDeploymentSpecs().iterator(); iterator
                        .hasNext(); ) {
                    existingDs = iterator.next();
                    // For a given Project, region and VS there will be only one SG
                    if (existingDs.getRegion().equals(this.ds.getRegion())
                            && existingDs.getProjectName().equals(this.ds.getProjectName())
                            && existingDs.getVirtualSystem().getName().equals(this.ds.getVirtualSystem().getName())) {
                        SecurityGroup sg = neutron.getSecurityGroupById(ds.getRegion(), sgReference.getSgRefId());
                        if (sg == null) {
                            // remove the ds from the collection, delete the stale os sg reference from the database and create a new OS SG
                            iterator.remove();
                            OSCEntityManager.delete(em, sgReference, this.txBroadcastUtil);
                            //TODO: sjallapx Hack to workaround Nuage SimpleDateFormat parse errors due to JCloud
                            if (!skipSecurityGroupCreation) {
                                this.tg.appendTask(this.createOsSecurityGroupTask.create(ds, endPoint));
                            }
                        } else {
                            syncSGRules(sg, neutron);
                            sgReference.setSgRefName(sg.getName());
                            OSCEntityManager.update(em, sgReference, this.txBroadcastUtil);
                            ds.setOsSecurityGroupReference(sgReference);
                            OSCEntityManager.update(em, ds, this.txBroadcastUtil);
                        }
                    }
                }
            }
        }
    }

    private void syncSGRules(SecurityGroup sg, Openstack4JNeutron neutron) throws Exception {
        final List<? extends SecurityGroupRule> rules = sg.getRules();

        List<SecurityGroupRule> expectedList = new ArrayList<>();
        expectedList.add(Builders.securityGroupRule().protocol(null).ethertype(CreateOsSecurityGroupTask.IPV4)
                .direction(CreateOsSecurityGroupTask.INGRESS).build());
        expectedList.add(Builders.securityGroupRule().protocol(null).ethertype(CreateOsSecurityGroupTask.IPV4)
                .direction(CreateOsSecurityGroupTask.EGRESS).build());
        expectedList.add(Builders.securityGroupRule().protocol(null).ethertype(CreateOsSecurityGroupTask.IPV6)
                .direction(CreateOsSecurityGroupTask.INGRESS).build());
        expectedList.add(Builders.securityGroupRule().protocol(null).ethertype(CreateOsSecurityGroupTask.IPV6)
                .direction(CreateOsSecurityGroupTask.EGRESS).build());
        ImmutableList.<SecurityGroupRule>builder().addAll(expectedList);

        // Filter the missing rules from the expected SG rules
        Collection<SecurityGroupRule> missingRules = Collections2.filter(expectedList, expRule -> {
            for (SecurityGroupRule osRule : rules) {
                if (expRule != null && osRule.getDirection().equals(expRule.getDirection())
                        && osRule.getEtherType().equals(expRule.getEtherType()) && osRule.getProtocol() == null) {
                    return false;
                }
            }
            return true;
        });
        if (!missingRules.isEmpty()) {
            neutron.addSecurityGroupRules(sg, this.ds.getRegion(), missingRules);
        }
    }

    @Override
    public TaskGraph getTaskGraph() {
        return this.tg;
    }

    @Override
    public Set<LockObjectReference> getObjects() {
        return LockObjectReference.getObjectReferences(this.ds);
    }

    @Override
    public String getName() {
        return String.format(
                "Checking if Openstack Security Group exists for Virtual System '%s' in project '%s' for region '%s'",
                this.ds.getVirtualSystem().getName(), this.ds.getProjectName(), this.ds.getRegion());
    }

}
