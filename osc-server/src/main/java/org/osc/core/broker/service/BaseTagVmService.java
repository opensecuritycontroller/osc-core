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

import org.hibernate.Session;
import org.osc.core.broker.model.entities.appliance.DistributedApplianceInstance;
import org.osc.core.broker.model.entities.virtualization.VirtualizationConnector;
import org.osc.core.broker.model.plugin.sdncontroller.VMwareSdnApiFactory;
import org.osc.core.broker.rest.server.model.TagVmRequest;
import org.osc.core.broker.rest.server.model.TagVmRequestValidator;
import org.osc.core.broker.service.exceptions.VmidcBrokerValidationException;
import org.osc.core.broker.service.request.RequestValidator;
import org.osc.core.broker.service.response.TagVmResponse;
import org.osc.core.broker.util.VimUtils;
import org.osc.core.util.EncryptionUtil;
import org.osc.sdk.sdn.api.SecurityTagApi;

import com.mcafee.vmidc.server.Server;
import com.vmware.vim25.mo.VirtualMachine;

abstract class BaseTagVmService extends ServiceDispatcher<TagVmRequest, TagVmResponse> {
    static final String DEFAULT_OSC_SECURITY_TAG = Server.SHORT_PRODUCT_NAME + "-Quarantine";

    private RequestValidator<TagVmRequest, DistributedApplianceInstance> validator;
    private VimUtils vimUtils;
    private SecurityTagApi securityTagApi;

    @Override
    public TagVmResponse exec(TagVmRequest request, Session session) throws Exception {
        if (this.validator == null) {
            this.validator = new TagVmRequestValidator(session);
        }

        DistributedApplianceInstance dai = this.validator.validateAndLoad(request);
        customValidate(request);

        VirtualMachine vm = findVm(dai, request);

        TagVmResponse resp = modifyVmTag(request, new TagVmResponse());

        if (this.securityTagApi == null) {
            this.securityTagApi = VMwareSdnApiFactory.createSecurityTagApi(dai.getVirtualSystem());
        }

        modifyNsxSecurityTagApi(this.securityTagApi, vm, resp);

        return resp;
    }

    private VirtualMachine findVm(DistributedApplianceInstance dai, TagVmRequest request) throws Exception {

        VirtualizationConnector vc = dai.getVirtualSystem().getVirtualizationConnector();

        if (this.vimUtils == null) {
            this.vimUtils =  new VimUtils(vc.getProviderIpAddress(), vc.getProviderUsername(), EncryptionUtil.decryptAESCTR(vc.getProviderPassword()));
        }

        return customFindVm(this.vimUtils, request);
    }

    protected abstract TagVmResponse modifyVmTag(TagVmRequest request, TagVmResponse response);

    protected abstract void modifyNsxSecurityTagApi(SecurityTagApi sta, VirtualMachine vm, TagVmResponse response) throws Exception;

    protected abstract void customValidate(TagVmRequest request) throws VmidcBrokerValidationException;

    protected abstract VirtualMachine customFindVm(VimUtils vmi, TagVmRequest request) throws VmidcBrokerValidationException;
}
