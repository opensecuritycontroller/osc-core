package org.osc.core.broker.service.securitygroup;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.osc.core.broker.service.dto.BaseDto;
import org.osc.core.broker.service.request.BaseRequest;
import org.osc.core.broker.util.ValidateUtil;

public class BindSecurityGroupRequest extends BaseRequest<BaseDto> {

    /**
     * The virtualization connector ID this security group belongs to. This is to support
     * validation from REST API Calls. VC Id will not be used to load the vc.
     * Intended to be null for all cases except for the API
     */
    private Long vcId;
    private Long securityGroupId;
    private List<VirtualSystemPolicyBindingDto> servicesToBindTo = new ArrayList<>();

    public boolean isUnBindSecurityGroup() {
        return this.servicesToBindTo == null || this.servicesToBindTo.isEmpty();
    }

    public Long getSecurityGroupId() {
        return this.securityGroupId;
    }

    public void setSecurityGroupId(Long securityGroupId) {
        this.securityGroupId = securityGroupId;
    }

    public List<VirtualSystemPolicyBindingDto> getServicesToBindTo() {
        return this.servicesToBindTo;
    }

    void setServicesToBindTo(List<VirtualSystemPolicyBindingDto> servicesToBindTo) {
        this.servicesToBindTo = servicesToBindTo;
    }

    public void addServiceToBindTo(VirtualSystemPolicyBindingDto serviceToBindTo) {
        this.servicesToBindTo.add(serviceToBindTo);
    }

    public Long getVcId() {
        return this.vcId;
    }

    public void setVcId(Long vcId) {
        this.vcId = vcId;
    }

    public static void checkForNullFields(BindSecurityGroupRequest request) throws Exception {

        // build a map of (field,value) pairs to be checked for null/empty
        // values
        Map<String, Object> map = new HashMap<String, Object>();

        map.put("Security Group Id", request.getSecurityGroupId());
        List<VirtualSystemPolicyBindingDto> services = request.getServicesToBindTo();
        if (services != null && !services.isEmpty()) {
            for (VirtualSystemPolicyBindingDto service : services) {
                if (service.getVirtualSystemId() == null || service.getPolicyId() == null
                        || StringUtils.isBlank(service.getName()) || service.getOrder() == null) {
                    map.put("Virtual System Id", service.getVirtualSystemId());
                    map.put("Service Name", service.getName());
                    map.put("Service Order", service.getOrder());
                    break;
                }
            }
        }

        ValidateUtil.checkForNullFields(map);
    }

}
