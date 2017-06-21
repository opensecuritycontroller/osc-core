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
package org.osc.core.broker.rest.client.openstack.openstack4j;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;

import com.google.common.collect.ImmutableList;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.network.IP;
import org.openstack4j.model.network.Network;
import org.openstack4j.model.network.Port;
import org.openstack4j.model.network.SecurityGroup;
import org.openstack4j.model.network.SecurityGroupRule;
import org.openstack4j.model.network.Subnet;
import org.openstack4j.model.network.builder.NetSecurityGroupBuilder;
import org.openstack4j.model.network.options.PortListOptions;
import org.openstack4j.openstack.networking.domain.NeutronSecurityGroup;

public class Openstack4JNeutron extends BaseOpenstack4jApi {

    private static final Logger log = Logger.getLogger(Openstack4JNeutron.class);

    private static final String QUERY_PARAM_COMPUTE_DEVICE_OWNER = "compute:";
    private static final String QUERY_PARAM_ROUTER_DEVICE_OWNER = "network:router_interface";

    public Openstack4JNeutron(Endpoint endPoint) {
        super(endPoint);
    }

    /**
     * Lists both tenant networks and shared networks
     */
    public List<Network> listNetworkByTenant(String region, String tenantId) {
        getOs().useRegion(region);
        List<? extends Network> list = getOs().networking().network().list().stream()
                .filter(subnet -> (tenantId.equals(subnet.getTenantId()) && !subnet.isShared())
                        || subnet.isShared()).collect(Collectors.toList());
        getOs().removeRegion();
        return ImmutableList.<Network>builder().addAll(list).build();
    }

    public List<Subnet> listSubnetByTenant(String region, String tenantId) {
        getOs().useRegion(region);
        List<? extends Subnet> list = getOs().networking().subnet().list()
                .stream().filter(subnet -> tenantId.equals(subnet.getTenantId())).collect(Collectors.toList());
        getOs().removeRegion();
        return ImmutableList.<Subnet>builder().addAll(list).build();
    }

    public Network getNetworkById(String region, String id) {
        getOs().useRegion(region);
        Network network = getOs().networking().network().get(id);
        getOs().removeRegion();
        return network;
    }

    public Subnet getSubnetById(String region, String id) {
        getOs().useRegion(region);
        Subnet subnet = getOs().networking().subnet().get(id);
        getOs().removeRegion();
        return subnet;
    }

    public List<Port> listComputePortsByNetwork(String region, String tenantId, String networkId) {
        List<? extends Port> portList = listPorts(region, tenantId, networkId);
        List<Port> computePorts = new ArrayList<>();

        for (Port port : portList) {
            String deviceOwner = port.getDeviceOwner();
            if (deviceOwner != null && deviceOwner.startsWith(QUERY_PARAM_COMPUTE_DEVICE_OWNER)) {
                computePorts.add(port);
            }
        }

        return computePorts;
    }

    public String getNetworkPortRouterDeviceId(String tenantId, String region, Port osPort) {

        String routerPortDeviceId = null;
        String networkId = osPort.getNetworkId();
        List<? extends Port> osPorts = listPorts(region, tenantId, networkId);
        for (Port port : osPorts) {
            String deviceOwner = port.getDeviceOwner();
            if (deviceOwner != null && deviceOwner.startsWith(QUERY_PARAM_ROUTER_DEVICE_OWNER)) {
                routerPortDeviceId = port.getDeviceId();
            }
        }
        return routerPortDeviceId;
    }

    public List<Port> listPortsBySubnet(String region, String tenantId, String networkId, String subnetId,
            boolean routerPortsOnly) {
        List<? extends Port> osPorts = listPorts(region, tenantId, networkId);
        List<Port> subnetPorts = new ArrayList<>();

        String classifier = routerPortsOnly ? QUERY_PARAM_ROUTER_DEVICE_OWNER : QUERY_PARAM_COMPUTE_DEVICE_OWNER;

        for (Port port : osPorts) {
            String deviceOwner = port.getDeviceOwner();
            if (deviceOwner != null && deviceOwner.startsWith(classifier)) {
                for (IP ip : port.getFixedIps()) {
                    if (ip.getSubnetId().equals(subnetId)) {
                        subnetPorts.add(port);
                        break;
                    }
                }
            }
        }
        return subnetPorts;
    }

