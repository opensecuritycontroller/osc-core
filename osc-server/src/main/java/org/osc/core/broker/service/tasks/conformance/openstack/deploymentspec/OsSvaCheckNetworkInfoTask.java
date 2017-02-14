package org.osc.core.broker.service.tasks.conformance.openstack.deploymentspec;

import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.jclouds.openstack.neutron.v2.domain.IP;
import org.jclouds.openstack.neutron.v2.domain.Network;
import org.jclouds.openstack.neutron.v2.domain.Port;
import org.jclouds.openstack.neutron.v2.domain.Subnet;
import org.osc.core.broker.job.TaskGraph;
import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.model.entities.appliance.DistributedApplianceInstance;
import org.osc.core.broker.model.entities.virtualization.openstack.DeploymentSpec;
import org.osc.core.broker.rest.client.openstack.jcloud.Endpoint;
import org.osc.core.broker.rest.client.openstack.jcloud.JCloudNeutron;
import org.osc.core.broker.service.persistence.DistributedApplianceInstanceEntityMgr;
import org.osc.core.broker.service.persistence.EntityManager;
import org.osc.core.broker.service.tasks.TransactionalMetaTask;

public class OsSvaCheckNetworkInfoTask extends TransactionalMetaTask {

    private static final Logger LOG = Logger.getLogger(OsSvaCheckNetworkInfoTask.class);

    private TaskGraph tg;
    private DistributedApplianceInstance dai;

    public OsSvaCheckNetworkInfoTask(DistributedApplianceInstance dai) {
        this.dai = dai;
    }

    @Override
    public void executeTransaction(Session session) throws Exception {
        this.tg = new TaskGraph();
        this.dai = DistributedApplianceInstanceEntityMgr.findById(session, this.dai.getId());
        DeploymentSpec ds = this.dai.getDeploymentSpec();

        if (StringUtils.isBlank(ds.getManagementNetworkId())) {
            throw new IllegalStateException(String.format(
                    "The ds %s should not have an empty management network identifier",
                    ds.getId()));
        }

        Endpoint endPoint = new Endpoint(ds);
        try (JCloudNeutron neutron = new JCloudNeutron(endPoint)){
            Network mgmtNetwork = neutron.getNetworkById(ds.getRegion(), ds.getManagementNetworkId());

            if (mgmtNetwork == null) {
                throw new IllegalStateException(String.format(
                        "A network was not found for the id %s",
                        ds.getManagementNetworkId()));
            }

            if (mgmtNetwork.getSubnets() == null || mgmtNetwork.getSubnets().isEmpty()) {
                throw new IllegalStateException(String.format(
                        "The network %s does not contain a subnet.",
                        ds.getManagementNetworkId()));
            }
            // Assumed the network contains only a single subnet.
            String mgmtSubnetId = (String) mgmtNetwork.getSubnets().toArray()[0];
            Subnet mgmtSubnet = neutron.getSubnetById(ds.getRegion(), mgmtSubnetId);

            if (mgmtSubnet == null) {
                throw new IllegalStateException(String.format(
                        "A subnet was not found with the identifier %s.",
                        mgmtSubnetId));
            }

            // Use the appliance IP address as management IP,
            // if none is set then use the first IP address assigned to the to the appliance inspection port.
            String mgmtIpAddress = this.dai.getIpAddress();

            if (StringUtils.isBlank(mgmtIpAddress)) {
                if (StringUtils.isBlank(this.dai.getInspectionOsIngressPortId())) {
                    throw new IllegalStateException(String.format(
                            "The dai %s does not contain an ingress port id.",
                            this.dai.getInspectionOsIngressPortId()));
                }

                Port port = neutron.getPortById(ds.getRegion(), this.dai.getInspectionOsIngressPortId());

                if (port == null || port.getFixedIps() == null || port.getFixedIps().isEmpty()) {
                    throw new IllegalStateException(String.format(
                            "The port %s was not found or did not contain any assigned IP.",
                            this.dai.getInspectionOsIngressPortId()));
                }

                IP ip = (IP) port.getFixedIps().toArray()[0];
                mgmtIpAddress = ip.getIpAddress();

                LOG.info("Retrieved mgmg IP address" + mgmtIpAddress);
            }

            String mgmtSubnetPrefixLength = mgmtSubnet.getCidr().split("/")[1];

            this.dai.setMgmtGateway(mgmtSubnet.getGatewayIp());
            this.dai.setMgmtSubnetPrefixLength(mgmtSubnetPrefixLength);
            this.dai.setMgmtIpAddress(mgmtIpAddress);

            LOG.info(String.format(
                    "Updating the DAI %s, with mgmtIpAddress %s, mgmtSubnetPrefixLength %s, mgmtGateway %s.",
                    this.dai.getId(),
                    mgmtIpAddress,
                    mgmtSubnetPrefixLength,
                    mgmtSubnet.getGatewayIp()));

            EntityManager.update(session, this.dai);
        }
    }

    @Override
    public String getName() {
        return String.format("Checking the network information for the distributed appliance instance '%s'", this.dai.getName());
    }

    @Override
    public Set<LockObjectReference> getObjects() {
        return LockObjectReference.getObjectReferences(this.dai);
    }

    @Override
    public TaskGraph getTaskGraph() {
        return this.tg;
    }
}
