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
package org.osc.core.broker.service.tasks.conformance.virtualizationconnector;

import org.apache.log4j.Logger;
import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.model.entities.virtualization.VirtualizationConnector;
import org.osc.core.broker.model.virtualization.VirtualizationType;
import org.osc.core.broker.service.dto.VirtualizationConnectorDto;
import org.osc.core.broker.service.request.DryRunRequest;
import org.osc.core.broker.service.tasks.BaseTask;
import org.osc.core.broker.util.VirtualizationConnectorUtil;
import org.osc.core.util.EncryptionUtil;

import java.util.Set;

public class CheckSSLConnectivityVcTask extends BaseTask {
    private static final Logger log = Logger.getLogger(CheckSSLConnectivityVcTask.class);

    private VirtualizationConnector vc;

    public CheckSSLConnectivityVcTask(VirtualizationConnector vc) {
        super(null);
        this.vc = vc;
        this.name = getName();
    }

    @Override
    public void execute() throws Exception {
        log.debug("Start executing CheckSSLConnectivityVcTask Task. VC: '" + this.vc.getName() + "'");
        VirtualizationConnectorUtil virtualizationConnectorUtil = new VirtualizationConnectorUtil();
        DryRunRequest<VirtualizationConnectorDto> request = createRequest(this.vc);
        if (VirtualizationType.fromText(this.vc.getVirtualizationType().name()).equals(VirtualizationType.VMWARE)) {
            virtualizationConnectorUtil.checkVmwareConnection(request, this.vc);
        } else {
            virtualizationConnectorUtil.checkOpenstackConnection(request, this.vc);
        }
    }

    @Override
    public String getName() {
        return "Checking ssl connectivity for virtualization connector '" + this.vc.getName() + "'";
    }

    @Override
    public Set<LockObjectReference> getObjects() {
        return LockObjectReference.getObjectReferences(this.vc);
    }

    protected DryRunRequest<VirtualizationConnectorDto> createRequest(VirtualizationConnector vc) throws Exception {
        DryRunRequest<VirtualizationConnectorDto> request = new DryRunRequest<>();
        request.setDto(new VirtualizationConnectorDto());
        VirtualizationConnectorDto dto = request.getDto();
        dto.setId(vc.getId());
        dto.setName(vc.getName());
        dto.setType(VirtualizationType.valueOf(vc.getVirtualizationType().name()));

        dto.setControllerIP(vc.getControllerIpAddress());
        dto.setControllerUser(vc.getControllerUsername());
        dto.setControllerPassword(EncryptionUtil.decryptAESCTR(vc.getControllerPassword()));

        dto.setProviderIP(vc.getProviderIpAddress());
        dto.setProviderUser(vc.getProviderUsername());
        dto.setProviderPassword(EncryptionUtil.decryptAESCTR(vc.getProviderPassword()));
        dto.setSslCertificateAttrSet(vc.getSslCertificateAttrSet());
        dto.setAdminTenantName(vc.getProviderAdminTenantName());
        return request;
    }
}
