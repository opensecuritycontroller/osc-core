package org.osc.core.broker.service.dto.openstack;

import java.util.HashMap;
import java.util.Map;

import org.osc.core.broker.service.dto.BaseDto;
import org.osc.core.broker.util.ValidateUtil;

public class AvailabilityZoneDto extends BaseDto {

    private String region;
    private String zone;

    public String getRegion() {
        return this.region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getZone() {
        return this.zone;
    }

    public void setZone(String zone) {
        this.zone = zone;
    }

    @Override
    public String toString() {
        return "AvailabilityZoneDto [region=" + this.region + ", zone=" + this.zone + "]";
    }

    public static void checkForNullFields(AvailabilityZoneDto dto) throws Exception {

        // build a map of (field,value) pairs to be checked for null/empty
        // values
        Map<String, Object> map = new HashMap<String, Object>();

        map.put("Region", dto.region);
        map.put("Zone", dto.zone);

        ValidateUtil.checkForNullFields(map);
    }

}
