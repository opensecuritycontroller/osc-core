package org.osc.core.broker.rest.client.openstack.jcloud;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jclouds.openstack.nova.v2_0.domain.regionscoped.AvailabilityZoneDetails;
import org.osc.core.broker.service.exceptions.VmidcException;
import org.osc.core.broker.service.tasks.conformance.openstack.deploymentspec.OpenstackUtil;

/**
 * Provides mapping information between host name and availability zone. Host name could either be a FQDN or just the
 * hostname. This class attempts to figure out the mapping given either of the two.
 */
public class HostAvailabilityZoneMapping {

    public static class HostAzInfo {
        private String hostName;
        private String availabilityZone;

        public HostAzInfo(String hostName, String availabilityZone) {
            this.hostName = hostName;
            this.availabilityZone = availabilityZone;
        }

        public String getHostName() {
            return this.hostName;
        }

        public String getAvailabilityZone() {
            return this.availabilityZone;
        }
    }

    private List<AvailabilityZoneDetails> availabilityZones = new ArrayList<>();
    private Map<String, String> hostAvailabilityZoneMap;
    private Map<String, Set<String>> availabilityZoneHostsMap;

    HostAvailabilityZoneMapping(List<AvailabilityZoneDetails> availabilityZones) {
        this.availabilityZones.addAll(availabilityZones);
        initAvailabilityZoneHostsMap();
        initHostAvailabilityZoneMap();
    }

    public Set<String> getHosts(String azName) {
        Set<String> hostSet = this.availabilityZoneHostsMap.get(azName);
        if (hostSet == null) {
            return new HashSet<>();
        }
        return new HashSet<>(hostSet);
    }

    public Set<String> getAvailabilityZones() {
        return new HashSet<>(this.availabilityZoneHostsMap.keySet());
    }

    /**
     * Gets the host availability zone information.
     *
     * @param hostName the FQDN Host or just host name
     * @return availability zone of the hosts
     * @throws VmidcException if unable to find an availability zone for the host
     */
    public String getHostAvailibilityZone(String hostName) throws VmidcException {
        return getHostAvailibilityZoneInfo(hostName).getAvailabilityZone();
    }

    /**
     * Gets the host availability zone information. Openstack might have either the FQDN or just simple host name
     * for a given host, we try to find the AZ by using either of the names AND return the name present in the map.
     *
     * @param hostName the FQDN Host or just host name
     * @return availability zone of the host and the name of the host represented in the availibility zone api
     * @throws VmidcException if unable to find an availability zone for the host
     */
    public HostAzInfo getHostAvailibilityZoneInfo(String hostName) throws VmidcException {
        String az = this.hostAvailabilityZoneMap.get(hostName);
        if (az == null) {
            hostName = OpenstackUtil.normalizeHostName(hostName);
            az = this.hostAvailabilityZoneMap.get(hostName);
        }
        if (az == null) {
            throw new VmidcException("Unable to determine availability zone for host '" + hostName + "'");
        }
        return new HostAzInfo(hostName, az);
    }

    private void initAvailabilityZoneHostsMap() {
        this.availabilityZoneHostsMap = new HashMap<>();
        for (AvailabilityZoneDetails az : this.availabilityZones) {
            this.availabilityZoneHostsMap.put(az.getName(), az.getHosts().keySet());
        }
    }

    private void initHostAvailabilityZoneMap() {
        this.hostAvailabilityZoneMap = new HashMap<>();
        for (AvailabilityZoneDetails az : this.availabilityZones) {
            for (String hostName : az.getHosts().keySet()) {
                this.hostAvailabilityZoneMap.put(hostName, az.getName());
            }
        }
    }
}
