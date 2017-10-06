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
package org.osc.core.broker.window.add;

import org.osc.core.broker.service.api.AddUserServiceApi;
import org.osc.core.broker.service.request.AddUserRequest;
import org.osc.core.broker.service.response.AddUserResponse;
import org.osc.core.broker.view.UserView;
import org.osc.core.broker.view.util.ViewUtil;
import org.osc.core.broker.window.CRUDBaseWindow;
import org.osc.core.broker.window.button.OkCancelButtonModel;
import org.osc.core.broker.window.update.UpdateUserWindow;
import org.osc.core.ui.LogProvider;
import org.slf4j.Logger;

import com.vaadin.data.validator.EmailValidator;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Notification;
import com.vaadin.ui.PasswordField;
import com.vaadin.ui.TextField;

public class AddUserWindow extends CRUDBaseWindow<OkCancelButtonModel> {

    /**
	 *
	 */
    private static final long serialVersionUID = 1L;

    final String CAPTION = "Add User";

    private static final Logger log = LogProvider.getLogger(AddUserWindow.class);

    // form fields
    private TextField firstName = null;
    private TextField lastName = null;
    private TextField loginName = null;
    private PasswordField password = null;
    private TextField email = null;
    private ComboBox role = null;

    private final AddUserServiceApi addUserService;

    public AddUserWindow(UserView userView, AddUserServiceApi addUserService) throws Exception {
        this.addUserService = addUserService;
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

        // adding not null constraint
        this.loginName.setRequired(true);
        this.loginName.setRequiredError("User Name cannot be empty");
        this.password.setRequired(true);
        this.password.setRequiredError("Password cannot be empty");
        this.role.setRequired(true);

        this.form.setMargin(true);
        this.form.setSizeUndefined();
        this.form.addComponent(this.loginName);
        this.form.addComponent(this.firstName);
        this.form.addComponent(this.lastName);
        this.form.addComponent(this.password);
        this.form.addComponent(this.email);
        this.form.addComponent(this.role);
        this.loginName.focus();
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
            log.debug("Validation Error");
            ViewUtil.iscNotification(e.getMessage() + ".", Notification.Type.ERROR_MESSAGE);
        }
        return false;
    }

    @Override
    public void submitForm() {
        try {
            if (validateForm()) {
                // creating add request with user entered data
                AddUserRequest addRequest = new AddUserRequest();
                addRequest.setFirstName(this.firstName.getValue().trim());
                addRequest.setLastName(this.lastName.getValue().trim());
                addRequest.setLoginName(this.loginName.getValue().trim());
                addRequest.setEmail(this.email.getValue().trim());
                addRequest.setPassword(this.password.getValue());
                addRequest.setRole(this.role.getValue().toString());

                // calling add service
                AddUserResponse addResponse;
                log.info("adding new user - " + this.loginName.getValue());
                addResponse = this.addUserService.dispatch(addRequest);
                // adding returned ID to the request DTO object
                addRequest.setId(addResponse.getId());
                close();
            }
        } catch (Exception e) {
            log.info(e.getMessage());
            ViewUtil.iscNotification(e.getMessage(), Notification.Type.ERROR_MESSAGE);
        }

    }

}
