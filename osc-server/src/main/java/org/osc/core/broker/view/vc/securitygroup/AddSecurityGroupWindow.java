package org.osc.core.broker.view.vc.securitygroup;

import org.apache.log4j.Logger;
import org.jclouds.openstack.keystone.v2_0.domain.Tenant;
import org.osc.core.broker.service.dto.VirtualizationConnectorDto;
import org.osc.core.broker.service.response.BaseJobResponse;
import org.osc.core.broker.service.securitygroup.AddOrUpdateSecurityGroupRequest;
import org.osc.core.broker.service.securitygroup.AddSecurityGroupService;
import org.osc.core.broker.service.securitygroup.SecurityGroupDto;
import org.osc.core.broker.view.util.ViewUtil;

import com.vaadin.ui.Notification;

public class AddSecurityGroupWindow extends BaseSecurityGroupWindow {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    final String CAPTION = "Add Security Group";

    private static final Logger log = Logger.getLogger(AddSecurityGroupWindow.class);

    public AddSecurityGroupWindow(VirtualizationConnectorDto vcDto) throws Exception {
        this.currentSecurityGroup = new SecurityGroupDto();
        this.currentSecurityGroup.setParentId(vcDto.getId());
        createWindow(this.CAPTION);
        enableSelection(false);

    }

    @Override
    public void submitForm() {
        try {
            if (validateForm()) {
                AddOrUpdateSecurityGroupRequest request = new AddOrUpdateSecurityGroupRequest();

                SecurityGroupDto dto = new SecurityGroupDto();
                dto.setParentId(this.currentSecurityGroup.getParentId());
                dto.setName(this.name.getValue().trim());
                dto.setTenantId(((Tenant) this.tenant.getValue()).getId());
                dto.setTenantName(((Tenant) this.tenant.getValue()).getName());
                dto.setProtectAll(this.protectionTypeOption.getValue() == TYPE_ALL);

                request.setDto(dto);
                request.setMembers(getSelectedMembers());

                AddSecurityGroupService addService = new AddSecurityGroupService();
                BaseJobResponse response = addService.dispatch(request);

                close();
                ViewUtil.showJobNotification(response.getJobId());
            }

        } catch (Exception e) {
            log.error("Fail to add Security Group", e);
            ViewUtil.iscNotification(e.getMessage(), Notification.Type.ERROR_MESSAGE);
        }
    }

}
