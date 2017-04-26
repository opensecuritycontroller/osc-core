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
package org.osc.core.broker.view;

import org.osgi.service.component.ComponentServiceObjects;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.vaadin.server.UIClassSelectionEvent;
import com.vaadin.server.UICreateEvent;
import com.vaadin.server.UIProvider;
import com.vaadin.ui.UI;

@Component(service=MainUIProvider.class)
public class MainUIProvider extends UIProvider {

    private static final long serialVersionUID = 3597397776591350473L;

    @Reference
    ComponentServiceObjects<MainUI> mainUIFactory;

    @Override
    public Class<? extends UI> getUIClass(UIClassSelectionEvent event) {
        return MainUI.class;
    }

    @Override
    public UI createInstance(UICreateEvent event) {
        MainUI ui = this.mainUIFactory.getService();

        ui.addComponentDetachListener(x -> this.mainUIFactory.ungetService(ui));

        return ui;
    }

}
