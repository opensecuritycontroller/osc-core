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
package org.osc.core.broker.rest.client.openstack.jcloud;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.jclouds.openstack.neutron.v2.NeutronApi;
import org.jclouds.openstack.neutron.v2.domain.Port;
import org.jclouds.openstack.neutron.v2.domain.Port.CreatePort;
import org.jclouds.openstack.neutron.v2.features.PortApi;
import org.jclouds.openstack.nova.v2_0.NovaApi;
import org.jclouds.openstack.nova.v2_0.domain.Flavor;
import org.jclouds.openstack.nova.v2_0.domain.FloatingIP;
import org.jclouds.openstack.nova.v2_0.domain.FloatingIPPool;
import org.jclouds.openstack.nova.v2_0.domain.Host;
import org.jclouds.openstack.nova.v2_0.domain.HostAggregate;
import org.jclouds.openstack.nova.v2_0.domain.InterfaceAttachment;
import org.jclouds.openstack.nova.v2_0.domain.Network;
import org.jclouds.openstack.nova.v2_0.domain.Server;
import org.jclouds.openstack.nova.v2_0.domain.ServerCreated;
import org.jclouds.openstack.nova.v2_0.domain.regionscoped.AvailabilityZone;
import org.jclouds.openstack.nova.v2_0.domain.regionscoped.AvailabilityZoneDetails;
import org.jclouds.openstack.nova.v2_0.extensions.AttachInterfaceApi;
import org.jclouds.openstack.nova.v2_0.extensions.AvailabilityZoneApi;
import org.jclouds.openstack.nova.v2_0.extensions.FloatingIPApi;
import org.jclouds.openstack.nova.v2_0.extensions.FloatingIPPoolApi;
import org.jclouds.openstack.nova.v2_0.extensions.HostAdministrationApi;
import org.jclouds.openstack.nova.v2_0.extensions.HostAggregateApi;
import org.jclouds.openstack.nova.v2_0.features.FlavorApi;
import org.jclouds.openstack.nova.v2_0.features.ServerApi;
import org.jclouds.openstack.nova.v2_0.options.CreateServerOptions;
import org.jclouds.openstack.v2_0.domain.Resource;
import org.jclouds.openstack.v2_0.options.PaginationOptions;
import org.jclouds.rest.ResourceNotFoundException;
import org.osc.sdk.manager.element.ApplianceBootstrapInformationElement;
import org.osc.sdk.manager.element.ApplianceBootstrapInformationElement.BootstrapFileElement;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.ArrayListMultimap;

public class JCloudNova extends BaseJCloudApi {

    private static final Logger log = Logger.getLogger(JCloudNova.class);
    private static final String NOVA_SERVICE_COMPUTE = "compute";

    private static final String OPENSTACK_SERVICE_NOVA = "openstack-nova";
    private NovaApi novaApi;
    private NeutronApi neutronApi;
    private Set<String> regions;

    public static final class CreatedServerDetails {

        private String serverId;
        private String ingressInspectionPortId;
        private String ingressInspectionMacAddr;
        private String egressInspectionPortId;
        private String egressInspectionMacAddr;

        public CreatedServerDetails(String serverId, String ingressInspectionPortId, String ingressInspectionMacAddr,
                String egressInspectionPortId, String egressInspectionMacAddr) {
            this.serverId = serverId;
            this.ingressInspectionPortId = ingressInspectionPortId;
            this.ingressInspectionMacAddr = ingressInspectionMacAddr;
            this.egressInspectionPortId = egressInspectionPortId;
            this.egressInspectionMacAddr = egressInspectionMacAddr;
        }

        public String getServerId() {
            return this.serverId;
        }

        public String getIngressInspectionPortId() {
            return this.ingressInspectionPortId;
        }

        public String getIngressInspectionMacAddr() {
            return this.ingressInspectionMacAddr;
        }

        public String getEgressInspectionPortId() {
            return this.egressInspectionPortId;
        }

        public String getEgressInspectionMacAddr() {
            return this.egressInspectionMacAddr;
        }
    }

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

    public JCloudNova(Endpoint endPoint) {
        super(endPoint);
        this.novaApi = JCloudUtil.buildApi(NovaApi.class, OPENSTACK_SERVICE_NOVA, endPoint);
    }

