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
package org.osc.core.broker.util;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;

import org.apache.log4j.Logger;

public class NetworkUtil {
    private static final Logger log = Logger.getLogger(NetworkUtil.class);

    static final List<String> validNetMasks = Arrays.asList("255.255.255.255", "255.255.255.254", "255.255.255.252",
            "255.255.255.248", "255.255.255.240", "255.255.255.224", "255.255.255.192", "255.255.255.128",
            "255.255.255.0", "255.255.254.0", "255.255.252.0", "255.255.248.0", "255.255.240.0", "255.255.224.0",
            "255.255.192.0", "255.255.128.0", "255.255.0.0", "255.254.0.0", "255.252.0.0", "255.248.0.0", "255.240.0.0",
            "255.224.0.0", "255.192.0.0", "255.128.0.0", "255.0.0.0", "254.0.0.0", "252.0.0.0", "248.0.0.0",
            "240.0.0.0", "224.0.0.0", "192.0.0.0", "128.0.0.0", "0.0.0.0");

    public static String getHostIpAddress() throws SocketException, UnknownHostException {
        try {
            String hostName = InetAddress.getLocalHost().getHostName();

            InetAddress[] addrs = InetAddress.getAllByName(hostName);

            for (InetAddress addr : addrs) {
                if (log.isDebugEnabled()) {
                    log.debug("addr.getHostAddress() = " + addr.getHostAddress());
                    log.debug("addr.getHostName() = " + addr.getHostName());
                    log.debug("addr.isAnyLocalAddress() = " + addr.isAnyLocalAddress());
                    log.debug("addr.isLinkLocalAddress() = " + addr.isLinkLocalAddress());
                    log.debug("addr.isLoopbackAddress() = " + addr.isLoopbackAddress());
                    log.debug("addr.isMulticastAddress() = " + addr.isMulticastAddress());
                    log.debug("addr.isSiteLocalAddress() = " + addr.isSiteLocalAddress());
                }

                if (!addr.isLoopbackAddress() && addr.isSiteLocalAddress()) {
                    return addr.getHostAddress();
                }
            }
        } catch (Exception e) {
            log.debug("Fail to retrieve IP based on FQDN (" + e.getMessage() + ")");
        }

        /*
         * we know which network interface on MLOS server is used for external
         * communication
         */
        Enumeration<NetworkInterface> eni = NetworkInterface.getNetworkInterfaces();
        while (eni.hasMoreElements()) {
            NetworkInterface ni = eni.nextElement();
            Enumeration<InetAddress> eia = ni.getInetAddresses();
            while (eia.hasMoreElements()) {
                InetAddress addr = eia.nextElement();
                if (log.isDebugEnabled()) {
                    log.debug("===== Network Inteface Elements =====");
                    log.debug("addr.getHostAddress() = " + addr.getHostAddress());
                    log.debug("addr.getHostName() = " + addr.getHostName());
                    log.debug("addr.isAnyLocalAddress() = " + addr.isAnyLocalAddress());
                    log.debug("addr.isLinkLocalAddress() = " + addr.isLinkLocalAddress());
                    log.debug("addr.isLoopbackAddress() = " + addr.isLoopbackAddress());
                    log.debug("addr.isMulticastAddress() = " + addr.isMulticastAddress());
                    log.debug("addr.isSiteLocalAddress() = " + addr.isSiteLocalAddress());
                }
                if (!addr.isLoopbackAddress() && addr.isSiteLocalAddress()) {
                    return addr.getHostAddress();
                }
            }
        }
        return null;
    }

    /**
     * Given a valid netmask returns the prefix length.
     *
     * @param netmask
     * @throws IllegalArgumentException
     *             in case the netmask is not valid
     * @return
     */
    public static int getPrefixLength(String netmask) {
        if (!validNetMasks.contains(netmask)) {
            throw new IllegalArgumentException(netmask + " is not a valid netmask.");
        }
        String[] octets = netmask.split("\\.");
        int octet1 = Integer.parseInt(octets[0]);
        int octet2 = Integer.parseInt(octets[1]);
        int octet3 = Integer.parseInt(octets[2]);
        int octet4 = Integer.parseInt(octets[3]);

        int mask = Integer.bitCount(octet1) + Integer.bitCount(octet2) + Integer.bitCount(octet3)
                + Integer.bitCount(octet4);

        return mask;
    }

    public static String resolveIpToName(String ipAddress) {
        try {
            return InetAddress.getByName(ipAddress).getHostName();
        } catch (UnknownHostException e) {
            log.error("Unable to resolve " + ipAddress + " to a host name", e);
        }
        return null;
    }

}
