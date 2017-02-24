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
package org.osc.core.broker.view.maintenance;

import org.apache.log4j.Logger;
import org.osc.core.broker.service.email.EmailSettingsDto;
import org.osc.core.broker.service.email.GetEmailSettingsService;
import org.osc.core.broker.service.request.Request;
import org.osc.core.broker.service.response.BaseDtoResponse;
import org.osc.core.broker.view.common.StyleConstants;
import org.osc.core.broker.view.util.ViewUtil;
import org.osc.core.broker.window.update.SetEmailSettingsWindow;

import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Table;
import com.vaadin.ui.VerticalLayout;

public class EmailLayout extends FormLayout {

    private static final long serialVersionUID = 1L;
    private static final Logger log = Logger.getLogger(EmailLayout.class);

    public Table emailTable = null;
    private VerticalLayout container = null;
    private Button edit = null;

    public EmailLayout() {
        super();
        try {

            this.emailTable = createTable();

            // creating layout to hold edit button
            HorizontalLayout optionLayout = new HorizontalLayout();
            optionLayout.addComponent(createEditButton());

            // populating Email Settings in the Table
            populateEmailtable();

            // adding all components to Container
            this.container = new VerticalLayout();
            this.container.addComponent(optionLayout);
            this.container.addComponent(this.emailTable);

            // adding container to the root Layout
            addComponent(this.container);
        } catch (Exception ex) {
            log.error("Failed to get email settings", ex);
        }
    }

    @SuppressWarnings("serial")
    private Button createEditButton() {
        // creating edit button
        this.edit = new Button("Edit");
        this.edit.setEnabled(true);
        this.edit.addStyleName(StyleConstants.BUTTON_TOOLBAR);
        this.edit.addStyleName(StyleConstants.EDIT_BUTTON);
        this.edit.addClickListener(new ClickListener() {
            @Override
            public void buttonClick(ClickEvent event) {
                try {
                    editClicked();
                } catch (Exception e) {
                    ViewUtil.showError("Error editing email settings", e);
                }
            }
        });
        return this.edit;
    }

    private void editClicked() throws Exception {
        try {
            ViewUtil.addWindow(new SetEmailSettingsWindow(this));
        } catch (Exception ex) {
            log.error("Error: " + ex);
        }
    }

    private Table createTable() {
        Table table = new Table();
        table.setSizeFull();
        table.setPageLength(0);
        table.setSelectable(false);
        table.setColumnCollapsingAllowed(true);
        table.setColumnReorderingAllowed(true);
        table.setImmediate(true);
        table.setNullSelectionAllowed(false);
        table.setColumnHeaderMode(Table.ColumnHeaderMode.HIDDEN);
        table.addContainerProperty("Name", String.class, null);
        table.addContainerProperty("Value", String.class, null);

        // initializing email table with empty values
        table.addItem(new Object[] { "Outgoing Mail Server (SMTP): ", "" }, new Integer(1));
        table.addItem(new Object[] { "Port: ", "" }, new Integer(2));
        table.addItem(new Object[] { "Email Id: ", "" }, new Integer(3));

        return table;
    }

    @SuppressWarnings("unchecked")
    public void populateEmailtable() {
        try {
            GetEmailSettingsService emailService = new GetEmailSettingsService();

            BaseDtoResponse<EmailSettingsDto> emailSettingsResponse = emailService.dispatch(new Request() {
            });

            if (emailSettingsResponse.getDto() != null) {
                this.emailTable.getItem(1).getItemProperty("Value")
                        .setValue(emailSettingsResponse.getDto().getMailServer());
                this.emailTable.getItem(2).getItemProperty("Value").setValue(emailSettingsResponse.getDto().getPort());
                this.emailTable.getItem(3).getItemProperty("Value")
                        .setValue(emailSettingsResponse.getDto().getEmailId());
            }

        } catch (Exception ex) {
            log.error("Failed to get email settings", ex);
        }

    }

}
