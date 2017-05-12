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
package org.osc.core.broker.service;

import java.io.IOException;
import java.util.HashMap;

import javax.persistence.EntityManager;

import org.apache.log4j.Logger;
import org.jclouds.openstack.nova.v2_0.domain.Server;
import org.osc.core.broker.model.entities.appliance.DistributedApplianceInstance;
import org.osc.core.broker.model.entities.appliance.VirtualizationType;
import org.osc.core.broker.model.entities.virtualization.VirtualizationConnector;
import org.osc.core.broker.model.entities.virtualization.openstack.VM;
import org.osc.core.broker.model.plugin.sdncontroller.ControllerType;
import org.osc.core.broker.model.plugin.sdncontroller.SdnControllerApiFactory;
import org.osc.core.broker.rest.client.openstack.jcloud.Endpoint;
import org.osc.core.broker.rest.client.openstack.jcloud.JCloudNeutron;
import org.osc.core.broker.rest.client.openstack.jcloud.JCloudNova;
import org.osc.core.broker.service.api.QueryVmInfoServiceApi;
import org.osc.core.broker.service.api.server.EncryptionApi;
import org.osc.core.broker.service.exceptions.VmidcBrokerValidationException;
import org.osc.core.broker.service.persistence.OSCEntityManager;
import org.osc.core.broker.service.persistence.VMPortEntityManager;
import org.osc.core.broker.service.request.QueryVmInfoRequest;
import org.osc.core.broker.service.response.QueryVmInfoResponse;
import org.osc.core.broker.service.response.QueryVmInfoResponse.FlowVmInfo;
import org.osc.core.broker.service.response.QueryVmInfoResponse.VmInfo;
import org.osc.core.broker.util.ValidateUtil;
import org.osc.core.broker.util.VimUtils;
import org.osc.sdk.controller.FlowInfo;
import org.osc.sdk.controller.FlowPortInfo;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.vmware.vim25.mo.HostSystem;
import com.vmware.vim25.mo.VirtualMachine;

