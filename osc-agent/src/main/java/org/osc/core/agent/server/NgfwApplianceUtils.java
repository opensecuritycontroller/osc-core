package org.osc.core.agent.server;

import java.io.File;
import java.io.InputStream;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.osc.core.util.ServerUtil;

public class NgfwApplianceUtils implements ApplianceUtils {

    private Logger log = Logger.getLogger(NgfwApplianceUtils.class);

    public static final String CD_ROM_MOUNT_POINT = "./media3";

    @Override
    public void persistFile(String filename) {
    }

    @Override
    public boolean processMgrFile(InputStream is, String fileName) {
        return true;
    }

    @Override
    public void mountCdRom() {
        if (VirtualizationUtils.getVirtualizationType() == null) {
            this.log.info("Mounting config cdrom");
            File mountDirectory = new File(CD_ROM_MOUNT_POINT);
            mountDirectory.mkdirs();
            ServerUtil.execWithLog("mount /dev/cdroms/cdrom0 " + CD_ROM_MOUNT_POINT);
        }
    }

    @Override
    public void enableFirewallVmiDCPort() {
    }

    @Override
    public boolean isAuthenticationNeeded() {
        int rc = executePythonScript("check-ngfw-smc-status.py");
        return rc != 0 && rc != 32;
    }

    @Override
    public boolean isDiscovered() {
        int rc = executePythonScript("check-ngfw-smc-status.py");
        return rc == 0;
    }

    @Override
    public boolean isInspectionReady() {
        int rc = executePythonScript("check-ngfw-traffic-status.py");
        return rc == 0;
    }

    @Override
    public int setApplianceNetworkInfo(String ipAddress, String netmask, String gateway, int applianceMtu,
            String iscIpAddress) {
        return executePythonScript("set-ngfw-network-info.py", ipAddress + " " + netmask + " " + gateway + " "
                + applianceMtu + " " + iscIpAddress);
    }

    @Override
    public int authenticateAppliance(String applianceName, String managerIp, String sharedSecretKey,
            String extraConfig, boolean reauthenticate) {
        File configFile = new File("applianceConfig.cfg");
        return executePythonScript("set-ngfw-mgmt-info.py",
                applianceName + " " + managerIp + " " + configFile.getAbsolutePath() + " "
                        + (reauthenticate ? "1" : "0"));
    }

    @Override
    public int updateApplianceConsolePassword(String oldPassword, String newPassword) {
        if (StringUtils.isBlank(oldPassword)) {
            oldPassword = "admin123";
        }
        return executePythonScript("set-ngfw-root-password.py", oldPassword + " " + newPassword);
    }

    @Override
    public String getPidFilePath() {
        return "/var/run/cpa.pid";
    }

    private int executePythonScript(String script) {
        this.log.info("Executing python script: 'python scripts/" + script);
        return ServerUtil.execWithLog("python scripts/" + script);
    }

    private int executePythonScript(String script, String arguments) {
        this.log.info("Executing python script: 'python scripts/" + script + " " + arguments);
        return ServerUtil.execWithLog("python scripts/" + script + " " + arguments);
    }

    @Override
    public String getName() {
        return "ngfw";
    }

    @Override
    public boolean persistConfig(byte[] applianceConfig1, byte[] applianceConfig2, boolean forced) {
        boolean isInitialConfigChanged = false;
        try {

            if (applianceConfig1 != null) {
                isInitialConfigChanged = AgentUtils.compareAndPersistBytesToFile("applianceConfig.cfg",
                        applianceConfig1, forced);
            }

        } catch (Exception ex) {

            this.log.error("Failed to persist to initial config file", ex);
        }

        return isInitialConfigChanged;
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
        return true;
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
