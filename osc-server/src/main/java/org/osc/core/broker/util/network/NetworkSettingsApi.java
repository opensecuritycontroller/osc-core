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

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Scanner;

import org.osc.core.broker.service.dto.NetworkSettingsDto;
import org.osc.core.broker.util.NetworkUtil;
import org.osc.core.broker.util.ServerUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



public class NetworkSettingsApi {

    private final String NETWORK_RESOLV = "/etc/resolv.conf";
    private static final String NETWORK_SETTINGS_SCRIPT = "./scripts/networkSettings.sh";
    private static final Logger log = LoggerFactory.getLogger(NetworkSettingsApi.class);

    public NetworkSettingsDto getNetworkSettings() {

        NetworkSettingsDto networkSettingsDto = new NetworkSettingsDto();

        if (ServerUtil.isWindows()) {
            return networkSettingsDto;
        }
        networkSettingsDto.setDhcp(true);
        String hostIpAddress = null;
        try {
            hostIpAddress = NetworkUtil.getHostIpAddress();
            networkSettingsDto.setHostIpAddress(hostIpAddress);
            networkSettingsDto.setHostSubnetMask(getIPv4LocalNetMask());
            networkSettingsDto.setHostDefaultGateway(getDefaultGateway());
            String[] dns = getDNSSettings();
            networkSettingsDto.setHostDnsServer1(dns[0]);
            networkSettingsDto.setHostDnsServer2(dns[1]);

        } catch (SocketException | UnknownHostException e) {
            log.error("Failed to get host and/or ip address", e);
        } catch (IOException e) {
            log.error("Failed to get DNS Server(s)", e);
		}
        return networkSettingsDto;
    }

    private String[] getDNSSettings() throws IOException {
        String[] dns = { "", "", "" };
        int index = 0;

        try (FileReader fileReader = new FileReader(this.NETWORK_RESOLV);
             Scanner in = new Scanner(fileReader)) {
            while (in.hasNextLine()) {
                String[] tokens = in.nextLine().split(" ");
                String s = tokens[0];
                if (s.startsWith("name") && tokens.length > 1) {
                    dns[index++] = tokens[1];
                    if (index > 2) {
                        break;
                    }
                }
            }
        }

    return dns;
    }

    String getIPv4LocalNetMask() {
        String netMask="";

        try {
            InetAddress localHost = Inet4Address.getLocalHost();
            NetworkInterface networkInterface = NetworkInterface.getByInetAddress(localHost);
            int netPrefix = networkInterface.getInterfaceAddresses().get(0).getNetworkPrefixLength();
            int shift = (1 << 31);
            for (int i = netPrefix - 1; i > 0; i--) {
                shift = (shift >> 1);
            }
            netMask = Integer.toString((shift >> 24) & 255) + "." + Integer.toString((shift >> 16) & 255)
                    + "." + Integer.toString((shift >> 8) & 255) + "." + Integer.toString(shift & 255);
        } catch (Exception e) {
            log.error("Exception occured during netmask fetch "+e);
        }

    return netMask;
    }

    String getDefaultGateway() {
        String defaultGateway = null;
        Process networkSettingsProcess = null;
        BufferedReader netwokInput = null;

        try {
            ProcessBuilder networkSettingsbuilder = new ProcessBuilder("/bin/sh", NETWORK_SETTINGS_SCRIPT);
            networkSettingsProcess = networkSettingsbuilder.start();
            netwokInput = new BufferedReader(new InputStreamReader(networkSettingsProcess.getInputStream()));
            String line;
            while ((line = netwokInput.readLine()) != null) {
                defaultGateway = line;
            }
            int statusCode = networkSettingsProcess.waitFor();
            if (statusCode != 0) {
                log.error("Problem encountered during " + NETWORK_SETTINGS_SCRIPT + " execution");
            }
        } catch (Exception e) {
            log.error("Exception occured during " + NETWORK_SETTINGS_SCRIPT + " execution");
            if (networkSettingsProcess != null) {
                networkSettingsProcess.destroy();
            }
        }

    return defaultGateway;
    }
}
