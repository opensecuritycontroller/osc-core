package org.osc.core.broker.rest.client.openstack.jcloud;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.apache.log4j.Logger;
import org.jclouds.openstack.neutron.v2.NeutronApi;
import org.jclouds.openstack.neutron.v2.domain.IP;
import org.jclouds.openstack.neutron.v2.domain.Network;
import org.jclouds.openstack.neutron.v2.domain.Port;
import org.jclouds.openstack.neutron.v2.domain.Rule;
import org.jclouds.openstack.neutron.v2.domain.SecurityGroup;
import org.jclouds.openstack.neutron.v2.domain.Subnet;
import org.jclouds.openstack.neutron.v2.extensions.SecurityGroupApi;
import org.jclouds.openstack.v2_0.options.PaginationOptions;
import org.jclouds.rest.ResourceAlreadyExistsException;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;

public class JCloudNeutron extends BaseJCloudApi {

    private static final Logger log = Logger.getLogger(JCloudNeutron.class);

    private static final String QUERY_PARAM_SHARED = "shared";
    private static final String QUERY_PARAM_COMPUTE_DEVICE_OWNER = "compute:";
    private static final String QUERY_PARAM_ROUTER_DEVICE_OWNER = "network:router_interface";
    private static final String QUERY_PARAM_TENANT_ID = "tenant_id";
    private static final String QUERY_PARAM_NETWORK_ID = "network_id";
    private static final String QUERY_PARAM_MAC_ADDRESS_ID = "mac_address";

    static final String OPENSTACK_SERVICE_NEUTRON = "openstack-neutron";
    private final NeutronApi neutronApi;

    /**
     * @param endPointIP
     *            - OpenStack Server IP
     * @param tenant
     *            - Name of current tenant
     * @param user
     *            - key stone user name
     * @param pw
     *            - key stone password
     */

    public JCloudNeutron(Endpoint endPoint) {
        super(endPoint);
        this.neutronApi = JCloudUtil.buildApi(NeutronApi.class, OPENSTACK_SERVICE_NEUTRON, endPoint);
    }

    /**
     * Lists both tenant networks and shared networks
     */
    public List<Network> listNetworkByTenant(String region, String tenantId) {

        ArrayListMultimap<String, String> queryParams = ArrayListMultimap.create();
        queryParams.put(QUERY_PARAM_TENANT_ID, tenantId);
        queryParams.put(QUERY_PARAM_SHARED, Boolean.FALSE.toString());

        List<Network> osNetList = this.neutronApi.getNetworkApi(region)
                .list(new PaginationOptions().queryParameters(queryParams)).toList();

        queryParams.clear();
        queryParams.put(QUERY_PARAM_SHARED, Boolean.TRUE.toString());
        List<Network> sharedNetList = this.neutronApi.getNetworkApi(region)
                .list(new PaginationOptions().queryParameters(queryParams)).toList();

        return ImmutableList.<Network>builder().addAll(osNetList).addAll(sharedNetList).build();
    }

    public List<Subnet> listSubnetByTenant(String region, String tenantId) {

        ArrayListMultimap<String, String> queryParams = ArrayListMultimap.create();
        queryParams.put(QUERY_PARAM_TENANT_ID, tenantId);

        List<Subnet> osSubnetList = this.neutronApi.getSubnetApi(region)
                .list(new PaginationOptions().queryParameters(queryParams)).toList();

        return ImmutableList.<Subnet>builder().addAll(osSubnetList).build();

    }

    public Network getNetworkById(String region, String id) {
        return this.neutronApi.getNetworkApi(region).get(id);
    }

    public Subnet getSubnetById(String region, String id) {
        return this.neutronApi.getSubnetApi((region)).get(id);
    }

