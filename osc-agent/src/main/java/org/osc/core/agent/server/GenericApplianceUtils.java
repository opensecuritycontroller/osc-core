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
package org.osc.core.agent.server;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.osc.core.util.PKIUtil;

public class GenericApplianceUtils implements ApplianceUtils {

    private Logger log = Logger.getLogger(GenericApplianceUtils.class);

    @Override
    public String getPidFilePath() {
        return "/var/run/isc-cpa.pid";
    }

    @Override
    public String getName() {
        return AgentUtils.executeScriptWithOutput("get-name.py");
    }

    @Override
    public void mountCdRom() {
        AgentUtils.executeScript("mount-cdrom.py");
    }

    @Override
    public String getCDRomMountPoint() {
        return AgentUtils.executeScriptWithOutput("get-cdrom-mount-path.py");
    }

    @Override
    public void enableFirewallVmiDCPort() {
        // enable-firewall-isc-port.py script is optional.
        if (!new File("enable-firewall-isc-port.py").exists()) {
            return;
        }

        AgentUtils.executeScript("enable-firewall-isc-port.py");
    }

    @Override
    public void persistFile(String filename) {
        // persist-file.py script is optional.
        if (!new File("persist-file.py").exists()) {
            return;
        }

        AgentUtils.executeScript("persist-file.py", "'" + filename + "'");
    }

    @Override
    public int setApplianceNetworkInfo(String ipAddress, String netmask, String gateway, int applianceMtu,
            String iscIpAddress) {
        return AgentUtils.executeScript("set-network-info.py", ipAddress + " " + netmask + " " + gateway + " "
                + applianceMtu + " " + iscIpAddress);
    }

    @Override
    public int authenticateAppliance(String applianceName, String managerIp, String sharedSecretKey,
            String extraConfig, boolean reauthenticate) {
        return AgentUtils.executeScript("set-mgmt-info.py", applianceName + " " + managerIp + " '" + sharedSecretKey
                + "' '" + extraConfig + "' " + (reauthenticate ? "1" : "0"));
    }

    @Override
    public boolean isAuthenticationNeeded() {
        int rc = AgentUtils.executeScript("check-authenticated.py");
        return rc != 0;
    }

    @Override
    public boolean isDiscovered() {
        int rc = AgentUtils.executeScript("check-discovered.py");
        return rc == 0;
    }

    @Override
    public boolean isInspectionReady() {
        int rc = AgentUtils.executeScript("check-inspection-ready.py");
        return rc == 0;
    }

    @Override
    public int updateApplianceConsolePassword(String oldPassword, String newPassword) {
        return AgentUtils.executeScript("set-cli-password.py", oldPassword + " " + newPassword);
    }

    @Override
    public boolean persistConfig(byte[] applianceConfig1, byte[] applianceConfig2, boolean forced) {
        boolean isInitialConfigChanged = false;
        try {

            if (applianceConfig1 != null) {
                isInitialConfigChanged = AgentUtils.compareAndPersistBytesToFile("applianceConfig1.cfg",
                        applianceConfig1, forced);
            }
            if (applianceConfig2 != null) {
                isInitialConfigChanged = AgentUtils.compareAndPersistBytesToFile("applianceConfig2.cfg",
                        applianceConfig2, forced);
            }

        } catch (Exception ex) {

            this.log.error("Failed to persist appliance config file", ex);
        }

        return isInitialConfigChanged;
    }

    private void generateProcessingMarkerFile(File markerFile) {
        FileOutputStream fos = null;

        try {
            fos = new FileOutputStream(markerFile);
            fos.write(new String("marker").getBytes());
            fos.flush();
        } catch (Exception ex) {
            this.log.error("failed to generate marker file", ex);
        } finally {
            IOUtils.closeQuietly(fos);
        }
    }

    @Override
    public boolean processMgrFile(InputStream is, String fileName) {
        File markerFile = new File("marker_" + fileName);
        if (markerFile.exists()) {
            return false;
        }

        generateProcessingMarkerFile(markerFile);
        PKIUtil.writeInputStreamToFile(is, ".", fileName);

        int rc = AgentUtils.executeScript("process-mgr-file.py", "./" + fileName);
        return rc == 0;
    }

    @Override
    public void generateDhcpIpInfo() {
        AgentUtils.executeScript("get-dhcp-ip-info.sh");
    }

    @Override
    public boolean isCpaWatchDogEnabled() {
        return false;
    }

    @Override
    public int cpaInitOnce() {
        if (new File("scripts/cpa-init.sh").exists()) {
            return AgentUtils.executeScript("cpa-init.sh");
        } else {
            return 0;
        }
    }

}
