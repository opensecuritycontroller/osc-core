package org.osc.core.broker.service.dto;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import org.osc.sdk.controller.TagEncapsulationType;

@XmlRootElement(name = "applianceModelSoftwareVersion")
@XmlAccessorType(XmlAccessType.FIELD)
public class ApplianceModelSoftwareVersionDto extends BaseDto {

    private String name; // combo name: Model + "-" + swVersion

    private Long applianceId;
    private String applianceModel = "";
    private String swVersion = "";
    private String virtualizationSoftwareVersion = "";
    private List<TagEncapsulationType> encapsulationTypes = new ArrayList<TagEncapsulationType>();

    /**
     * @return the name
     */
    public String getName() {
        return this.name;
    }

    /**
     * @param name
     *            the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return the applianceId
     */
    public Long getApplianceId() {
        return this.applianceId;
    }

    /**
     * @param applianceId
     *            the applianceId to set
     */
    public void setApplianceId(Long applianceId) {
        this.applianceId = applianceId;
    }

    /**
     * @return the applianceModel
     */
    public String getApplianceModel() {
        return this.applianceModel;
    }

    /**
     * @param applianceModel
     *            the applianceModel to set
     */
    public void setApplianceModel(String applianceModel) {
        this.applianceModel = applianceModel;
    }

    /**
     * @return the swVersion
     */
    public String getSwVersion() {
        return this.swVersion;
    }

    /**
     * @param swVersion
     *            the swVersion to set
     */
    public void setSwVersion(String swVersion) {
        this.swVersion = swVersion;
    }

    public String getVirtualizationSoftwareVersion() {
        return this.virtualizationSoftwareVersion;
    }

    public void setVirtualizationSoftwareVersion(String virtualizationSoftwareVersion) {
        this.virtualizationSoftwareVersion = virtualizationSoftwareVersion;
    }

    public List<TagEncapsulationType> getEncapsulationTypes() {
        return this.encapsulationTypes;
    }

    public void setEncapsulationTypes(List<TagEncapsulationType> encapsulationTypes) {
        this.encapsulationTypes = encapsulationTypes;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "ApplianceModelSoftwareVersionDto [name=" + this.name + ", applianceId=" + this.applianceId
                + ", applianceModel=" + this.applianceModel + ", swVersion=" + this.swVersion
                + ", virtualizationSoftwareVersion=" + this.virtualizationSoftwareVersion + "]";
    }
}
