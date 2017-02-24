package org.osc.core.broker.service.response;

import java.util.HashMap;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import org.jclouds.openstack.nova.v2_0.domain.Server;
import org.osc.core.broker.model.entities.virtualization.openstack.VM;
import org.osc.core.broker.util.VimUtils;
import org.osc.sdk.controller.FlowInfo;

import com.vmware.vim25.mo.HostSystem;
import com.vmware.vim25.mo.VirtualMachine;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel(description="Contains a map of key/values. Key can be IP,MAC,VM-UUID or unique-request-identifier in case "
        + "of Flow based query")
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class QueryVmInfoResponse implements Response {

    @XmlRootElement
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class VmInfo {
        public String vmName;
        public String vmId;
        public String vmUuid;
        public String vmIpAddress;
        public String vmMacAddress;
        public String hostName;
        public String hostId;

        public VmInfo() {

        }

        public VmInfo(VirtualMachine vm, VimUtils vmi) {
            this.vmName = vm.getName();
            this.vmId = vm.getMOR().getVal();
            this.vmUuid = vm.getConfig().getInstanceUuid();
            this.vmIpAddress = vm.getGuest().getIpAddress();
            HostSystem host = vmi.getVmHost(vm);
            this.hostName = host.getName();
            this.hostId = host.getMOR().get_value();
        }

        public VmInfo(VM vm) {
            this.vmName = vm.getName();
            this.vmId = vm.getId().toString();
            this.vmUuid = vm.getOpenstackId();
            this.hostName = vm.getHost();
            this.hostId = vm.getHost();
        }

        public VmInfo(Server vm) {
            this.vmName = vm.getName();
            this.vmId = vm.getId();
            this.vmUuid = vm.getId();
            // TODO: Future Maybe add comma seperated list of ip addresses
            this.vmIpAddress = "";
            this.hostName = vm.getHostId();
            this.hostId = vm.getHostId();
        }

        @Override
        public String toString() {
            return "VmInfo [vmName=" + this.vmName + ", vmId=" + this.vmId + ", vmUuid=" + this.vmUuid + ", vmIpAddress=" + this.vmIpAddress
                    + ", hostName=" + this.hostName + ", hostId=" + this.hostId + ", vmMacAddress=" + this.vmMacAddress + "]";
        }

    }

    @XmlRootElement
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class FlowVmInfo {
        @ApiModelProperty(value = "A request unique identifier")
        public String requestId;

        @ApiModelProperty(value = "The flow used to query VM info")
        public FlowInfo flow;

        @ApiModelProperty(value = "The VM info corresponding to the flow source")
        public VmInfo sourceVmInfo;
        @ApiModelProperty(value = "The VM info corresponding to the flow destination")
        public VmInfo destinationVmInfo;

        @Override
        public String toString() {
            return "FlowVmInfo [requestId=" + this.requestId + ", flow=" + this.flow + ", sourceVmInfo=" + this.sourceVmInfo
                    + ", destinationVmInfo=" + this.destinationVmInfo + "]";
        }

    }

    @ApiModelProperty(value = "A map containing the query identifier key (IP, MAC, VM UUID) and the value "
            + "holding the VM info")
    public HashMap<String, VmInfo> vmInfo = new HashMap<>();
    @ApiModelProperty(value = "A map containing a flow based request unique identifier key and the value holding "
            + "holding the VM info")
    public HashMap<String, FlowVmInfo> flowVmInfo = new HashMap<>();

    @Override
    public String toString() {
        return "QueryVmInfoResponse [vmInfo=" + this.vmInfo + ", flowVmInfo=" + this.flowVmInfo + "]";
    }

}
