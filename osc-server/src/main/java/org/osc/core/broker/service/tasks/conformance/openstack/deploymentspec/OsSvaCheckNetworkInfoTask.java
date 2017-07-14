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
package org.osc.core.broker.service.tasks.conformance.openstack.deploymentspec;

import java.util.List;
import java.util.Set;

import javax.persistence.EntityManager;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.openstack4j.model.network.IP;
import org.openstack4j.model.network.Network;
import org.openstack4j.model.network.Port;
import org.openstack4j.model.network.Subnet;
import org.osc.core.broker.job.TaskGraph;
import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.model.entities.appliance.DistributedApplianceInstance;
import org.osc.core.broker.model.entities.virtualization.openstack.DeploymentSpec;
import org.osc.core.broker.rest.client.openstack.openstack4j.Endpoint;
import org.osc.core.broker.rest.client.openstack.openstack4j.Openstack4JNeutron;
import org.osc.core.broker.service.persistence.DistributedApplianceInstanceEntityMgr;
import org.osc.core.broker.service.persistence.OSCEntityManager;
import org.osc.core.broker.service.tasks.TransactionalMetaTask;
import org.osgi.service.component.annotations.Component;

@Component(service = OsSvaCheckNetworkInfoTask.class)
public class OsSvaCheckNetworkInfoTask extends TransactionalMetaTask {

    private static final Logger LOG = Logger.getLogger(OsSvaCheckNetworkInfoTask.class);

    private TaskGraph tg;
    private DistributedApplianceInstance dai;

    public OsSvaCheckNetworkInfoTask create(DistributedApplianceInstance dai) {
        OsSvaCheckNetworkInfoTask task = new OsSvaCheckNetworkInfoTask();
        task.dai = dai;
        task.dbConnectionManager = this.dbConnectionManager;
        task.txBroadcastUtil = this.txBroadcastUtil;

        return task;
    }

    @Override
    public void executeTransaction(EntityManager em) throws Exception {
        this.tg = new TaskGraph();
        this.dai = DistributedApplianceInstanceEntityMgr.findById(em, this.dai.getId());
        DeploymentSpec ds = this.dai.getDeploymentSpec();

        Endpoint endPoint = new Endpoint(ds);
        try (Openstack4JNeutron neutron = new Openstack4JNeutron(endPoint)) {
            Network mgmtNetwork = neutron.getNetworkById(ds.getRegion(), ds.getManagementNetworkId());

            if (mgmtNetwork.getSubnets() == null || mgmtNetwork.getSubnets().isEmpty()) {
                throw new IllegalStateException(String.format(
                        "The network %s does not contain a subnet.",
                        ds.getManagementNetworkId()));
            }

            if (mgmtNetwork.getSubnets().size() > 1) {
                throw new IllegalStateException(String.format(
                        "The management network %s should have only one subnet, but it has %s.",
                        ds.getManagementNetworkId(),
                        mgmtNetwork.getSubnets().size()));
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
                mgmtIpAddress = getMgmtIpAddress(ds, mgmtNetwork, mgmtSubnet, this.dai, neutron);
                this.dai.setIpAddress(mgmtIpAddress);
                LOG.info("Retrieved mgmt IP address" + mgmtIpAddress);
            } else if (this.dai.getMgmtOsPortId() == null) {
                Port mgmtPort = getMgmtPort(ds, mgmtNetwork, mgmtSubnet, this.dai, neutron);
                this.dai.setMgmtOsPortId(mgmtPort.getId());
            }
            String mgmtSubnetPrefixLength = mgmtSubnet.getCidr().split("/")[1];

            this.dai.setMgmtGateway(mgmtSubnet.getGateway());
            this.dai.setMgmtSubnetPrefixLength(mgmtSubnetPrefixLength);
            this.dai.setMgmtIpAddress(mgmtIpAddress);

            LOG.info(String.format(
                    "Updating the DAI %s, with mgmtIpAddress %s, mgmtSubnetPrefixLength %s, mgmtGateway %s.",
                    this.dai.getId(),
                    mgmtIpAddress,
                    mgmtSubnetPrefixLength,
                    mgmtSubnet.getGateway()));

            OSCEntityManager.update(em, this.dai, this.txBroadcastUtil);
        }
    }

    private Port getMgmtPort(DeploymentSpec ds, Network mgmgNetwork, Subnet mgmtSubnet, DistributedApplianceInstance dai, Openstack4JNeutron neutron) {
        List<Port> ports = neutron.listPortsBySubnet(ds.getRegion(), ds.getProjectId(), mgmgNetwork.getId(), mgmtSubnet.getId(), false);
        for (Port port : ports) {
            if (port.getDeviceId().equals(dai.getOsServerId())) {
                return port;
            }
        }
        throw new IllegalStateException(String.format("No management port found for dai %s.", dai.getName()));
    }

    private String getMgmtIpAddress(DeploymentSpec ds, Network mgmtNetwork, Subnet mgmtSubnet, DistributedApplianceInstance dai, Openstack4JNeutron neutron) {
        String mgmtPortId = dai.getMgmtOsPortId();
        Port mgmtPort;

        // In case the DAI does not have a mgmtPortId yet, for instance in database upgrade scenarios.
        if (mgmtPortId == null) {
            mgmtPort = getMgmtPort(ds, mgmtNetwork, mgmtSubnet, this.dai, neutron);
            dai.setMgmtOsPortId(mgmtPort.getId());
            dai.setMgmtMacAddress(mgmtPort.getMacAddress());
        } else {
            mgmtPort = neutron.getPortById(ds.getRegion(), mgmtPortId);
        }

        if (mgmtPort == null) {
            throw new IllegalStateException(String.format("No management port found for dai %s.", dai.getName()));
        }

        IP ip = (IP) mgmtPort.getFixedIps().toArray()[0];
        return ip.getIpAddress();
    }


    @Override
    public String getName() {
        return String.format("Checking the network information for the distributed appliance instance '%s'",
                this.dai != null ? this.dai.getName() : "dai is null");
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
