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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.ws.rs.core.Response;

import org.apache.commons.collections4.CollectionUtils;
import org.openstack4j.api.exceptions.ClientResponseException;
import org.openstack4j.api.exceptions.ResponseException;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.network.IP;
import org.openstack4j.model.network.NetFloatingIP;
import org.openstack4j.model.network.Network;
import org.openstack4j.model.network.Port;
import org.openstack4j.model.network.SecurityGroup;
import org.openstack4j.model.network.SecurityGroupRule;
import org.openstack4j.model.network.State;
import org.openstack4j.model.network.Subnet;
import org.openstack4j.model.network.builder.NetFloatingIPBuilder;
import org.openstack4j.model.network.builder.NetSecurityGroupBuilder;
import org.openstack4j.model.network.options.PortListOptions;
import org.openstack4j.openstack.networking.domain.NeutronFloatingIP;
import org.openstack4j.openstack.networking.domain.NeutronSecurityGroup;
import org.osc.core.broker.util.log.LogProvider;
import org.slf4j.Logger;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;

public class Openstack4JNeutron extends BaseOpenstack4jApi {

    private static final Logger log = LogProvider.getLogger(Openstack4JNeutron.class);

    private static final String QUERY_PARAM_COMPUTE_DEVICE_OWNER = "compute:";
    private static final String QUERY_PARAM_ROUTER_DEVICE_OWNER = "network:router_interface";
    private static final String QUERY_PARAM_EXTERNAL_ROUTER = "router:external";

    private static final int OPENSTACK_CONFLICT_STATUS = 409;

    public Openstack4JNeutron(Endpoint endPoint) {
        super(endPoint);
    }

    /**
     * Lists both Project networks and shared networks
     */
    public List<Network> listNetworkByProject(String region, String projectId) {
        getOs().useRegion(region);
        List<? extends Network> originalList = getOs().networking().network().list();

        if (CollectionUtils.isEmpty(originalList)) {
            log.info(String.format("No networks found in region: %s for project: %s", region, projectId));
        }

        List<? extends Network> list = originalList.stream()
                .filter(subnet -> projectId.equals(subnet.getTenantId()) && !subnet.isShared()
                        || subnet.isShared()).collect(Collectors.toList());
        return ImmutableList.<Network>builder().addAll(list).build();
    }

    public List<Subnet> listSubnetByProject(String region, String projectId) {
        getOs().useRegion(region);
        List<? extends Subnet> originalList = getOs().networking().subnet().list();

        if (CollectionUtils.isEmpty(originalList)) {
            log.info(String.format("No subnets found in region: %s for project: %s", region, projectId));
        }

        List<? extends Subnet> list = originalList
                .stream().filter(subnet -> projectId.equals(subnet.getTenantId())).collect(Collectors.toList());
        return ImmutableList.<Subnet>builder().addAll(list).build();
    }

    public Network getNetworkById(String region, String id) {
        getOs().useRegion(region);
        return getOs().networking().network().get(id);
    }

    public Network getNetworkByName(String region, String name) throws Exception {
        getOs().useRegion(region);
        return getOs().networking().network().list()
                .stream().filter(network -> network.getName().equals(name)).findFirst()
                .orElseThrow(() -> new Exception("Cannot find network by name: " + name));
    }

    public Subnet getSubnetById(String region, String id) {
        getOs().useRegion(region);
        return getOs().networking().subnet().get(id);
    }

    public List<Port> listComputePortsByNetwork(String region, String projectId, String networkId) {
        List<? extends Port> portList = listPorts(region, projectId, networkId);
        List<Port> computePorts = new ArrayList<>();

        for (Port port : portList) {
            String deviceOwner = port.getDeviceOwner();
            if (deviceOwner != null
                    && deviceOwner.startsWith(QUERY_PARAM_COMPUTE_DEVICE_OWNER)
                    && port.getState() == State.ACTIVE) {
                computePorts.add(port);
            }
        }

        return computePorts;
    }

    public String getNetworkPortRouterDeviceId(String projectId, String region, Port osPort) {

        String routerPortDeviceId = null;
        String networkId = osPort.getNetworkId();
        List<? extends Port> osPorts = listPorts(region, projectId, networkId);
        for (Port port : osPorts) {
            String deviceOwner = port.getDeviceOwner();
            if (deviceOwner != null
                    && deviceOwner.startsWith(QUERY_PARAM_ROUTER_DEVICE_OWNER)
                    && port.getState() == State.ACTIVE) {
                routerPortDeviceId = port.getDeviceId();
            }
        }
        return routerPortDeviceId;
    }