    private List<? extends Port> listPorts(String region, String tenantId, String networkId) {
        getOs().useRegion(region);
        List<? extends Port> list = getOs().networking().port().list(PortListOptions.create().networkId(networkId).tenantId(tenantId));
        getOs().removeRegion();
        return list;
    }

    private Port getPortByMacAddress(String region, String macAddress) {
        getOs().useRegion(region);
        List<? extends Port> portList = getOs().networking().port().list(PortListOptions.create().macAddress(macAddress));
        getOs().removeRegion();
        return (portList == null || portList.isEmpty()) ? null : portList.get(0);
    }

    public Port getPortById(String region, String portId) {
        getOs().useRegion(region);
        Port port = getOs().networking().port().get(portId);
        getOs().removeRegion();
        return port;
    }

    /**
     * Deletes the port given an Id. Returns true if the port is successfully deleted. Does not throw any error
     * if the port does not exist.
     *
     * @param region region
     * @param portId non-null port id
     *
     * @return true if successfully deleted
     */
    public boolean deletePortById(String region, String portId) {
        boolean success = true;
        getOs().useRegion(region);

        Port port = getOs().networking().port().get(portId);
        if(port != null){
            ActionResponse delete = getOs().networking().port().delete(portId);
            success = delete.isSuccess();
        }
        getOs().removeRegion();
        return success;
    }

    public SecurityGroup getSecurityGroupById(String region, String id) {
        getOs().useRegion(region);
        SecurityGroup securityGroup = getOs().networking().securitygroup().get(id);
        getOs().removeRegion();
        return securityGroup;
    }

    private Optional<? extends SecurityGroup> getSecurityGroupByName(String region, String sgName){
        getOs().useRegion(region);
        List<? extends SecurityGroup> list = getOs().networking().securitygroup().list();
        Optional<? extends SecurityGroup> first = list.stream().filter(o -> o.getName().equals(sgName)).findFirst();
        getOs().removeRegion();
        return first;
    }

    public String getVmIdByPortId(String region, String portId) {
        getOs().useRegion(region);
        Port port = getOs().networking().port().get(portId);
        getOs().removeRegion();
        return getVmIdByPort(port);
    }

    public String getVmIdByMacAddress(String region, String macAddress) {
        return getVmIdByPort(getPortByMacAddress(region, macAddress));
    }

    public SecurityGroup createSecurityGroup(String sgName, String region) throws Exception {
        Optional<? extends SecurityGroup> securityGroupByName = getSecurityGroupByName(region, sgName);
        SecurityGroup securityGroup;
        if(securityGroupByName.isPresent()){
            securityGroup = securityGroupByName.get();
            log.info("Found security group with name: " + sgName + "and using it");
        } else {
            log.info("Creating security group with name: " + sgName);
            getOs().useRegion(region);
            NeutronSecurityGroup.SecurityGroupConcreteBuilder securityGroupConcreteBuilder =
                    new NeutronSecurityGroup.SecurityGroupConcreteBuilder();
            NetSecurityGroupBuilder securityGroupBuilder = securityGroupConcreteBuilder
                    .description("OSC default Openstack Security Group for virtual system " + sgName)
                    .name(sgName);
            securityGroup = getOs().networking().securitygroup().create(securityGroupBuilder.build());
            getOs().removeRegion();
        }

        return securityGroup;
    }

    public void addSecurityGroupRules(SecurityGroup sg, String region, Collection<SecurityGroupRule> rules) {
        getOs().useRegion(region);
        for (SecurityGroupRule rule : rules) {
            getOs().networking().securityrule().create(rule.toBuilder().securityGroupId(sg.getId()).build());
        }
        getOs().removeRegion();
        //log.info("Rule already exists for Openstack Security Group name " + sg.getName());
    }

    public boolean deleteSecurityGroupById(String region, String sgRefId) {
        getOs().useRegion(region);
        ActionResponse actionResponse = getOs().networking().securitygroup().delete(sgRefId);
        getOs().removeRegion();
        return actionResponse.isSuccess();
    }

    private String getVmIdByPort(Port port) {
        String deviceOwner = port.getDeviceOwner();
        if (deviceOwner != null && deviceOwner.startsWith(QUERY_PARAM_COMPUTE_DEVICE_OWNER)) {
            return port.getDeviceId();
        }
        return null;
    }
}
