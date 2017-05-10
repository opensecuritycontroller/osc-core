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

import java.util.Arrays;
import java.util.Map;
import java.util.Set;

import javax.persistence.EntityManager;

import org.apache.log4j.Logger;
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
import org.osc.core.broker.model.plugin.sdncontroller.SdnControllerApiFactory;
import org.osc.core.broker.rest.client.openstack.jcloud.Endpoint;
import org.osc.core.broker.rest.client.openstack.jcloud.JCloudNova;
import org.osc.core.broker.rest.client.openstack.jcloud.JCloudNova.CreatedServerDetails;
import org.osc.core.broker.service.persistence.DistributedApplianceInstanceEntityMgr;
import org.osc.core.broker.service.persistence.OSCEntityManager;
import org.osc.core.broker.service.tasks.TransactionalTask;
import org.osc.core.broker.util.StaticRegistry;
import org.osc.sdk.controller.DefaultInspectionPort;
import org.osc.sdk.controller.DefaultNetworkPort;
import org.osc.sdk.controller.api.SdnRedirectionApi;
import org.osc.sdk.manager.api.ManagerDeviceApi;
import org.osc.sdk.manager.element.ApplianceBootstrapInformationElement;
import org.osc.sdk.manager.element.BootStrapInfoProviderElement;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.google.common.collect.ImmutableMap;

/**
 * Creates SVA for given dai
 */
@Component(service = OsSvaServerCreateTask.class)
class OsSvaServerCreateTask extends TransactionalTask {

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

    private final Logger log = Logger.getLogger(OsSvaServerCreateTask.class);

    @Reference
    private ApiFactoryService apiFactoryService;

    private DistributedApplianceInstance dai;
    private String availabilityZone;
    private String hypervisorHostName;

    public OsSvaServerCreateTask create(DistributedApplianceInstance dai, String hypervisorName, String availabilityZone) {
        OsSvaServerCreateTask task = new OsSvaServerCreateTask();
        task.apiFactoryService = this.apiFactoryService;
        task.dai = dai;
        task.availabilityZone = availabilityZone;
        task.hypervisorHostName = hypervisorName;
        return task;
    }

    @Override
    public void executeTransaction(EntityManager em) throws Exception {
        this.dai = DistributedApplianceInstanceEntityMgr.findById(em, this.dai.getId());
        DeploymentSpec ds = this.dai.getDeploymentSpec();

        VirtualSystem vs = ds.getVirtualSystem();

        VirtualizationConnector vc = vs.getVirtualizationConnector();
        Endpoint endPoint = new Endpoint(vc, ds.getTenantName());
        JCloudNova nova = new JCloudNova(endPoint);
        SdnRedirectionApi controller = null;
        try {
            this.dai = DistributedApplianceInstanceEntityMgr.findById(em, this.dai.getId());
            if (vc.isControllerDefined()){
                controller = SdnControllerApiFactory.createNetworkRedirectionApi(this.dai);
            }

            String applianceName = this.dai.getName();
            String imageRefId = getImageRefIdByRegion(vs, ds.getRegion());
            String flavorRef = getFlavorRefIdByRegion(vs, ds.getRegion());

            OsSecurityGroupReference sgReference = ds.getOsSecurityGroupReference();

            // Just some name for the file. We dont use file injection, this name gets populated within the
            // meta_data.json file within openstack. We hardcode and look for content within 0000 file
            String availabilityZone = this.availabilityZone.concat(":").concat(this.hypervisorHostName);

            ApplianceSoftwareVersion applianceSoftwareVersion = this.dai.getVirtualSystem()
                    .getApplianceSoftwareVersion();
            CreatedServerDetails createdServer = null;

            // TODO: sjallapx - Hack to workaround issue SimpleDateFormat parse errors due to JCloud on some partner environments.
            boolean createServerWithNoOSTSecurityGroup = this.dai.getVirtualSystem().getVirtualizationConnector().isControllerDefined()
                    ? SdnControllerApiFactory.supportsPortGroup(this.dai.getVirtualSystem()) : false;
            if (createServerWithNoOSTSecurityGroup) {
                createdServer = nova.createServer(ds.getRegion(), availabilityZone, applianceName,
                        imageRefId, flavorRef, generateBootstrapInfo(vs, applianceName), ds.getManagementNetworkId(),
                        ds.getInspectionNetworkId(), applianceSoftwareVersion.hasAdditionalNicForInspection(),
                        null);
            } else {
                createdServer = nova.createServer(ds.getRegion(), availabilityZone, applianceName,
                        imageRefId, flavorRef, generateBootstrapInfo(vs, applianceName), ds.getManagementNetworkId(),
                        ds.getInspectionNetworkId(), applianceSoftwareVersion.hasAdditionalNicForInspection(),
                        sgReference.getSgRefName());
            }
            this.dai.updateDaiOpenstackSvaInfo(createdServer.getServerId(),
                    createdServer.getIngressInspectionMacAddr(),
                    createdServer.getIngressInspectionPortId(),
                    createdServer.getEgressInspectionMacAddr(),
                    createdServer.getEgressInspectionPortId()
                    );
            // Add new server ID to VM notification listener for this DS

            StaticRegistry.server().getActiveRabbitMQRunner().getOsDeploymentSpecNotificationRunner()
                .addSVAIdToListener(this.dai.getDeploymentSpec().getId(), createdServer.getServerId());

            if (vc.isControllerDefined()) {
                try {
                    DefaultNetworkPort ingressPort = new DefaultNetworkPort(createdServer.getIngressInspectionPortId(),
                            createdServer.getIngressInspectionMacAddr());
                    DefaultNetworkPort egressPort = new DefaultNetworkPort(createdServer.getEgressInspectionPortId(),
                            createdServer.getEgressInspectionMacAddr());

                    if (SdnControllerApiFactory.supportsPortGroup(this.dai.getVirtualSystem())) {
                        String domainId = OpenstackUtil.extractDomainId(
                                ds.getTenantId(),
                                ds.getTenantName(),
                                ds.getVirtualSystem().getVirtualizationConnector(),
                                Arrays.asList(ingressPort));
                        ingressPort.setParentId(domainId);
                        egressPort.setParentId(domainId);
                    }
                    controller.registerInspectionPort(new DefaultInspectionPort(ingressPort, egressPort));

                } finally {
                    controller.close();
                }
            }

            this.log.info("Dai: " + this.dai + " Server Id set to: " + this.dai.getOsServerId());

            OSCEntityManager.update(em, this.dai);

        } finally {
            nova.close();
        }
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

        ManagerDeviceApi deviceApi = this.apiFactoryService.createManagerDeviceApi(vs);
        Map<String, String> bootstrapProperties = vs.getApplianceSoftwareVersion().getConfigProperties();

        return deviceApi
                .getBootstrapinfo(new ApplianceBootStrap(applianceName, ImmutableMap.copyOf(bootstrapProperties)));
    }

    @Override
    public String getName() {
        return String.format("Deploying SVA for Distributed Appliance Instance '%s'", this.dai.getName());
    };

    @Override
    public Set<LockObjectReference> getObjects() {
        return LockObjectReference.getObjectReferences(this.dai);
    }

}
