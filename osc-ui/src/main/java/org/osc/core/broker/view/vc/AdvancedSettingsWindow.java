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
package org.osc.core.broker.view.vc;

import org.osc.core.broker.view.common.VmidcMessages;
import org.osc.core.broker.view.common.VmidcMessages_;
import org.osc.core.broker.view.util.ViewUtil;
import org.osc.core.broker.window.CRUDBaseWindow;
import org.osc.core.broker.window.button.OkCancelButtonModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.Notification;
import com.vaadin.ui.PasswordField;
import com.vaadin.ui.TextField;

public class AdvancedSettingsWindow extends CRUDBaseWindow<OkCancelButtonModel> {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    private static final String ADVANCED_SETTINGS_CAPTION = VmidcMessages.getString(VmidcMessages_.ADVANCED);

    private static final Logger LOG = LoggerFactory.getLogger(AdvancedSettingsWindow.class);

    private CheckBox providerHttps = null;
    private TextField rabbitMQIp = null;
    private TextField rabbitMQUserName = null;
    private PasswordField rabbitMQUserPassword = null;
    private TextField rabbitMQPort = null;
    private final BaseVCWindow baseVCWindow;

    public AdvancedSettingsWindow(BaseVCWindow baseVCWindow) throws Exception {
        this.baseVCWindow = baseVCWindow;
        createWindow(ADVANCED_SETTINGS_CAPTION);
        getComponentModel().setOkClickedListener(new ClickListener() {
            /**
             *
             */
            private static final long serialVersionUID = -8326846388022657979L;

            @Override
            public void buttonClick(ClickEvent event) {
                submitForm();

            }
        });
    }

    @Override
    public void populateForm() throws Exception {

        this.providerHttps = new CheckBox("Https");
        this.providerHttps.setValue(false);

        this.rabbitMQIp = new TextField("RabbitMQ IP");
        this.rabbitMQUserName = new TextField("RabbitMQ User Name");
        this.rabbitMQUserName.setRequired(true);
        this.rabbitMQUserPassword = new PasswordField("RabbitMQ Password");
        this.rabbitMQUserPassword.setRequired(true);
        this.rabbitMQPort = new TextField("RabbitMQ Port");
        this.rabbitMQPort.setRequired(true);

        this.rabbitMQUserName.setRequiredError(this.rabbitMQUserName.getCaption() + " cannot be empty");
        this.rabbitMQUserPassword.setRequiredError(this.rabbitMQUserPassword.getCaption() + " cannot be empty");
        this.rabbitMQPort.setRequiredError(this.rabbitMQPort.getCaption() + " cannot be empty");

        //fill this form with default/previous values
        this.providerHttps.setValue(new Boolean(this.baseVCWindow.providerAttributes
                .get(BaseVCWindow.ATTRIBUTE_KEY_HTTPS)));
        if (this.baseVCWindow.providerAttributes.get(BaseVCWindow.ATTRIBUTE_KEY_RABBITMQ_IP) != null) {
            this.rabbitMQIp.setValue(this.baseVCWindow.providerAttributes
                    .get(BaseVCWindow.ATTRIBUTE_KEY_RABBITMQ_IP));
        }
        if (this.baseVCWindow.providerAttributes.get(BaseVCWindow.ATTRIBUTE_KEY_RABBITMQ_USER) != null) {
            this.rabbitMQUserName.setValue(this.baseVCWindow.providerAttributes
                    .get(BaseVCWindow.ATTRIBUTE_KEY_RABBITMQ_USER));
        }
        if (this.baseVCWindow.providerAttributes.get(BaseVCWindow.ATTRIBUTE_KEY_RABBITMQ_USER_PASSWORD) != null) {
            this.rabbitMQUserPassword.setValue(this.baseVCWindow.providerAttributes
                    .get(BaseVCWindow.ATTRIBUTE_KEY_RABBITMQ_USER_PASSWORD));
        }
        if (this.baseVCWindow.providerAttributes.get(BaseVCWindow.ATTRIBUTE_KEY_RABBITMQ_PORT) != null) {
            this.rabbitMQPort.setValue(this.baseVCWindow.providerAttributes
                    .get(BaseVCWindow.ATTRIBUTE_KEY_RABBITMQ_PORT));
        }

        this.form.addComponent(this.providerHttps);
        this.form.addComponent(this.rabbitMQIp);
        this.form.addComponent(this.rabbitMQUserName);
        this.form.addComponent(this.rabbitMQUserPassword);
        this.form.addComponent(this.rabbitMQPort);
    }

    @Override
    public boolean validateForm() {
        try {
            if (this.rabbitMQIp != null) {
                this.rabbitMQIp.validate();
            }
            this.rabbitMQUserName.validate();
            this.rabbitMQUserPassword.validate();
            this.rabbitMQPort.validate();
            return true;
        } catch (Exception e) {
            ViewUtil.iscNotification(e.getMessage() + ".", Notification.Type.ERROR_MESSAGE);
        }
        return false;
    }

    @Override
    public void submitForm() {
        if (validateForm()) {
            try {
                //override all default values with user provided ones...
                this.baseVCWindow.providerAttributes.clear();
                this.baseVCWindow.providerAttributes.put(BaseVCWindow.ATTRIBUTE_KEY_HTTPS, this.providerHttps
                        .getValue().toString());
                this.baseVCWindow.providerAttributes.put(BaseVCWindow.ATTRIBUTE_KEY_RABBITMQ_IP,
                        this.rabbitMQIp.getValue().toString());
                this.baseVCWindow.providerAttributes.put(BaseVCWindow.ATTRIBUTE_KEY_RABBITMQ_USER,
                        this.rabbitMQUserName.getValue().toString());
                    this.baseVCWindow.providerAttributes.put(BaseVCWindow.ATTRIBUTE_KEY_RABBITMQ_USER_PASSWORD,
                            this.rabbitMQUserPassword.getValue().toString());
                this.baseVCWindow.providerAttributes.put(BaseVCWindow.ATTRIBUTE_KEY_RABBITMQ_PORT,
                        this.rabbitMQPort.getValue().toString());
                close();
            } catch (Exception e) {
                String msg = "Failed to encrypt rabbit MQ user password";
                LOG.error(msg, e);
                ViewUtil.iscNotification(msg, Notification.Type.ERROR_MESSAGE);
            }
        }
    }
}
