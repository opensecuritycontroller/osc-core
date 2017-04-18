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
package org.osc.core.broker.window.delete;

import org.apache.log4j.Logger;
import org.osc.core.broker.service.api.DeleteUserServiceApi;
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

    private final UserView userView;
    private final DeleteUserServiceApi deleteUserService;

    public DeleteUserWindow(UserView userView, DeleteUserServiceApi deleteUserService) throws Exception {
        this.userView = userView;
        this.deleteUserService = deleteUserService;
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

            log.info("deleting User - "
                    + this.userView.getParentContainer().getItem(this.userView.getParentItemId())
                            .getItemProperty("loginName").getValue().toString());

            this.deleteUserService.dispatch(delRequest);

            // deleting a row from the table reference provided
            this.userView.getParentContainer().removeItem(delRequest.getId());
        } catch (Exception e) {
            log.info(e.getMessage());
            ViewUtil.iscNotification(e.getMessage(), Notification.Type.ERROR_MESSAGE);
        }
        close();
    }

}