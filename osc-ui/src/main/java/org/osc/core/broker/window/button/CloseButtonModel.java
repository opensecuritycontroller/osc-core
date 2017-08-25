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
 * A Button model which has close button. The button model allows you to set click listners on the
 * button so custom application logic can be run when the buttons are clicked
 */
public class CloseButtonModel implements ComponentModel {

    protected Button closeButton;

    private ClickListener closeClickListner;

    public CloseButtonModel() {
        this.closeButton = new Button(VmidcMessages.getString(VmidcMessages_.WINDOW_COMMON_BUTTON_CLOSE));
        this.closeButton.setClickShortcut(KeyCode.ESCAPE, null);
    }

    public void setCloseClickedListener(ClickListener listener) {
        this.closeButton.removeClickListener(this.closeClickListner);
        this.closeButton.addClickListener(listener);
        this.closeClickListner = listener;
    }

    public void removeCloseClickShortcut() {
        this.closeButton.removeClickShortcut();
    }

    public void addCloseClickShortcut() {
        this.closeButton.setClickShortcut(KeyCode.ESCAPE, null);
    }

    @Override
    public List<Component> getComponents() {
        return Arrays.asList(this.closeButton);
    }

}