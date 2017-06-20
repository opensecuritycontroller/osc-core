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
package org.osc.core.broker.window.update;

import org.apache.log4j.Logger;
import org.osc.core.broker.service.api.UpdateUserServiceApi;
import org.osc.core.broker.service.api.server.ServerApi;
import org.osc.core.broker.service.dto.UserDto;
import org.osc.core.broker.service.request.UpdateUserRequest;
import org.osc.core.broker.service.response.UpdateUserResponse;
import org.osc.core.broker.view.UserView;
import org.osc.core.broker.view.util.ViewUtil;
import org.osc.core.broker.window.CRUDBaseWindow;
import org.osc.core.broker.window.button.OkCancelButtonModel;

import com.vaadin.data.util.BeanItem;
import com.vaadin.data.validator.EmailValidator;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Notification;
import com.vaadin.ui.PasswordField;
import com.vaadin.ui.TextField;

public class UpdateUserWindow extends CRUDBaseWindow<OkCancelButtonModel> {

    /**
     *
     */
    private static final long serialVersionUID = 1L;
    final String CAPTION = "Edit User";
    private static final Logger log = Logger.getLogger(UpdateUserWindow.class);

    public static final String ROLE_ADMIN = "ADMIN";

    // form fields
    private TextField firstName = null;
    private TextField lastName = null;
    private TextField loginName = null;
    private PasswordField password = null;
    private TextField email = null;
    private ComboBox role = null;

    private final BeanItem<UserDto> currentUser;
    private final UpdateUserServiceApi updateUserService;

    private final ServerApi server;
    public UpdateUserWindow(UserView userView, UpdateUserServiceApi updateUserService,
            ServerApi server) throws Exception {
        this.currentUser = userView.getParentItem();
        this.updateUserService = updateUserService;
        this.server = server;
        createWindow(this.CAPTION);
    }

    @Override
    public void populateForm() {
        this.loginName = new TextField("User Name");
        this.loginName.setImmediate(true);
        this.firstName = new TextField("First Name");
        this.lastName = new TextField("Last Name");
        this.password = new PasswordField("Password");
        this.password.setImmediate(true);
        this.email = new TextField("Email");
        this.email.addValidator(new EmailValidator("Please enter a valid email address"));
        this.role = new ComboBox("Role");
        this.role.setTextInputAllowed(false);
        this.role.setNullSelectionAllowed(false);
        this.role.addItem(UpdateUserWindow.ROLE_ADMIN);
        this.role.select(UpdateUserWindow.ROLE_ADMIN);

        // filling fields with existing information
        this.loginName.setValue(this.currentUser.getItemProperty("loginName").getValue().toString());
        this.loginName.setEnabled(false);
        this.password.setValue(this.currentUser.getItemProperty("password").getValue().toString());
        if (this.currentUser.getItemProperty("email").getValue() != null) {
            this.email.setValue(this.currentUser.getItemProperty("email").getValue().toString());
        }
        if (this.currentUser.getItemProperty("firstName").getValue() != null) {
            this.firstName.setValue(this.currentUser.getItemProperty("firstName").getValue().toString());
        }
        if (this.currentUser.getItemProperty("lastName").getValue() != null) {
            this.lastName.setValue(this.currentUser.getItemProperty("lastName").getValue().toString());
        }
        this.role.setValue(this.currentUser.getItemProperty("role").getValue().toString());

        // adding not null constraint
        this.loginName.setRequired(true);
        this.loginName.setRequiredError("User Name cannot be Empty");
        this.password.setRequired(true);
        this.password.setRequiredError("Password Cannot be empty");
        this.role.setRequired(true);

        this.form.setMargin(true);
        this.form.setSizeUndefined();
        this.form.addComponent(this.loginName);
        this.form.addComponent(this.firstName);
        this.form.addComponent(this.lastName);
        this.form.addComponent(this.password);
        this.form.addComponent(this.email);
        this.form.addComponent(this.role);
        this.firstName.focus();
    }

    @Override
    public boolean validateForm() {
        try {
            this.loginName.validate();
            this.password.validate();
            this.email.validate();
            this.role.validate();
            return true;
        } catch (Exception e) {
            log.debug("Validation error");
            ViewUtil.iscNotification(e.getMessage() + ".", Notification.Type.ERROR_MESSAGE);
        }
        return false;
    }

    @Override
    public void submitForm() {
        try {
            if (validateForm()) {
                // creating add request with user entered data
                UpdateUserRequest updateRequest = new UpdateUserRequest();
                updateRequest.setId(this.currentUser.getBean().getId());
                updateRequest.setFirstName(this.firstName.getValue().trim());
                updateRequest.setLastName(this.lastName.getValue().trim());
                updateRequest.setLoginName(this.loginName.getValue().trim());
                updateRequest.setEmail(this.email.getValue().trim());
                updateRequest.setPassword(this.password.getValue());
                updateRequest.setRole(this.role.getValue().toString());

                log.info("Updating user - " + this.loginName.getValue());

                UpdateUserResponse res = this.updateUserService.dispatch(updateRequest);
                if (res.getJobId() != null) {
                    ViewUtil.showJobNotification(res.getJobId(), this.server);
                }

                close();
            }
        } catch (Exception e) {
            log.info("Error updating user", e);
            ViewUtil.iscNotification(e.getMessage(), Notification.Type.ERROR_MESSAGE);
        }

    }

}