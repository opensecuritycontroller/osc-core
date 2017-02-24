/*******************************************************************************
 * Copyright (c) 2017 Intel Corporation
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

import com.google.common.collect.ImmutableList;
import com.vaadin.event.ShortcutAction;
import com.vaadin.ui.Button;
import com.vaadin.ui.Component;
import org.osc.core.broker.view.common.VmidcMessages;
import org.osc.core.broker.view.common.VmidcMessages_;

import java.util.List;

public class ApproveCancelButtonModel extends OkCancelButtonModel implements ComponentModel {

    private Button approveButton;
    private Button rejectButton;

    private Button.ClickListener cancelClickListener;
    private Button.ClickListener okClickListener;

    public ApproveCancelButtonModel() {
        this.rejectButton = new Button(VmidcMessages.getString(VmidcMessages_.WINDOW_COMMON_BUTTON_CANCEL));
        this.rejectButton.setClickShortcut(ShortcutAction.KeyCode.ESCAPE, null);

        this.approveButton = new Button(VmidcMessages.getString(VmidcMessages_.WINDOW_COMMON_BUTTON_APPROVEALL));
        this.approveButton.setClickShortcut(ShortcutAction.KeyCode.ENTER, null);
    }

    public void setCancelClickedListener(Button.ClickListener listener) {
        this.rejectButton.removeClickListener(this.cancelClickListener);
        this.rejectButton.addClickListener(listener);
        this.cancelClickListener = listener;
    }

    public void setApproveClickedListener(Button.ClickListener listener) {
        this.approveButton.removeClickListener(this.okClickListener);
        this.approveButton.addClickListener(listener);
        this.okClickListener = listener;
    }

    public void removeCancelClickShortcut() {
        this.rejectButton.removeClickShortcut();
    }

    public void addCancelClickShortcut() {
        this.rejectButton.setClickShortcut(ShortcutAction.KeyCode.ESCAPE, null);
    }

    @Override
    public List<Component> getComponents() {
        return ImmutableList.of(this.rejectButton, this.approveButton);
    }
}
