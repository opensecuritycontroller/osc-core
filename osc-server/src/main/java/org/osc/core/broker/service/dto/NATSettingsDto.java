package org.osc.core.broker.service.dto;

import java.util.HashMap;
import java.util.Map;

import org.osc.core.broker.util.ValidateUtil;

public class NATSettingsDto extends BaseDto {

    private String publicIPAddress;

    public NATSettingsDto(String publicIPAddress) {
        super();
        this.publicIPAddress = publicIPAddress;
    }

    public String getPublicIPAddress() {
        return this.publicIPAddress;
    }

    public void setPublicIPAddress(String publicIPAddress) {
        this.publicIPAddress = publicIPAddress;
    }

    public static void checkForNullFields(NATSettingsDto dto) throws Exception {
        // build a map of (field,value) pairs to be checked for null/empty
        // values
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("IP Address", dto.getPublicIPAddress());
        ValidateUtil.checkForNullFields(map);
    }

}
