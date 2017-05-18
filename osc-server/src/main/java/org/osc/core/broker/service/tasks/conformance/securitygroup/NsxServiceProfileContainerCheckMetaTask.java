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
package org.osc.core.broker.service.tasks.conformance.securitygroup;

import java.util.List;
import java.util.Set;

import javax.persistence.EntityManager;

import org.osc.core.broker.job.TaskGraph;
import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.model.entities.appliance.VirtualSystem;
import org.osc.core.broker.model.entities.virtualization.SecurityGroup;
import org.osc.core.broker.model.entities.virtualization.SecurityGroupInterface;
import org.osc.core.broker.model.entities.virtualization.SecurityGroupMember;
import org.osc.core.broker.model.entities.virtualization.SecurityGroupMemberType;
import org.osc.core.broker.service.persistence.OSCEntityManager;
import org.osc.core.broker.service.persistence.SecurityGroupEntityMgr;
import org.osc.core.broker.service.persistence.SecurityGroupInterfaceEntityMgr;
import org.osc.core.broker.service.request.ContainerSet;
import org.osc.core.broker.service.request.ContainerSet.Container;
import org.osc.core.broker.service.tasks.InfoTask;
import org.osc.core.broker.service.tasks.TransactionalMetaTask;
import org.osc.core.broker.service.tasks.conformance.openstack.securitygroup.AddSecurityGroupMemberTask;
import org.osc.core.broker.service.tasks.conformance.openstack.securitygroup.DeleteSecurityGroupFromDbTask;
import org.osc.core.broker.service.tasks.conformance.openstack.securitygroup.SecurityGroupMemberDeleteTask;
import org.osc.sdk.sdn.element.ServiceProfileElement;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(service = NsxServiceProfileContainerCheckMetaTask.class)
public class NsxServiceProfileContainerCheckMetaTask extends TransactionalMetaTask {
    //private static final Logger log = Logger.getLogger(NsxServiceProfileContainerCheckMetaTask.class);

    @Reference
    AddSecurityGroupMemberTask addSecurityGroupMemberTask;

    @Reference
    SecurityGroupMemberDeleteTask securityGroupMemberDeleteTask;

    @Reference
    DeleteSecurityGroupFromDbTask deleteSecurityGroupFromDbTask;

    private VirtualSystem vs;
    private SecurityGroupInterface sgi;
    private ContainerSet containerSet;
    private TaskGraph tg;
    private String tag;
    private String sgiName;

    private NsxServiceProfileContainerCheckMetaTask create() {
        NsxServiceProfileContainerCheckMetaTask task = new NsxServiceProfileContainerCheckMetaTask();
        task.addSecurityGroupMemberTask = this.addSecurityGroupMemberTask;
        task.securityGroupMemberDeleteTask = this.securityGroupMemberDeleteTask;
        task.deleteSecurityGroupFromDbTask = this.deleteSecurityGroupFromDbTask;
        task.dbConnectionManager = this.dbConnectionManager;
        task.txBroadcastUtil = this.txBroadcastUtil;

        return task;
    }

    public NsxServiceProfileContainerCheckMetaTask create(SecurityGroupInterface sgi, ContainerSet containerSet) {
        NsxServiceProfileContainerCheckMetaTask task = create();
        this.sgi = sgi;
        this.vs = sgi.getVirtualSystem();
        this.tag = sgi.getTag();
        this.sgiName = sgi.getName();
        this.containerSet = containerSet;
        this.name = getName();
        return task;
    }

    public NsxServiceProfileContainerCheckMetaTask create(VirtualSystem vs, ServiceProfileElement serviceProfile,
            ContainerSet containerSet) {
        NsxServiceProfileContainerCheckMetaTask task = create();
        this.vs = vs;
        this.tag = serviceProfile.getId();
        this.sgiName = serviceProfile.getName();
        this.containerSet = containerSet;
        this.name = getName();

        return task;
    }

