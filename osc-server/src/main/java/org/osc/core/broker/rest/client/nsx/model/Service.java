package org.osc.core.broker.rest.client.nsx.model;

import org.osc.sdk.sdn.element.ServiceElement;

public class Service implements ServiceElement {
    private String id;
    private String vsmId;
    //private final String serviceInstanceId;
    private String name;
    private String version;
    private String managerId;
    private String functionalityType;
    //private final String type;
    private String virtualSystemId;
    private String oscUserName;
    private String oscPassword;
    private String oscIpAddress;
    private String applianceModel;
    private String applianceSoftwareVersion;

    public Service(
            String id,
            String vsmId,
            //String serviceInstanceId,
            String name,
            String version,
            String managerId,
            String functionalityType,
            //String type,
            String virtualSystemId,
            String oscUserName,
            String oscPassword,
            String oscIpAddress,
            String applianceModel,
            String applianceSoftwareVersion) {
        this.id = id;
        this.vsmId = vsmId;
        //this.serviceInstanceId = serviceInstanceId;
        this.name = name;
        this.version = version;
        this.managerId = managerId;
        this.functionalityType = functionalityType;
        //this.type = type;
        this.virtualSystemId = virtualSystemId;
        this.oscUserName = oscUserName;
        this.oscPassword = oscPassword;
        this.oscIpAddress = oscIpAddress;
        this.applianceModel = applianceModel;
        this.applianceSoftwareVersion = applianceSoftwareVersion;
    }

    public Service() {
    }

    @Override
    public String getId() {
        return this.id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Override
    public String getVsmId() {
        return this.vsmId;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public String getVersion() {
        return this.version;
    }

    @Override
    public String getManagerId() {
        return this.managerId;
    }

    @Override
    public String getFunctionalityType() {
        return this.functionalityType;
    }

    @Override
    public String getVirtualSystemId() {
        return this.virtualSystemId;
    }

    @Override
    public String getOscUserName() {
        return this.oscUserName;
    }

    @Override
    public String getOscPassword() {
        return this.oscPassword;
    }

    @Override
    public String getOscIpAddress() {
        return this.oscIpAddress;
    }

    @Override
    public String getApplianceModel() {
        return this.applianceModel;
    }

    @Override
    public String getApplianceSoftwareVersion() {
        return this.applianceSoftwareVersion;
    }
}