@Component
public class QueryVmInfoService extends ServiceDispatcher<QueryVmInfoRequest, QueryVmInfoResponse>
        implements QueryVmInfoServiceApi {

    private static final Logger log =
            Logger.getLogger(QueryVmInfoService.class);

    @Reference
    EncryptionApi encryption;

    @Override
    public QueryVmInfoResponse exec(QueryVmInfoRequest request, EntityManager em) throws Exception {

        DistributedApplianceInstance dai = validate(em, request);

        VirtualizationConnector vc = dai.getVirtualSystem().getVirtualizationConnector();

        QueryVmInfoResponse response = new QueryVmInfoResponse();

        if (vc.getVirtualizationType() == VirtualizationType.VMWARE) {

            if (request.macAddress != null && !request.macAddress.isEmpty()) {
                throw new VmidcBrokerValidationException("MAC address based query are not supported for VMware.");
            }

            VimUtils vmi = new VimUtils(vc.getProviderIpAddress(), vc.getProviderUsername(),
                    this.encryption.decryptAESCTR(vc.getProviderPassword()));

            if (request.ipAddress != null && !request.ipAddress.isEmpty()) {
                for (String ipAddress : request.ipAddress) {
                    VirtualMachine vm = vmi.findVmByIp(ipAddress);

                    VmInfo vmInfo = null;
                    if (vm != null) {
                        vmInfo = newVmInfo(vm, vmi);
                    }
                    response.vmInfo.put(ipAddress, vmInfo);
                }
            }

            if (request.vmUuid != null && !request.vmUuid.isEmpty()) {
                for (String vmUuid : request.vmUuid) {
                    VmInfo vmInfo = null;
                    VirtualMachine vm = vmi.findVmByInstanceUuid(vmUuid);

                    if (vm != null) {
                        vmInfo = newVmInfo(vm, vmi);
                    }
                    response.vmInfo.put(vmUuid, vmInfo);
                }
            }

            if (request.flow != null && !request.flow.isEmpty()) {
                for (String requestId : request.flow.keySet()) {
                    FlowInfo flowInfo = request.flow.get(requestId);

                    FlowVmInfo flowVmInfo = new FlowVmInfo();
                    flowVmInfo.requestId = requestId;
                    flowVmInfo.flow = flowInfo;
                    flowVmInfo.sourceVmInfo = findVmByMacOrIp(vmi, flowInfo.sourceMacAddress, flowInfo.sourceIpAddress);
                    flowVmInfo.destinationVmInfo = findVmByMacOrIp(vmi, flowInfo.destinationMacAddress,
                            flowInfo.destinationIpAddress);
                    response.flowVmInfo.put(requestId, flowVmInfo);
                }
            }

        } else if (vc.getVirtualizationType() == VirtualizationType.OPENSTACK) {

            if (request.ipAddress != null && !request.ipAddress.isEmpty()) {
                JCloudNeutron neutron = null;
                JCloudNova nova = null;

                try {
                    neutron = new JCloudNeutron(new Endpoint(vc));
                    nova = new JCloudNova(new Endpoint(vc));
                    for (String ipAddress : request.ipAddress) {
                        VmInfo vmInfo = null;
                        VM vm = VMPortEntityManager.findByIpAddress(em, dai, ipAddress);

                        if (vm != null) {
                            vmInfo = newVmInfo(vm);
                            vmInfo.vmIpAddress = ipAddress;
                        } else {
                            vmInfo = findVmByIpAddress(nova, neutron, dai, ipAddress);
                        }

                        response.vmInfo.put(ipAddress, vmInfo);
                    }
                } finally {
                    if (neutron != null) {
                        neutron.close();
                    }
                    if (nova != null) {
                        nova.close();
                    }
                }
            }
            if (request.macAddress != null && !request.macAddress.isEmpty()) {
                JCloudNeutron neutron = null;
                JCloudNova nova = null;

                try {
                    neutron = new JCloudNeutron(new Endpoint(vc));
                    nova = new JCloudNova(new Endpoint(vc));
                    for (String macAddress : request.macAddress) {
                        VmInfo vmInfo = null;
                        VM vm = VMPortEntityManager.findByMacAddress(em, macAddress);

                        if (vm != null) {
                            vmInfo = newVmInfo(vm);
                            vmInfo.vmMacAddress = macAddress;
                        } else {
                            vmInfo = findVmByMacAddress(nova, neutron, dai, macAddress);
                        }
                        response.vmInfo.put(macAddress, vmInfo);
                    }
                } finally {
                    if (neutron != null) {
                        neutron.close();
                    }
                    if (nova != null) {
                        nova.close();
                    }
                }
            }

            if (request.flow != null && !request.flow.isEmpty()) {
                JCloudNeutron neutron = null;
                JCloudNova nova = null;

                try {
                    neutron = new JCloudNeutron(new Endpoint(vc));
                    nova = new JCloudNova(new Endpoint(vc));

                    if (SdnControllerApiFactory.providesTrafficPortInfo(ControllerType.fromText(vc.getControllerType()))) {
                        // Search using SDN controller
                        HashMap<String, FlowPortInfo> flowPortInfo = SdnControllerApiFactory.queryPortInfo(vc, null, request.flow);

                        log.info("SDN Controller Response: " + flowPortInfo);
                        for (String requestId : flowPortInfo.keySet()) {
                            FlowPortInfo portInfo = flowPortInfo.get(requestId);

                            FlowVmInfo flowVmInfo = new FlowVmInfo();
                            flowVmInfo.flow = portInfo.flow;
                            flowVmInfo.requestId = requestId;

                            if (portInfo.sourcePortId != null) {
                                flowVmInfo.sourceVmInfo = findVmByPortId(nova, neutron, dai, portInfo.sourcePortId);
                            }
                            if (portInfo.destinationPortId != null) {
                                flowVmInfo.destinationVmInfo = findVmByPortId(nova, neutron, dai,
                                        portInfo.destinationPortId);
                            }
                            response.flowVmInfo.put(requestId, flowVmInfo);
                        }
                    } else {
                        // Search using DB or openstack
                        for (String requestId : request.flow.keySet()) {
                            FlowInfo flowInfo = request.flow.get(requestId);

                            FlowVmInfo flowVmInfo = new FlowVmInfo();
                            flowVmInfo.requestId = requestId;
                            flowVmInfo.flow = flowInfo;
                            flowVmInfo.sourceVmInfo = findVmByMacOrIp(em, nova, neutron, dai,
                                    flowInfo.sourceMacAddress, flowInfo.sourceIpAddress);
                            flowVmInfo.destinationVmInfo = findVmByMacOrIp(em, nova, neutron, dai,
                                    flowInfo.destinationMacAddress, flowInfo.destinationIpAddress);

                            response.flowVmInfo.put(requestId, flowVmInfo);
                        }
                    }
                } finally {
                    if (neutron != null) {
                        neutron.close();
                    }
                    if (nova != null) {
                        nova.close();
                    }
                }
            }
        }

        return response;
    }

    private VmInfo findVmByMacOrIp(VimUtils vmi, String macAddress, String ipAddress) {
        if (macAddress != null) {
            // TODO: Future. Lookup VMware VM by MAC address
        }
        VirtualMachine vm = vmi.findVmByIp(ipAddress);
        if (vm != null) {
            return newVmInfo(vm, vmi);
        }
        return null;
    }

    private VmInfo findVmByPortId(JCloudNova nova, JCloudNeutron neutron, DistributedApplianceInstance dai,
            String portId) throws IOException, VmidcBrokerValidationException {
        String region = dai.getDeploymentSpec().getRegion();
        String vmId = neutron.getVmIdByPortId(region, portId);
        if(vmId == null) {
            throw new VmidcBrokerValidationException("Unable to find Server attached to the port " + portId);
        }
        Server vm = nova.getServer(region, vmId);
        return newVmInfo(vm);
    }

    private VmInfo findVmByMacOrIp(EntityManager em, JCloudNova nova, JCloudNeutron neutron,
            DistributedApplianceInstance dai, String macAddress, String ipAddress) throws IOException {
        VmInfo vmInfo = null;
        if (macAddress != null) {
            VM vm = VMPortEntityManager.findByMacAddress(em, macAddress);
            if (vm != null) {
                // From local cache
                vmInfo = newVmInfo(vm);
                vmInfo.vmMacAddress = macAddress;
            } else {
                // From openstack
                vmInfo = findVmByMacAddress(nova, neutron, dai, macAddress);
            }
        } else {
            // By IP
            VM vm = VMPortEntityManager.findByIpAddress(em, dai, ipAddress);
            if (vm != null) {
                // From local cache
                vmInfo = newVmInfo(vm);
                vmInfo.vmIpAddress = ipAddress;
            } else {
                // From openstack
                vmInfo = findVmByIpAddress(nova, neutron, dai, ipAddress);
            }
        }
        return vmInfo;
    }

    private VmInfo findVmByIpAddress(JCloudNova nova, JCloudNeutron neutron, DistributedApplianceInstance dai,
            String ipAddress) {
        // TODO: Future. Locate VM by IP.
        return null;
    }

    private DistributedApplianceInstance validate(EntityManager em, QueryVmInfoRequest request) throws Exception {

        if (request.applianceInstanceName == null || request.applianceInstanceName.isEmpty()) {
            throw new VmidcBrokerValidationException("Invalid Appliance Instance Name.");
        }

        OSCEntityManager<DistributedApplianceInstance> emgr = new OSCEntityManager<DistributedApplianceInstance>(
                DistributedApplianceInstance.class, em);
        DistributedApplianceInstance dai = emgr.findByFieldName("name", request.applianceInstanceName);

        if (dai == null) {
            throw new VmidcBrokerValidationException(
                    "Appliance Instance Name '" + request.applianceInstanceName + "' not found.");
        }

        if ((request.ipAddress == null || request.ipAddress.isEmpty())
                && (request.vmUuid == null || request.vmUuid.isEmpty())
                && (request.macAddress == null || request.macAddress.isEmpty())
                && (request.flow == null || request.flow.isEmpty())) {
            throw new VmidcBrokerValidationException("Invalid IP Address, Mac Address, Uuid or Flow List.");
        }

        if (request.ipAddress != null) {
            for (String IpAddress : request.ipAddress) {
                ValidateUtil.checkForValidIpAddressFormat(IpAddress);
            }
        }

        if (request.flow != null) {
            if (dai.getVirtualSystem().getVirtualizationConnector().getVirtualizationType() == VirtualizationType.OPENSTACK
                    && !dai.getVirtualSystem().getVirtualizationConnector().isControllerDefined()) {
                throw new VmidcBrokerValidationException(
                        "Flow based queries can only be supported if SDN controller is defined.");
            }
        }

        return dai;
    }

    private VmInfo findVmByMacAddress(JCloudNova nova, JCloudNeutron neutron, DistributedApplianceInstance dai,
            String macAddress) throws IOException {
        String region = dai.getDeploymentSpec().getRegion();
        String vmId = neutron.getVmIdByMacAddress(region, macAddress);
        Server server = nova.getServer(region, vmId);
        return newVmInfo(server);
    }

    private VmInfo newVmInfo(VirtualMachine vm, VimUtils vmu) {
        VmInfo vmi = new VmInfo();
        vmi.vmName = vm.getName();
        vmi.vmId = vm.getMOR().getVal();
        vmi.vmUuid = vm.getConfig().getInstanceUuid();
        vmi.vmIpAddress = vm.getGuest().getIpAddress();
        HostSystem host = vmu.getVmHost(vm);
        vmi.hostName = host.getName();
        vmi.hostId = host.getMOR().get_value();
        return vmi;
    }

    private VmInfo newVmInfo(VM vm) {
        VmInfo vmi = new VmInfo();
        vmi.vmName = vm.getName();
        vmi.vmId = vm.getId().toString();
        vmi.vmUuid = vm.getOpenstackId();
        vmi.hostName = vm.getHost();
        vmi.hostId = vm.getHost();
        return vmi;
    }

    private VmInfo newVmInfo(Server vm) {
        VmInfo vmi = new VmInfo();
        vmi.vmName = vm.getName();
        vmi.vmId = vm.getId();
        vmi.vmUuid = vm.getId();
        // TODO: Future Maybe add comma seperated list of ip addresses
        vmi.vmIpAddress = "";
        vmi.hostName = vm.getHostId();
        vmi.hostId = vm.getHostId();
        return vmi;
    }
}
