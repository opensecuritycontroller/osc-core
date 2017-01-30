package org.osc.core.broker.window.button;

import java.util.List;

import org.osc.core.broker.view.common.VmidcMessages;
import org.osc.core.broker.view.common.VmidcMessages_;

import com.google.common.collect.ImmutableList;
import com.vaadin.event.ShortcutAction.KeyCode;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.Component;

/**
 * A Button model which has OK and cancel buttons. The button model allows you to set click listners on the
 * button so custom application logic can be run when the buttons are clicked
 */
public class OkCancelButtonModel implements ComponentModel {

    protected Button okButton;
    protected Button cancelButton;

    private ClickListener cancelClickListner;
    private ClickListener okClickListner;

    public OkCancelButtonModel() {
        //TODO: Future. have a constructor which will enable.disable parent window shortcut listener...
        this.cancelButton = new Button(VmidcMessages.getString(VmidcMessages_.WINDOW_COMMON_BUTTON_CANCEL));
        this.cancelButton.setClickShortcut(KeyCode.ESCAPE, null);

        this.okButton = new Button(VmidcMessages.getString(VmidcMessages_.WINDOW_COMMON_BUTTON_OK));
        this.okButton.setClickShortcut(KeyCode.ENTER, null);
    }

    public void setCancelClickedListener(ClickListener listener) {
        this.cancelButton.removeClickListener(this.cancelClickListner);
        this.cancelButton.addClickListener(listener);
        this.cancelClickListner = listener;
    }

    public void setOkClickedListener(ClickListener listener) {
        this.okButton.removeClickListener(this.okClickListner);
        this.okButton.addClickListener(listener);
        this.okClickListner = listener;
    }

    public void removeCancelClickShortcut() {
        this.cancelButton.removeClickShortcut();
    }

    public void addCancelClickShortcut() {
        this.cancelButton.setClickShortcut(KeyCode.ESCAPE, null);
    }

    public Button getOkButton() {
        return this.okButton;
    }

    @Override
    public List<Component> getComponents() {
        return ImmutableList.<Component>of(this.cancelButton, this.okButton);
    }

}
