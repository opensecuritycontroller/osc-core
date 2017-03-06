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
package org.osc.core.broker.service.tasks.conformance.virtualsystem;

import java.util.Set;

import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.model.entities.appliance.VirtualSystem;
import org.osc.core.broker.model.entities.appliance.VirtualSystemPolicy;
import org.osc.core.broker.model.plugin.sdncontroller.VMwareSdnApiFactory;
import org.osc.core.broker.service.persistence.EntityManager;
import org.osc.core.broker.service.tasks.TransactionalTask;
import org.osc.sdk.sdn.api.ServiceApi;
import org.osc.sdk.sdn.api.ServiceInstanceApi;
import org.osc.sdk.sdn.api.ServiceManagerApi;
import org.osc.sdk.sdn.api.VendorTemplateApi;
import org.osc.sdk.sdn.element.ServiceElement;
import org.osc.sdk.sdn.element.ServiceManagerElement;
import org.osc.sdk.sdn.exception.HttpException;


public class ValidateNsxTask extends TransactionalTask {
    private static final Logger LOG = Logger.getLogger(ValidateNsxTask.class);

    private VirtualSystem vs;

    public ValidateNsxTask(VirtualSystem vs) {

        this.vs = vs;
        this.name = getName();
    }

    @Override
    public void executeTransaction(Session session) throws Exception {

        this.vs = (VirtualSystem) session.get(VirtualSystem.class, this.vs.getId());
        ServiceManagerApi serviceManagerApi = VMwareSdnApiFactory.createServiceManagerApi(this.vs);
        // Handle service name change. Should not be interpret as a service removal
        ServiceManagerElement svcMgr = null;
        if (this.vs.getNsxServiceManagerId() != null) {
            svcMgr = serviceManagerApi.getServiceManager(this.vs.getNsxServiceManagerId());
        }
        if (svcMgr == null) {
            svcMgr = serviceManagerApi.findServiceManager(CreateNsxServiceManagerTask.generateServiceManagerName(this.vs));
        }
        String svcMgrId = svcMgr == null ? null : svcMgr.getId();
        if (!isNsxServiceManagerInSync(svcMgrId)) {
            LOG.info("Service Manager for VS '" + this.vs.getVirtualizationConnector().getName() + "' was out of sync");
            setNsxServiceManager(svcMgr);
        }

        if (this.vs.getNsxServiceManagerId() != null) {
            //NsxServiceApi svcApi = new NsxServiceApi(this.vs);
            ServiceApi serviceApi = VMwareSdnApiFactory.createServiceApi(this.vs);
            ServiceElement service = null;

            // Handle name change. Should not be interpret as a service removal
            if (this.vs.getNsxServiceId() != null) {
                try {
                    service = serviceApi.getService(this.vs.getNsxServiceId());
                } catch (HttpException he) {
//                    if (!he.getSatus().equals(ClientResponse.Status.NOT_FOUND.getStatusCode())) {
//                        throw he;
//                    }

                    LOG.info("Nsx Service ID '" + this.vs.getNsxServiceId() + "' not found");
                }
            }
            if (service == null) {
                service = serviceApi.findService(this.vs.getDistributedAppliance().getName());
            }

            String svcId = service == null ? null : service.getId();
            if (!isNsxServiceInSync(svcId)) {
                LOG.info("Service for VS '" + this.vs.getVirtualizationConnector().getName() + "' was out of sync");
                this.vs.setNsxServiceId(svcId);
            }
        } else {
            this.vs.setNsxServiceId(null);
        }

        if (this.vs.getNsxServiceId() != null) {
            ServiceInstanceApi serviceInstanceApi = VMwareSdnApiFactory.createServiceInstanceApi(this.vs);
            String serviceInstanceId = serviceInstanceApi.getServiceInstanceIdByServiceId(this.vs.getNsxServiceId());

            if (!isNsxServiceInstanceInSync(serviceInstanceId)) {
                LOG.info("Service Instance for VS '" + this.vs.getVirtualizationConnector().getName() + "' was out of sync");
                this.vs.setNsxServiceInstanceId(serviceInstanceId);
            }

            Set<VirtualSystemPolicy> policies = this.vs.getVirtualSystemPolicies();
            for (VirtualSystemPolicy vsp : policies) {
                VendorTemplateApi templateApi = VMwareSdnApiFactory.createVendorTemplateApi(vsp.getVirtualSystem());
                String templateId = templateApi.getVendorTemplateIdByVendorId(
                        vsp.getVirtualSystem().getNsxServiceId(),
                        vsp.getPolicy().getId().toString());

                if (!isNsxVendorTemplateInSync(vsp, templateId)) {
                    LOG.info("Vendor template for policy '" + vsp.getPolicy().getName() + "' was out of sync");
                    vsp.setNsxVendorTemplateId(templateId);
                    EntityManager.update(session, vsp);
                }
            }
        } else {
            // Since there is no service, the others won't be there as well. We'll make sure to reset and
            // remove everything else.
            this.vs.setNsxServiceInstanceId(null);
            this.vs.getNsxDeploymentSpecIds().clear();
            this.vs.getVirtualSystemPolicies().clear();
        }

        EntityManager.update(session, this.vs);
    }

    private void setNsxServiceManager(ServiceManagerElement svcMgr) {
        if (svcMgr == null) {
            this.vs.setNsxServiceManagerId(null);
            this.vs.setNsxVsmUuid(null);
        } else {
            this.vs.setNsxServiceManagerId(svcMgr.getId());
            this.vs.setNsxVsmUuid(svcMgr.getVsmId());
        }
    }

    private boolean isNsxVendorTemplateInSync(VirtualSystemPolicy vsp, String templateId) {
        return com.google.common.base.Objects.equal(vsp.getNsxVendorTemplateId(), templateId);
    }

    private boolean isNsxServiceManagerInSync(String svcMgrId) {
        return com.google.common.base.Objects.equal(this.vs.getNsxServiceManagerId(), svcMgrId);
    }

    private boolean isNsxServiceInSync(String svcId) {
        return com.google.common.base.Objects.equal(this.vs.getNsxServiceId(), svcId);
    }

    private boolean isNsxServiceInstanceInSync(String svcInstanceId) {
        return com.google.common.base.Objects.equal(this.vs.getNsxServiceInstanceId(), svcInstanceId);
    }

    @Override
    public String getName() {
        return "Validating Existing NSX Service Components '" + this.vs.getVirtualizationConnector().getName() + "'";
    }

    @Override
    public Set<LockObjectReference> getObjects() {
        return LockObjectReference.getObjectReferences(this.vs);
    }

}
