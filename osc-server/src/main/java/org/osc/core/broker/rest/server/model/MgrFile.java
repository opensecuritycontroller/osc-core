package org.osc.core.broker.rest.server.model;

import java.util.Set;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import io.swagger.annotations.ApiModelProperty;

@XmlRootElement(name = "mgrFile")
@XmlAccessorType(XmlAccessType.FIELD)
public class MgrFile {

    @ApiModelProperty(required=true, value="A stream of bytes represent the content of the file.")
    private byte[] mgrFile = null;

    @ApiModelProperty(required=true, value="The filename will be used when file is persisted.")
    private String mgrFileName = null;

    @ApiModelProperty(value="list of dai IDs, null or empty list will indicate ALL option")
    @XmlElement(name = "applianceInstances")
    private Set<String> applianceInstances = null;

    public Set<String> getApplianceInstances() {
        return this.applianceInstances;
    }

    public void setApplianceInstances(Set<String> applianceInstances) {
        this.applianceInstances = applianceInstances;
    }

    public byte[] getMgrFile() {
        return this.mgrFile;
    }

    public void setMgrfile(byte[] mgrFile) {
        this.mgrFile = mgrFile;
    }

    public String getMgrFileName() {
        return this.mgrFileName;
    }

    public void setMgrFileName(String mgrFileName) {
        this.mgrFileName = mgrFileName;
    }

}
