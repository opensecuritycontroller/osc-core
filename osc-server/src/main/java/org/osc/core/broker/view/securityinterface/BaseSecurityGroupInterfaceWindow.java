package org.osc.core.broker.view.securityinterface;

import java.util.List;

import org.apache.log4j.Logger;
import org.osc.core.broker.service.policy.ListVirtualSystemPolicyService;
import org.osc.core.broker.service.policy.PolicyDto;
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

    public BaseSecurityGroupInterfaceWindow(Long vsId) {
        super();
        this.vsId = vsId;
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
            ListVirtualSystemPolicyService service = new ListVirtualSystemPolicyService();

            List<PolicyDto> vsPolicyDto = service.dispatch(req).getList();

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
