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
package org.osc.core.broker.window.delete;

import org.apache.log4j.Logger;
import org.osc.core.broker.service.DeleteApplianceManagerConnectorService;
import org.osc.core.broker.service.request.BaseIdRequest;
import org.osc.core.broker.service.response.BaseJobResponse;
import org.osc.core.broker.view.ManagerConnectorView;
import org.osc.core.broker.view.util.ViewUtil;
import org.osc.core.broker.window.CRUDBaseWindow;
import org.osc.core.broker.window.button.OkCancelButtonModel;

import com.vaadin.ui.Label;
import com.vaadin.ui.Notification;

public class DeleteManagerConnectorWindow extends CRUDBaseWindow<OkCancelButtonModel> {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    private static final Logger log = Logger.getLogger(DeleteManagerConnectorWindow.class);

    final String CAPTION = "Delete Manager Connector";

    private ManagerConnectorView mcView = null;

    public DeleteManagerConnectorWindow(ManagerConnectorView mcView) throws Exception {
        this.mcView = mcView;
        createWindow(this.CAPTION);
    }

    @Override
    public void populateForm() {
        Label delete = new Label(this.CAPTION
                + " - "
                + this.mcView.getParentContainer().getItem(this.mcView.getParentItemId()).getItemProperty("name")
                        .getValue().toString());
        this.form.addComponent(delete);
    }

    @Override
    public void submitForm() {
        BaseIdRequest delRequest = new BaseIdRequest();
        // Delete MC service has no response so not needed.
        try {
            delRequest.setId(this.mcView.getParentItemId());
            DeleteApplianceManagerConnectorService dmc = new DeleteApplianceManagerConnectorService();

            log.info("deleting Manager Connector - "
                    + this.mcView.getParentContainer().getItem(this.mcView.getParentItemId()).getItemProperty("name")
                            .getValue().toString());

            BaseJobResponse response = dmc.dispatch(delRequest);

            ViewUtil.showJobNotification(response.getJobId());

        } catch (Exception e) {
            log.info(e.getMessage());
            ViewUtil.iscNotification(e.getMessage(), Notification.Type.ERROR_MESSAGE);
        }
        close();
    }

    @Override
    public boolean validateForm() {
        // no UI validation needed for deleting a MC
        return false;
    }

}
