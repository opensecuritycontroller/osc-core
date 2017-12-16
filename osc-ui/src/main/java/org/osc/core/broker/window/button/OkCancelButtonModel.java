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
package org.osc.core.broker.window.button;

import java.util.Arrays;
import java.util.List;

import org.osc.core.broker.view.common.VmidcMessages;
import org.osc.core.broker.view.common.VmidcMessages_;

import com.vaadin.event.ShortcutAction.KeyCode;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.Component;

/**
 * A Button model which has OK and cancel buttons. The button model allows you to set click listeners on the
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
        return Arrays.asList(this.cancelButton, this.okButton);
    }

}