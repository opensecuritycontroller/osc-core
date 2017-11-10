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
package org.osc.core.broker.service;

import java.util.List;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;

import org.osc.core.broker.job.Job;
import org.osc.core.broker.job.JobEngine;
import org.osc.core.broker.job.TaskGraph;
import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.model.entities.appliance.DistributedAppliance;
import org.osc.core.broker.model.entities.appliance.VirtualSystem;
import org.osc.core.broker.model.entities.virtualization.SecurityGroup;
import org.osc.core.broker.model.entities.virtualization.ServiceFunctionChain;
import org.osc.core.broker.model.entities.virtualization.openstack.DeploymentSpec;
import org.osc.core.broker.service.api.DeleteDeploymentSpecServiceApi;
import org.osc.core.broker.service.exceptions.VmidcBrokerValidationException;
import org.osc.core.broker.service.persistence.DeploymentSpecEntityMgr;
import org.osc.core.broker.service.persistence.OSCEntityManager;
import org.osc.core.broker.service.persistence.SecurityGroupEntityMgr;
import org.osc.core.broker.service.persistence.VirtualSystemEntityMgr;
import org.osc.core.broker.service.request.BaseDeleteRequest;
import org.osc.core.broker.service.response.BaseJobResponse;
import org.osc.core.broker.service.tasks.conformance.UnlockObjectMetaTask;
import org.osc.core.broker.service.tasks.conformance.openstack.deploymentspec.ForceDeleteDSTask;
import org.osc.core.broker.service.validator.BaseIdRequestValidator;
import org.osc.core.common.job.TaskGuard;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component
public class DeleteDeploymentSpecService extends ServiceDispatcher<BaseDeleteRequest, BaseJobResponse>
implements DeleteDeploymentSpecServiceApi {

    @Reference
    private DeploymentSpecConformJobFactory dsConformJobFactory;

    @Reference
    private ForceDeleteDSTask forceDeleteDSTask;

    private DeploymentSpec ds;

    @Override
    public BaseJobResponse exec(BaseDeleteRequest request, EntityManager em) throws Exception {

        BaseJobResponse response = new BaseJobResponse();
        validateAndLoad(em, request);
        UnlockObjectMetaTask dsUnlock = null;

        try {
            DistributedAppliance da = this.ds.getVirtualSystem().getDistributedAppliance();
            dsUnlock = LockUtil.tryLockDS(this.ds, da, da.getApplianceManagerConnector(), this.ds.getVirtualSystem()
                    .getVirtualizationConnector());

            if (request.isForceDelete()) {
                TaskGraph tg = new TaskGraph();
                tg.addTask(this.forceDeleteDSTask.create(this.ds));
                tg.appendTask(dsUnlock, TaskGuard.ALL_PREDECESSORS_COMPLETED);
                Job job = JobEngine.getEngine().submit("Force Delete Deployment Spec '" + this.ds.getName() + "'", tg,
                        LockObjectReference.getObjectReferences(this.ds));

                response.setJobId(job.getId());
            } else {
                OSCEntityManager.markDeleted(em, this.ds, this.txBroadcastUtil);
                UnlockObjectMetaTask forLambda = dsUnlock;
                chain(() -> {
                    try {
                        Job job = this.dsConformJobFactory.startDsConformanceJob(em, this.ds, forLambda);
                        response.setJobId(job.getId());
                        return response;
                    } catch (Exception e) {
                        LockUtil.releaseLocks(forLambda);
                        throw e;
                    }
                });
            }
        } catch (Exception e) {
            LockUtil.releaseLocks(dsUnlock);
            throw e;
        }

        return response;

    }

    private void validateAndLoad(EntityManager em, BaseDeleteRequest request) throws Exception {
        BaseIdRequestValidator.checkForNullIdAndParentNullId(request);

        VirtualSystem vs = VirtualSystemEntityMgr.findById(em, request.getParentId());

        if (vs == null) {
            throw new VmidcBrokerValidationException("Virtual System with Id: " + request.getParentId()
            + "  is not found.");
        }

        this.ds = em.find(DeploymentSpec.class, request.getId());

        // entry must pre-exist in db
        if (this.ds == null) { // note: we cannot use name here in error msg since
            // del req does not have name, only ID
            throw new VmidcBrokerValidationException("Deployment Specification entry with ID " + request.getId()
            + " is not found.");
        }

        if (DeploymentSpecEntityMgr.isProtectingWorkload(this.ds)) {
            throw new VmidcBrokerValidationException(
                    String.format("The deployment spec with name '%s' is currently protecting a workload",
                            this.ds.getName()));
        }

        //check if virtual system of this DS is attached to a SFC and only allow deletion of DS if no SG is binding
        // to this virtual systems SFC.
        validateDSReferenceToSfcAndSecurityGroup(em, vs);

        if (!this.ds.getMarkedForDeletion() && request.isForceDelete()) {
            throw new VmidcBrokerValidationException(
                    "Deployment Spec '"
                            + this.ds.getName()
                            + "' is not marked for deletion and force delete operation is applicable only for entries marked for deletion.");
        }
    }

    private void validateDSReferenceToSfcAndSecurityGroup(EntityManager em, VirtualSystem vs) throws Exception {
        if (!vs.getServiceFunctionChains().isEmpty()) {
            //Look if these SFCs are binded to any of the SG of same tenant as of DS
            for (ServiceFunctionChain sfc : vs.getServiceFunctionChains()) {
                List<SecurityGroup> sgList = SecurityGroupEntityMgr.listSecurityGroupsBySfcIdAndProjectId(em,
                        sfc.getId(), this.ds.getProjectId());
                String sgNames = sgList.stream().filter(sg -> !sg.getMarkedForDeletion()).map(sg -> sg.getName()).collect(Collectors.joining(", "));
                if (!sgNames.isEmpty()) {
                    throw new VmidcBrokerValidationException(String.format(
                            "Cannot delete deployment spec with name '%s' which is currently referencing Service Function Chain '%s' and binded to a SecurityGroup(s) '%s'",
                            this.ds.getName(), sfc.getName(), sgNames));
                }
            }
        }
    }
}
