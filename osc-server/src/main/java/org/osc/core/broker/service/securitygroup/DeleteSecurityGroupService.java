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

import javax.persistence.EntityManager;

import org.osc.core.broker.job.Job;
import org.osc.core.broker.job.JobEngine;
import org.osc.core.broker.job.TaskGraph;
import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.model.entities.virtualization.SecurityGroup;
import org.osc.core.broker.model.entities.virtualization.VirtualizationConnector;
import org.osc.core.broker.service.LockUtil;
import org.osc.core.broker.service.SecurityGroupConformJobFactory;
import org.osc.core.broker.service.ServiceDispatcher;
import org.osc.core.broker.service.api.DeleteSecurityGroupServiceApi;
import org.osc.core.broker.service.exceptions.VmidcBrokerValidationException;
import org.osc.core.broker.service.persistence.OSCEntityManager;
import org.osc.core.broker.service.persistence.VirtualizationConnectorEntityMgr;
import org.osc.core.broker.service.request.BaseDeleteRequest;
import org.osc.core.broker.service.response.BaseJobResponse;
import org.osc.core.broker.service.tasks.conformance.UnlockObjectMetaTask;
import org.osc.core.broker.service.tasks.conformance.openstack.securitygroup.ForceDeleteSecurityGroupTask;
import org.osc.core.broker.service.validator.BaseIdRequestValidator;
import org.osc.core.common.job.TaskGuard;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class DeleteSecurityGroupService extends ServiceDispatcher<BaseDeleteRequest, BaseJobResponse>
implements DeleteSecurityGroupServiceApi {

    private static final Logger log = LoggerFactory.getLogger(DeleteSecurityGroupService.class);

    @Reference
    private SecurityGroupConformJobFactory sgConformJobFactory;

    @Reference
    ForceDeleteSecurityGroupTask forceDeleteSecurityGroupTask;

    @Override
    public BaseJobResponse exec(BaseDeleteRequest request, EntityManager em) throws Exception {
        SecurityGroup securityGroup = validate(em, request);

        UnlockObjectMetaTask unlockTask = null;
        BaseJobResponse response = new BaseJobResponse();
        log.info("Deleting SecurityGroup: " + securityGroup.getName());
        try {
            unlockTask = LockUtil.tryLockSecurityGroup(securityGroup,
                    securityGroup.getVirtualizationConnector());
            //remove sfc link to SG
            securityGroup.setServiceFunctionChain(null);

            if (request.isForceDelete()) {
                TaskGraph tg = new TaskGraph();
                tg.addTask(this.forceDeleteSecurityGroupTask.create(securityGroup));
                tg.appendTask(unlockTask, TaskGuard.ALL_PREDECESSORS_COMPLETED);
                Job job = JobEngine.getEngine().submit(
                        "Force Delete Security Group '" + securityGroup.getName() + "'", tg,
                        LockObjectReference.getObjectReferences(securityGroup));

                response.setJobId(job.getId());
            } else {

                // Mark this security Group for Deletion and Trigger  Sync Job
                OSCEntityManager.markDeleted(em, securityGroup, this.txBroadcastUtil);
                UnlockObjectMetaTask forLambda = unlockTask;
                chain(() -> {
                    try {
                        Job job = this.sgConformJobFactory.startSecurityGroupConformanceJob(em, securityGroup,
                                forLambda, false);
                        response.setJobId(job.getId());
                        return response;
                    } catch (Exception e) {
                        LockUtil.releaseLocks(forLambda);
                        throw e;
                    }
                });
            }
        } catch (Exception e) {
            LockUtil.releaseLocks(unlockTask);
            throw e;
        }

        return response;
    }

    private SecurityGroup validate(EntityManager em, BaseDeleteRequest request) throws Exception {
        BaseIdRequestValidator.checkForNullIdAndParentNullId(request);

        VirtualizationConnector vc = VirtualizationConnectorEntityMgr.findById(em, request.getParentId());

        if (vc == null) {
            throw new VmidcBrokerValidationException("Virtualization Connector with Id: " + request.getParentId()
            + "  is not found.");
        }

        SecurityGroup securityGroup = em.find(SecurityGroup.class, request.getId());
        if (securityGroup == null) {
            throw new VmidcBrokerValidationException("Security Group with Id: " + request.getId() + "  is not found.");
        }

        if (!securityGroup.getMarkedForDeletion() && request.isForceDelete()) {
            throw new VmidcBrokerValidationException(
                    "Security Group with ID "
                            + request.getId()
                            + " is not marked for deletion and force delete operation is applicable only for entries marked for deletion.");
        }
        return securityGroup;
    }

}
