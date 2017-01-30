package org.osc.core.broker.window.delete;

import org.apache.log4j.Logger;
import org.osc.core.broker.service.DeleteApplianceService;
import org.osc.core.broker.service.request.BaseIdRequest;
import org.osc.core.broker.view.ApplianceView;
import org.osc.core.broker.view.util.ViewUtil;
import org.osc.core.broker.window.CRUDBaseWindow;
import org.osc.core.broker.window.button.OkCancelButtonModel;

import com.vaadin.ui.Label;
import com.vaadin.ui.Notification;

public class DeleteApplianceWindow extends CRUDBaseWindow<OkCancelButtonModel> {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    private static final Logger log = Logger.getLogger(DeleteApplianceWindow.class);

    // current view reference
    private ApplianceView applianceView = null;

    final String CAPTION = "Delete Appliance";

    public DeleteApplianceWindow(ApplianceView applianceView) throws Exception {
        this.applianceView = applianceView;
        createWindow(this.CAPTION);
    }

    @Override
    public void populateForm() {
        Label delete = new Label(this.CAPTION
                + " - "
                + this.applianceView.getParentContainer().getItem(this.applianceView.getParentItemId())
                        .getItemProperty("model").getValue().toString());
        this.form.addComponent(delete);
    }

    @Override
    public boolean validateForm() {
        return false;
    }

    @Override
    public void submitForm() {
        BaseIdRequest delRequest = new BaseIdRequest();
        // Delete appliance service has no response so not needed.
        try {
            delRequest.setId(this.applianceView.getParentItemId());
            DeleteApplianceService dmc = new DeleteApplianceService();

            log.info("deleting Appliance - "
                    + this.applianceView.getParentContainer().getItem(this.applianceView.getParentItemId())
                            .getItemProperty("model").getValue().toString());

            dmc.dispatch(delRequest);

            // deleting a row from the table reference provided by the current
            // view
            this.applianceView.getParentContainer().removeItem(delRequest.getId());
        } catch (Exception e) {
            log.info(e.getMessage());
            ViewUtil.iscNotification(e.getMessage(), Notification.Type.ERROR_MESSAGE);
        }
        close();
    }
}
