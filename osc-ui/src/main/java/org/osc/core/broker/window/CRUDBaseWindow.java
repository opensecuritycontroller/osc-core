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

import org.osc.core.broker.view.PageInformationComponent;
import org.osc.core.broker.view.common.StyleConstants;
import org.osc.core.broker.view.util.ViewUtil;
import org.osc.core.broker.window.button.OkCancelButtonModel;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.Notification;
import com.vaadin.ui.VerticalLayout;

/**
 * Base Windows which provides default functionality for child class to extend
 *
 */
@SuppressWarnings("serial")
public abstract class CRUDBaseWindow<T extends OkCancelButtonModel> extends VmidcWindow<T> {

    private static final Logger log = LoggerFactory.getLogger(CRUDBaseWindow.class);

    protected FormLayout form = null;
    protected VerticalLayout content;

    private PageInformationComponent infoText;

    @SuppressWarnings("unchecked")
    public CRUDBaseWindow() {
        super((T) new OkCancelButtonModel());
    }

    public CRUDBaseWindow(T componentModel) {
        super(componentModel);
    }

    // returns an empty form layout to derived classes with OK and submit in it.
    public void createWindow(String caption) throws Exception {
        setCaption(caption);
        // creating top level layout for every window
        this.content = new VerticalLayout();

        // creating form layout shared by all derived classes
        this.form = new FormLayout();
        this.form.setMargin(true);
        this.form.setSizeUndefined();

        this.infoText = new PageInformationComponent();
        this.infoText.addStyleName(StyleConstants.PAGE_INFO_COMPONENT_WINDOW);

        this.content.addComponent(this.infoText);
        this.content.addComponent(this.form);

        getComponentModel().setOkClickedListener((ClickListener) event -> submitForm());
        getComponentModel().setCancelClickedListener((ClickListener) event -> {
            cancelForm();
            close();
        });

        // calling populateForm to create window specific content
        populateForm();

        // adding content to this window
        setContent(this.content);
    }

    /**
     * @see PageInformationComponent#setInfoText(String, String)
     */

    public void setInfoText(String title, String content) {
        this.infoText.setInfoText(title, content);
    }

    protected void handleCatchAllException(Throwable e) {
        log.error(e.getMessage(), e);
        ViewUtil.iscNotification(e.getMessage(), Notification.Type.ERROR_MESSAGE);
    }

    public void cancelForm(){ }

    // creating window specific forms
    public abstract void populateForm() throws Exception;

    public abstract boolean validateForm();
    // window specific implementation for submitting a form
    public abstract void submitForm();

}
