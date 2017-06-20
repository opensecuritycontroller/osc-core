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
package org.osc.core.broker.util.network;

import java.io.FileReader;
import java.io.IOException;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Properties;
import java.util.Scanner;

import org.apache.log4j.Logger;
import org.osc.core.broker.service.dto.NetworkSettingsDto;
import org.osc.core.broker.util.FileUtil;
import org.osc.core.broker.util.NetworkUtil;
import org.osc.core.broker.util.ServerUtil;



public class NetworkSettingsApi {

    final String BOOTPROTOCOL = "BOOTPROTO";
    final String IPADDRESS = "IPADDR";
    final String NETMASK = "NETMASK";
    final String GATEWAY = "GATEWAY";
    final String DNSSERVER = "nameserver";

    final String DEFAULT_INTERFACE_CONFIG_FILE = "/etc/sysconfig/network-scripts/ifcfg-eth0";
    final String NETWORK_CONFIG_FILE = "/etc/sysconfig/network";
    final String NETWORK_RESOLV = "/etc/resolv.conf";
    final String BASH_SCRIPT = "./scripts/networkSettings.sh";

    private static final Logger log = Logger.getLogger(NetworkSettingsApi.class);

    public void setNetworkSettings(NetworkSettingsDto networkSettingsDto) {

        String proto = "static";

        // in case in future we decide to allow admins to switch from static IP
        // address back to dynamic
        if (networkSettingsDto.isDhcp()) {
            proto = "dhcp";
            networkSettingsDto.setHostIpAddress("");
            networkSettingsDto.setHostSubnetMask("");
            networkSettingsDto.setHostDefaultGateway("");
            networkSettingsDto.setHostDnsServer1("");
            networkSettingsDto.setHostDnsServer2("");
        }

        ServerUtil.execWithLog(new String[] { "/bin/sh", this.BASH_SCRIPT, proto, networkSettingsDto.getHostIpAddress(),
                networkSettingsDto.getHostSubnetMask(), networkSettingsDto.getHostDefaultGateway(),
                networkSettingsDto.getHostDnsServer1(), networkSettingsDto.getHostDnsServer2() });
    }

    public NetworkSettingsDto getNetworkSettings() {

        NetworkSettingsDto networkSettingsDto = new NetworkSettingsDto();

        if (ServerUtil.isWindows()) {
            return networkSettingsDto;
        }

        Properties networkInterfaceConfig = new Properties();

        try {
            networkInterfaceConfig = FileUtil.loadProperties(this.DEFAULT_INTERFACE_CONFIG_FILE);
        } catch (IOException e) {
            log.error("Failed to load network settings", e);
        }

        String bootProtocol = networkInterfaceConfig.getProperty(this.BOOTPROTOCOL, "");

        // return IP, netmask, GW, and DNS IP in static mode
        if (bootProtocol.equals("static")) {

            networkSettingsDto.setDhcp(false);
            networkSettingsDto.setHostIpAddress(networkInterfaceConfig.getProperty(this.IPADDRESS));
            networkSettingsDto.setHostSubnetMask(networkInterfaceConfig.getProperty(this.NETMASK));

            Properties networkConfig = new Properties();
            try {
                networkConfig = FileUtil.loadProperties(this.NETWORK_CONFIG_FILE);
            } catch (IOException e) {
                log.error("Failed to load network settings", e);
            }

            networkSettingsDto.setHostDefaultGateway(networkConfig.getProperty(this.GATEWAY));
            try {
                String[] dns = getDNSSettings();
                networkSettingsDto.setHostDnsServer1(dns[0]);
                networkSettingsDto.setHostDnsServer2(dns[1]);
            } catch (IOException e) {
                log.error("Failed to load DNS settings", e);
            }

        } else {
            // only return IP in DHCP mode
            networkSettingsDto.setDhcp(true);
            String hostIpAddress = null;
            try {
                hostIpAddress = NetworkUtil.getHostIpAddress();
            } catch (SocketException | UnknownHostException e) {
                log.error("Failed to get host and/or ip address", e);
            }
            networkSettingsDto.setHostIpAddress(hostIpAddress);
            networkSettingsDto.setHostSubnetMask("");
            networkSettingsDto.setHostDefaultGateway("");
            networkSettingsDto.setHostDnsServer1("");
            networkSettingsDto.setHostDnsServer2("");
        }

        return networkSettingsDto;
    }

    private String[] getDNSSettings() throws IOException {
        String[] dns = { "", "" };

        int i = 0;
        try (FileReader fileReader = new FileReader(this.NETWORK_RESOLV);
             Scanner in = new Scanner(fileReader)) {
            while (in.hasNextLine()) {
                String[] tokens = in.nextLine().split(" ");
                String s = tokens[0];
                if (s.startsWith("name") && tokens.length > 1) {
                    dns[i++] = tokens[1];
                    if (i > 2) {
                        break;
                    }
                }
            }
        }

        return dns;
    }

}
