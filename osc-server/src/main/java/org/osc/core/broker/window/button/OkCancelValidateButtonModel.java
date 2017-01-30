package org.osc.core.broker.window.button;

import java.util.List;

import org.osc.core.broker.view.common.VmidcMessages;
import org.osc.core.broker.view.common.VmidcMessages_;

import com.google.common.collect.ImmutableList;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.Component;

/**
 * A Component model which extends OkCancel ButtonModel and adds a validate Button to existing Component Model. This
 * model
 * allows you to set value Change listeners on the Force Deletion Check box
 */
public class OkCancelValidateButtonModel extends OkCancelButtonModel implements ComponentModel {

    private Button validate;
    private ClickListener validateClickListener;

    public OkCancelValidateButtonModel() {

        this.validate = new Button(VmidcMessages.getString(VmidcMessages_.WINDOW_COMMON_BUTTON_VALIDATE));

    }

    public void setValidateClickListener(ClickListener listener) {
        this.validate.removeClickListener(this.validateClickListener);
        this.validate.addClickListener(listener);
        this.validateClickListener = listener;
    }

    public Button getValidate() {
        return this.validate;
    }

    @Override
    public List<Component> getComponents() {
        return ImmutableList.<Component>of(this.validate, this.cancelButton, this.okButton);
    }
}
