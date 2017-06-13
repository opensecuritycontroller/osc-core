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
package org.osc.core.broker.service.securitygroup;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.persistence.EntityManager;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.jclouds.openstack.keystone.v2_0.domain.Tenant;
import org.osc.core.broker.model.entities.virtualization.SecurityGroup;
import org.osc.core.broker.model.entities.virtualization.SecurityGroupMember;
import org.osc.core.broker.model.entities.virtualization.SecurityGroupMemberType;
import org.osc.core.broker.model.entities.virtualization.VirtualizationConnector;
import org.osc.core.broker.model.entities.virtualization.openstack.Network;
import org.osc.core.broker.model.entities.virtualization.openstack.OsProtectionEntity;
import org.osc.core.broker.model.entities.virtualization.openstack.Subnet;
import org.osc.core.broker.model.entities.virtualization.openstack.VM;
import org.osc.core.broker.model.entities.virtualization.openstack.VMPort;
import org.osc.core.broker.model.plugin.sdncontroller.ControllerType;
import org.osc.core.broker.rest.client.openstack.jcloud.Endpoint;
import org.osc.core.broker.rest.client.openstack.jcloud.JCloudKeyStone;
import org.osc.core.broker.rest.client.openstack.jcloud.JCloudNova;
import org.osc.core.broker.service.ServiceDispatcher;
import org.osc.core.broker.service.exceptions.VmidcBrokerInvalidEntryException;
import org.osc.core.broker.service.exceptions.VmidcBrokerValidationException;
import org.osc.core.broker.service.persistence.NetworkEntityManager;
import org.osc.core.broker.service.persistence.OSCEntityManager;
import org.osc.core.broker.service.persistence.SubnetEntityManager;
import org.osc.core.broker.service.persistence.VMEntityManager;
import org.osc.core.broker.service.persistence.VirtualizationConnectorEntityMgr;
import org.osc.core.broker.service.request.Request;
import org.osc.core.broker.service.response.Response;
import org.osc.core.broker.service.securitygroup.exception.SecurityGroupMemberPartOfAnotherSecurityGroupException;
import org.osc.core.broker.view.common.VmidcMessages;
import org.osc.core.broker.view.common.VmidcMessages_;

public abstract class BaseSecurityGroupService<I extends Request, O extends Response> extends ServiceDispatcher<I, O> {

    private static final Logger log = Logger.getLogger(BaseSecurityGroupService.class);

    protected VirtualizationConnector vc;
    protected List<String> regions = new ArrayList<>();

    /**
     * Validates Virtualization connector and tenant exists
     */
    protected void validateAndLoad(EntityManager em, SecurityGroupDto dto) throws Exception {
        SecurityGroupDto.checkForNullFields(dto);
        SecurityGroupDto.checkFieldLength(dto);

        if (dto.getParentId() == null) {
            throw new VmidcBrokerValidationException("Virtualization Connector Id needs to be specified");
        }

        this.vc = VirtualizationConnectorEntityMgr.findById(em, dto.getParentId());

        if (this.vc == null) {
            throw new VmidcBrokerValidationException("Virtualization Connector with Id: " + dto.getParentId()
                    + "  is not found.");
        }

        if (this.vc.getControllerType().equals(ControllerType.NONE.getValue())) {
            throw new VmidcBrokerValidationException(
                    "Creation of Security Groups is not allowed in the absence of SDN Controller.");
        }

        JCloudKeyStone keystone = null;
        JCloudNova novaApi = null;
        try {
            keystone = new JCloudKeyStone(new Endpoint(this.vc));
            Tenant tenant = keystone.getTenantById(dto.getTenantId());

            if (tenant == null) {
                throw new VmidcBrokerValidationException("Tenant: '" + dto.getTenantName() + "' does not exist.");
            }

            novaApi = new JCloudNova(new Endpoint(this.vc, tenant.getName()));

            this.regions.addAll(novaApi.listRegions());
        } finally {
            if (keystone != null) {
                keystone.close();
            }
            if (novaApi != null) {
                novaApi.close();
            }
        }
    }

