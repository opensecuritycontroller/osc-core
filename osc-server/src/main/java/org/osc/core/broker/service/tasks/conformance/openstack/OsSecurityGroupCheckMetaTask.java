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
import org.jclouds.openstack.neutron.v2.domain.Rule;
import org.jclouds.openstack.neutron.v2.domain.RuleDirection;
import org.jclouds.openstack.neutron.v2.domain.RuleEthertype;
import org.jclouds.openstack.neutron.v2.domain.SecurityGroup;
import org.osc.core.broker.job.TaskGraph;
import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.model.entities.appliance.VirtualSystem;
import org.osc.core.broker.model.entities.virtualization.openstack.DeploymentSpec;
import org.osc.core.broker.model.entities.virtualization.openstack.OsSecurityGroupReference;
import org.osc.core.broker.model.plugin.sdncontroller.SdnControllerApiFactory;
import org.osc.core.broker.rest.client.openstack.jcloud.Endpoint;
import org.osc.core.broker.rest.client.openstack.jcloud.JCloudNeutron;
import org.osc.core.broker.service.persistence.DeploymentSpecEntityMgr;
import org.osc.core.broker.service.persistence.OSCEntityManager;
import org.osc.core.broker.service.tasks.TransactionalMetaTask;

import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableList;

public class OsSecurityGroupCheckMetaTask extends TransactionalMetaTask {

    private static final Logger log = Logger.getLogger(OsSecurityGroupCheckMetaTask.class);

    private DeploymentSpec ds;
    private TaskGraph tg;

    public OsSecurityGroupCheckMetaTask(DeploymentSpec ds) {
        this.ds = ds;
        this.name = getName();
    }

    @Override
    public void executeTransaction(EntityManager em) throws Exception {
        this.tg = new TaskGraph();
        DeploymentSpec ds = em.find(DeploymentSpec.class, this.ds.getId(),
                LockModeType.PESSIMISTIC_WRITE);
        VirtualSystem vs = ds.getVirtualSystem();
        log.info(
                "Checking if VS" + vs.getName() + " has the corresponding Openstack Security Group");

        Endpoint endPoint = new Endpoint(ds);
        try (JCloudNeutron neutron = new JCloudNeutron(endPoint)) {
            // Check if the VS have ds or dds with os security group reference
            OsSecurityGroupReference sgReference = null;
            List<DeploymentSpec> dss = DeploymentSpecEntityMgr.findDeploymentSpecsByVirtualSystemTenantAndRegion(
                    em, ds.getVirtualSystem(), ds.getTenantId(), ds.getRegion());
            for (DeploymentSpec depSpec : dss) {
                if (depSpec.getOsSecurityGroupReference() != null) {
                    sgReference = depSpec.getOsSecurityGroupReference();
                    break;
                }
            }

            // The only SDN controller that currently returns true for supportsPortGroup is Nuage.
            boolean isNuageController = SdnControllerApiFactory.supportsPortGroup(vs);
            // If DS or DDS both have no os security group reference, create OS SG
            if (sgReference == null) {
                //TODO: sjallapx Hack to workaround Nuage SimpleDateFormat parse errors due to JCloud
                if (!isNuageController) {
                    this.tg.appendTask(new CreateOsSecurityGroupTask(ds, endPoint));
                }
            } else {
                DeploymentSpec existingDs = null;
                for (Iterator<DeploymentSpec> iterator = sgReference.getDeploymentSpecs().iterator(); iterator
                        .hasNext();) {
                    existingDs = iterator.next();
                    // For a given tenant, region and VS there will be only one SG
                    if (existingDs.getRegion().equals(this.ds.getRegion())
                            && existingDs.getTenantName().equals(this.ds.getTenantName())
                            && existingDs.getVirtualSystem().getName().equals(this.ds.getVirtualSystem().getName())) {
                        SecurityGroup sg = neutron.getSecurityGroupById(ds.getRegion(), sgReference.getSgRefId());
                        if (sg == null) {
                            // remove the ds from the collection, delete the stale os sg reference from the database and create a new OS SG
                            iterator.remove();
                            OSCEntityManager.delete(em, sgReference);
                            //TODO: sjallapx Hack to workaround Nuage SimpleDateFormat parse errors due to JCloud
                            if (!isNuageController) {
                                this.tg.appendTask(new CreateOsSecurityGroupTask(ds, endPoint));
                            }
                        } else {
                            syncSGRules(sg, neutron);
                            sgReference.setSgRefName(sg.getName());
                            OSCEntityManager.update(em, sgReference);
                            ds.setOsSecurityGroupReference(sgReference);
                            OSCEntityManager.update(em, ds);
                        }
                    }
                }
            }
        }
    }

    private void syncSGRules(SecurityGroup sg, JCloudNeutron neutron) throws Exception {
        final ImmutableList<Rule> rules = sg.getRules();
        List<Rule> expectedList = new ArrayList<>();
        expectedList.add(
                Rule.createBuilder(RuleDirection.INGRESS, "").ethertype(RuleEthertype.IPV4).protocol(null).build());
        expectedList
        .add(Rule.createBuilder(RuleDirection.EGRESS, "").ethertype(RuleEthertype.IPV4).protocol(null).build());
        expectedList.add(
                Rule.createBuilder(RuleDirection.INGRESS, "").ethertype(RuleEthertype.IPV6).protocol(null).build());
        expectedList
        .add(Rule.createBuilder(RuleDirection.EGRESS, "").ethertype(RuleEthertype.IPV6).protocol(null).build());

        ImmutableList.<Rule>builder().addAll(expectedList);

        // Filter the missing rules from the expected SG rules
        Collection<Rule> missingRules = Collections2.filter(expectedList, new Predicate<Rule>() {

            @Override
            public boolean apply(Rule expRule) {
                for (Rule osRule : rules) {
                    if (osRule.getDirection().equals(expRule.getDirection())
                            && osRule.getEthertype().equals(expRule.getEthertype()) && osRule.getProtocol() == null) {
                        return false;
                    }
                }
                return true;
            }

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
                "Checking if Openstack Security Group exists for Virtual System '%s' in tenant '%s' for region '%s'",
                this.ds.getVirtualSystem().getName(), this.ds.getTenantName(), this.ds.getRegion());
    }

}
