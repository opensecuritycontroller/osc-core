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

import org.apache.commons.lang.StringUtils;
import org.osc.core.broker.service.api.TagVmServiceApi;
import org.osc.core.broker.service.exceptions.VmidcBrokerValidationException;
import org.osc.core.broker.service.request.TagVmRequest;
import org.osc.core.broker.service.response.TagVmResponse;
import org.osc.core.broker.util.VimUtils;
import org.osc.sdk.sdn.api.SecurityTagApi;

import com.vmware.vim25.mo.VirtualMachine;

public class TagVmService extends BaseTagVmService implements TagVmServiceApi {

    @Override
    protected void customValidate(TagVmRequest request) throws VmidcBrokerValidationException {
        if (StringUtils.isBlank(request.getVmUuid()) && StringUtils.isBlank(request.getIpAddress())) {
            throw new VmidcBrokerValidationException("Missing IP Address or VM Uuid input.");
        }

        if (!StringUtils.isBlank(request.getVmUuid()) && !StringUtils.isBlank(request.getIpAddress())) {
            throw new VmidcBrokerValidationException("Input must be either IP Address or VM Uuid but not both.");
        }
    }

    @Override
    protected VirtualMachine customFindVm(VimUtils vmi, TagVmRequest request) throws VmidcBrokerValidationException {
        VirtualMachine vm = null;
        if (!StringUtils.isBlank(request.getVmUuid())) {
            vm = vmi.findVmByInstanceUuid(request.getVmUuid());
        } else if (!StringUtils.isBlank(request.getIpAddress())) {
            vm = vmi.findVmByIp(request.getIpAddress());
        }

        if (vm == null) {
            if (!StringUtils.isBlank(request.getVmUuid())) {
                throw new VmidcBrokerValidationException("VM with Uuid '" + request.getVmUuid() + "' not found.");
            } else if (!StringUtils.isBlank(request.getIpAddress())) {
                throw new VmidcBrokerValidationException("VM with IP address '" + request.getIpAddress() + "' not found.");
            } else {
                throw new VmidcBrokerValidationException("VM not found.");
            }
        }

        return vm;
    }

    @Override
    protected TagVmResponse modifyVmTag(TagVmRequest request, TagVmResponse response) {
        if (StringUtils.isBlank(request.getTag())) {
            response.setVmTag(DEFAULT_OSC_SECURITY_TAG);
        } else {
            response.setVmTag(request.getTag());
        }

        return response;
    }

    @Override
    protected void modifyNsxSecurityTagApi(SecurityTagApi sta, VirtualMachine vm, TagVmResponse response) throws Exception {
        sta.addSecurityTagToVM(vm.getMOR().getVal(), response.getVmTag());
    }
}
