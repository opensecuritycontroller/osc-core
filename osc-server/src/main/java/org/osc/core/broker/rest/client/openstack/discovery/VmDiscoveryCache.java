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

import org.openstack4j.model.compute.InterfaceAttachment;
import org.openstack4j.model.compute.Server;
import org.osc.core.broker.model.entities.virtualization.VirtualizationConnector;
import org.osc.core.broker.rest.client.openstack.openstack4j.Endpoint;
import org.osc.core.broker.rest.client.openstack.openstack4j.Openstack4JNova;
import org.osc.core.broker.service.api.server.EncryptionException;
import org.osc.sdk.controller.element.NetworkElement;

import com.google.common.collect.Maps;

public class VmDiscoveryCache {

    private VirtualizationConnector vc;
    private String project;

    public static class PortInfo implements NetworkElement {
        private VmInfo vm;
        private String osNetworkId;
        private String osPortId;
        private String macAddress;
        private List<String> portIPs = new ArrayList<>();
        private String parentId;

        @Override
        public String toString() {
            return "PortInfo [vm=" + this.vm + ", osNetworkId=" + this.osNetworkId + ", osPortId=" + this.osPortId
                    + ", macAddress=" + this.macAddress + ", portIPs=" + this.portIPs + ", parentId=" + this.parentId + "]";
        }

        @Override
        public String getElementId() {
            return this.osPortId;
        }

        public String getOsNetworkId() {
            return this.osNetworkId;
        }

        public String getMacAddress() {
            return this.macAddress;
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
        private String vmId;
        private String projectId;
        private String name;
        private String host;

        private Map<String, PortInfo> macAddressToPortMap = Maps.newConcurrentMap();

        public String getName() {
            return this.name;
        }

        public String getHost() {
            return this.host;
        }

        public Map<String, PortInfo> getMacAddressToPortMap() {
            return this.macAddressToPortMap;
        }

        @Override
        public String toString() {
            return "VmInfo [vmId=" + this.vmId + ", projectId=" + this.projectId + ", name=" + this.name + ", host="
                    + this.host + ", portsByMacMap=" + this.macAddressToPortMap + "]";
        }
    }

    private Map<String, VmInfo> vmIdToVmMap = Maps.newConcurrentMap();
    private Map<String, PortInfo> osPortIdToPortMap = new HashMap<>();
    private Map<String, PortInfo> macAddressToPortMap = new HashMap<>();

    private Openstack4JNova novaApi;

    public VmDiscoveryCache(VirtualizationConnector vc, String project) throws IOException, EncryptionException {
        this.vc = vc;
        this.project = project;
        this.novaApi = new Openstack4JNova(new Endpoint(vc, project));
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

        try {
            Server vm = this.novaApi.getServer(region, vmId);
            if (vm == null) {
                return null;
            }

            vmInfo.projectId = vm.getTenantId();
            vmInfo.name = vm.getName();
            vmInfo.host = vm.getHypervisorHostname();
            List<? extends InterfaceAttachment> interfaces = this.novaApi.getVmAttachedNetworks(region, vmId);
            for (InterfaceAttachment infs : interfaces) {
                if (infs.getMacAddr() == null) {
                    continue;
                }

                PortInfo portInfo = new PortInfo();
                portInfo.vm = vmInfo;
                portInfo.macAddress = infs.getMacAddr();
                portInfo.osNetworkId = infs.getNetId();
                portInfo.osPortId = infs.getPortId();

                // add IP addresses for give port
                infs.getFixedIps().forEach(ip -> portInfo.portIPs.add(ip.getIpAddress()));

                vmInfo.macAddressToPortMap.put(infs.getMacAddr(), portInfo);

                this.osPortIdToPortMap.put(portInfo.osPortId, portInfo);
                this.macAddressToPortMap.put(portInfo.macAddress, portInfo);
            }
            this.vmIdToVmMap.put(vmId, vmInfo);
        } finally {
            this.novaApi.close();
        }

        return vmInfo;
    }

    @Override
    public String toString() {
        return "VmDiscoveryCache [vc=" + this.vc + ", project=" + this.project + ", vmMap=" + this.vmIdToVmMap + "]";
    }

    public synchronized void clear() {
        this.vmIdToVmMap.clear();
    }

}
