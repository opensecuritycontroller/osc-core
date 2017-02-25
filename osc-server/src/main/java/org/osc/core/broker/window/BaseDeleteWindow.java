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
package org.osc.core.broker.window;

import org.osc.core.broker.window.button.OkCancelButtonModel;

import com.vaadin.ui.CheckBox;

/**
 * Base Windows which provides default functionality for child class to extend
 * 
 */

public abstract class BaseDeleteWindow extends CRUDBaseWindow<OkCancelButtonModel> {
    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    protected CheckBox forceDeletion;

    public BaseDeleteWindow() throws Exception {
        super();
        this.forceDeletion = new CheckBox("Force Delete");

    }
}
