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
package org.osc.core.broker.util;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.vmware.vim25.ws.Client;
import com.vmware.vim25.InvalidProperty;
import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vim25.RuntimeFault;
import com.vmware.vim25.mo.Datacenter;
import com.vmware.vim25.mo.Folder;
import com.vmware.vim25.mo.HostSystem;
import com.vmware.vim25.mo.InventoryNavigator;
import com.vmware.vim25.mo.ManagedEntity;
import com.vmware.vim25.mo.ServiceInstance;
import com.vmware.vim25.mo.VirtualMachine;
import com.vmware.vim25.mo.util.MorUtil;

public class VimUtils {

    private static final int DEFAULT_READ_TIMEOUT = 60 * 1000;
    private static final int DEFAULT_CONNECTION_TIMEOUT = 5 * 1000;
    private static final Logger log = Logger.getLogger(VimUtils.class);

    private ServiceInstance serviceInstance;

    private String hostname;
    private String username;
    private String password;
    private boolean isConnected = false;

    public VimUtils(final String hostname, final String username, final String password) throws Exception {
        this.hostname = hostname;
        this.username = username;
        this.password = password;
        connect();
    }

    public static URL getServiceURL(String hostname) throws MalformedURLException, URISyntaxException {
        URI testUri = new URI("https", hostname, "/sdk", null);
        return testUri.toURL();
    }

    public synchronized void connect() throws Exception {
        try {
            if (this.isConnected) {
                verifyConnected();
                return;
            }

            log.info("Connecting to vCenter...");

            this.serviceInstance = new ServiceInstance(VimUtils.getServiceURL(this.hostname), this.username, this.password, false);
            Client wsc = this.serviceInstance.getServerConnection().getVimService().getWsc();
            wsc.setConnectTimeout(DEFAULT_CONNECTION_TIMEOUT);
            wsc.setReadTimeout(DEFAULT_READ_TIMEOUT);

            log.info("Successfully connected to vCenter");
            this.isConnected = true;
        } catch (NullPointerException e) {
            // Connect throws NullPointerException for vCenter IP 0.0.0.0
            // Rethrowing RemoteException and log standard message for Invalid IP
            throw new RemoteException("Connect to vCenter failed", e);
        }
    }

    public synchronized void disconnect() {
        if (this.isConnected) {
            log.info("Disconnecting from vCenter");
            try {
                log.info("Successfully disconnected from vCenter");
            } catch (Exception e) {
                log.warn("Failed to logout - " + e.getMessage());
            }
            this.isConnected = false;
        }
    }

    public VirtualMachine findVmByIp(String vmIP) {
        return findVmByIp(null, vmIP);
    }

    public VirtualMachine findVmByIp(String datacenterName, String vmIP) {
        try {
            connect();
            Datacenter dc = null;
            if (datacenterName != null) {
                dc = (Datacenter) this.serviceInstance.getSearchIndex().findByInventoryPath(datacenterName);
            }
            VirtualMachine vm = (VirtualMachine) this.serviceInstance.getSearchIndex().findByIp(dc, vmIP, true);
            return vm;

        } catch (Exception e) {
            log.error("Error", e);
            disconnect();
        }

        return null;
    }

    public List<ManagedEntity> enumDatacenters() {
        return enumObjectByType("Datacenter");
    }

    public List<ManagedEntity> enumDatastores() {
        return enumObjectByType("Datastore");
    }

    public List<ManagedEntity> enumPortGroups() {
        List<ManagedEntity> list = new ArrayList<ManagedEntity>();
        for (ManagedEntity network : enumObjectByType("Network")) {
            if (network.getMOR().getType().equals("DistributedVirtualPortgroup")) {
                list.add(network);
            }
        }
        return list;
    }

    public List<ManagedEntity> enumNetworks() {
        return enumObjectByType("Network");
    }

    public List<ManagedEntity> enumClusters() {
        return enumObjectByType("ClusterComputeResource");
    }

