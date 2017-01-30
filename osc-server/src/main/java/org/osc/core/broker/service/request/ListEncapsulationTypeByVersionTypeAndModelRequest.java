package org.osc.core.broker.service.request;

import org.osc.core.broker.model.virtualization.VirtualizationType;

public class ListEncapsulationTypeByVersionTypeAndModelRequest implements Request {

    private String appliacneSoftwareVersion;
    private String appliacneModel;
    private VirtualizationType vcType;

    public String getAppliacneSoftwareVersion() {
        return this.appliacneSoftwareVersion;
    }

    public ListEncapsulationTypeByVersionTypeAndModelRequest(String appliacneSoftwareVersion, String appliacneModel,
            VirtualizationType vcType) {
        super();
        this.appliacneSoftwareVersion = appliacneSoftwareVersion;
        this.appliacneModel = appliacneModel;
        this.vcType = vcType;
    }

    public void setAppliacneSoftwareVersion(String appliacneSoftwareVersion) {
        this.appliacneSoftwareVersion = appliacneSoftwareVersion;
    }

    public String getAppliacneModel() {
        return this.appliacneModel;
    }

    public void setAppliacneModel(String appliacneModel) {
        this.appliacneModel = appliacneModel;
    }

    public VirtualizationType getVcType() {
        return this.vcType;
    }

    public void setVcType(VirtualizationType vcType) {
        this.vcType = vcType;
    }

}
