package org.osc.core.broker.window.update;

import org.apache.log4j.Logger;
import org.osc.core.broker.service.SetNATSettingsService;
import org.osc.core.broker.service.dto.NATSettingsDto;
import org.osc.core.broker.service.request.DryRunRequest;
import org.osc.core.broker.service.response.BaseJobResponse;
import org.osc.core.broker.util.ValidateUtil;
import org.osc.core.broker.view.maintenance.NetworkLayout;
import org.osc.core.broker.view.util.ViewUtil;
import org.osc.core.broker.window.CRUDBaseWindow;
import org.osc.core.broker.window.button.OkCancelButtonModel;

import com.vaadin.ui.Notification;
import com.vaadin.ui.TextField;

public class SetNATSettingsWindow extends CRUDBaseWindow<OkCancelButtonModel> {

    private static final long serialVersionUID = 1L;
    private static final Logger log = Logger.getLogger(SetNetworkSettingsWindow.class);

    final String CAPTION = "Set Network Settings";

    private TextField ipAddress = null;

    private NetworkLayout networkLayout = null;

    public SetNATSettingsWindow(NetworkLayout networkLayout) throws Exception {
        super();
        this.networkLayout = networkLayout;
        createWindow(this.CAPTION);

    }

    @Override
    public void populateForm() {
        this.ipAddress = new TextField("IPv4 Address");
        this.ipAddress.setImmediate(true);

        // filling form with existing data
        if (this.networkLayout.natTable.getItem(1).getItemProperty("Value").getValue() != null) {
            this.ipAddress.setValue(this.networkLayout.natTable.getItem(1).getItemProperty("Value").getValue()
                    .toString());
        }

        // adding not null constraint
        this.ipAddress.setRequired(true);
        this.ipAddress.setRequiredError("IPv4 Address cannot be empty");

        this.form.addComponent(this.ipAddress);
        this.ipAddress.focus();

    }

    @Override
    public boolean validateForm() {
        try {
            this.ipAddress.validate();
            ValidateUtil.checkForValidIpAddressFormat(this.ipAddress.getValue());
            return true;
        } catch (Exception e) {
            ViewUtil.iscNotification(e.getMessage() + ".", Notification.Type.ERROR_MESSAGE);
        }

        return false;
    }

    @Override
    public void submitForm() {
        try {
            if (validateForm()) {
                NATSettingsDto dto = new NATSettingsDto(this.ipAddress.getValue());
                DryRunRequest<NATSettingsDto> req = new DryRunRequest<NATSettingsDto>();
                req.setDto(dto);

                SetNATSettingsService service = new SetNATSettingsService();
                BaseJobResponse response = service.dispatch(req);
                this.networkLayout.populateNATTable();
                if (response.getJobId() != null) {
                    ViewUtil.showJobNotification(response.getJobId());
                }
                close();
            }
        } catch (Exception e) {
            log.error("Failed to update the NAT settings", e);
            ViewUtil.iscNotification(e.getMessage(), Notification.Type.ERROR_MESSAGE);
        }

    }
}
