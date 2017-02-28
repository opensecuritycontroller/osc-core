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

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.osc.core.util.PKIUtil;
import org.osc.core.util.ServerUtil;

public class NspApplianceUtils implements ApplianceUtils {

    private Logger log = Logger.getLogger(NspApplianceUtils.class);

    public static final String CD_ROM_MOUNT_POINT = "/mnt/media3";

    @Override
    public void mountCdRom() {
        if (VirtualizationUtils.getVirtualizationType() == null) {
            this.log.info("Mounting config cdrom");
            File mountDirectory = new File(CD_ROM_MOUNT_POINT);
            mountDirectory.mkdirs();
            ServerUtil.execWithLog("mount /dev/sr0 " + CD_ROM_MOUNT_POINT);
        }
    }

    @Override
    public void persistFile(String filename) {
        ServerUtil.execWithLog("/usr/local/bin/updateconfig.sh " + filename);
    }

    @Override
    public boolean processMgrFile(InputStream is, String fileName) {

        File markerFile = new File("/tftpboot/marker_brokertosensorfile");
        if (markerFile.exists()) {
            return false;
        }

        generateMarkerFile(markerFile);
        PKIUtil.writeInputStreamToFile(is, "/tftpboot", fileName);

        executePythonScript("notify-mgr-file.py", "/tftpboot/" + fileName);
        return true;
    }

    private void generateMarkerFile(File markerFile) {
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
    public void enableFirewallVmiDCPort() {
        // Check if already added to iptables.
        List<String> lines = new ArrayList<String>();
        ServerUtil.execWithLines("/usr/local/sbin/iptables --list", lines);
        for (String line : lines) {
            if (line.contains(Server.port.toString())) {
                this.log.info("Firewall port is already enabled.");
                return;
            }
        }

        this.log.info("Enable firewall port.");
        ServerUtil
                .execWithLog("/usr/local/sbin/iptables -I INPUT -i eth0 -p tcp --dport " + Server.port + " -j ACCEPT");
    }

    @Override
    public boolean isAuthenticationNeeded() {
        Map<String, String> keys = getApplianceStatusKeys();

        String trust = keys.get("Trust Established");
        this.log.info("Trust Established: " + trust);
        return trust != null && trust.contains("no");
    }

    @Override
    public boolean isDiscovered() {
        Map<String, String> keys = getApplianceStatusKeys();

        String trust = keys.get("Trust Established");
        this.log.info("Trust Established: " + trust);

        String alertChannel = keys.get("Alert Channel");
        this.log.info("Alert Channel: " + alertChannel);

        String logChannel = keys.get("Log Channel");
        this.log.info("Log Channel: " + logChannel);

        return trust != null && trust.contains("yes") && alertChannel != null && alertChannel.contains("up")
                && logChannel != null && logChannel.contains("up");
    }

    @Override
    public boolean isInspectionReady() {
        Map<String, String> keys = getApplianceStatusKeys();

        String sigfile = keys.get("Present");
        this.log.info("Present: " + sigfile);

        String systemInitialized = keys.get("System Initialized");
        this.log.info("System Initialized: " + systemInitialized);

        String systemHealth = keys.get("System Health Status");
        this.log.info("System Health Status: " + systemHealth);

        return sigfile != null && sigfile.contains("yes") && systemInitialized != null
                && systemInitialized.contains("yes") && systemHealth != null && systemHealth.contains("good");
    }

    private Map<String, String> getKeys(List<String> statusLines) {
        Map<String, String> keys = new HashMap<String, String>();
        for (String line : statusLines) {
            String[] pair = line.split(":");
            if (pair.length >= 2) {
                keys.put(pair[0].trim(), pair[1].trim());
            }
        }
        return keys;
    }

    private Map<String, String> getApplianceStatusKeys() {
        List<String> statusLines = new ArrayList<String>();
        ServerUtil.execWithLines("/usr/bin/python scripts/get-vnsp-status.py", statusLines);
        return getKeys(statusLines);
    }

    private int executePythonScript(String script, String arguments) {
        this.log.info("Executing python script: 'python scripts/" + script + " " + arguments);
        return ServerUtil.execWithLog("/usr/bin/python scripts/" + script + " " + arguments);
    }

    @Override
    public int setApplianceNetworkInfo(String ipAddress, String netmask, String gateway, int applianceMtu,
            String iscIpAddress) {
        return executePythonScript("set-vnsp-network-info.py", ipAddress + " " + netmask + " " + gateway + " "
                + applianceMtu + " " + iscIpAddress);
    }

    @Override
    public int authenticateAppliance(String applianceName, String managerIp, String sharedSecretKey,
            String extraConfig, boolean reauthenticate) {

        return executePythonScript("set-vnsp-mgmt-info.py", applianceName + " " + managerIp + " dummy1234 "
                + (reauthenticate ? "1" : "0"));
    }

    @Override
    public int updateApplianceConsolePassword(String oldPassword, String newPassword) {
        if (StringUtils.isBlank(oldPassword)) {
            oldPassword = "admin123";
        }
        return executePythonScript("set-vnsp-password.py", oldPassword + " " + newPassword);
    }

    @Override
    public String getPidFilePath() {
        return "/var/run/cpa.pid";
    }

    @Override
    public String getName() {
        return "vnsp";
    }

    @Override
    public boolean persistConfig(byte[] vmidcApplianceKeyStore, byte[] nsmPubKey, boolean forced) {

        boolean isSensorCertChanged = false;
        boolean isSensorKeyChanged = false;
        boolean isEmsCertChanged = false;
        try {

            if (vmidcApplianceKeyStore != null) {
                isSensorCertChanged = AgentUtils.compareAndPersistBytesToFile("sensorcert",
                        PKIUtil.extractCertificate(vmidcApplianceKeyStore), forced);
                isSensorKeyChanged = AgentUtils.compareAndPersistBytesToFile("sensorkey",
                        PKIUtil.extractPrivateKey(vmidcApplianceKeyStore), forced);
            }

            if (nsmPubKey != null) {
                isEmsCertChanged = AgentUtils.compareAndPersistBytesToFile("emscert", nsmPubKey, forced);
            }

        } catch (Exception ex) {

            this.log.error("Failed to persist to key/cert files", ex);
        }

        return isSensorCertChanged || isSensorKeyChanged || isEmsCertChanged;
    }

    @Override
    public String getCDRomMountPoint() {
        return CD_ROM_MOUNT_POINT;
    }

    @Override
    public void generateDhcpIpInfo() {
        ServerUtil.execWithLog("scripts/get-dhcp-ip-info.sh");
    }

    @Override
    public boolean isCpaWatchDogEnabled() {
        return false;
    }

    @Override
    public int cpaInitOnce() {
        if (new File("scripts/cpa-init.sh").exists()) {
            return ServerUtil.execWithLog("scripts/cpa-init.sh");
        } else {
            return 0;
        }
    }

}