    private List<ManagedEntity> enumObjectByType(String objectType) {
        List<ManagedEntity> list = new ArrayList<ManagedEntity>();
        try {
            connect();
            Folder rootFolder = this.serviceInstance.getRootFolder();

            ManagedEntity[] mos = new InventoryNavigator(rootFolder).searchManagedEntities(new String[][] { {
                    objectType, "name" }, }, true);

            for (ManagedEntity managedEntity : mos) {
                list.add(managedEntity);
            }
            return list;

        } catch (Exception e) {
            disconnect();
            log.error("Error", e);
        }

        return list;
    }

    public Map<String, ManagedObjectReference> enumMoRefsByType(String objectType) {
        try {
            connect();
            ManagedEntity[] mos = new InventoryNavigator(this.serviceInstance.getRootFolder())
                    .searchManagedEntities(objectType);

            Map<String, ManagedObjectReference> tgtMoref = new HashMap<String, ManagedObjectReference>();

            for (ManagedEntity me : mos) {
                tgtMoref.put(me.getMOR().getVal(), me.getMOR());
            }

            return tgtMoref;

        } catch (Exception e) {
            log.error("Error", e);
            disconnect();
        }

        return null;
    }

    public String findDatacenterByName(String datacenterName) throws InvalidProperty, RuntimeFault, RemoteException {
        return findObjectByTypeAndName("Datacenter", datacenterName).getMOR().getVal();
    }

    public String findDatastoreIdByName(String datastoreName) {
        return findObjectByTypeAndName("Datastore", datastoreName).getMOR().getVal();
    }

    public String findNetworkIdByName(String networkName) {
        return findObjectByTypeAndName("Network", networkName).getMOR().getVal();
    }

    public String findClusterIdByName(String clusterName) {
        return findObjectByTypeAndName("ClusterComputeResource", clusterName).getMOR().getVal();
    }

    private ManagedEntity findObjectByTypeAndName(String objectType, String objectName) {
        try {
            connect();
            ManagedEntity mos = new InventoryNavigator(this.serviceInstance.getRootFolder()).searchManagedEntity(
                    objectType, objectName);
            return mos;
        } catch (Exception e) {
            disconnect();
            log.error("Error", e);
        }

        return null;
    }

    public void verifyConnected() throws Exception {
        try {
            Calendar ct = this.serviceInstance.currentTime();
            log.info("Server current time: " + ct.toString());

        } catch (Exception e) {

            log.warn("Server connection lost. " + e.getMessage() + " Reconnecting...");
            disconnect();
            connect();
        }
    }

    public HostSystem getVmHost(VirtualMachine vm) {
        ManagedObjectReference host = vm.getSummary().getRuntime().getHost();
        HostSystem targetHost = new HostSystem(this.serviceInstance.getServerConnection(), host);
        return targetHost;
    }

    public VirtualMachine findVmByName(String vmName) {

        try {
            connect();

            VirtualMachine vm = (VirtualMachine) new InventoryNavigator(this.serviceInstance.getRootFolder())
                    .searchManagedEntity("VirtualMachine", vmName);
            return vm;

        } catch (Exception e) {

            disconnect();
            log.error("Error", e);
        }

        return null;
    }

    public VirtualMachine findVmById(String vmId) {
        try {
            connect();

            ManagedObjectReference mor = new ManagedObjectReference();
            mor.setVal(vmId);
            mor.setType("VirtualMachine");
            VirtualMachine vm = (VirtualMachine) MorUtil.createExactManagedObject(
                    this.serviceInstance.getServerConnection(), mor);
            return vm;

        } catch (Exception e) {

            disconnect();
            log.error("Error", e);
        }

        return null;
    }

    public VirtualMachine findVmByInstanceUuid(String vmUuid) {
        try {
            connect();

            VirtualMachine vm = (VirtualMachine) this.serviceInstance.getSearchIndex().findByUuid(null, vmUuid, true,
                    true);
            return vm;

        } catch (Exception e) {
            log.error("Error", e);
            disconnect();
        }

        return null;
    }

}
