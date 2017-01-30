package org.osc.core.broker.view.vc;

import org.apache.log4j.Logger;
import org.osc.core.broker.model.plugin.sdncontroller.ControllerType;
import org.osc.core.broker.service.dto.VirtualizationConnectorDto;
import org.osc.core.broker.service.request.DryRunRequest;
import org.osc.core.broker.service.vc.UpdateVirtualizationConnectorService;

public class UpdateVirtualizationConnectorWindow extends BaseVCWindow {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    private static final Logger log = Logger.getLogger(UpdateVirtualizationConnectorWindow.class);

    final String CAPTION = "Edit Virtualization Connector";

    public UpdateVirtualizationConnectorWindow(VirtualizationConnectorView vcView) throws Exception {
        this.currentVCObject = vcView.getParentContainer().getItem(vcView.getParentItemId());
        createWindow(this.CAPTION);
    }

    @Override
    public void populateForm() {

        buildForm();

        // filling text fields with existing table data

        this.name.setValue(this.currentVCObject.getItemProperty("name").getValue().toString());
        this.virtualizationType.select(this.currentVCObject.getItemProperty("type").getValue().toString());
        this.virtualizationType.setEnabled(false);

        updateForm(this.virtualizationType.getValue().toString());

        VirtualizationConnectorDto vcObject = this.currentVCObject.getBean();
        if (vcObject.isControllerDefined()) {
            this.controllerIP.setValue(vcObject.getControllerIP());
            this.controllerUser.setValue(vcObject.getControllerUser());
            this.controllerPW.setValue(vcObject.getControllerPassword());
            this.controllerType.setValue(vcObject.getControllerType().toString());
        } else {
            this.controllerType.setValue(ControllerType.NONE.toString());
            this.controllerType.setEnabled(true);
        }

        this.providerIP.setValue(vcObject.getProviderIP());
        this.adminTenantName.setValue(vcObject.getAdminTenantName());
        this.providerUser.setValue(vcObject.getProviderUser());
        this.providerPW.setValue(vcObject.getProviderPassword());
        this.providerAttributes = vcObject.getProviderAttributes();

    }

    @Override
    public void submitForm() {
        try {
            if (validateForm()) {
                // creating add request with user entered data
                DryRunRequest<VirtualizationConnectorDto> updateRequest = createRequest();
                updateRequest.getDto().setId(this.currentVCObject.getBean().getId());
                UpdateVirtualizationConnectorService updateService = new UpdateVirtualizationConnectorService();
                log.debug("Updating virtualization connector - " + this.name.getValue().trim());
                // no response needed for update request
                updateService.dispatch(updateRequest);
                close();
            }
        } catch (Exception exception) {
            sslAwareHandleException(exception);
        }
    }
}
