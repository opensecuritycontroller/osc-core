package org.osc.core.broker.model.plugin.sdncontroller;

import org.osc.core.broker.model.entities.appliance.VirtualSystem;
import org.osc.sdk.sdn.api.AgentApi;
import org.osc.sdk.sdn.api.DeploymentSpecApi;
import org.osc.sdk.sdn.api.SecurityTagApi;
import org.osc.sdk.sdn.api.ServiceApi;
import org.osc.sdk.sdn.api.ServiceInstanceApi;
import org.osc.sdk.sdn.api.ServiceManagerApi;
import org.osc.sdk.sdn.api.ServiceProfileApi;
import org.osc.sdk.sdn.api.VendorTemplateApi;

public class VMwareSdnApiFactory {
    public static AgentApi createAgentApi(VirtualSystem vs) throws Exception {
        return SdnControllerApiFactory.createVMwareSdnApi(vs.getVirtualizationConnector())
                .createAgentApi(new VMwareSdnConnector(vs.getVirtualizationConnector()));
    }

    public static ServiceProfileApi createServiceProfileApi(VirtualSystem vs) throws Exception {
        return SdnControllerApiFactory.createVMwareSdnApi(vs.getVirtualizationConnector())
                .createServiceProfileApi(new VMwareSdnConnector(vs.getVirtualizationConnector()));
    }

    public static SecurityTagApi createSecurityTagApi(VirtualSystem vs) throws Exception {
        return SdnControllerApiFactory.createVMwareSdnApi(vs.getVirtualizationConnector())
                .createSecurityTagApi(new VMwareSdnConnector(vs.getVirtualizationConnector()));
    }

    public static ServiceApi createServiceApi(VirtualSystem vs) throws Exception {
        return SdnControllerApiFactory.createVMwareSdnApi(vs.getVirtualizationConnector())
                .createServiceApi(new VMwareSdnConnector(vs.getVirtualizationConnector()));
    }

    public static ServiceManagerApi createServiceManagerApi(VirtualSystem vs) throws Exception {
        return SdnControllerApiFactory.createVMwareSdnApi(vs.getVirtualizationConnector())
                .createServiceManagerApi(new VMwareSdnConnector(vs.getVirtualizationConnector()));
    }

    public static ServiceInstanceApi createServiceInstanceApi(VirtualSystem vs) throws Exception {
        return SdnControllerApiFactory.createVMwareSdnApi(vs.getVirtualizationConnector())
                .createServiceInstanceApi(new VMwareSdnConnector(vs.getVirtualizationConnector()));
    }

    public static VendorTemplateApi createVendorTemplateApi(VirtualSystem vs) throws Exception {
        return SdnControllerApiFactory.createVMwareSdnApi(vs.getVirtualizationConnector())
                .createVendorTemplateApi(new VMwareSdnConnector(vs.getVirtualizationConnector()));
    }

    public static DeploymentSpecApi createDeploymentSpecApi(VirtualSystem vs) throws Exception {
        return SdnControllerApiFactory.createVMwareSdnApi(vs.getVirtualizationConnector())
                .createDeploymentSpecApi(new VMwareSdnConnector(vs.getVirtualizationConnector()));
    }
}
