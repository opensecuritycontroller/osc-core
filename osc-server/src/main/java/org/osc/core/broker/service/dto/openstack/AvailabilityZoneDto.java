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
