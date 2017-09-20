package org.osc.core.broker.rest.client.openstack.openstack4j;

import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.openstack4j.api.Builders;
import org.openstack4j.model.network.SecurityGroup;
import org.openstack4j.model.network.SecurityGroupRule;

public class Openstack4JNeutronTest {

    @Test
    public void testClose() {
        fail("Not yet implemented");
    }

    @Test
    public void testOpenstack4JNeutron() {
        fail("Not yet implemented");
    }

    @Test
    public void testListNetworkByProject() {
        fail("Not yet implemented");
    }

    @Test
    public void testListSubnetByProject() {
        fail("Not yet implemented");
    }

    @Test
    public void testGetNetworkById() {
        fail("Not yet implemented");
    }

    @Test
    public void testGetNetworkByName() {
        fail("Not yet implemented");
    }

    @Test
    public void testGetSubnetById() {
        fail("Not yet implemented");
    }

    @Test
    public void testListComputePortsByNetwork() {
        fail("Not yet implemented");
    }

    @Test
    public void testGetNetworkPortRouterDeviceId() {
        fail("Not yet implemented");
    }

    @Test
    public void testListPortsBySubnet() {
        fail("Not yet implemented");
    }

    @Test
    public void testGetPortById() {
        fail("Not yet implemented");
    }

    @Test
    public void testDeletePortById() {
        fail("Not yet implemented");
    }

    @Test
    public void testGetSecurityGroupById() {
        fail("Not yet implemented");
    }

    @Test
    public void testGetVmIdByPortId() {
        fail("Not yet implemented");
    }

    @Test
    public void testGetVmIdByMacAddress() {
        fail("Not yet implemented");
    }

    @Test
    public void testCreateSecurityGroup() {
        fail("Not yet implemented");
    }

    final static String INGRESS = "ingress";
    final static String EGRESS = "egress";
    final static String IPV4 = "IPv4";
    final static String IPV6 = "IPv6";

    @Test
    public void testAddSecurityGroupRules() {
        Openstack4JNeutron neutron = new Openstack4JNeutron(new Endpoint("10.3.240.183", "default", "admin", "admin", "admin1234", false, null));
        SecurityGroup sg = neutron.getSecurityGroupById("RegionOne", "1157c72f-2159-403c-9b94-79b7dbb9915f");
        List<SecurityGroupRule> expectedList = new ArrayList<>();
        expectedList.add(Builders.securityGroupRule().protocol(null).ethertype(IPV4).direction(INGRESS).build());
        expectedList.add(Builders.securityGroupRule().protocol(null).ethertype(IPV4).direction(INGRESS).build());
        expectedList.add(Builders.securityGroupRule().protocol(null).ethertype(IPV6).direction(INGRESS).build());

        neutron.addSecurityGroupRules(sg, "RegionOne", expectedList);
    }

    @Test
    public void testDeleteSecurityGroupById() {
        fail("Not yet implemented");
    }

    @Test
    public void testGetFloatingIpPools() {
        fail("Not yet implemented");
    }

    @Test
    public void testGetFloatingIp() {
        fail("Not yet implemented");
    }

    @Test
    public void testAssociateMgmtPortWithFloatingIp() {
        fail("Not yet implemented");
    }

    @Test
    public void testCreateFloatingIp() {
        fail("Not yet implemented");
    }

    @Test
    public void testDeleteFloatingIp() {
        fail("Not yet implemented");
    }

}
