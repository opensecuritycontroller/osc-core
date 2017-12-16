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
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;

import org.osc.core.broker.job.Job;
import org.osc.core.broker.model.entities.appliance.VirtualSystem;
import org.osc.core.broker.model.entities.management.Policy;
import org.osc.core.broker.model.entities.virtualization.SecurityGroup;
import org.osc.core.broker.model.entities.virtualization.SecurityGroupInterface;
import org.osc.core.broker.model.entities.virtualization.ServiceFunctionChain;
import org.osc.core.broker.model.entities.virtualization.VirtualizationConnector;
import org.osc.core.broker.model.plugin.ApiFactoryService;
import org.osc.core.broker.service.LockUtil;
import org.osc.core.broker.service.SecurityGroupConformJobFactory;
import org.osc.core.broker.service.ServiceDispatcher;
import org.osc.core.broker.service.api.BindSecurityGroupServiceApi;
import org.osc.core.broker.service.broadcast.EventType;
import org.osc.core.broker.service.dto.VirtualSystemPolicyBindingDto;
import org.osc.core.broker.service.dto.VirtualSystemPolicyBindingDto.VirtualSystemPolicyBindingDtoComparator;
import org.osc.core.broker.service.exceptions.VmidcBrokerValidationException;
import org.osc.core.broker.service.persistence.OSCEntityManager;
import org.osc.core.broker.service.persistence.PolicyEntityMgr;
import org.osc.core.broker.service.persistence.SecurityGroupEntityMgr;
import org.osc.core.broker.service.persistence.SecurityGroupInterfaceEntityMgr;
import org.osc.core.broker.service.persistence.ServiceFunctionChainEntityMgr;
import org.osc.core.broker.service.persistence.VirtualSystemEntityMgr;
import org.osc.core.broker.service.persistence.VirtualizationConnectorEntityMgr;
import org.osc.core.broker.service.request.BindSecurityGroupRequest;
import org.osc.core.broker.service.response.BaseJobResponse;
import org.osc.core.broker.service.tasks.conformance.UnlockObjectMetaTask;
import org.osc.core.broker.service.validator.BindSecurityGroupRequestValidator;
import org.osc.core.broker.util.ValidateUtil;
import org.osc.sdk.controller.FailurePolicyType;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Sets;

