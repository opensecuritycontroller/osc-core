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
import java.util.Collections;
import java.util.List;
import java.util.TreeSet;

import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.osc.core.broker.job.Job;
import org.osc.core.broker.model.entities.appliance.VirtualSystem;
import org.osc.core.broker.model.entities.appliance.VirtualizationType;
import org.osc.core.broker.model.entities.management.Policy;
import org.osc.core.broker.model.entities.virtualization.SecurityGroup;
import org.osc.core.broker.model.entities.virtualization.SecurityGroupInterface;
import org.osc.core.broker.model.plugin.manager.ManagerApiFactory;
import org.osc.core.broker.model.plugin.sdncontroller.SdnControllerApiFactory;
import org.osc.core.broker.service.ConformService;
import org.osc.core.broker.service.LockUtil;
import org.osc.core.broker.service.ServiceDispatcher;
import org.osc.core.broker.service.exceptions.ActionNotSupportedException;
import org.osc.core.broker.service.exceptions.VmidcBrokerValidationException;
import org.osc.core.broker.service.persistence.EntityManager;
import org.osc.core.broker.service.persistence.PolicyEntityMgr;
import org.osc.core.broker.service.persistence.SecurityGroupEntityMgr;
import org.osc.core.broker.service.persistence.SecurityGroupInterfaceEntityMgr;
import org.osc.core.broker.service.persistence.VirtualSystemEntityMgr;
import org.osc.core.broker.service.response.BaseJobResponse;
import org.osc.core.broker.service.securitygroup.VirtualSystemPolicyBindingDto.VirtualSystemPolicyBindingDtoComparator;
import org.osc.core.broker.service.tasks.conformance.UnlockObjectMetaTask;
import org.osc.core.broker.util.TransactionalBroadcastUtil;
import org.osc.core.broker.util.ValidateUtil;
import org.osc.core.broker.view.util.EventType;
import org.osc.sdk.controller.FailurePolicyType;

public class BindSecurityGroupService extends ServiceDispatcher<BindSecurityGroupRequest, BaseJobResponse> {

    private static final Logger log = Logger.getLogger(BindSecurityGroupService.class);

    private SecurityGroup securityGroup;

