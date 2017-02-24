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
package org.osc.core.broker.view;

import org.osc.core.broker.view.common.StyleConstants;

import com.vaadin.server.ThemeResource;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.AbstractOrderedLayout;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.Component;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.Reindeer;

@SuppressWarnings("serial")
public class PageInformationComponent extends CustomComponent {

    private Label titleLabel;
    private Label contentLabel;

    /**
     * By default the visibility is set to false. If title text is specified, visibility is triggered if the text
     * is not null
     * 
     */
    public PageInformationComponent() {
        setCompositionRoot(buildMainLayout());
        setVisible(false);
    }

    /**
     * Sets the text to show. If the text passed in is null, it hides the component
     * 
     * @param titleText
     *            the text or null
     */
    public void setInfoText(String titleText, String contentText) {
        this.titleLabel.setValue(titleText);
        this.contentLabel.setValue(contentText);
        if (titleText != null) {
            setVisible(true);
        } else {
            setVisible(false);
        }
    }

    private Component buildMainLayout() {
        // top-level component properties
        setWidth("100.0%");
        setHeight("-1px");
        setStyleName(StyleConstants.PAGE_INFO_COMPONENT_COMMON);

        // infoLabel
        this.titleLabel = new Label();
        initializeLabel(this.titleLabel);

        final Button collapseButton = new Button();
        collapseButton.setStyleName(Reindeer.BUTTON_LINK);
        collapseButton.setIcon(new ThemeResource(StyleConstants.EXPAND_IMAGE));
        collapseButton.addClickListener(new ClickListener() {

            @Override
            public void buttonClick(ClickEvent event) {
                PageInformationComponent.this.contentLabel.setVisible(!PageInformationComponent.this.contentLabel
                        .isVisible());
                if (PageInformationComponent.this.contentLabel.isVisible()) {
                    collapseButton.setIcon(new ThemeResource(StyleConstants.COLLAPSE_IMAGE));
                } else {
                    collapseButton.setIcon(new ThemeResource(StyleConstants.EXPAND_IMAGE));
                }
            }
        });

        HorizontalLayout titleLayout = new HorizontalLayout();
        initializeLayout(titleLayout);
        titleLayout.setStyleName(StyleConstants.PAGE_INFO_TITLE_LAYOUT);

        titleLayout.addComponent(this.titleLabel);
        titleLayout.addComponent(collapseButton);
        titleLayout.setExpandRatio(this.titleLabel, 1.0f);

        this.contentLabel = new Label();
        initializeLabel(this.contentLabel);
        this.contentLabel.setVisible(false);

        this.contentLabel.setStyleName(StyleConstants.PAGE_INFO_CONTENT_LABEL);
        this.contentLabel.setContentMode(ContentMode.HTML);

        VerticalLayout mainLayout = new VerticalLayout();
        initializeLayout(mainLayout);
        mainLayout.addComponent(titleLayout);
        mainLayout.addComponent(this.contentLabel);

        return mainLayout;
    }

    private void initializeLabel(Label label) {
        label.setImmediate(true);
        label.setWidth("100%");
        label.setHeight("-1px");
    }

    private void initializeLayout(AbstractOrderedLayout layout) {
        layout.setImmediate(false);
        layout.setWidth("100%");
        layout.setHeight("-1px");
        layout.setMargin(false);
    }

}