@Component
public class BindSecurityGroupService extends ServiceDispatcher<BindSecurityGroupRequest, BaseJobResponse>
		implements BindSecurityGroupServiceApi {

	private static final Logger log = LoggerFactory.getLogger(BindSecurityGroupService.class);

	@Reference
	private SecurityGroupConformJobFactory sgConformJobFactory;

	@Reference
	public ApiFactoryService apiFactoryService;

	private SecurityGroup securityGroup;

	@Override
	public BaseJobResponse exec(BindSecurityGroupRequest request, EntityManager em) throws Exception {

		validateRequest(em, request);
		UnlockObjectMetaTask unlockTask = null;
		BaseJobResponse response = null;
		boolean sfcBind = this.apiFactoryService.supportsNeutronSFC(this.securityGroup);
		ServiceFunctionChain sfc = null;
        if (sfcBind) {
            sfc = validateAndLoadSfcBind(em, request);
        } else {
            validateAndLoad(em, request);
        }

		try {
			unlockTask = LockUtil.tryLockSecurityGroup(this.securityGroup,
					this.securityGroup.getVirtualizationConnector());

            List<VirtualSystemPolicyBindingDto> servicesToBindTo = request.getServicesToBindTo();

            if (sfcBind) {
                this.securityGroup.setServiceFunctionChain(sfc);
            }

            // Sorts the services by the order specified.
            // We want to collapse the ordering, so we will 'reset' the order based on the request.

            Collections.sort(servicesToBindTo, new VirtualSystemPolicyBindingDtoComparator());

            long order = 0;

            // For all services selected, create or update the security group interfaces
			for (VirtualSystemPolicyBindingDto serviceToBindTo : servicesToBindTo) {
				Long virtualSystemId = serviceToBindTo.getVirtualSystemId();
				VirtualSystem vs = validateAndLoadVirtualSystem(em, virtualSystemId);

				if (vs.getMarkedForDeletion()) {
					throw new VmidcBrokerValidationException(String.format(
							"Cannot bind security group '%s' to service"
									+ " '%s' as the service is marked for deletion",
							this.securityGroup.getName(), vs.getDistributedAppliance().getName()));
				}
				boolean isPolicyMappingSupported = this.apiFactoryService.syncsPolicyMapping(vs);
				if (!this.apiFactoryService.supportsMultiplePolicies(vs) && serviceToBindTo.getPolicies().size() > 1) {
					throw new VmidcBrokerValidationException(
							"Security group interface cannot have more than one policy for security manager not supporting multiple policy binding");
				}
				if (!isPolicyMappingSupported && !serviceToBindTo.getPolicyIds().isEmpty()) {
					throw new VmidcBrokerValidationException(
							"Security manager not supporting policy mapping cannot have one or more policies");
				}
				if (isPolicyMappingSupported && serviceToBindTo.getPolicyIds().isEmpty()) {
					throw new VmidcBrokerValidationException(
							"Service to bind: " + serviceToBindTo + " must have a policy id.");
				}

				Set<Policy> policies = null;
				policies = PolicyEntityMgr.findPoliciesById(em, serviceToBindTo.getPolicyIds(),
						vs.getDistributedAppliance().getApplianceManagerConnector());

				if (this.apiFactoryService.supportsFailurePolicy(this.securityGroup)) {
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

				FailurePolicyType failurePolicyType = this.apiFactoryService.supportsFailurePolicy(this.securityGroup)
						? serviceToBindTo.getFailurePolicyType() : FailurePolicyType.NA;

				SecurityGroupInterface sgi = SecurityGroupInterfaceEntityMgr
						.findSecurityGroupInterfacesByVsAndSecurityGroup(em, vs, this.securityGroup);
				if (sgi == null) {
					// If the policy is null the tag should also be null
					Long tag = policies == null ? null : VirtualSystemEntityMgr.generateUniqueTag(em, vs);
					String tagString = tag == null ? null : SecurityGroupInterface.ISC_TAG_PREFIX + tag;

					// Create a new security group interface for this service
					sgi = new SecurityGroupInterface(vs, policies, tagString,
							org.osc.core.broker.model.entities.virtualization.FailurePolicyType
									.valueOf(failurePolicyType.name()),
							order);

					SecurityGroupInterfaceEntityMgr.toEntity(sgi, this.securityGroup, serviceToBindTo.getName());

					log.info("Creating security group interface " + sgi.getName());
					OSCEntityManager.create(em, sgi, this.txBroadcastUtil);

					this.securityGroup.addSecurityGroupInterface(sgi);
					sgi.setSecurityGroup(this.securityGroup);
					OSCEntityManager.update(em, this.securityGroup, this.txBroadcastUtil);
				} else {
					if (hasServiceChanged(sgi, serviceToBindTo, policies, order)) {
						log.info("Updating Security group interface " + sgi.getName());
						sgi.setPolicies(policies);
						sgi.setFailurePolicyType(org.osc.core.broker.model.entities.virtualization.FailurePolicyType
								.valueOf(failurePolicyType.name()));
						sgi.setOrder(order);
					}
					OSCEntityManager.update(em, sgi, this.txBroadcastUtil);
				}

				order++;
			}

			// Go through all the security group interfaces and if they are not selected, remove them
			for (SecurityGroupInterface sgi : this.securityGroup.getSecurityGroupInterfaces()) {
				boolean isServiceSelected = isServiceSelected(servicesToBindTo, sgi.getVirtualSystem().getId());
				if (!isServiceSelected || sgi.getMarkedForDeletion()) {
					log.info("Marking service " + sgi.getName() + " for deletion");
					OSCEntityManager.markDeleted(em, sgi, this.txBroadcastUtil);
				}
			}

			this.txBroadcastUtil.addMessageToMap(this.securityGroup.getId(),
					this.securityGroup.getClass().getSimpleName(), EventType.UPDATED);

			Job job = this.sgConformJobFactory.startBindSecurityGroupConformanceJob(em, this.securityGroup, unlockTask);

			response = new BaseJobResponse(job.getId());

		} catch (Exception e) {
			LockUtil.releaseLocks(unlockTask);
			throw e;
		}

		return response;
	}

	private boolean hasServiceChanged(SecurityGroupInterface sgi, VirtualSystemPolicyBindingDto serviceToBindTo,
			Set<Policy> policies, long order) {
		boolean policyChanged = validatePoliciesIfEquals(sgi.getPolicies(), policies);
		return policyChanged || FailurePolicyType.valueOf(sgi.getFailurePolicyType().name()) != serviceToBindTo
				.getFailurePolicyType() || sgi.getOrder() != order;
	}

	private boolean validatePoliciesIfEquals(Set<Policy> policySet, Set<Policy> newPolicySet) {
		return !Sets.symmetricDifference(policySet, newPolicySet).isEmpty();
	}

	private boolean isServiceSelected(List<VirtualSystemPolicyBindingDto> servicesToBindTo, Long virtualSystemId) {
		for (VirtualSystemPolicyBindingDto service : servicesToBindTo) {
			if (service.getVirtualSystemId().equals(virtualSystemId)) {
				return true;
			}
		}
		return false;
	}

	private void validateRequest(EntityManager em, BindSecurityGroupRequest request) throws Exception {
		BindSecurityGroupRequestValidator.checkForNullFields(request);

		this.securityGroup = SecurityGroupEntityMgr.findById(em, request.getSecurityGroupId());

		if (this.securityGroup == null) {
			throw new VmidcBrokerValidationException(
					"Security Group with Id: " + request.getSecurityGroupId() + "  is not found.");
		}


		Long vcId = request.getVcId();
		if (!vcId.equals(this.securityGroup.getVirtualizationConnector().getId())) {
			throw new VmidcBrokerValidationException(
					String.format("The Security Group '%s' does not belong to the Parent Object with ID %d",
							this.securityGroup.getName(), vcId));
		}

		ValidateUtil.checkMarkedForDeletion(this.securityGroup, this.securityGroup.getName());

		List<VirtualSystemPolicyBindingDto> services = request.getServicesToBindTo();

		if (services == null) {
			services = new ArrayList<>();
			request.setServicesToBindTo(services);
		}
	}

	private void validateAndLoad(EntityManager em, BindSecurityGroupRequest request) throws Exception {

		List<VirtualSystemPolicyBindingDto> services = request.getServicesToBindTo();
		if (services.size() > 1) {
			if (!this.apiFactoryService.supportsServiceFunctionChaining(this.securityGroup)) {
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
						//for SFC there no possibility of mark for deletion in equals case because, SFC in not mark for deletion and that means
						//virtual systems in that SFC will cannot be delete. So through an exception if user try to duplicate the v
					}
				}
			}

			// Adding elements to the tree set using the order comparator
			// ensures any services with duplicate order
			// are pruned out of the set.
			TreeSet<VirtualSystemPolicyBindingDto> serviceOrderSet = new TreeSet<>(
					new VirtualSystemPolicyBindingDtoComparator());
			for (VirtualSystemPolicyBindingDto serviceToBindTo : services) {
				boolean serviceAdded = serviceOrderSet.add(serviceToBindTo);
				if (!serviceAdded) {
					// We are always expected to find the other service
					VirtualSystemPolicyBindingDto otherService = findDuplicateServiceByOrder(services, serviceToBindTo);
					Long virtualSystemId = serviceToBindTo.getVirtualSystemId();
					VirtualSystem vs = validateAndLoadVirtualSystem(em, virtualSystemId);
					Long otherVirtualSystemId = otherService.getVirtualSystemId();
					VirtualSystem otherVs = validateAndLoadVirtualSystem(em, otherVirtualSystemId);

					SecurityGroupInterface sgi = SecurityGroupInterfaceEntityMgr
							.findSecurityGroupInterfacesByVsAndSecurityGroup(em, vs, this.securityGroup);
					SecurityGroupInterface otherSgi = SecurityGroupInterfaceEntityMgr
							.findSecurityGroupInterfacesByVsAndSecurityGroup(em, otherVs, this.securityGroup);

					// if either of the SGI's are not marked for deleting, we
					// should fail because of duplicate order
					if (!sgi.getMarkedForDeletion() && !otherSgi.getMarkedForDeletion()) {
						throw new VmidcBrokerValidationException(
								"Service with duplicate order found. Ensure the services have unique order values");
					}

				}
			}
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

	private VirtualSystem validateAndLoadVirtualSystem(EntityManager em, Long vsId) throws Exception {
		VirtualSystem vs = VirtualSystemEntityMgr.findById(em, vsId);
			if (vs == null) {
				throw new VmidcBrokerValidationException(
						"Virtual System with Id: " + vsId + "  is not found.");
				}
		return vs;
	}

	private ServiceFunctionChain validateAndLoadSfcBind(EntityManager em, BindSecurityGroupRequest request) throws Exception {

    	ServiceFunctionChain sfc = null;
        if (!request.isBindSfc()) {
            //user is trying to configure binding SFC with either UI which currently not supported or APIs that are not
            //valid for SFC binding, so throw an exception.
            throw new VmidcBrokerValidationException(String.format(
                    "Binding individual Virtual Systems/Services is not supported for the Security Group %s. Please bind to a Service Function Chain",
                    this.securityGroup.getName()));
        }
		if(request.getSfcId() == null) {
			//user wants to unbind serviceFunctonChain with Security Group
			return null;
		}

		VirtualizationConnector vc = VirtualizationConnectorEntityMgr.findById(em, request.getVcId());
		sfc = em.find(ServiceFunctionChain.class, request.getSfcId());
		if (sfc == null) {
			throw new VmidcBrokerValidationException(
					"Service Function Chain entry with Id: " + request.getSfcId() + " is not found.");
		}

		 ValidateUtil.checkMarkedForDeletion(sfc, sfc.getName());

		//sfc parent and vcid check
        if (!sfc.getVirtualizationConnector().getId().equals(vc.getId())) {
            throw new VmidcBrokerValidationException(
                    String.format("The Service Function Chain '%s' does not belong to the Parent Object with ID %d",
                            sfc.getName(), vc.getId()));
		}

        List<Long> sfcVsIdList = sfc.getVirtualSystems().stream().map(vs -> vs.getId()).collect(Collectors.toList());
        if (sfcVsIdList.isEmpty()) {
            throw new VmidcBrokerValidationException(String.format(
                    "Service Function Chain : %s has no Virtual System references, cannot be binded",
                    sfc.getName()));
        }
        List<Long> sfcVsIdOrderList = sfc.getVirtualSystems().stream().map(vs -> vs.getId()).collect(Collectors.toList());

        List<VirtualSystemPolicyBindingDto> servicesToBindTo = request.getServicesToBindTo();

         // For all services selected, create or update the security group interfaces
         for (VirtualSystemPolicyBindingDto serviceToBindTo : servicesToBindTo) {
             Long virtualSystemId = serviceToBindTo.getVirtualSystemId();
            if (sfcVsIdList.size() == 0 || virtualSystemId == null || !sfcVsIdList.contains(virtualSystemId)) {
            	 throw new VmidcBrokerValidationException(
     					"Binding request Virtual System Id : " + virtualSystemId +
     					" do not match with any of the ids in Service Function Chain Virtual System Id list");
             }
             //by removing virtual system id from the list , will help to throw any exception in case of duplication
             sfcVsIdList.remove(virtualSystemId);

             //To get virtual system services in align with sfc virtual system , add order
             serviceToBindTo.setOrder(sfcVsIdOrderList.indexOf(virtualSystemId));

             VirtualSystem vs = validateAndLoadVirtualSystem(em, virtualSystemId);

             //if this virtual system is pointing to more than one SFC and one of the SFC other than given SFC
             //is already binded to SG and active, throw exception;
             //Get All the sfcs having this virtual system in the chain
             List<ServiceFunctionChain> serviceFunctionChainList = ServiceFunctionChainEntityMgr.
                                                                 listServiceFunctionChainsByVirtualSystem(em, vs);

            for (ServiceFunctionChain serviceFunctionChain : serviceFunctionChainList) {
                if (!serviceFunctionChain.getId().equals(sfc.getId())) {
                    checkVirtualSystemRedundancyInServiceFunctionChains(em, serviceFunctionChain.getId(), vs);
                }
            }
         }

         //number of vsIds in sfc should match with number of binding request vsIds
        if (!sfcVsIdList.isEmpty()) {
            throw new VmidcBrokerValidationException(String.format(
                    "Number of Virtual Systems in Service Function Chain(%s:%s)"
                            + "and Binding request Virtual Systems:%s do not match",
                    sfc.getName(), sfc.getVirtualSystems().size(), servicesToBindTo.size()));
        }
    	return sfc;
    }

	/**
     * This methods checks if there exist Security Groups binded with given sfcId and TentantId.
     * and If there are Security groups, checks if they are binded and not marked for deletion.
     * throws an exception if all the above conditions are true, declaring service function chain with this virtual system,
     *  tenant already exist and binded to SecurityGroup.
     * @param sfcid
     * @param projectId(TentantId)
     * @param VirtualSystem
     * @return
     */
    private void checkVirtualSystemRedundancyInServiceFunctionChains(EntityManager em, Long sfcId,
            VirtualSystem virtualSystem) throws Exception {

        List<SecurityGroup> sgList = SecurityGroupEntityMgr.listSecurityGroupsBySfcIdAndProjectId(em, sfcId,
                this.securityGroup.getProjectId());
        for (SecurityGroup sg : sgList) {
            if (!sg.getMarkedForDeletion() && !sg.equals(this.securityGroup)) {
                SecurityGroupInterface sgi = SecurityGroupInterfaceEntityMgr
                        .findSecurityGroupInterfacesByVsAndSecurityGroup(em, virtualSystem, sg);
                if (sgi != null && !sgi.getMarkedForDeletion()) {
                    throw new VmidcBrokerValidationException("Service with VirtualSystem " + virtualSystem.getId()
                            + " is already chained to ServiceFunctionChain Id " + sfcId
                            + " and binded to SecurityGroup : " + sg.getName());
                }
            }
        }
    }
}