    public List<Port> listPortsBySubnet(String region, String projectId, String networkId, String subnetId,
                                        boolean routerPortsOnly) {
        List<? extends Port> osPorts = listPorts(region, projectId, networkId);
        List<Port> subnetPorts = new ArrayList<>();

        String classifier = routerPortsOnly ? QUERY_PARAM_ROUTER_DEVICE_OWNER : QUERY_PARAM_COMPUTE_DEVICE_OWNER;

        for (Port port : osPorts) {
            String deviceOwner = port.getDeviceOwner();
            if (deviceOwner != null && deviceOwner.startsWith(classifier) && port.getState() == State.ACTIVE) {
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

    private List<? extends Port> listPorts(String region, String projectId, String networkId) {
        getOs().useRegion(region);
        List<? extends Port> portList = getOs().networking().port().list(PortListOptions.create().networkId(networkId).tenantId(projectId));
        if (CollectionUtils.isEmpty(portList)) {
            log.info(String.format("No ports found in network: %s for project: %s in region: %s", networkId, projectId,
                    region));
        }
        return portList;
    }

    private Port getPortByMacAddress(String region, String macAddress) {
        getOs().useRegion(region);
        List<? extends Port> portList = getOs().networking().port().list(PortListOptions.create().macAddress(macAddress));
        if (CollectionUtils.isEmpty(portList)) {
            log.info(String.format("No ports found with mac: %s in region: %s", macAddress, region));
        }
        return portList.isEmpty() ? null : portList.get(0);
    }

    public Port getPortById(String region, String portId) {
        getOs().useRegion(region);
        return getOs().networking().port().get(portId);
    }

    /**
     * Deletes the port given an Id. Returns true if the port is successfully deleted. Does not throw any error
     * if the port does not exist.
     *
     * @param region region
     * @param portId non-null port id
     * @return true if successfully deleted
     */
    public boolean deletePortById(String region, String portId) {
        boolean success = true;
        getOs().useRegion(region);

        Port port = getOs().networking().port().get(portId);
        if (port != null) {
            ActionResponse delete = getOs().networking().port().delete(portId);
            success = delete.isSuccess();
        } else {
            log.warn(String.format("No port: %s found in region: %s", portId, region));
        }
        return success;
    }

    public SecurityGroup getSecurityGroupById(String region, String id) {
        getOs().useRegion(region);
        return getOs().networking().securitygroup().get(id);
    }

    private Optional<? extends SecurityGroup> getSecurityGroupByName(String region, String sgName) {
        getOs().useRegion(region);
        List<? extends SecurityGroup> list = getOs().networking().securitygroup().list();
        return list.stream().filter(o -> o.getName().equals(sgName)).findFirst();
    }

    public String getVmIdByPortId(String region, String portId) {
        getOs().useRegion(region);
        Port port = getOs().networking().port().get(portId);
        return getVmIdByPort(port);
    }

    public String getVmIdByMacAddress(String region, String macAddress) {
        return getVmIdByPort(getPortByMacAddress(region, macAddress));
    }

    public SecurityGroup createSecurityGroup(String sgName, String region) throws Exception {
        Optional<? extends SecurityGroup> securityGroupByName = getSecurityGroupByName(region, sgName);
        SecurityGroup securityGroup;
        if (securityGroupByName.isPresent()) {
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
        }

        if (securityGroup == null) {
            String message = String.format("Cannot create Security group with name: %s in region: %s.", sgName, region);
            log.warn(message);
            throw new ResponseException(message, 500);
        }
        return securityGroup;
    }

    public void addSecurityGroupRules(SecurityGroup sg, String region, Collection<SecurityGroupRule> rules) {
        getOs().useRegion(region);
        for (SecurityGroupRule rule : rules) {
            try {
                getOs().networking().securityrule().create(rule.toBuilder().securityGroupId(sg.getId()).build());
            } catch (ResponseException re) {
                if (re instanceof ClientResponseException
                        && ((ClientResponseException) re).getStatusCode().getCode() == OPENSTACK_CONFLICT_STATUS) {
                    log.info(String.format("Rule: %s already exists for Security Group: %s  ", rule, sg.getName()));
                } else {
                    String message = String.format("Unable to create rule: %s for Security Group: %s  ", rule,
                            sg.getName());
                    log.error(message, re);
                    throw new ResponseException(message, 500);
                }
            }
        }
    }

    public boolean deleteSecurityGroupById(String region, String sgRefId) throws IllegalStateException {
        getOs().useRegion(region);
        ActionResponse actionResponse = getOs().networking().securitygroup().delete(sgRefId);
        if (!actionResponse.isSuccess()) {
            String message = String.format("Deleting Security group: %s in region: %s failed. Error: %s", sgRefId,
                    region, actionResponse.getFault());
            log.warn(message);
            throw new IllegalStateException(message);
        }
        return actionResponse.isSuccess();
    }

    private String getVmIdByPort(Port port) {
        String deviceOwner = port.getDeviceOwner();
        if (deviceOwner != null && deviceOwner.startsWith(QUERY_PARAM_COMPUTE_DEVICE_OWNER)) {
            return port.getDeviceId();
        }
        return null;
    }

    // Floating IP API
    public List<String> getFloatingIpPools(String region) throws Exception {
        getOs().useRegion(region);

        Map<String, String> filter = Maps.newHashMap();
        filter.put(QUERY_PARAM_EXTERNAL_ROUTER, Boolean.TRUE.toString());

        List<? extends Network> floatingIpPoolList = getOs().networking().network().list(filter);
        if (CollectionUtils.isEmpty(floatingIpPoolList)) {
            log.info("No floating ip found in region: " + region);
        }
        return floatingIpPoolList
                .stream()
                .map(Network::getName)
                .collect(Collectors.toList());
    }

    public NetFloatingIP getFloatingIp(String region, String id) {
        if (id == null) {
            return null;
        }

        getOs().useRegion(region);
        return getOs().networking().floatingip().get(id);
    }

    public synchronized void associateMgmtPortWithFloatingIp(String region, String netFloatingIpId, String portId) {
        getOs().useRegion(region);
        NetFloatingIP associateToPort = getOs().networking().floatingip().associateToPort(netFloatingIpId, portId);
        if(associateToPort == null) {
            String message = String.format("Associating floating ip: %s to port: %s in region: %s failed.", netFloatingIpId,
                    portId, region);
            log.warn(message);
            throw new ResponseException(message, 500);
        }
    }

    /**
     * A synchronous way to allocate floating ip(within ourselfs). Since this is a static method, we would lock on
     * the class objects which prevents multiple threads from making the floating ip call at the same time.
     */
    public synchronized NetFloatingIP createFloatingIp(String region, String networkId, String serverId,
            String portId) {
        getOs().useRegion(region);

        NetFloatingIPBuilder builder = new NeutronFloatingIP.FloatingIPConcreteBuilder();
        builder.floatingNetworkId(networkId);
        builder.portId(portId);

        NetFloatingIP netFloatingIP = null;
        netFloatingIP = getOs().networking().floatingip().create(builder.build());
        if (netFloatingIP != null) {
            log.info("Allocated Floating ip: " + netFloatingIP.getId() + " To server with Id: " + serverId);
        } else {
            String message = String.format(
                    "Cannot create floating ip for server: %s with port: %s under network: %s in region: %s", serverId,
                    portId, networkId, region);
            log.warn(message);
            throw new ResponseException(message, 500);
        }
        return netFloatingIP;
    }

    public synchronized void deleteFloatingIp(String region, String floatingIpId) {
        getOs().useRegion(region);
        log.info("Deleting Floating ip with id: " + floatingIpId);
        ActionResponse actionResponse = getOs().networking().floatingip().delete(floatingIpId);
        if (!actionResponse.isSuccess()) {
            String message = String.format("Deleting floating ip with id: %s in region: %s failed with message: %s",
                    floatingIpId, region, actionResponse.getFault());
            log.warn(message);
            if (actionResponse.getCode() != Response.Status.NOT_FOUND.getStatusCode()) {
                throw new ResponseException(message, actionResponse.getCode());
            }
        }
    }

    @Override
    public void close() throws IOException {
        if (getOs() != null) {
            getOs().removeRegion();
        }
        super.close();
    }
}