    @Override
    public BaseJobResponse exec(BindSecurityGroupRequest request, Session session) throws Exception {

        validateAndLoad(session, request);
        UnlockObjectMetaTask unlockTask = null;
        BaseJobResponse response = null;

        try {
            unlockTask = LockUtil.tryLockSecurityGroup(this.securityGroup,
                    this.securityGroup.getVirtualizationConnector());

            // Sorts the services by the order specified.
            // We want to collapse the ordering, so we will 'reset' the order based on the request.
            List<VirtualSystemPolicyBindingDto> servicesToBindTo = request.getServicesToBindTo();

            Collections.sort(servicesToBindTo, new VirtualSystemPolicyBindingDtoComparator());
            long order = 0;

            // For all services selected, create or update the security group interfaces
            for (VirtualSystemPolicyBindingDto serviceToBindTo : servicesToBindTo) {
                Long virtualSystemId = serviceToBindTo.getVirtualSystemId();
                VirtualSystem vs = VirtualSystemEntityMgr.findById(session, virtualSystemId);

                if (vs == null) {
                    throw new VmidcBrokerValidationException(
                            "Virtual System with Id: " + virtualSystemId + "  is not found.");
                }
                if (vs.getMarkedForDeletion()) {
                    throw new VmidcBrokerValidationException(String.format(
                            "Cannot bind security group '%s' to service"
                                    + " '%s' as the service is marked for deletion",
                                    this.securityGroup.getName(), vs.getDistributedAppliance().getName()));
                }

                Policy policy = null;
                boolean isPolicyMappingSupported = ManagerApiFactory.syncsPolicyMapping(vs);

                if (serviceToBindTo.getPolicyId() == null) {
                    if (isPolicyMappingSupported) {
                        throw new VmidcBrokerValidationException(
                                "Service to bind: " + serviceToBindTo + " must have a policy id.");
                    }
                } else if (!isPolicyMappingSupported) {
                    throw new VmidcBrokerValidationException(
                            "Service to bind: " + serviceToBindTo + " is associated with a manager that does not support "
                                    + "policy mapping and it should not have a policy id.");
                } else {
                    policy = PolicyEntityMgr.findById(session, serviceToBindTo.getPolicyId());

                    if (policy == null) {
                        throw new VmidcBrokerValidationException(
                                "Policy with Id: " + serviceToBindTo.getPolicyId() + "  is not found.");
                    }
                }

                if (SdnControllerApiFactory.supportsFailurePolicy(this.securityGroup)){
                    // If failure policy is supported, failure policy is a required field
                    if (serviceToBindTo.getFailurePolicyType() == null
                            || serviceToBindTo.getFailurePolicyType() == FailurePolicyType.NA) {
                        throw new VmidcBrokerValidationException("Failure Policy should not have an empty value.");
                    }
                } else {
                    // If failure policy is not supported, only valid values are null or NA
                    if (serviceToBindTo.getFailurePolicyType() == FailurePolicyType.FAIL_CLOSE
                            || serviceToBindTo.getFailurePolicyType() == FailurePolicyType.FAIL_OPEN) {
                        throw new VmidcBrokerValidationException("SDN Controller Plugin of type '"
                                + this.securityGroup.getVirtualizationConnector().getControllerType()
                                + "' does not support Failure Policy. Only valid values are null or NA");
                    }
                }

                FailurePolicyType failurePolicyType =
                        SdnControllerApiFactory.supportsFailurePolicy(this.securityGroup) ? serviceToBindTo.getFailurePolicyType() : FailurePolicyType.NA;

                        SecurityGroupInterface sgi = SecurityGroupInterfaceEntityMgr
                                .findSecurityGroupInterfacesByVsAndSecurityGroup(session, vs, this.securityGroup);
                        if (sgi == null) {
                            // If the policy is null the tag should also be null
                            Long tag = policy == null ? null : VirtualSystemEntityMgr.generateUniqueTag(session, vs);
                            String tagString = tag == null ? null : SecurityGroupInterface.ISC_TAG_PREFIX + tag;

                            // Create a new security group interface for this service
                            sgi = new SecurityGroupInterface(
                                    vs,
                                    policy,
                                    tagString,
                                    org.osc.core.broker.model.entities.virtualization.FailurePolicyType.valueOf(
                                            failurePolicyType.name()),
                                    order);

                            SecurityGroupInterfaceEntityMgr.toEntity(sgi, this.securityGroup, serviceToBindTo.getName());

                            log.info("Creating security group interface " + sgi.getName());
                            EntityManager.create(session, sgi);

                            this.securityGroup.addSecurityGroupInterface(sgi);
                            sgi.addSecurityGroup(this.securityGroup);
                            EntityManager.update(session, this.securityGroup);
                        } else {
                            if (hasServiceChanged(sgi, serviceToBindTo, policy, order)) {
                                log.info("Updating Security group interface " + sgi.getName());
                                sgi.setPolicy(policy);
                                sgi.setFailurePolicyType(org.osc.core.broker.model.entities.virtualization.FailurePolicyType.valueOf(
                                        failurePolicyType.name()));
                                sgi.setOrder(order);
                            }
                            EntityManager.update(session, sgi);
                        }

                        order++;
            }

            // Go through all the security group interfaces and if they are not selected, remove them
            for (SecurityGroupInterface sgi : this.securityGroup.getSecurityGroupInterfaces()) {
                boolean isServiceSelected = isServiceSelected(servicesToBindTo, sgi.getVirtualSystem().getId());
                if (!isServiceSelected || sgi.getMarkedForDeletion()) {
                    log.info("Marking service " + sgi.getName() + " for deletion");
                    EntityManager.markDeleted(session, sgi);
                }
            }

            TransactionalBroadcastUtil.addMessageToMap(session, this.securityGroup.getId(),
                    this.securityGroup.getClass().getSimpleName(), EventType.UPDATED);

            Job job = ConformService.startBindSecurityGroupConformanceJob(session, this.securityGroup, unlockTask);

            response = new BaseJobResponse(job.getId());

        } catch (Exception e) {
            LockUtil.releaseLocks(unlockTask);
            throw e;
        }

        return response;
    }

    private boolean hasServiceChanged(SecurityGroupInterface sgi, VirtualSystemPolicyBindingDto serviceToBindTo,
            Policy policy, long order) {
        boolean policyChanged = policy != null && (sgi.getPolicy() == null || !sgi.getPolicy().equals(policy));
        return policyChanged ||
                FailurePolicyType.valueOf(sgi.getFailurePolicyType().name()) != serviceToBindTo.getFailurePolicyType() ||
                sgi.getOrder() != order;
    }

    private boolean isServiceSelected(List<VirtualSystemPolicyBindingDto> servicesToBindTo, Long virtualSystemId) {
        for (VirtualSystemPolicyBindingDto service : servicesToBindTo) {
            if (service.getVirtualSystemId().equals(virtualSystemId)) {
                return true;
            }
        }
        return false;
    }

