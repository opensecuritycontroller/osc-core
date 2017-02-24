package org.osc.core.broker.window.delete;

import org.apache.log4j.Logger;
import org.osc.core.broker.service.DeleteUserService;
import org.osc.core.broker.service.request.DeleteUserRequest;
import org.osc.core.broker.view.UserView;
import org.osc.core.broker.view.util.ViewUtil;
import org.osc.core.broker.window.CRUDBaseWindow;
import org.osc.core.broker.window.button.OkCancelButtonModel;

import com.vaadin.ui.Label;
import com.vaadin.ui.Notification;

public class DeleteUserWindow extends CRUDBaseWindow<OkCancelButtonModel> {

    /**
	 *
	 */
    private static final long serialVersionUID = 1L;

    private static final Logger log = Logger.getLogger(DeleteUserWindow.class);
    final String CAPTION = "Delete User";

    private UserView userView = null;

    public DeleteUserWindow(UserView userView) throws Exception {
        this.userView = userView;
        createWindow(this.CAPTION);
    }

    @Override
    public void populateForm() {
        Label delete = new Label(this.CAPTION
                + " - "
                + this.userView.getParentContainer().getItem(this.userView.getParentItemId())
                        .getItemProperty("loginName").getValue().toString());
        this.form.addComponent(delete);

    }

    @Override
    public boolean validateForm() {
        // no UI validation needed for deleting an User
        return false;
    }

    @Override
    public void submitForm() {
        DeleteUserRequest delRequest = new DeleteUserRequest();
        // Delete MC service has no response so not needed.
        try {
            delRequest.setId(this.userView.getParentItemId());
            DeleteUserService du = new DeleteUserService();

            log.info("deleting User - "
                    + this.userView.getParentContainer().getItem(this.userView.getParentItemId())
                            .getItemProperty("loginName").getValue().toString());

            du.dispatch(delRequest);

            // deleting a row from the table reference provided
            this.userView.getParentContainer().removeItem(delRequest.getId());
        } catch (Exception e) {
            log.info(e.getMessage());
            ViewUtil.iscNotification(e.getMessage(), Notification.Type.ERROR_MESSAGE);
        }
        close();
    }

}
