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

import org.osc.core.broker.view.common.StyleConstants;

import com.vaadin.ui.Alignment;
import com.vaadin.ui.Label;
import com.vaadin.ui.ProgressBar;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;

/**
 * A modal window which shows a busy cursor
 *
 */
@SuppressWarnings("serial")
public class ProgressIndicatorWindow extends Window {

    private Label currentStatus;

    public ProgressIndicatorWindow() {
        center();
        setVisible(true);
        setResizable(false);
        setDraggable(false);
        setImmediate(true);
        setModal(true);
        setClosable(false);
        setCaption("Loading");

        VerticalLayout layout = new VerticalLayout();
        layout.setMargin(true);
        layout.setWidth("100%");

        this.currentStatus = new Label();
        this.currentStatus.addStyleName(StyleConstants.TEXT_ALIGN_CENTER);
        this.currentStatus.setSizeFull();
        this.currentStatus.setImmediate(true);

        ProgressBar progressBar = new ProgressBar();
        progressBar.setSizeFull();
        progressBar.setIndeterminate(true);
        progressBar.setImmediate(true);
        progressBar.setVisible(true);

        layout.addComponent(progressBar);
        layout.addComponent(this.currentStatus);
        layout.setComponentAlignment(this.currentStatus, Alignment.MIDDLE_CENTER);
        layout.setComponentAlignment(progressBar, Alignment.MIDDLE_CENTER);
        setContent(layout);
    }

    public void updateStatus(String status) {
        this.currentStatus.setCaption(status);
        UI.getCurrent().push();
    }

}
