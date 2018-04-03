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

import java.util.Map;
import java.util.Set;

import javax.persistence.EntityManager;

import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.model.entities.appliance.ApplianceSoftwareVersion;
import org.osc.core.broker.model.entities.appliance.DistributedApplianceInstance;
import org.osc.core.broker.model.entities.appliance.VirtualSystem;
import org.osc.core.broker.model.entities.virtualization.VirtualizationConnector;
import org.osc.core.broker.model.entities.virtualization.openstack.DeploymentSpec;
import org.osc.core.broker.model.entities.virtualization.openstack.OsFlavorReference;
import org.osc.core.broker.model.entities.virtualization.openstack.OsImageReference;
import org.osc.core.broker.model.entities.virtualization.openstack.OsSecurityGroupReference;
import org.osc.core.broker.model.plugin.ApiFactoryService;
import org.osc.core.broker.rest.client.openstack.openstack4j.Endpoint;
import org.osc.core.broker.rest.client.openstack.openstack4j.Openstack4JNova;
import org.osc.core.broker.rest.client.openstack.openstack4j.Openstack4JNova.CreatedServerDetails;
import org.osc.core.broker.rest.client.openstack.vmidc.notification.runner.RabbitMQRunner;
import org.osc.core.broker.service.persistence.DistributedApplianceInstanceEntityMgr;
import org.osc.core.broker.service.persistence.OSCEntityManager;
import org.osc.core.broker.service.tasks.TransactionalTask;
import org.osc.sdk.manager.api.ManagerDeviceApi;
import org.osc.sdk.manager.element.ApplianceBootstrapInformationElement;
import org.osc.sdk.manager.element.BootStrapInfoProviderElement;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMap;

/**
 * Creates SVA for given dai
 */
@Component(service = OsSvaServerCreateTask.class)
public class OsSvaServerCreateTask extends TransactionalTask {

    private static class ApplianceBootStrap implements BootStrapInfoProviderElement {

        private String applianceName;
        private Map<String, String> bootStrapProperties;

        public ApplianceBootStrap(String applianceName, Map<String, String> bootStrapProperties) {
            this.applianceName = applianceName;
            this.bootStrapProperties = bootStrapProperties;
        }

        @Override
        public String getName() {
            return this.applianceName;
        }

        @Override
        public Map<String, String> getBootStrapProperties() {
            return this.bootStrapProperties;
        }

    }

    private final Logger log = LoggerFactory.getLogger(OsSvaServerCreateTask.class);

    @Reference
    private ApiFactoryService apiFactoryService;

    // target ensures this only binds to active runner published by Server
    @Reference(target = "(active=true)")
    private RabbitMQRunner activeRunner;

    private DistributedApplianceInstance dai;
    private String availabilityZone;
    private String hypervisorHostName;

    public OsSvaServerCreateTask create(DistributedApplianceInstance dai, String hypervisorName,
            String availabilityZone) {
        OsSvaServerCreateTask task = new OsSvaServerCreateTask();
        task.apiFactoryService = this.apiFactoryService;
        task.activeRunner = this.activeRunner;
        task.dai = dai;
        task.availabilityZone = availabilityZone;
        task.hypervisorHostName = hypervisorName;
        task.dbConnectionManager = this.dbConnectionManager;
        task.txBroadcastUtil = this.txBroadcastUtil;

        return task;
    }

    @Override
    public void executeTransaction(EntityManager em) throws Exception {
        this.dai = DistributedApplianceInstanceEntityMgr.findById(em, this.dai.getId());
        DeploymentSpec ds = this.dai.getDeploymentSpec();

        VirtualSystem vs = ds.getVirtualSystem();

        VirtualizationConnector vc = vs.getVirtualizationConnector();
        Endpoint endPoint = new Endpoint(vc, ds.getProjectName());

        this.dai = DistributedApplianceInstanceEntityMgr.findById(em, this.dai.getId());

        String applianceName = this.dai.getName();
        String imageRefId = getImageRefIdByRegion(vs, ds.getRegion());
        String flavorRef = getFlavorRefIdByRegion(vs, ds.getRegion());

        OsSecurityGroupReference sgReference = ds.getOsSecurityGroupReference();

        // Just some name for the file. We dont use file injection, this name gets populated within the
        // meta_data.json file within openstack. We hardcode and look for content within 0000 file
        String availabilityZone = this.availabilityZone.concat(":").concat(this.hypervisorHostName);

        ApplianceSoftwareVersion applianceSoftwareVersion = this.dai.getVirtualSystem().getApplianceSoftwareVersion();
        CreatedServerDetails createdServer;

        // TODO: sjallapx - Hack to workaround issue SimpleDateFormat parse errors due to JCloud on some partner environments.
        boolean createServerWithNoOSTSecurityGroup = this.dai.getVirtualSystem().getVirtualizationConnector()
                .isControllerDefined() ? this.apiFactoryService.supportsPortGroup(this.dai.getVirtualSystem()) : false;

        String sgRefName = createServerWithNoOSTSecurityGroup ? null : sgReference.getSgRefName();

        try (Openstack4JNova nova = new Openstack4JNova(endPoint)) {
            createdServer = nova.createServer(ds.getRegion(), availabilityZone, applianceName, imageRefId, flavorRef,
                    generateBootstrapInfo(vs, applianceName), ds.getManagementNetworkId(), ds.getInspectionNetworkId(),
                    applianceSoftwareVersion.hasAdditionalNicForInspection(), sgRefName);
        }

        this.dai.updateDaiOpenstackSvaInfo(createdServer.getServerId(),
                                           createdServer.getIngressInspectionMacAddr(),
                                           createdServer.getIngressInspectionPortId(),
                                           createdServer.getEgressInspectionMacAddr(),
                                           createdServer.getEgressInspectionPortId());

        // Add new server ID to VM notification listener for this DS

        this.activeRunner.getOsDeploymentSpecNotificationRunner()
                .addSVAIdToListener(this.dai.getDeploymentSpec().getId(), createdServer.getServerId());

        OSCEntityManager.update(em, this.dai, this.txBroadcastUtil);
        this.log.info("Dai: " + this.dai + " Server Id set to: " + this.dai.getExternalId());
    }

    private String getImageRefIdByRegion(VirtualSystem vs, String region) {
        for (OsImageReference imageRef : vs.getOsImageReference()) {
            if (imageRef.getRegion().equals(region)) {
                return imageRef.getImageRefId();
            }
        }
        return null;
    }

    private String getFlavorRefIdByRegion(VirtualSystem vs, String region) {
        for (OsFlavorReference flavorRef : vs.getOsFlavorReference()) {
            if (flavorRef.getRegion().equals(region)) {
                return flavorRef.getFlavorRefId();
            }
        }
        return null;
    }

    private ApplianceBootstrapInformationElement generateBootstrapInfo(final VirtualSystem vs,
            final String applianceName) throws Exception {

        try (ManagerDeviceApi deviceApi = this.apiFactoryService.createManagerDeviceApi(vs)) {
            Map<String, String> bootstrapProperties = vs.getApplianceSoftwareVersion().getConfigProperties();

            return deviceApi
                    .getBootstrapinfo(new ApplianceBootStrap(applianceName, ImmutableMap.copyOf(bootstrapProperties)));
        }
    }

    @Override
    public String getName() {
        return String.format("Deploying SVA for Distributed Appliance Instance '%s'", this.dai.getName());
    }

    @Override
    public Set<LockObjectReference> getObjects() {
        return LockObjectReference.getObjectReferences(this.dai);
    }

}
