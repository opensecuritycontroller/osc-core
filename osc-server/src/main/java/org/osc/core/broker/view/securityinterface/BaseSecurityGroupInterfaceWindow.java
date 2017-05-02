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
package org.osc.core.broker.view.securityinterface;

import java.util.List;

import org.apache.log4j.Logger;
import org.osc.core.broker.service.api.ListVirtualSystemPolicyServiceApi;
import org.osc.core.broker.service.dto.PolicyDto;
import org.osc.core.broker.service.request.BaseIdRequest;
import org.osc.core.broker.view.util.ViewUtil;
import org.osc.core.broker.window.CRUDBaseWindow;
import org.osc.core.broker.window.button.OkCancelButtonModel;

import com.vaadin.data.Validator.InvalidValueException;
import com.vaadin.data.util.BeanItemContainer;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.Notification;
import com.vaadin.ui.TextField;

public abstract class BaseSecurityGroupInterfaceWindow extends CRUDBaseWindow<OkCancelButtonModel> {

    /**
     *
     */
    private static final long serialVersionUID = 1L;
    private static Logger log = Logger.getLogger(BaseSecurityGroupInterfaceWindow.class);

    protected TextField name;
    protected ComboBox policy;
    protected TextField tag;
    protected Long vsId;

    private final ListVirtualSystemPolicyServiceApi listVirtualSystemPolicyService;

    public BaseSecurityGroupInterfaceWindow(Long vsId, ListVirtualSystemPolicyServiceApi listVirtualSystemPolicyService) {
        super();
        this.vsId = vsId;
        this.listVirtualSystemPolicyService = listVirtualSystemPolicyService;
    }

    protected TextField getName() {
        this.name = new TextField("Name");
        this.name.setImmediate(true);
        this.name.setRequired(true);
        this.name.setRequiredError("Name cannot be empty");
        return this.name;
    }

    protected TextField getTag() {
        this.tag = new TextField("Tag");
        this.tag.setImmediate(true);
        this.tag.setRequired(true);
        this.tag.setRequiredError("Tag cannot be empty");
        return this.tag;
    }

    protected Component getPolicy() {
        try {
            this.policy = new ComboBox("Select Policy");
            this.policy.setTextInputAllowed(false);
            this.policy.setNullSelectionAllowed(false);
            this.policy.setImmediate(true);
            this.policy.setRequired(true);
            this.policy.setRequiredError("Policy cannot be empty");
            populatePolicy();

        } catch (Exception e) {
            ViewUtil.iscNotification(e.getMessage(), Notification.Type.ERROR_MESSAGE);
            log.error("Error populating Policy List combobox", e);
        }

        return this.policy;
    }

    private void populatePolicy() {
        try {
            // Calling List Service
            BaseIdRequest req = new BaseIdRequest();
            req.setId(this.vsId);

            List<PolicyDto> vsPolicyDto = this.listVirtualSystemPolicyService.dispatch(req).getList();

            BeanItemContainer<PolicyDto> vsPolicyListContainer = new BeanItemContainer<PolicyDto>(PolicyDto.class,
                    vsPolicyDto);
            this.policy.setContainerDataSource(vsPolicyListContainer);
            this.policy.setItemCaptionPropertyId("policyName");

            if (vsPolicyListContainer.size() > 0) {
                this.policy.select(vsPolicyListContainer.getIdByIndex(0));
            }
        } catch (Exception e) {
            ViewUtil.iscNotification(e.getMessage(), Notification.Type.ERROR_MESSAGE);
            log.error("Error getting Virtual System Policy List", e);
        }
    }

    @Override
    public boolean validateForm() {
        this.name.validate();
        this.policy.validate();
        this.tag.validate();
        try {
            Long.parseLong(this.tag.getValue());
        } catch (NumberFormatException nfe) {
            log.error("Invalid tag value. Parse Excetion.", nfe);
            throw new InvalidValueException("Invalid tag value. Only Numbers are allowed.");
        }
        return true;
    }
}
