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
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.openstack4j.api.Builders;
import org.openstack4j.api.exceptions.ResponseException;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.compute.Action;
import org.openstack4j.model.compute.Flavor;
import org.openstack4j.model.compute.HostAggregate;
import org.openstack4j.model.compute.InterfaceAttachment;
import org.openstack4j.model.compute.Server;
import org.openstack4j.model.compute.builder.ServerCreateBuilder;
import org.openstack4j.model.compute.ext.AvailabilityZone;
import org.openstack4j.model.compute.ext.Hypervisor;
import org.openstack4j.model.identity.v3.Region;
import org.openstack4j.model.network.Port;
import org.osc.sdk.manager.element.ApplianceBootstrapInformationElement;
import org.osc.sdk.manager.element.ApplianceBootstrapInformationElement.BootstrapFileElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Maps;

public class Openstack4JNova extends BaseOpenstack4jApi {

    private static final Logger log = LoggerFactory.getLogger(Openstack4JNova.class);

    private Set<String> regions;
    private static final String OPENSTACK_NAME_PROPERTY = "name";

    public static final class CreatedServerDetails {

        private String serverId;
        private String ingressInspectionPortId;
        private String ingressInspectionMacAddr;
        private String egressInspectionPortId;
        private String egressInspectionMacAddr;