    public Set<String> listRegions() {
        if (this.regions == null) {
            this.regions = this.novaApi.getConfiguredRegions();
        }
        return this.regions;
    }

    // Server APIS
    public CreatedServerDetails createServer(String region, String availabilityZone, String svaName, String imageRef,
            String flavorRef, ApplianceBootstrapInformationElement bootstrapInfo, String mgmtNetworkUuid,
            String inspectionNetworkUuid, boolean additionalNicForInspection, String sgName) {
        initNeutronApi();

        PortApi portApi = this.neutronApi.getPortApi(region);
        Port ingressInspectionPort = portApi.create(CreatePort.createBuilder(inspectionNetworkUuid).build());
        Port egressInspectionPort;
        if (additionalNicForInspection) {
            egressInspectionPort = portApi.create(CreatePort.createBuilder(inspectionNetworkUuid).build());
        } else {
            egressInspectionPort = ingressInspectionPort;
        }
        ServerCreated server = null;
        try {
            List<Network> neutronNetworks = new ArrayList<>();
            neutronNetworks.add(Network.builder().networkUuid(mgmtNetworkUuid).build());
            neutronNetworks.add(Network.builder().portUuid(ingressInspectionPort.getId()).build());
            if (additionalNicForInspection) {
                neutronNetworks.add(Network.builder().portUuid(egressInspectionPort.getId()).build());
            }

            CreateServerOptions options = new CreateServerOptions();
            options.configDrive(true);
            for (BootstrapFileElement file : bootstrapInfo.getBootstrapFiles()) {
                options.writeFileToPath(file.getContent(), file.getName());
            }
            options.novaNetworks(neutronNetworks);

            if (sgName!=null) {
                options.securityGroupNames(sgName);
            }

            if (availabilityZone != null) {
                options.availabilityZone(availabilityZone);
            }

            ServerApi serverApi = this.novaApi.getServerApi(region);

            server = serverApi.create(svaName, imageRef, flavorRef, options);
            log.info("Server '" + svaName + "' Created with Id: " + server.getId());
        } catch (Exception e) {
            // Server creating failed for some reason, delete the inspection port created
            portApi.delete(ingressInspectionPort.getId());
            if (additionalNicForInspection) {
                // If we have multiple interfaces, egress and ingress ports are different else they are the same)
                portApi.delete(egressInspectionPort.getId());
            }
            throw e;
        }

        return new CreatedServerDetails(server.getId(), ingressInspectionPort.getId(),
                ingressInspectionPort.getMacAddress(), egressInspectionPort.getId(),
                egressInspectionPort.getMacAddress());
    }

    public Server getServer(String region, String serverId) {
        return this.novaApi.getServerApi(region).get(serverId);
    }

    public List<Resource> listServers(String region) {
        List<Resource> servers = new ArrayList<Resource>();
        ServerApi serverApi = this.novaApi.getServerApi(region);
        for (Resource server : serverApi.list().concat()) {
            servers.add(server);
        }
        return servers;
    }

    public void startServer(String region, String serverId) {
        ServerApi serverApi = this.novaApi.getServerApi(region);
        serverApi.start(serverId);
    }

    public Server getServerByName(String region, String name) {
        String regExName = "^" + name + "$";
        ArrayListMultimap<String, String> queryParam = ArrayListMultimap.create();
        queryParam.put("name", regExName);

        ServerApi serverApi = this.novaApi.getServerApi(region);
        PaginationOptions options = new PaginationOptions().queryParameters(queryParam);

        Server sva = serverApi.listInDetail(options).first().orNull();
        return sva;
    }

    public boolean terminateInstance(String region, String serverId) {
        return this.novaApi.getServerApi(region).delete(serverId);
    }

    // Floating IP API
    public List<? extends FloatingIPPool> getFloatingIpPools(String region) throws Exception {
        FloatingIPPoolApi floatingIPPoolApi = getOptionalOrThrow(this.novaApi.getFloatingIPPoolApi(region),
                "Floating IP Pool API");

        return floatingIPPoolApi.list().toList();
    }

