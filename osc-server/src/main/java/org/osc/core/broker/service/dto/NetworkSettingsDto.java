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
package org.osc.core.broker.service.dto;

import java.util.HashMap;
import java.util.Map;

import org.osc.core.broker.util.ValidateUtil;

public class NetworkSettingsDto extends BaseDto{

    private boolean dhcp;
    private String hostIpAddress;
    private String hostSubnetMask;
    private String hostDefaultGateway;
    private String hostDnsServer1;
    private String hostDnsServer2;

    public boolean isDhcp() {
        return dhcp;
    }

    public void setDhcp(boolean dhcp) {
        this.dhcp = dhcp;
    }

    public String getHostIpAddress() {
        return hostIpAddress;
    }

    public void setHostIpAddress(String hostIpAddress) {
        this.hostIpAddress = hostIpAddress;
    }

    public String getHostSubnetMask() {
        return hostSubnetMask;
    }

    public void setHostSubnetMask(String hostSubnetMask) {
        this.hostSubnetMask = hostSubnetMask;
    }

    public String getHostDefaultGateway() {
        return hostDefaultGateway;
    }

    public void setHostDefaultGateway(String hostDefaultGateway) {
        this.hostDefaultGateway = hostDefaultGateway;
    }

    public String getHostDnsServer1() {
        return hostDnsServer1 == null ? "" : hostDnsServer1;
    }

    public void setHostDnsServer1(String hostDnsServer1) {
        this.hostDnsServer1 = hostDnsServer1;
    }

    public String getHostDnsServer2() {
        return hostDnsServer2 == null ? "" : hostDnsServer2;
    }

    public void setHostDnsServer2(String hostDnsServer2) {
        this.hostDnsServer2 = hostDnsServer2;
    }

    @Override
    public String toString() {
        return "NetworkSettingsDto [dhcp=" + dhcp + ", hostIpAddress=" + hostIpAddress + ", hostSubnetMask="
                + hostSubnetMask + ", hostDefaultGateway=" + hostDefaultGateway + ", hostDnsServer1=" + hostDnsServer1
                + ", hostDnsServer2=" + hostDnsServer2 + "]";
    }

    public static void checkForNullFields(NetworkSettingsDto dto) throws Exception {
        // build a map of (field,value) pairs to be checked for null/empty
        // values
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("IP Address", dto.getHostIpAddress());
        map.put("Net Mask", dto.getHostSubnetMask());
        map.put("Default Gateway", dto.getHostDefaultGateway());

        ValidateUtil.checkForNullFields(map);
    }

}
