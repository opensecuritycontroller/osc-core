/*******************************************************************************
 * Copyright (c) Intel Corporation
 * Copyright (c) 2017
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package org.osc.core.broker.rest.client.openstack.discovery;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jclouds.openstack.nova.v2_0.domain.FixedIP;
import org.jclouds.openstack.nova.v2_0.domain.InterfaceAttachment;
import org.jclouds.openstack.nova.v2_0.domain.Server;
import org.jclouds.openstack.nova.v2_0.domain.ServerExtendedAttributes;
import org.osc.core.broker.model.entities.virtualization.VirtualizationConnector;
import org.osc.core.broker.rest.client.openstack.jcloud.Endpoint;
import org.osc.core.broker.rest.client.openstack.jcloud.JCloudNova;
import org.osc.core.broker.service.api.server.EncryptionException;
import org.osc.sdk.controller.element.NetworkElement;

import com.google.common.collect.Maps;

public class VmDiscoveryCache {

    private VirtualizationConnector vc;
    private String tenant;

    public static class PortInfo implements NetworkElement {
        public VmInfo vm;
        public String osNetworkId;
        public String osPortId;
        public String macAddress;
        public List<String> portIPs = new ArrayList<>();
        public String parentId;

        @Override
        public String toString() {
            return "PortInfo [vm=" + this.vm + ", osNetworkId=" + this.osNetworkId + ", osPortId=" + this.osPortId
                    + ", macAddress=" + this.macAddress + ", portIPs=" + this.portIPs + ", parentId=" + this.parentId +"]";
        }

        @Override
        public String getElementId() {
            return this.osPortId;
        }

        @Override
        public List<String> getMacAddresses() {
            List<String> macAddresses = new ArrayList<String>();
            macAddresses.add(this.macAddress);
            return macAddresses;
        }

        @Override
        public List<String> getPortIPs() {
            return this.portIPs;
        }

        @Override
        public String getParentId() {
            return this.parentId;
        }
    }

    public class VmInfo {
        public String vmId;
        public String tenantId;
        public String name;
        public String host;

        public Map<String, PortInfo> macAddressToPortMap = Maps.newConcurrentMap();

        @Override
        public String toString() {
            return "VmInfo [vmId=" + this.vmId + ", tenantId=" + this.tenantId + ", name=" + this.name + ", host="
                    + this.host + ", portsByMacMap=" + this.macAddressToPortMap + "]";
        }
    }

    private Map<String, VmInfo> vmIdToVmMap = Maps.newConcurrentMap();
    private Map<String, PortInfo> osPortIdToPortMap = new HashMap<String, PortInfo>();
    private Map<String, PortInfo> macAddressToPortMap = new HashMap<String, PortInfo>();

    private JCloudNova jcNovaApi;

    public VmDiscoveryCache(VirtualizationConnector vc, String tenant) throws IOException, EncryptionException {
        this.vc = vc;
        this.tenant = tenant;
        this.jcNovaApi = new JCloudNova(new Endpoint(vc, tenant));
    }

    public VmInfo discover(String region, String vmId) throws Exception {
        if (vmId == null) {
            return null;
        }
        VmInfo vmInfo = this.vmIdToVmMap.get(vmId);
        if (vmInfo != null) {
            return vmInfo;
        }

        vmInfo = new VmInfo();
        vmInfo.vmId = vmId;

        Server vm = this.jcNovaApi.getServer(region, vmId);
        if (vm == null) {
            return null;
        }

        vmInfo.tenantId = vm.getTenantId();
        vmInfo.name = vm.getName();
        ServerExtendedAttributes serverExtendedAttributes = vm.getExtendedAttributes().get();
        if (serverExtendedAttributes != null) {
            vmInfo.host = serverExtendedAttributes.getHypervisorHostName();
        }
        List<InterfaceAttachment> interfaces = this.jcNovaApi.getVmAttachedNetworks(region, vmId);
        for (InterfaceAttachment infs : interfaces) {
            if (infs.getMacAddress() == null) {
                continue;
            }

            PortInfo portInfo = new PortInfo();
            portInfo.vm = vmInfo;
            portInfo.macAddress = infs.getMacAddress();
            portInfo.osNetworkId = infs.getNetworkId();
            portInfo.osPortId = infs.getPortId();

            // add IP addresses for give port
            for (FixedIP ip : infs.getFixedIps()) {
                portInfo.portIPs.add(ip.getIpAddress());
            }

            vmInfo.macAddressToPortMap.put(infs.getMacAddress(), portInfo);

            this.osPortIdToPortMap.put(portInfo.osPortId, portInfo);
            this.macAddressToPortMap.put(portInfo.macAddress, portInfo);
        }
        this.vmIdToVmMap.put(vmId, vmInfo);

        return vmInfo;
    }

    @Override
    public String toString() {
        return "VmDiscoveryCache [vc=" + this.vc + ", tenant=" + this.tenant + ", vmMap=" + this.vmIdToVmMap + "]";
    }

    public synchronized void clear() {
        this.vmIdToVmMap.clear();
    }

    public void close() throws IOException {
        if (this.jcNovaApi != null) {
            this.jcNovaApi.close();
        }
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        close();
    }

    public PortInfo getPortByOsPortId(String portId) {
        return this.osPortIdToPortMap.get(portId);
    }

    public PortInfo getPortByMacAddress(String macAddress) {
        return this.macAddressToPortMap.get(macAddress);
    }

}
