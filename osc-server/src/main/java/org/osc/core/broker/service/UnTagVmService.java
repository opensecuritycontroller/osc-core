package org.osc.core.broker.service;

import org.osc.core.broker.rest.server.model.TagVmRequest;
import org.osc.core.broker.service.exceptions.VmidcBrokerValidationException;
import org.osc.core.broker.service.response.TagVmResponse;
import org.osc.core.broker.util.VimUtils;
import org.osc.sdk.sdn.api.SecurityTagApi;

import com.vmware.vim25.mo.VirtualMachine;

public class UnTagVmService extends BaseTagVmService {
    @Override
    protected void customValidate(TagVmRequest request) throws VmidcBrokerValidationException {
        if (request.getVmUuid() == null || request.getVmUuid().isEmpty()) {
            throw new VmidcBrokerValidationException("Invalid VM Uuid.");
        }
    }

    @Override
    protected VirtualMachine customFindVm(VimUtils vmi, TagVmRequest request) throws VmidcBrokerValidationException {
        VirtualMachine vm = vmi.findVmByInstanceUuid(request.getVmUuid());
        if (vm == null) {
            throw new VmidcBrokerValidationException("VM with Uuid '" + request.getVmUuid() + "' not found.");
        }

        return vm;
    }

    @Override
    protected TagVmResponse modifyVmTag(TagVmRequest request, TagVmResponse response) {
        return response;
    }

    @Override
    protected void modifyNsxSecurityTagApi(SecurityTagApi secTagApi, VirtualMachine vm, TagVmResponse response) throws Exception {
        secTagApi.removeSecurityTagFromVM(vm.getMOR().getVal(), DEFAULT_OSC_SECURITY_TAG);
    }
}
