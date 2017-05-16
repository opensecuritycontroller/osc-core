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

import java.util.List;

import org.osc.core.broker.window.button.ComponentModel;

import com.vaadin.ui.Alignment;
import com.vaadin.ui.Component;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;

/**
 * A window which allows you to specify any arbitrary content and allows you to specify button actions
 * 
 * @param <T>
 *            the button model type
 */
@SuppressWarnings("serial")
public class VmidcWindow<T extends ComponentModel> extends Window {

    private T componentModel;

    public VmidcWindow(T buttonModel) {
        setModal(true);
        setClosable(false);
        setResizable(false);
        this.componentModel = buttonModel;
    }

    /**
     * Create a Modal window with the given parameters.
     * 
     * @param content
     *            the content to be placed within the window
     */
    @Override
    public void setContent(Component content) {
        if (content == null) {
            super.setContent(content);
        } else {
            VerticalLayout panelContent = new VerticalLayout();

            panelContent.addComponent(content);
            panelContent.addComponent(submitLayout());

            super.setContent(panelContent);
        }
    }

    public T getComponentModel() {
        return this.componentModel;
    }

    private HorizontalLayout submitLayout() {
        // creating generic button layout
        HorizontalLayout submitLayout = new HorizontalLayout();
        submitLayout.setMargin(true);
        submitLayout.setSpacing(true);
        submitLayout.setWidth("100%");
        submitLayout.setDefaultComponentAlignment(Alignment.TOP_RIGHT);
        submitLayout.setSizeFull();

        Label fillItem = new Label();
        submitLayout.addComponent(fillItem);
        submitLayout.setExpandRatio(fillItem, 1);

        List<Component> components = this.componentModel.getComponents();

        for (Component comp : components) {
            submitLayout.addComponent(comp);
        }

        return submitLayout;

    }

}
