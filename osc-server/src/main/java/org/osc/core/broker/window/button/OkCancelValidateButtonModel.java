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
