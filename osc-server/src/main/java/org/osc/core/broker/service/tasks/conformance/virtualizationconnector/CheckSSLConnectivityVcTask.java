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

import static java.util.stream.Collectors.toSet;

import java.util.Set;

import javax.persistence.EntityManager;

import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.model.entities.virtualization.VirtualizationConnector;
import org.osc.core.broker.service.api.server.EncryptionApi;
import org.osc.core.broker.service.dto.VirtualizationConnectorDto;
import org.osc.core.broker.service.persistence.SslCertificateAttrEntityMgr;
import org.osc.core.broker.service.request.DryRunRequest;
import org.osc.core.broker.service.tasks.TransactionalTask;
import org.osc.core.broker.util.VirtualizationConnectorUtil;
import org.slf4j.LoggerFactory;
import org.osc.core.common.virtualization.VirtualizationType;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;

@Component(service=CheckSSLConnectivityVcTask.class)
public class CheckSSLConnectivityVcTask extends TransactionalTask {
    private static final Logger log = LoggerFactory.getLogger(CheckSSLConnectivityVcTask.class);

    private VirtualizationConnector vc;

    @Reference
    private EncryptionApi encryptionApi;

    @Reference
    private VirtualizationConnectorUtil virtualizationConnectorUtil;

    public CheckSSLConnectivityVcTask create(VirtualizationConnector vc) {
        CheckSSLConnectivityVcTask task = new CheckSSLConnectivityVcTask();
        task.vc = vc;
        task.name = task.getName();
        task.encryptionApi = this.encryptionApi;
        task.virtualizationConnectorUtil = this.virtualizationConnectorUtil;
        task.dbConnectionManager = this.dbConnectionManager;
        task.txBroadcastUtil = this.txBroadcastUtil;

        return task;
    }

    @Override
    public void executeTransaction(EntityManager em) throws Exception {
        this.vc = em.find(VirtualizationConnector.class, this.vc.getId());
        log.debug("Start executing CheckSSLConnectivityVcTask Task. VC: '" + this.vc.getName() + "'");
        DryRunRequest<VirtualizationConnectorDto> request = createRequest(this.vc);
        this.virtualizationConnectorUtil.checkConnection(request, this.vc);
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
        dto.setControllerPassword(this.encryptionApi.decryptAESCTR(vc.getControllerPassword()));
        dto.setProviderAttributes(vc.getProviderAttributes());

        dto.setProviderIP(vc.getProviderIpAddress());
        dto.setProviderUser(vc.getProviderUsername());
        dto.setProviderPassword(this.encryptionApi.decryptAESCTR(vc.getProviderPassword()));
        dto.setSslCertificateAttrSet(vc.getSslCertificateAttrSet().stream()
                .map(SslCertificateAttrEntityMgr::fromEntity)
                .collect(toSet()));
        dto.setAdminProjectName(vc.getProviderAdminProjectName());
        dto.setAdminDomainId(vc.getAdminDomainId());
        return request;
    }
}
