package org.osc.core.rest.client.agent.model.output;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import org.osc.core.rest.client.annotations.VmidcLogHidden;

import io.swagger.annotations.ApiModelProperty;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class AgentRegisterResponse {

    @ApiModelProperty(value= "The appliance name")
    private String applianceName;

    @ApiModelProperty(value= "The ip of the manager")
    private String nsmIp;

    @VmidcLogHidden
    private String sharedSecretKey;
    @VmidcLogHidden
    private byte[] nsmPubKey = null;
    @VmidcLogHidden
    private byte[] vmidcApplianceKeyStore = null;

    public AgentRegisterResponse(String applianceName, String nsmIp, String sharedSecretKey, byte[] nsmPubKey,
            byte[] vmidcApplianceKeyStore) {

        super();

        this.applianceName = applianceName;
        this.nsmIp = nsmIp;
        this.sharedSecretKey = sharedSecretKey;
        this.nsmPubKey = nsmPubKey;
        this.vmidcApplianceKeyStore = vmidcApplianceKeyStore;
    }

    public AgentRegisterResponse() {
    }

    public byte[] getApplianceConfig2() {
        return this.nsmPubKey;
    }

    public void setApplianceConfig2(byte[] applianceConfig) {
        this.nsmPubKey = applianceConfig;
    }

    public byte[] getApplianceConfig1() {
        return this.vmidcApplianceKeyStore;
    }

    public void setApplianceConfig1(byte[] applianceConfig) {
        this.vmidcApplianceKeyStore = applianceConfig;
    }

    public String getApplianceName() {
        return this.applianceName;
    }

    public void setApplianceName(String applianceName) {
        this.applianceName = applianceName;
    }

    public String getMgrIp() {
        return this.nsmIp;
    }

    public void setMgrIp(String mgrIp) {
        this.nsmIp = mgrIp;
    }

    public String getSharedSecretKey() {
        return this.sharedSecretKey;
    }

    public void setSharedSecretKey(String sharedSecretKey) {
        this.sharedSecretKey = sharedSecretKey;
    }

    @Override
    public String toString() {
        return "AgentRegisterResponse [applianceName=" + this.applianceName + ", nsmIp=" + this.nsmIp + ", sharedSecretKey= ***hidden*** ]";
    }

}
