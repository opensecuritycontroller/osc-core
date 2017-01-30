package org.osc.core.agent.server;

public class AgentEnv {

    private String nsxAgentName;
    private String applianceName;
    private String applianceModel;
    private String applianceSoftwareVersion;
    private String applianceIp;
    private String applianceNetmask;
    private String applianceGateway;
    private int applianceMtu;
    private String vmidcIp;
    private String vmidcUser;
    private String vmidcPassword;
    private long vsId;

    public String getApplianceName() {
        return this.applianceName;
    }

    public void setApplianceName(String applianceName) {
        this.applianceName = applianceName;
    }

    public String getApplianceIp() {
        return this.applianceIp;
    }

    public void setApplianceIp(String applianceIp) {
        this.applianceIp = applianceIp;
    }

    public String getApplianceNetmask() {
        return this.applianceNetmask;
    }

    public void setApplianceNetmask(String applianceNetmask) {
        this.applianceNetmask = applianceNetmask;
    }

    public String getApplianceGateway() {
        return this.applianceGateway;
    }

    public void setApplianceGateway(String applianceGateway) {
        this.applianceGateway = applianceGateway;
    }

    public String getVmidcIp() {
        return this.vmidcIp;
    }

    public void setVmidcIp(String vmidcIp) {
        this.vmidcIp = vmidcIp;
    }

    public String getVmidcUser() {
        return this.vmidcUser;
    }

    public void setVmidcUser(String vmidcUser) {
        this.vmidcUser = vmidcUser;
    }

    public String getVmidcPassword() {
        return this.vmidcPassword;
    }

    public void setVmidcPassword(String vmidcPassword) {
        this.vmidcPassword = vmidcPassword;
    }

    public long getVsId() {
        return this.vsId;
    }

    public void setVsId(long vsId) {
        this.vsId = vsId;
    }

    public String getNsxAgentName() {
        return this.nsxAgentName;
    }

    public void setNsxAgentName(String nsxAgentName) {
        this.nsxAgentName = nsxAgentName;
    }

    public String getApplianceSoftwareVersion() {
        return this.applianceSoftwareVersion;
    }

    public void setApplianceSoftwareVersion(String applianceSoftwareVersion) {
        this.applianceSoftwareVersion = applianceSoftwareVersion;
    }

    public String getApplianceModel() {
        return this.applianceModel;
    }

    public void setApplianceModel(String applianceModel) {
        this.applianceModel = applianceModel;
    }

    public int getApplianceMtu() {
        return this.applianceMtu;
    }

    public void setApplianceMtu(int applianceMtu) {
        this.applianceMtu = applianceMtu;
    }

    @Override
    public String toString() {
        return "AgentEnv [nsxAgentName=" + this.nsxAgentName + ", applianceName=" + this.applianceName + ", applianceModel="
                + this.applianceModel + ", applianceSoftwareVersion=" + this.applianceSoftwareVersion + ", applianceIp="
                + this.applianceIp + ", applianceNetmask=" + this.applianceNetmask + ", applianceGateway=" + this.applianceGateway
                + ", vmidcIp=" + this.vmidcIp + ", vmidcUser=" + this.vmidcUser + ", vmidcPassword=" + this.vmidcPassword + ", vsId="
                + this.vsId + "]";
    }

}