    @Override
    public void executeTransaction(EntityManager em) throws Exception {
        this.tg = new TaskGraph();

        if (this.sgi == null) {
            this.sgi = SecurityGroupInterfaceEntityMgr.findSecurityGroupInterfaceByVsAndTag(em, this.vs, this.tag);
        } else {
            this.sgi = em.find(SecurityGroupInterface.class, this.sgi.getId());
        }

        List<SecurityGroup> sgs = SecurityGroupEntityMgr.findByNsxServiceProfileIdAndVs(em,
                this.sgi.getVirtualSystem(), this.sgi.getTag());

        for (Container container : this.containerSet.getList()) {
            SecurityGroup sg = findSecurityGroup(sgs, container.getId());
            if (sg == null) {
                this.tg.appendTask(new InfoTask(String.format("Creating Security Group '%s'", container.getName())));

                sg = new SecurityGroup(this.sgi.getVirtualSystem().getVirtualizationConnector(), container.getId());
                sg.setName(container.getName());
                sg.addSecurityGroupInterface(this.sgi);
                OSCEntityManager.create(em, sg, this.txBroadcastUtil);
                this.sgi.addSecurityGroup(sg);
                OSCEntityManager.update(em, this.sgi, this.txBroadcastUtil);
            } else {
                if (!sg.getName().equals(container.getName())) {
                    this.tg.appendTask(new InfoTask(String.format("Renaming Security Group from '%s' to '%s'",
                            sg.getName(), container.getName())));

                    sg.setName(container.getName());
                    OSCEntityManager.update(em, sg, this.txBroadcastUtil);
                }
            }

            if (container.getAddress() != null) {
                for (String address : container.getAddress()) {
                    SecurityGroupMember sgm = findSecurityGroupMember(sg.getSecurityGroupMembers(),
                            SecurityGroupMemberType.fromText(container.getType()), address);
                    if (sgm == null) {
                        this.tg.appendTask(this.addSecurityGroupMemberTask.create(sg, SecurityGroupMemberType
                                .fromText(container.getType()), address));
                    }
                }
            }

            // Remove dangling sgm
            for (SecurityGroupMember sgm : sg.getSecurityGroupMembers()) {
                if (!isExistSecurityGroupMember(sgm, container)) {
                    this.tg.appendTask(this.securityGroupMemberDeleteTask.create(sgm));
                }
            }
        }

        // Remove dangling sg
        for (SecurityGroup sg : sgs) {
            if (!isExistSecurityGroup(sg, this.containerSet.getList())) {
                this.tg.appendTask(this.deleteSecurityGroupFromDbTask.create(sg));
            }
        }
    }

    private boolean isExistSecurityGroup(SecurityGroup sg, List<Container> containers) {
        for (Container container : containers) {
            if (sg.getNsxId().equals(container.getId())) {
                return true;
            }
        }
        return false;
    }

    private boolean isExistSecurityGroupMember(SecurityGroupMember sgm, Container container) {
        if (container.getAddress() != null) {
            for (String address : container.getAddress()) {
                if (sgm.getAddress().equals(address)
                        && SecurityGroupMemberType.fromText(container.getType()) == sgm.getType()) {
                    return true;
                }
            }
        }
        return false;
    }

    private SecurityGroupMember findSecurityGroupMember(Set<SecurityGroupMember> securityGroupMembers,
            SecurityGroupMemberType memberType, String address) {
        for (SecurityGroupMember sgm : securityGroupMembers) {
            if (sgm.getAddress().equals(address) && memberType == sgm.getType()) {
                return sgm;
            }
        }
        return null;
    }

    private SecurityGroup findSecurityGroup(List<SecurityGroup> sgs, String id) {
        for (SecurityGroup sg : sgs) {
            if (sg.getNsxId().equals(id)) {
                return sg;
            }
        }
        return null;
    }

    @Override
    public String getName() {
        return "Checking Service Profile Container '" + this.tag + "' on SGI '" + this.sgiName + "'";
    }

    @Override
    public TaskGraph getTaskGraph() {
        return this.tg;
    }

    @Override
    public Set<LockObjectReference> getObjects() {
        return LockObjectReference.getObjectReferences(this.sgi);
    }

}