        CreatedServerDetails(String serverId, String ingressInspectionPortId, String ingressInspectionMacAddr,
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

    public Openstack4JNova(Endpoint endPoint) {
        super(endPoint);
    }

    public Set<String> listRegions() {
        if (CollectionUtils.isEmpty(this.regions)) {
            List<? extends Region> endpoints = getOs().identity().regions().list();
            this.regions = endpoints.stream().map(Region::getId).collect(Collectors.toSet());
        }
        if (CollectionUtils.isEmpty(this.regions)) {
            log.warn("No regions found in the environment!");
        }
        return this.regions;
    }

    // Server APIS
    public CreatedServerDetails createServer(String region, String availabilityZone, String svaName, String imageRef,
                                             String flavorRef, ApplianceBootstrapInformationElement bootstrapInfo, String mgmtNetworkUuid,
                                             String inspectionNetworkUuid, boolean additionalNicForInspection, String sgName) {
        getOs().useRegion(region);

        Port ingressInspectionPort = getOs().networking().port().create(Builders.port().networkId(inspectionNetworkUuid).build());

        Port egressInspectionPort;
        if (additionalNicForInspection) {
            egressInspectionPort = getOs().networking().port().create(Builders.port().networkId(inspectionNetworkUuid).build());
        } else {
            egressInspectionPort = ingressInspectionPort;
        }

        try {
            ServerCreateBuilder sc = Builders.server().name(svaName).flavor(flavorRef).image(imageRef);

            sc.networks(Collections.singletonList(mgmtNetworkUuid));
            sc.addNetworkPort(ingressInspectionPort.getId());

            if (additionalNicForInspection) {
                sc.addNetworkPort(egressInspectionPort.getId());
            }

            for (BootstrapFileElement file : bootstrapInfo.getBootstrapFiles()) {
                sc.addPersonality(file.getName(), new String(file.getContent(), StandardCharsets.UTF_8));
            }
            sc.configDrive(true);

            if (sgName != null) {
                sc.addSecurityGroup(sgName);
            }

            if (availabilityZone != null) {
                sc.availabilityZone(availabilityZone);
            }

            Server server = getOs().compute().servers().boot(sc.build());

            log.info("Server '" + svaName + "' Created with Id: " + server.getId());

            return new CreatedServerDetails(server.getId(), ingressInspectionPort.getId(),
                    ingressInspectionPort.getMacAddress(), egressInspectionPort.getId(),
                    egressInspectionPort.getMacAddress());
        } catch (Exception e) {
            // Server creating failed for some reason, delete the inspection port created
            ActionResponse deleteResponse = getOs().networking().port().delete(ingressInspectionPort.getId());
            if (!deleteResponse.isSuccess()) {
                log.warn("Cannot delete ingress inspection port: " + deleteResponse.getFault());
            }
            if (additionalNicForInspection) {
                // If we have multiple interfaces, egress and ingress ports are different else they are the same)
                ActionResponse deleteEgressInspPort = getOs().networking().port().delete(egressInspectionPort.getId());
                if (!deleteEgressInspPort.isSuccess()) {
                    log.warn("Cannot delete egress inspection port: " + deleteResponse.getFault());
                }
            }
            throw e;
        }
    }

    public Server getServer(String region, String serverId) {
        getOs().useRegion(region);
        return getOs().compute().servers().get(serverId);
    }

    public List<? extends Server> listServers(String region) {
        getOs().useRegion(region);
        List<? extends Server> serverList = getOs().compute().servers().list();
        if (CollectionUtils.isEmpty(serverList)) {
            log.info("No servers found in region: " + region);
        }
        return serverList;
    }

    public boolean startServer(String region, String serverId) {
        getOs().useRegion(region);
        ActionResponse action = getOs().compute().servers().action(serverId, Action.START);
        return action.isSuccess();
    }

    public Server getServerByName(String region, String name) {
        getOs().useRegion(region);
        Map<String, String> filter = Maps.newHashMap();
        filter.put(OPENSTACK_NAME_PROPERTY, "^" + name + "$");

        List<? extends Server> servers = getOs().compute().servers().list(filter);
        return (servers.size() == 1) ? servers.get(0) : null;
    }

    public boolean terminateInstance(String region, String serverId) {
        getOs().useRegion(region);
        ActionResponse actionResponse = getOs().compute().servers().delete(serverId);
        return actionResponse.isSuccess();
    }

    // Flavor APIS
    public String createFlavor(String region, String id, String flavorName, int diskInGb, int ramInMB, int cpus) {
        getOs().useRegion(region);
        Flavor flavor = getOs().compute().flavors().create(
                Builders.flavor().disk(diskInGb).ram(ramInMB).vcpus(cpus).name(flavorName).id(id).build()
        );
        return flavor.getId();
    }

    public Flavor getFlavorById(String region, String id) {
        getOs().useRegion(region);
        return getOs().compute().flavors().get(id);
    }

    public void deleteFlavorById(String region, String id) {
        getOs().useRegion(region);
        ActionResponse actionResponse = getOs().compute().flavors().delete(id);
        if (!actionResponse.isSuccess()) {
            String message = String.format("Deleting flavor Id: %s failed. Error: %s", id, actionResponse.getFault());
            log.warn(message);
            throw new ResponseException(message, actionResponse.getCode());
        }
    }

    // Host Aggregates
    public List<? extends HostAggregate> listHostAggregates(String region) {
        getOs().useRegion(region);
        List<? extends HostAggregate> haList = getOs().compute().hostAggregates().list();
        if (CollectionUtils.isEmpty(haList)) {
            log.info("No Host Aggregates found in region: " + region);
        }
        return haList;
    }

    public HostAggregate getHostAggregateById(String region, String id) {
        getOs().useRegion(region);
        HostAggregate hostAggregate = getOs().compute().hostAggregates().get(id);
        if (hostAggregate == null) {
            log.info(String.format("Unable to find Host Aggregate with Id: %s in region: %s ", id, region));
        }
        return hostAggregate;
    }

    // Interface Attachment
    public List<? extends InterfaceAttachment> getVmAttachedNetworks(String region, String serverId) {
        getOs().useRegion(region);
        List<? extends InterfaceAttachment> interfaceList = getOs().compute().servers().interfaces().list(serverId);
        if (CollectionUtils.isEmpty(interfaceList)) {
            log.info(String.format("Unable to find Networks attached to Server Id: %s in region: %s ", serverId,
                    region));
        }
        return interfaceList;
    }

    // Availability Zone
    public List<? extends AvailabilityZone> listAvailabilityZones(String region) {
        getOs().useRegion(region);
        List<? extends AvailabilityZone> azList = getOs().compute().zones().list();
        if (CollectionUtils.isEmpty(azList)) {
            log.info("No Availability Zones found in region: " + region);
        }
        return azList;
    }

    public List<? extends AvailabilityZone> getAvailabilityZonesDetail(String region) throws Exception {
        getOs().useRegion(region);
        List<? extends AvailabilityZone> azDetailList = getOs().compute().zones().list(true);
        if (CollectionUtils.isEmpty(azDetailList)) {
            log.info("No Detailed Availability Zones found in region: " + region);
        }
        return azDetailList;
    }

    public static HostAvailabilityZoneMapping getMapping(List<? extends AvailabilityZone> availabilityZones)
            throws Exception {
        return new HostAvailabilityZoneMapping(availabilityZones);
    }

    public Set<String> getComputeHosts(String region) throws Exception {
        getOs().useRegion(region);
        List<? extends Hypervisor> list = getOs().compute().hypervisors().list();
        if (CollectionUtils.isEmpty(list)) {
            log.warn("No compute hosts found in region: " + region);
        }
        return list.stream().map(Hypervisor::getHypervisorHostname).collect(Collectors.toSet());
    }

    @Override
    public void close() throws IOException {
        if (getOs() != null) {
            getOs().removeRegion();
        }
        super.close();
    }
}
