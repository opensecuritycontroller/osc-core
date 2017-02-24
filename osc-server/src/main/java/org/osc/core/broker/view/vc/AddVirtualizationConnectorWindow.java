package org.osc.core.broker.view.vc;

import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import org.apache.log4j.Logger;
import org.osc.core.broker.service.dto.VirtualizationConnectorDto;
import org.osc.core.broker.service.request.DryRunRequest;
import org.osc.core.broker.service.response.BaseResponse;
import org.osc.core.broker.service.vc.AddVirtualizationConnectorService;

@SuppressWarnings("serial")
public class AddVirtualizationConnectorWindow extends BaseVCWindow {

    final String CAPTION = "Add Virtualization Connector";

    private static final Logger log = Logger.getLogger(AddVirtualizationConnectorWindow.class);

    public AddVirtualizationConnectorWindow(VirtualizationConnectorView vcView) throws Exception {
        this.vcView = vcView;
        createWindow(this.CAPTION);
    }

    @Override
    public void populateForm() {
        buildForm();
        updateForm(this.virtualizationType.getValue().toString());
        this.virtualizationType.addValueChangeListener(new ValueChangeListener() {
            @Override
            public void valueChange(ValueChangeEvent event) {
                updateForm(event.getProperty().getValue().toString());
            }
        });
    }

    @Override
    public void submitForm() {
        try {
            if (validateForm()) {
                // creating add request with user entered data

                DryRunRequest<VirtualizationConnectorDto> addRequest = createRequest();
                // calling add VC service
                AddVirtualizationConnectorService addService = new AddVirtualizationConnectorService();

                log.info("adding virtualization connector - " + this.name.getValue().trim());
                BaseResponse addResponse = addService.dispatch(addRequest);

                // adding returned ID to the request DTO object
                addRequest.getDto().setId(addResponse.getId());

                // adding DTO object in parent Table
                this.vcView.getParentContainer().addItemAt(0, addRequest.getDto().getId(), addRequest.getDto());
                this.vcView.parentTableClicked(addRequest.getDto().getId());
                close();

            }
        } catch (Exception exception) {
            sslAwareHandleException(exception);
        }
    }

}
