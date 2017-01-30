package org.osc.core.broker.view.vc;

import org.osc.core.broker.model.entities.virtualization.VirtualizationConnector;
import org.osc.core.broker.view.common.VmidcMessages;
import org.osc.core.broker.view.common.VmidcMessages_;
import org.osc.core.broker.view.util.ViewUtil;
import org.osc.core.broker.window.CRUDBaseWindow;
import org.osc.core.broker.window.button.OkCancelButtonModel;
import org.osc.core.util.EncryptionUtil;

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

    private CheckBox providerHttps = null;
    private TextField rabbitMQIp = null;
    private TextField rabbitMQUserName = null;
    private PasswordField rabbitMQUserPassword = null;
    private TextField rabbitMQPort = null;
    private BaseVCWindow baseVCWindow;

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
                .get(VirtualizationConnector.ATTRIBUTE_KEY_HTTPS)));
        if (this.baseVCWindow.providerAttributes.get(VirtualizationConnector.ATTRIBUTE_KEY_RABBITMQ_IP) != null) {
            this.rabbitMQIp.setValue(this.baseVCWindow.providerAttributes
                    .get(VirtualizationConnector.ATTRIBUTE_KEY_RABBITMQ_IP));
        }
        if (this.baseVCWindow.providerAttributes.get(VirtualizationConnector.ATTRIBUTE_KEY_RABBITMQ_USER) != null) {
            this.rabbitMQUserName.setValue(this.baseVCWindow.providerAttributes
                    .get(VirtualizationConnector.ATTRIBUTE_KEY_RABBITMQ_USER));
        }
        if (this.baseVCWindow.providerAttributes.get(VirtualizationConnector.ATTRIBUTE_KEY_RABBITMQ_USER_PASSWORD) != null) {
            this.rabbitMQUserPassword.setValue(EncryptionUtil.decrypt(this.baseVCWindow.providerAttributes
                    .get(VirtualizationConnector.ATTRIBUTE_KEY_RABBITMQ_USER_PASSWORD)));
        }
        if (this.baseVCWindow.providerAttributes.get(VirtualizationConnector.ATTRIBUTE_KEY_RABBITMQ_PORT) != null) {
            this.rabbitMQPort.setValue(this.baseVCWindow.providerAttributes
                    .get(VirtualizationConnector.ATTRIBUTE_KEY_RABBITMQ_PORT));
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
            //override all default values with user provided ones...
            this.baseVCWindow.providerAttributes.clear();
            this.baseVCWindow.providerAttributes.put(VirtualizationConnector.ATTRIBUTE_KEY_HTTPS, this.providerHttps
                    .getValue().toString());
            this.baseVCWindow.providerAttributes.put(VirtualizationConnector.ATTRIBUTE_KEY_RABBITMQ_IP,
                    this.rabbitMQIp.getValue().toString());
            this.baseVCWindow.providerAttributes.put(VirtualizationConnector.ATTRIBUTE_KEY_RABBITMQ_USER,
                    this.rabbitMQUserName.getValue().toString());
            this.baseVCWindow.providerAttributes.put(VirtualizationConnector.ATTRIBUTE_KEY_RABBITMQ_USER_PASSWORD,
                    EncryptionUtil.encrypt(this.rabbitMQUserPassword.getValue().toString()));
            this.baseVCWindow.providerAttributes.put(VirtualizationConnector.ATTRIBUTE_KEY_RABBITMQ_PORT,
                    this.rabbitMQPort.getValue().toString());
            close();
        }
    }
}
