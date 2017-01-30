package org.osc.core.agent.server;

import java.io.InputStream;

public interface ApplianceUtils {

     void persistFile(String filename);

     boolean processMgrFile(InputStream is, String filename);

     void mountCdRom();
     String getCDRomMountPoint();

     void enableFirewallVmiDCPort();

     boolean isAuthenticationNeeded();
     boolean isDiscovered();
     boolean isInspectionReady();

     int setApplianceNetworkInfo(String ipAddress, String netmask, String gateway, int applianceMtu, String iscIpAddress);

     int authenticateAppliance(String applianceName, String managerIp, String sharedSecretKey,
            String extraConfig, boolean reauthenticate);

     int updateApplianceConsolePassword(String oldPassword, String newPassword);

     String getPidFilePath();
     String getName();

    /**
     * Gets the DHCP ip information and persists the information like ip address,netmask, gateway and MTU information in
     * an ipconfig.conf file in property file format like
     * management.ip0=
     * management.netmask0=
     * management.mtu=
     * management.gateway=
     *
     * The ipconfig.conf file needs to be present in the same location as the jar file of the CPA
     */
     void generateDhcpIpInfo();

     boolean persistConfig(byte[] applianceConfig1, byte[] applianceConfig2, boolean forced);

     boolean isCpaWatchDogEnabled();

     int cpaInitOnce();
}
