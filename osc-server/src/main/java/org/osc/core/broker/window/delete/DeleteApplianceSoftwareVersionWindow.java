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
package org.osc.core.broker.window.delete;

import org.apache.log4j.Logger;
import org.osc.core.broker.service.DeleteApplianceSoftwareVersionService;
import org.osc.core.broker.service.request.BaseIdRequest;
import org.osc.core.broker.view.ApplianceView;
import org.osc.core.broker.view.util.ViewUtil;
import org.osc.core.broker.window.CRUDBaseWindow;
import org.osc.core.broker.window.button.OkCancelButtonModel;

import com.vaadin.ui.Label;
import com.vaadin.ui.Notification;

public class DeleteApplianceSoftwareVersionWindow extends CRUDBaseWindow<OkCancelButtonModel> {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    private static final Logger log = Logger.getLogger(DeleteApplianceWindow.class);

    // current view reference
    private ApplianceView applianceView = null;

    final String CAPTION = "Delete Appliance Software Version";

    public DeleteApplianceSoftwareVersionWindow(ApplianceView applianceView) throws Exception {
        this.applianceView = applianceView;
        createWindow(this.CAPTION);
    }

    @Override
    public void populateForm() {
        Label delete = new Label(this.CAPTION
                + " - "
                + this.applianceView.getChildContainer().getItem(this.applianceView.getChildItemId())
                        .getItemProperty("swVersion").getValue().toString());
        this.form.addComponent(delete);

    }

    @Override
    public boolean validateForm() {
        return false;
    }

    @Override
    public void submitForm() {
        BaseIdRequest delRequest = new BaseIdRequest();
        // Delete appliance software version service has no response so not
        // needed.
        try {
            delRequest.setId(this.applianceView.getChildItemId());
            DeleteApplianceSoftwareVersionService dmc = new DeleteApplianceSoftwareVersionService();

            log.info("deleting Appliance - "
                    + this.applianceView.getChildContainer().getItem(this.applianceView.getChildItemId())
                            .getItemProperty("swVersion").getValue().toString());

            dmc.dispatch(delRequest);

            // deleting a row from the table reference provided by the current
            // view
            this.applianceView.getChildContainer().removeItem(delRequest.getId());
        } catch (Exception e) {
            log.info(e.getMessage());
            ViewUtil.iscNotification(e.getMessage(), Notification.Type.ERROR_MESSAGE);
        }
        close();
    }

}
