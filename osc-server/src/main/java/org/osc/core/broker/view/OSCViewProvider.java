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

import java.util.Objects;

import org.osgi.service.component.ComponentServiceObjects;

import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewProvider;
import com.vaadin.ui.Component;

public class OSCViewProvider<T extends View> implements ViewProvider {

    private static final long serialVersionUID = -5813320160981331573L;

    private final String name;

    private final Class<T> type;

    private final ComponentServiceObjects<T> factory;

    public OSCViewProvider(String name, Class<T> type, ComponentServiceObjects<T> factory) {
        this.name = Objects.requireNonNull(name, "The view must have a name");
        Objects.requireNonNull(type, "The view must have a type");
        if (!Component.class.isAssignableFrom(type)) {
            throw new IllegalArgumentException("The type must be a Vaadin Component");
        }
        this.type = type;

        this.factory = Objects.requireNonNull(factory, "The view must have a factory");
    }

    @Override
    public String getViewName(String viewAndParameters) {
        if (this.name.equals(viewAndParameters)) {
            return this.name;
        } else if (viewAndParameters != null
                    && viewAndParameters.startsWith(this.name + "/")) {
            return this.name;
        }
        return null;
    }

    @Override
    public View getView(String viewName) {
        if(this.name.equals(viewName)){
            T view = this.factory.getService();
            ((Component)view).addDetachListener(x -> this.factory.ungetService(view));
            return view;
        }
        return null;
    }

    public String getName() {
        return this.name;
    }

    public Class<T> getType() {
        return this.type;
    }

}
