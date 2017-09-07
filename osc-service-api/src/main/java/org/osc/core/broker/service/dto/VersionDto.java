package org.osc.core.broker.service.dto;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import io.swagger.annotations.ApiModelProperty;

@XmlRootElement(name = "version")
@XmlAccessorType(XmlAccessType.FIELD)
public class VersionDto extends BaseDto {

    private Long major;
    private Long minor;
    private Long patch;
    private String build;

    @ApiModelProperty(value = "The optional version description string."
                            + "If set to Version.DEBUG property, will override any other.")
    private String versionStr;

    public VersionDto() {
    }

    public VersionDto(Long major, Long minor, Long patch, String build) {
        this.major = major;
        this.minor = minor;
        this.patch = patch;
        this.build = build;
    }

    public Long getMajor() {
        return this.major;
    }
    public void setMajor(Long major) {
        this.major = major;
    }
    public Long getMinor() {
        return this.minor;
    }
    public void setMinor(Long minor) {
        this.minor = minor;
    }
    public Long getPatch() {
        return this.patch;
    }
    public void setPatch(Long patch) {
        this.patch = patch;
    }
    public String getBuild() {
        return this.build;
    }
    public void setBuild(String build) {
        this.build = build;
    }
    public String getVersionStr() {
        return this.versionStr;
    }
    public void setVersionStr(String versionStr) {
        this.versionStr = versionStr;
    }

    @Override
    public String toString() {
        return "VersionDto [major=" + this.major + ", minor=" + this.minor + ", patch=" + this.patch
                + ", build=" + this.build
                + ", versionStr=" + this.versionStr
                + "]";
    }
}
