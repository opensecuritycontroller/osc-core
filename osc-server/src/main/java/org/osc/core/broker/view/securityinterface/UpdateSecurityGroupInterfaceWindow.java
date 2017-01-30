package org.osc.core.broker.view.securityinterface;

import org.apache.log4j.Logger;
import org.osc.core.broker.service.policy.PolicyDto;
import org.osc.core.broker.service.request.BaseRequest;
import org.osc.core.broker.service.securityinterface.SecurityGroupInterfaceDto;
import org.osc.core.broker.service.securityinterface.UpdateSecurityGroupInterfaceService;
import org.osc.core.broker.view.util.ViewUtil;
import org.osc.sdk.controller.FailurePolicyType;

import com.vaadin.ui.Notification;

public class UpdateSecurityGroupInterfaceWindow extends BaseSecurityGroupInterfaceWindow {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    private static final Logger log = Logger.getLogger(UpdateSecurityGroupInterfaceWindow.class);
    private final SecurityGroupInterfaceDto dto;

    final String CAPTION = "Update Policy Mapping";

    public UpdateSecurityGroupInterfaceWindow(SecurityGroupInterfaceDto dto) throws Exception {
        super(dto.getParentId());
        this.dto = dto;
        createWindow(this.CAPTION);
    }

    @Override
    public void populateForm() {
        try {
            this.form.addComponent(getName());
            this.form.addComponent(getPolicy());
            this.form.addComponent(getTag());

            // filling existing information to our form
            this.name.setValue(this.dto.getName());

            for (Object id : this.policy.getContainerDataSource().getItemIds()) {
                if (this.dto.getPolicyId().equals(
                        this.policy.getContainerDataSource().getContainerProperty(id, "id").getValue())) {
                    this.policy.select(id);
                }
            }

            this.tag.setValue(this.dto.getTagValue().toString());

        } catch (Exception e) {
            ViewUtil.iscNotification(e.getMessage(), Notification.Type.ERROR_MESSAGE);
        }
    }

    @Override
    public void submitForm() {
        try {
            if (validateForm()) {
                SecurityGroupInterfaceDto newDto = new SecurityGroupInterfaceDto();
                newDto.setId(this.dto.getId());
                newDto.setParentId(this.dto.getParentId());
                newDto.setName(this.name.getValue().trim());
                newDto.setIsUserConfigurable(true);
                newDto.setTagValue(Long.parseLong(this.tag.getValue()));
                PolicyDto policyDto = (PolicyDto) this.policy.getValue();
                newDto.setPolicyId(policyDto.getId());
                newDto.setFailurePolicyType(FailurePolicyType.NA);

                BaseRequest<SecurityGroupInterfaceDto> req = new BaseRequest<SecurityGroupInterfaceDto>();
                req.setDto(newDto);

                UpdateSecurityGroupInterfaceService updateService = new UpdateSecurityGroupInterfaceService();
                updateService.dispatch(req);

                close();
            }

        } catch (Exception e) {
            log.error("Fail to update Security Group Interface", e);
            ViewUtil.iscNotification(e.getMessage(), Notification.Type.ERROR_MESSAGE);
        }
    }

}