    public List<Port> listComputePortsByNetwork(String region, String tenantId, String networkId) {
        List<Port> portList = listPorts(region, tenantId, networkId);
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
        List<Port> osPorts = listPorts(region, tenantId, networkId);
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
        List<Port> osPorts = listPorts(region, tenantId, networkId);
        List<Port> subnetPorts = new ArrayList<>();

        String classifier = routerPortsOnly ? QUERY_PARAM_ROUTER_DEVICE_OWNER
                : QUERY_PARAM_COMPUTE_DEVICE_OWNER;

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

    public List<Port> listPorts(String region, String tenantId, String networkId) {
        ArrayListMultimap<String, String> queryParams = ArrayListMultimap.create();
        queryParams.put(QUERY_PARAM_NETWORK_ID, networkId);
        queryParams.put(QUERY_PARAM_TENANT_ID, tenantId);
        List<Port> portList = this.neutronApi.getPortApi(region)
                .list(new PaginationOptions().queryParameters(queryParams)).toList();

        return portList;
    }

    public Port getPortByMacAddress(String region, String macAddress) {
        ArrayListMultimap<String, String> queryParams = ArrayListMultimap.create();
        queryParams.put(QUERY_PARAM_MAC_ADDRESS_ID, macAddress);

        List<Port> portList = this.neutronApi.getPortApi(region)
                .list(new PaginationOptions().queryParameters(queryParams)).toList();

        if (portList == null || portList.isEmpty()) {
            return null;
        }

        return portList.get(0);
    }

    public Port getPortById(String region, String portId) {
        return this.neutronApi.getPortApi(region).get(portId);
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
        this.neutronApi.getPortApi(region).delete(portId);
        return getPortById(region, portId) == null;
    }

    public SecurityGroup getSecurityGroupById(String region, String id) {
        SecurityGroupApi securityGroupApi = this.neutronApi.getSecurityGroupApi(region).get();
        return securityGroupApi.getSecurityGroup(id);
    }

    public String getVmIdByPortId(String region, String portId) {
        return getVmIdByPort(this.neutronApi.getPortApi(region).get(portId));
    }

    public String getVmIdByMacAddress(String region, String macAddress) {
        return getVmIdByPort(getPortByMacAddress(region, macAddress));
    }

    public SecurityGroup createSecurityGroup(String sgName, String region) throws Exception {
        SecurityGroupApi securityGroupApi = this.neutronApi.getSecurityGroupApi(region).get();
        SecurityGroup securityGroup = null;

        securityGroup = securityGroupApi.create(SecurityGroup.createBuilder().name(sgName)
                .description("OSC default Openstack Security Group for virtual system " + sgName).build());

        return securityGroup;
    }

    public void addSecurityGroupRules(SecurityGroup sg, String region, Collection<Rule> rules) {
        SecurityGroupApi securityGroupApi = this.neutronApi.getSecurityGroupApi(region).get();
        try {
            for (Rule rule : rules) {
                securityGroupApi.create(Rule.createBuilder(rule.getDirection(), sg.getId())
                        .ethertype(rule.getEthertype()).protocol(rule.getProtocol()).build());
            }
        } catch (ResourceAlreadyExistsException ex) {
            log.info("Rule already exists for Openstack Security Group name " + sg.getName());
        }
    }

    public boolean deleteSecurityGroupById(String region, String sgRefId) {
        SecurityGroupApi securityGroupApi = this.neutronApi.getSecurityGroupApi(region).get();
        return securityGroupApi.deleteSecurityGroup(sgRefId);
    }

    public String getVmIdByPort(Port port) {
        String deviceOwner = port.getDeviceOwner();
        if (deviceOwner != null && deviceOwner.startsWith(QUERY_PARAM_COMPUTE_DEVICE_OWNER)) {
            return port.getDeviceId();
        }
        return null;
    }

    @Override
    protected List<? extends Closeable> getApis() {
        return Arrays.asList(this.neutronApi);
    }

    public String getVmIdByIpAddress(String region, String sourceIpAddress) {
        // TODO Auto-generated method stub
        return null;
    }

}
