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
package org.osc.core.broker.window;

import com.vaadin.ui.Button.ClickListener;
import org.osc.core.broker.window.button.ApproveCancelButtonModel;
import org.osc.core.broker.window.button.ComponentModel;

@SuppressWarnings("serial")
public abstract class CRUDBaseApproveWindow extends CRUDBaseWindow<ApproveCancelButtonModel> {

    public CRUDBaseApproveWindow(ComponentModel model) {
        super(new ApproveCancelButtonModel());
    }

    @Override
    public void createWindow(String caption) throws Exception {
        super.createWindow(caption);
        getComponentModel().setApproveClickedListener((ClickListener) event -> submitForm());
        getComponentModel().setCancelClickedListener((ClickListener) event -> {
            cancelForm();
            close();
        });
    }
}