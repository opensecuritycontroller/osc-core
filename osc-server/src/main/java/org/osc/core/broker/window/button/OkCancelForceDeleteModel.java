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

import java.util.List;

import org.osc.core.broker.view.common.VmidcMessages;
import org.osc.core.broker.view.common.VmidcMessages_;

import com.google.common.collect.ImmutableList;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.Component;

/**
 * A Component model which extends OkCancel ButtonModel and adds a Force Delete CheckBox. This model
 * allows you to set value Change listeners on the Force Deletion Check box
 */
public class OkCancelForceDeleteModel extends OkCancelButtonModel implements ComponentModel {

    private CheckBox forceDelete;
    private ValueChangeListener forceDeleteValueChangeListener;

    public OkCancelForceDeleteModel() {
        this.forceDelete = new CheckBox(VmidcMessages.getString(VmidcMessages_.WINDOW_COMMON_CHECKBOX_FORCE_DELETE));
        this.forceDelete.addStyleName("forceDelete");
    }

    public void setForceDeleteValueChangeListener(ValueChangeListener forceDeleteValueChangeListener) {
        this.forceDelete.removeValueChangeListener(this.forceDeleteValueChangeListener);
        this.forceDelete.addValueChangeListener(forceDeleteValueChangeListener);
        this.forceDeleteValueChangeListener = forceDeleteValueChangeListener;
    }

    @Override
    public List<Component> getComponents() {
        return ImmutableList.<Component>of(this.forceDelete, this.cancelButton, this.okButton);
    }

    public boolean getForceDeleteCheckBoxValue() {
        return this.forceDelete.getValue();
    }

    public void setForceDeleteValue(Boolean value) {
        this.forceDelete.setValue(value);
    }
}