    public FloatingIP getFloatingIp(String region, String id) {
        if (id == null) {
            return null;
        }
        FloatingIPApi floatingIpApi = getOptionalOrThrow(this.novaApi.getFloatingIPApi(region), "Floating IP Pool API");

        return floatingIpApi.get(id);
    }

    public void allocateFloatingIpToServer(String region, String serverId, FloatingIP floatingIp) {
        FloatingIPApi floatingIpApi = getOptionalOrThrow(this.novaApi.getFloatingIPApi(region), "Floating IP Pool API");

        floatingIpApi.addToServer(floatingIp.getIp(), serverId);
    }

    // Flavor APIS
    public String createFlavor(String region, String id, String flavorName, int diskInGb, int ramInMB, int cpus) {
        FlavorApi flavorApi = this.novaApi.getFlavorApi(region);

        Flavor newFlavor = Flavor.builder().disk(diskInGb).ram(ramInMB).vcpus(cpus).name(flavorName).id(id).build();

        return flavorApi.create(newFlavor).getId();
    }

    public Flavor getFlavorById(String region, String id) {
        FlavorApi flavorApi = this.novaApi.getFlavorApi(region);
        return flavorApi.get(id);
    }

    public void deleteFlavorById(String region, String id) {
        FlavorApi flavorApi = this.novaApi.getFlavorApi(region);
        try {
            flavorApi.delete(id);
        } catch (ResourceNotFoundException ex) {
            log.warn("Image Id: " + id + " not found.");
        }
    }

    // Host Aggregates
    public List<HostAggregate> listHostAggregates(String region) {
        return getHostAggregateApi(region).list().toList();
    }

    public HostAggregate getHostAggregateById(String region, String id) {
        return getHostAggregateApi(region).get(id);
    }

    // Interface Attachment
    public List<InterfaceAttachment> getVmAttachedNetworks(String region, String vmId) {
        return getAttachIntefaceApi(region).list(vmId).toList();
    }

    // Availability Zone
    public List<AvailabilityZone> listAvailabilityZones(String region) {
        return getAvailabilityZoneApi(region).listAvailabilityZones().toList();
    }

    public List<AvailabilityZoneDetails> getAvailabilityZonesDetail(String region) throws Exception {
        return getAvailabilityZoneApi(region).listInDetail().toList();
    }

    public static HostAvailabilityZoneMapping getMapping(List<AvailabilityZoneDetails> availabilityZones)
            throws Exception {
        return new HostAvailabilityZoneMapping(availabilityZones);
    }

    public Set<String> getComputeHosts(String region) throws Exception {
        HostAdministrationApi hostApi = getHostAdministrationApi(region);
        return hostApi.list().filter(new Predicate<Host>() {

            @Override
            public boolean apply(Host input) {
                return input.getService().equals(NOVA_SERVICE_COMPUTE);
            }
        }).transform(new Function<Host, String>() {

            @Override
            public String apply(Host input) {
                return input.getName();
            }
        }).toSet();
    }

    NovaApi getNovaApi() {
        return this.novaApi;
    }

    private HostAggregateApi getHostAggregateApi(String region) {
        return getOptionalOrThrow(this.novaApi.getHostAggregateApi(region), "Host Aggregate Api");
    }

    private AttachInterfaceApi getAttachIntefaceApi(String region) {
        return getOptionalOrThrow(this.novaApi.getAttachInterfaceApi(region), "Attach Inteface Api");
    }

    private AvailabilityZoneApi getAvailabilityZoneApi(String region) {
        return getOptionalOrThrow(this.novaApi.getAvailabilityZoneApi(region), "Availability Zone Api");
    }

    private HostAdministrationApi getHostAdministrationApi(String region) {
        return getOptionalOrThrow(this.novaApi.getHostAdministrationApi(region), "Host Administration Api");
    }

    private void initNeutronApi() {
        if (this.neutronApi == null) {
            this.neutronApi = JCloudUtil.buildApi(NeutronApi.class, JCloudNeutron.OPENSTACK_SERVICE_NEUTRON,
                    this.endPoint);
        }
    }

    @Override
    protected List<? extends Closeable> getApis() {
        return Arrays.asList(this.novaApi, this.neutronApi);
    }

}