    protected void validateAndLoad(Session session, BindSecurityGroupRequest request) throws Exception {
        BindSecurityGroupRequest.checkForNullFields(request);

        this.securityGroup = SecurityGroupEntityMgr.findById(session, request.getSecurityGroupId());

        if (this.securityGroup == null) {
            throw new VmidcBrokerValidationException(
                    "Security Group with Id: " + request.getSecurityGroupId() + "  is not found.");
        }

        ValidateUtil.checkMarkedForDeletion(this.securityGroup, this.securityGroup.getName());

        if (this.securityGroup.getVirtualizationConnector().getVirtualizationType() == VirtualizationType.VMWARE) {
            throw new ActionNotSupportedException(
                    "Invalid Action. Binding of Security Group for Vmware Virtualization Connectors needs to done "
                            + "through NSX.");
        }

        List<VirtualSystemPolicyBindingDto> services = request.getServicesToBindTo();
        if (services == null) {
            services = new ArrayList<VirtualSystemPolicyBindingDto>();
            request.setServicesToBindTo(services);
        }

        if (services.size() > 1) {
            if (!SdnControllerApiFactory.supportsServiceFunctionChaining(this.securityGroup)){
                throw new VmidcBrokerValidationException("SDN Controller Plugin of type '"
                        + this.securityGroup.getVirtualizationConnector().getControllerType()
                        + "' does not support more then one Service (Service Function Chaining)");
            }

            for (VirtualSystemPolicyBindingDto serviceBinding : services) {
                if (serviceBinding.getOrder() == null || serviceBinding.getOrder() < 0) {
                    throw new VmidcBrokerValidationException(String.format(
                            "Service '%s' needs to have a valid order specified. '%s' value for order is not valid.",
                            serviceBinding.getName(), serviceBinding.getOrder()));
                }

                for (VirtualSystemPolicyBindingDto otherService : services) {
                    if (!serviceBinding.equals(otherService)
                            && otherService.getVirtualSystemId().equals(serviceBinding.getVirtualSystemId())) {
                        throw new VmidcBrokerValidationException(String.format(
                                "Duplicate Service found. Cannot Bind Security group to the same Service: '%s' twice.",
                                serviceBinding.getName()));
                    }
                }
            }

            // Adding elements to the tree set using the order comparator ensures any services with duplicate order
            // are pruned out of the set.
            TreeSet<VirtualSystemPolicyBindingDto> serviceOrderSet = new TreeSet<>(
                    new VirtualSystemPolicyBindingDtoComparator());
            for (VirtualSystemPolicyBindingDto serviceToBindTo : services) {
                boolean serviceAdded = serviceOrderSet.add(serviceToBindTo);
                if (!serviceAdded) {
                    // We are always expected to find the other service
                    VirtualSystemPolicyBindingDto otherService = findDuplicateServiceByOrder(services, serviceToBindTo);
                    Long virtualSystemId = serviceToBindTo.getVirtualSystemId();
                    VirtualSystem vs = VirtualSystemEntityMgr.findById(session, virtualSystemId);
                    Long otherVirtualSystemId = otherService.getVirtualSystemId();
                    VirtualSystem otherVs = VirtualSystemEntityMgr.findById(session, otherVirtualSystemId);

                    if (vs == null || otherVs == null) {
                        throw new VmidcBrokerValidationException("Virtual System with Id: " + vs == null
                                ? virtualSystemId.toString() : otherVirtualSystemId.toString() + "  is not found.");
                    }

                    SecurityGroupInterface sgi = SecurityGroupInterfaceEntityMgr
                            .findSecurityGroupInterfacesByVsAndSecurityGroup(session, vs, this.securityGroup);
                    SecurityGroupInterface otherSgi = SecurityGroupInterfaceEntityMgr
                            .findSecurityGroupInterfacesByVsAndSecurityGroup(session, otherVs, this.securityGroup);

                    // if either of the SGI's are not marked for deleting, we should fail because of duplicate order
                    if (!sgi.getMarkedForDeletion() && !otherSgi.getMarkedForDeletion()) {
                        throw new VmidcBrokerValidationException(
                                "Service with duplicate order found. Ensure the services have unique order values");
                    }

                }
            }
        }

        Long vcId = request.getVcId();
        if (vcId != null && !vcId.equals(this.securityGroup.getVirtualizationConnector().getId())) {
            throw new VmidcBrokerValidationException(
                    String.format("The Security Group '%s' does not belong to the Parent Object with ID %d",
                            this.securityGroup.getName(), vcId));
        }

    }

    /**
     * Returns the first service in the list which has the same order as the service passed in. Since the list might
     * contain the service passed in, excludes the service passed in.
     *
     * retuns null in case duplicate service is not found
     *
     * @param services
     * @param originalService
     * @return
     */
    private VirtualSystemPolicyBindingDto findDuplicateServiceByOrder(List<VirtualSystemPolicyBindingDto> services,
            VirtualSystemPolicyBindingDto originalService) {
        List<VirtualSystemPolicyBindingDto> servicesListWithoutOriginalService = new ArrayList<>(services);
        servicesListWithoutOriginalService.remove(originalService);
        for (VirtualSystemPolicyBindingDto otherService : servicesListWithoutOriginalService) {
            if (otherService.getOrder().equals(originalService.getOrder())) {
                return otherService;
            }
        }
        return null;
    }
}
