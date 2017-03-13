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
package org.osc.core.agent.server;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.osc.core.broker.model.virtualization.VirtualizationEnvironmentProperties;
import org.osc.core.broker.model.virtualization.VirtualizationType;
import org.osc.core.util.FileUtil;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Properties;

public class VirtualizationUtils {

    private static final String OS_DHCP_IP_CONFIG_FILE = "ipconfig.conf";

    private static Logger log = Logger.getLogger(VirtualizationUtils.class);

    public static final String OPEN_STACK_ENV_FILE = "openstack";
    public static final String OVF_ENV_XML_FILE = "ovf-env.xml";
    private static final String OPENSTACK_CONTENT_FILE = "openstack/content/0000";

    /**
     * Dynamically figures out the virtualization environment the agent is running on based on the environment files
     * present on the appliance. This method assumes the files have been made accessible already(by mounting the CDROM
     * or
     * by any other way)
     *
     * @return the virtualization type or null if the virtualization type cannot be determined.
     */
    public static VirtualizationType getVirtualizationType() {
        File ovfenv = new File(Server.applianceUtils.getCDRomMountPoint() + File.separator + OVF_ENV_XML_FILE);
        File osEnv = new File(Server.applianceUtils.getCDRomMountPoint() + File.separator + OPEN_STACK_ENV_FILE);
        if (ovfenv.exists()) {
            return VirtualizationType.VMWARE;
        } else if (osEnv.exists()) {
            return VirtualizationType.OPENSTACK;
        } else {
            return null;
        }
    }

    /**
     * Gets the agent environment information. Assumes the information is already available in the environment files.
     *
     * @return the agent environment information
     * @throws ParserConfigurationException
     *             in case vmware information cannot be parsed
     * @throws SAXException
     *             in case vmware information cannot be parsed
     * @throws FileNotFoundException
     *             if the agent files dont exist
     * @throws IOException
     *             if we cannot read the agent information in the files
     * @throws Exception
     *             if we cannot determine the agent type information
     */
    public static AgentEnv getAgentEnvironment() throws ParserConfigurationException, SAXException,
            FileNotFoundException, IOException, Exception {
        VirtualizationType virtualizationType = getVirtualizationType();

        if (virtualizationType.isVmware()) {
            return parseAgentEnvironmentVmware();
        } else if (virtualizationType.isOpenstack()) {
            return parseAgentEnvironmentOpenstack();
        }
        throw new Exception("No Agent Environment information found.");
    };

    private static AgentEnv parseAgentEnvironmentVmware() throws ParserConfigurationException, SAXException,
            IOException {

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document document = db.parse(new File(Server.applianceUtils.getCDRomMountPoint() + File.separator
                + OVF_ENV_XML_FILE));

        AgentEnv env = new AgentEnv();
        // In case of VMware NSX, appliance name should be set to what we already have.
        env.setApplianceName(Server.applianceName);

        NodeList nodeList = document.getElementsByTagName("Property");
        for (int i = 0, size = nodeList.getLength(); i < size; i++) {
            String key = nodeList.item(i).getAttributes().getNamedItem("oe:key").getTextContent();
            String value = nodeList.item(i).getAttributes().getNamedItem("oe:value").getTextContent();

            if (value == null || value.isEmpty()) {
                continue;
            }

            if (key.equalsIgnoreCase(VirtualizationEnvironmentProperties.VMIDC_IP)) {
                env.setVmidcIp(value);
            } else if (key.equalsIgnoreCase(VirtualizationEnvironmentProperties.VMIDC_USER)) {
                env.setVmidcUser(value);
            } else if (key.equalsIgnoreCase(VirtualizationEnvironmentProperties.VMIDC_PASSWORD)) {
                env.setVmidcPassword(value);
            } else if (key.equalsIgnoreCase(VirtualizationEnvironmentProperties.VIRTUAL_SYSTEM_ID)) {
                env.setVsId(Long.parseLong(value));
            } else if (key.equalsIgnoreCase(VirtualizationEnvironmentProperties.MANAGEMENT_GATEWAY)) {
                env.setApplianceGateway(value);
            } else if (key.equalsIgnoreCase(VirtualizationEnvironmentProperties.MANAGEMENT_IP)) {
                env.setApplianceIp(value);
            } else if (key.equalsIgnoreCase(VirtualizationEnvironmentProperties.MANAGEMENT_NETMASK)) {
                env.setApplianceNetmask(value);
            } else if (key.equalsIgnoreCase(VirtualizationEnvironmentProperties.APPLIANCE_MODEL)) {
                env.setApplianceModel(value);
            } else if (key.equalsIgnoreCase(VirtualizationEnvironmentProperties.APPLIANCE_SOFTWARE_VERSION)) {
                env.setApplianceSoftwareVersion(value);
            } else if (key.equalsIgnoreCase(VirtualizationEnvironmentProperties.NSX_AGENT_NAME)) {
                env.setNsxAgentName(value);
            }

        }

        log.info("NSX Env Info= " + env.toString());

        return env;
    }

    private static AgentEnv parseAgentEnvironmentOpenstack() throws FileNotFoundException, IOException {
        File configFile = new File(Server.applianceUtils.getCDRomMountPoint() + File.separator + OPENSTACK_CONTENT_FILE);
        AgentEnv env = new AgentEnv();
        Properties prop = new Properties();

        try(FileInputStream fileInputStream = new FileInputStream(configFile)) {
            String decodedFile = new String(Base64.getDecoder().decode(IOUtils.toString(fileInputStream)));
            try (InputStream configFileStream = new ByteArrayInputStream(decodedFile.getBytes(StandardCharsets.UTF_8))) {
                prop.load(configFileStream);
            }
        }

        env.setVmidcIp(prop.getProperty(VirtualizationEnvironmentProperties.VMIDC_IP));
        env.setVmidcUser(prop.getProperty(VirtualizationEnvironmentProperties.VMIDC_USER));
        env.setVmidcPassword(prop.getProperty(VirtualizationEnvironmentProperties.VMIDC_PASSWORD));
        env.setVsId(Long.parseLong(prop.getProperty(VirtualizationEnvironmentProperties.VIRTUAL_SYSTEM_ID)));
        env.setApplianceName(prop.getProperty(VirtualizationEnvironmentProperties.APPLIANCE_NAME));

        File ipAddrFile = new File(OS_DHCP_IP_CONFIG_FILE);
        if (!ipAddrFile.exists()) {
            Server.applianceUtils.generateDhcpIpInfo();
            if(!ipAddrFile.exists()) {
                throw new IllegalStateException("Cannot determine IP information from DHCP");
            }
        }

        Properties ipAddrProperties = FileUtil.loadProperties(OS_DHCP_IP_CONFIG_FILE);
        env.setApplianceIp(ipAddrProperties.getProperty(VirtualizationEnvironmentProperties.MANAGEMENT_IP));
        env.setApplianceGateway(ipAddrProperties.getProperty(VirtualizationEnvironmentProperties.MANAGEMENT_GATEWAY));
        env.setApplianceNetmask(ipAddrProperties.getProperty(VirtualizationEnvironmentProperties.MANAGEMENT_NETMASK));
        String mtuValue = ipAddrProperties.getProperty(VirtualizationEnvironmentProperties.MANAGEMENT_MTU);
        if (StringUtils.isBlank(mtuValue)) {
            throw new IllegalStateException("MTU value not provided");
        }
        env.setApplianceMtu(Integer.parseInt(mtuValue));

        log.info("Env Info= " + env.toString());

        return env;
    }
}
