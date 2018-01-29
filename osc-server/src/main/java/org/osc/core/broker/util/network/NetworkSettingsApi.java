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
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import org.osc.core.broker.service.dto.NetworkSettingsDto;
import org.osc.core.broker.util.NetworkUtil;
import org.osc.core.broker.util.ServerUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



public class NetworkSettingsApi {

    private final String NETWORK_RESOLV = "/etc/resolv.conf";
    private static final Logger log = LoggerFactory.getLogger(NetworkSettingsApi.class);

    public NetworkSettingsDto getNetworkSettings() {

        NetworkSettingsDto networkSettingsDto = new NetworkSettingsDto();

        if (ServerUtil.isWindows()) {
            return networkSettingsDto;
        }

        networkSettingsDto.setDhcp(true);
        String hostIpAddress = null;
        String dnsSvr1 = "";
        String dnsSvr2 = "";

        try {
            hostIpAddress = NetworkUtil.getHostIpAddress();
            networkSettingsDto.setHostIpAddress(hostIpAddress);
            networkSettingsDto.setHostSubnetMask(getIPv4LocalNetMask());
            networkSettingsDto.setHostDefaultGateway(getDefaultGateway());

            List<String> dns = getDNSSettings();
            for (int index = 0; index < dns.size(); index++) {
                if (index == 0) {
                    dnsSvr1 = dns.get(index);
                }
                if (index == 1) {
                    dnsSvr2 = dns.get(index);
                    break;
                }
            }
            networkSettingsDto.setHostDnsServer1(dnsSvr1);
            networkSettingsDto.setHostDnsServer2(dnsSvr2);

        } catch (SocketException | UnknownHostException e) {
            log.error("Failed to get host and/or ip address", e);
        } catch (IOException e) {
            log.error("Failed to get DNS Server(s)", e);
        }

        return networkSettingsDto;
    }

    private List<String> getDNSSettings() throws IOException {

        List<String> dns = new ArrayList<>();
        int index = 0;

        try (FileReader fileReader = new FileReader(this.NETWORK_RESOLV);
            Scanner in = new Scanner(fileReader)) {
            while (in.hasNextLine()) {
                String[] tokens = in.nextLine().split(" ");
                String s = tokens[0];
                if (s.startsWith("name") && tokens.length > 1) {
                    dns.add(tokens[1]);
                    index++;
                    if (index > 1) {
                        break;
                    }
                }
            }
        }

        return dns;
    }

    String getIPv4LocalNetMask() {

        String netMask = "";
        String[] cmd = { "/bin/sh", "-c", "ip addr show eth0 |grep inet|tr -s ' ' |cut -d' ' -f3" };
        List<String> outlines = new ArrayList<>();

        int exitCode = ServerUtil.execWithLog(cmd, outlines);
        if (exitCode != 0) {
            log.error("Encountered error during: {} execution", cmd);
            return null;
        }

        for(String line:outlines) {
            netMask = line;
            break;
        }

        return netMask;
    }

    String getDefaultGateway() {

        String defaultGateway = "";
        String[] cmd = { "/bin/sh", "-c", "ip route|grep default|tr -s ' ' |cut -d' ' -f3" };
        List<String> outlines = new ArrayList<>();

        int exitCode = ServerUtil.execWithLog(cmd, outlines);
        if (exitCode != 0) {
            log.error("Encountered error during: {} execution", cmd);
            return null;
        }

        for (String line:outlines) {
            defaultGateway = line;
            break;
        }

        return defaultGateway;
    }
}