    /**
     * Validates the member to check for null fields and also check the region specified exists
     *
     */
    protected void validate(SecurityGroupMemberItemDto memberItem) throws VmidcBrokerInvalidEntryException,
            VmidcBrokerValidationException {
        SecurityGroupMemberItemDto.checkForNullFields(memberItem);
        if (!this.regions.contains(memberItem.getRegion())) {
            throw new VmidcBrokerValidationException(String.format("Region: '%s' does not exist for member '%s'",
                    memberItem.getRegion(), memberItem.getName()));
        }
        if (memberItem.isProtectExternal()){
            if (memberItem.getType().equals(SecurityGroupMemberType.VM) ||
                    memberItem.getType().equals(SecurityGroupMemberType.NETWORK)){
                throw new VmidcBrokerValidationException(String.format("Protect External: Not allowed for type '%s' member '%s'",
                        memberItem.getType(), memberItem.getName()));
            }
        }
    }

    public static void addSecurityGroupMember(EntityManager em, SecurityGroup securityGroup,
            SecurityGroupMemberItemDto securityGroupMemberDto) throws VmidcBrokerValidationException {
        String openstackId = securityGroupMemberDto.getOpenstackId();
        SecurityGroupMemberType type = securityGroupMemberDto.getType();
        OsProtectionEntity entity = null;

        if (type == SecurityGroupMemberType.VM) {
            entity = VMEntityManager.findByOpenstackId(em, openstackId);
        } else if (type == SecurityGroupMemberType.NETWORK) {
            entity = NetworkEntityManager.findByOpenstackId(em, openstackId);
        } else if (type == SecurityGroupMemberType.SUBNET) {
            entity = SubnetEntityManager.findByOpenstackId(em, openstackId);
        } else {
            throw new VmidcBrokerValidationException(String.format("Invalid Security Group Member Type ('%s')", type));
        }

        if (entity == null) {
            if (type == SecurityGroupMemberType.VM) {
                entity = new VM(securityGroupMemberDto.getRegion(), openstackId, securityGroupMemberDto.getName());
                log.info("Creating VM Member: " + securityGroupMemberDto.getName());

            } else if (type == SecurityGroupMemberType.NETWORK) {
                entity = new Network(securityGroupMemberDto.getRegion(), openstackId, securityGroupMemberDto.getName());
                log.info("Creating Network Member: " + securityGroupMemberDto.getName());
            } else if (type == SecurityGroupMemberType.SUBNET) {
                entity = new Subnet(securityGroupMemberDto.getParentOpenStackId(),
                        securityGroupMemberDto.getOpenstackId(), securityGroupMemberDto.getName(),
                        securityGroupMemberDto.getRegion(), securityGroupMemberDto.isProtectExternal());
                log.info("Creating Subnet Member: " + securityGroupMemberDto.getName());
            } else {
                throw new VmidcBrokerValidationException(String.format("Invalid Security Group Member Type ('%s')",
                        type));
            }
            OSCEntityManager.create(em, entity);
            SecurityGroupMember securityGroupMember = new SecurityGroupMember(securityGroup, entity);
            OSCEntityManager.create(em, securityGroupMember);
        } else {
            log.info(type.toString() + " Already exists in DB: " + securityGroupMemberDto.getName());

            Set<SecurityGroupMember> securityGroupMembers = entity.getSecurityGroupMembers();
            if (securityGroupMembers != null && !securityGroupMembers.isEmpty()) {
                for (SecurityGroupMember sgm : securityGroupMembers) {
                    SecurityGroup otherSecurityGroup = sgm.getSecurityGroup();
                    if (otherSecurityGroup.equals(securityGroup)) {
                        log.info(type.toString() + ": " + securityGroupMemberDto.getName()
                                + " already part of security Group: " + securityGroup.getName());
                        // If entity is already a member of this group, but it was marked as deleted, unmark it since its
                        // been added again
                        if (sgm.getMarkedForDeletion()) {
                            log.info(type.toString() + ": " + securityGroupMemberDto.getName()
                                    + " Marked as deleted, marking undeleted.");
                            OSCEntityManager.unMarkDeleted(em, sgm);
                        }
                    } else {
                        // entity is part of another security group. Check if its an active member
                        if (!sgm.getMarkedForDeletion()) {
                            log.info(type.toString() + " exists as part of another security group: "
                                    + otherSecurityGroup.getName());
                            String defaultExceptionMessage = SecurityGroupMemberPartOfAnotherSecurityGroupException
                                    .getDefaultExceptionMessage(entity.getName(), otherSecurityGroup.getName());
                            throw new SecurityGroupMemberPartOfAnotherSecurityGroupException(entity.getName(),
                                    defaultExceptionMessage);
                        }
                        // If entity is not already a member but its an 'in-active'(deleted) member in another security group
                        // create a SGM for it.
                        SecurityGroupMember securityGroupMember = new SecurityGroupMember(securityGroup, entity);
                        OSCEntityManager.create(em, securityGroupMember);
                    }
                }

                //if SGM already exist but user modified protect external flag, update entity.
                if (entity.getType().equals(SecurityGroupMemberType.SUBNET)) {
                    // Update Protect External flag for every SG update
                    ((Subnet) entity).setProtectExternal(securityGroupMemberDto.isProtectExternal());
                    OSCEntityManager.update(em, entity);
                }

            } else {
                // Entity exists in the DB but is not a direct member in any security groups.
                // If the entity is a VM, we try to find out the security group(s) its indirectly a part of
                Set<SecurityGroup> securityGroups = new HashSet<>();
                if (entity.getType() == SecurityGroupMemberType.VM) {
                    VM vm = (VM) entity;
                    Set<VMPort> vmPorts = vm.getPorts();
                    if (vmPorts != null) {
                        for (VMPort vmport : vmPorts) {
                            if (vmport.getNetwork() != null) {
                                Set<SecurityGroupMember> networkMembers = vmport.getNetwork().getSecurityGroupMembers();
                                if (networkMembers != null) {
                                    for (SecurityGroupMember nwMember : networkMembers) {
                                        securityGroups.add(nwMember.getSecurityGroup());
                                    }
                                }
                            }
                        }
                    }
                }
                // If entity is indirect member of this and ONLY this security group, create a member for it.
                if (securityGroups.size() == 1 && securityGroups.contains(securityGroup)) {
                    SecurityGroupMember securityGroupMember = new SecurityGroupMember(securityGroup, entity);
                    OSCEntityManager.create(em, securityGroupMember);
                } else {
                    StringBuilder securityGroupNamesBuilder = new StringBuilder();
                    for (SecurityGroup sg : securityGroups) {
                        securityGroupNamesBuilder.append(sg.getName() + ",");
                    }
                    String securityGroupNames = securityGroupNamesBuilder.toString();
                    if (StringUtils.isBlank(securityGroupNames)) {
                        throw new SecurityGroupMemberPartOfAnotherSecurityGroupException(entity.getName(),
                                VmidcMessages.getString(VmidcMessages_.SG_OVERLAP_PORT, entity.getName()));
                    } else {
                        securityGroupNames = StringUtils.removeEnd(securityGroupNames, ",");
                        throw new SecurityGroupMemberPartOfAnotherSecurityGroupException(entity.getName(),
                                VmidcMessages.getString(VmidcMessages_.SG_OVERLAP_PORT_SGNAME, entity.getName(),
                                        securityGroupNames));
                    }
                }

                //
                /*
                 * //There are no security group members associated with this entity, create one
                 * SecurityGroupMember securityGroupMember = new SecurityGroupMember(securityGroup, entity);
                 * EntityManager.create(session, securityGroupMember);
                 */
            }
        }
    }
}
