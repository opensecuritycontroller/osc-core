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

import org.osc.core.broker.window.button.ComponentModel;
import org.osc.core.broker.window.button.OkCancelValidateButtonModel;

import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;

/**
 * Base Windows which provides default functionality for child class to extend
 * 
 */
@SuppressWarnings("serial")
public abstract class CRUDBaseValidateWindow extends CRUDBaseWindow<OkCancelValidateButtonModel> {

    public CRUDBaseValidateWindow(ComponentModel model) {
        super(new OkCancelValidateButtonModel());
    }

    // returns an empty form layout to derived classes with OK and submit in it.
    @Override
    public void createWindow(String caption) throws Exception {
        super.createWindow(caption);
        getComponentModel().setValidateClickListener(new ClickListener() {
            @Override
            public void buttonClick(ClickEvent event) {
                validateSettings();
            }
        });
    }

    public abstract void validateSettings();
}
