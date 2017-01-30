package org.osc.core.broker.service.tasks.conformance.openstack.deploymentspec;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.model.entities.appliance.ApplianceSoftwareVersion;
import org.osc.core.broker.model.entities.appliance.DistributedApplianceInstance;
import org.osc.core.broker.model.entities.appliance.VirtualSystem;
import org.osc.core.broker.model.entities.virtualization.VirtualizationConnector;
import org.osc.core.broker.model.entities.virtualization.openstack.DeploymentSpec;
import org.osc.core.broker.model.entities.virtualization.openstack.OsFlavorReference;
import org.osc.core.broker.model.entities.virtualization.openstack.OsImageReference;
import org.osc.core.broker.model.entities.virtualization.openstack.OsSecurityGroupReference;
import org.osc.core.broker.model.plugin.manager.ManagerApiFactory;
import org.osc.core.broker.model.plugin.sdncontroller.SdnControllerApiFactory;
import org.osc.core.broker.model.virtualization.VirtualizationEnvironmentProperties;
import org.osc.core.broker.rest.client.openstack.jcloud.Endpoint;
import org.osc.core.broker.rest.client.openstack.jcloud.JCloudNova;
import org.osc.core.broker.rest.client.openstack.jcloud.JCloudNova.CreatedServerDetails;
import org.osc.core.broker.rest.client.openstack.vmidc.notification.runner.OsDeploymentSpecNotificationRunner;
import org.osc.core.broker.rest.server.AgentAuthFilter;
import org.osc.core.broker.service.persistence.DistributedApplianceInstanceEntityMgr;
import org.osc.core.broker.service.persistence.EntityManager;
import org.osc.core.broker.service.tasks.TransactionalTask;
import org.osc.core.util.EncryptionUtil;
import org.osc.core.util.ServerUtil;
import org.osc.sdk.controller.DefaultInspectionPort;
import org.osc.sdk.controller.DefaultNetworkPort;
import org.osc.sdk.controller.api.SdnControllerApi;
import org.osc.sdk.controller.element.NetworkElement;
import org.osc.sdk.manager.api.ApplianceManagerApi;
import org.osc.sdk.manager.api.ManagerDeviceApi;
import org.osc.sdk.manager.element.ApplianceBootstrapInformationElement;
import org.osc.sdk.manager.element.BootStrapInfoProviderElement;

import com.google.common.collect.ImmutableMap;
import com.sun.jersey.core.util.Base64;

/**
 * Creates SVA for given dai
 */
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

    private static class ApplianceBootstrapInformation implements ApplianceBootstrapInformationElement {

        BootstrapFileElement file;

        public ApplianceBootstrapInformation(final String filename, final byte[] fileContent) {
            this.file = new BootstrapFileElement() {

                @Override
                public String getName() {
                    return filename;
                }

                @Override
                public byte[] getContent() {
                    return fileContent;
                }
            };
        }

        @Override
        public List<BootstrapFileElement> getBootstrapFiles() {
            return Arrays.asList(this.file);
        }

    }

    private final Logger log = Logger.getLogger(OsSvaServerCreateTask.class);

    private DistributedApplianceInstance dai;
    private final String availabilityZone;
    private final String hypervisorHostName;

    public OsSvaServerCreateTask(DistributedApplianceInstance dai, String hypervisorName, String availabilityZone) {
        this.dai = dai;
        this.availabilityZone = availabilityZone;
        this.hypervisorHostName = hypervisorName;
    }

    @Override
    public void executeTransaction(Session session) throws Exception {
        this.dai = DistributedApplianceInstanceEntityMgr.findById(session, this.dai.getId());
        DeploymentSpec ds = this.dai.getDeploymentSpec();

        VirtualSystem vs = ds.getVirtualSystem();

        VirtualizationConnector vc = vs.getVirtualizationConnector();
        Endpoint endPoint = new Endpoint(vc, ds.getTenantName());
        JCloudNova nova = new JCloudNova(endPoint);
        try {

            this.dai = DistributedApplianceInstanceEntityMgr.findById(session, this.dai.getId());

            String applianceName = this.dai.getName();
            String imageRefId = getImageRefIdByRegion(vs, ds.getRegion());
            String flavorRef = getFlavorRefIdByRegion(vs, ds.getRegion());

            OsSecurityGroupReference sgReference = ds.getOsSecurityGroupReference();

            // Just some name for the file. We dont use file injection, this name gets populated within the
            // meta_data.json file within openstack. We hardcode and look for content within 0000 file
            String availabilityZone = this.availabilityZone.concat(":").concat(this.hypervisorHostName);

            ApplianceSoftwareVersion applianceSoftwareVersion = this.dai.getVirtualSystem()
                    .getApplianceSoftwareVersion();

            CreatedServerDetails createdServer = nova.createServer(ds.getRegion(), availabilityZone, applianceName,
                    imageRefId, flavorRef, generateBootstrapInfo(vs, applianceName), ds.getManagementNetworkId(),
                    ds.getInspectionNetworkId(), applianceSoftwareVersion.hasAdditionalNicForInspection(),
                    sgReference.getSgRefName());
            this.dai.updateDaiOpenstackSvaInfo(createdServer);
            // Add new server ID to VM notification listener for this DS
            OsDeploymentSpecNotificationRunner.addSVAIdToListener(this.dai.getDeploymentSpec().getId(),
                    createdServer.getServerId());

            if (vc.isControllerDefined()) {
                SdnControllerApi controller = SdnControllerApiFactory.createNetworkControllerApi(this.dai);
                try {
                    DefaultNetworkPort ingressPort = new DefaultNetworkPort(createdServer.getIngressInspectionPortId(),
                            createdServer.getIngressInspectionMacAddr());
                    DefaultNetworkPort egressPort = new DefaultNetworkPort(createdServer.getEgressInspectionPortId(),
                            createdServer.getEgressInspectionMacAddr());

                    if (controller.isPortGroupSupported()) {
                        String domainId = OpenstackUtil.extractDomainId(ds.getTenantId(), ds.getTenantName(),
                                ds.getVirtualSystem().getVirtualizationConnector(),
                                new ArrayList<NetworkElement>(Arrays.asList(ingressPort)));
                        ingressPort.setParentId(domainId);
                        egressPort.setParentId(domainId);
                    }
                    controller.registerInspectionPort(new DefaultInspectionPort(ingressPort, egressPort));

                } finally {
                    controller.close();
                }
            }

            this.log.info("Dai: " + this.dai + " Server Id set to: " + this.dai.getOsServerId());

            EntityManager.update(session, this.dai);

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

        ApplianceManagerApi managerApi = ManagerApiFactory.createApplianceManagerApi(vs);
        ManagerDeviceApi deviceApi = ManagerApiFactory.createManagerDeviceApi(vs);
        Map<String, String> bootstrapProperties = vs.getApplianceSoftwareVersion().getConfigProperties();
        if (managerApi.isAgentManaged()) {
            StringBuilder configString = new StringBuilder();
            configString.append(VirtualizationEnvironmentProperties.VMIDC_IP + "=" + ServerUtil.getServerIP() + "\n");
            configString.append(
                    VirtualizationEnvironmentProperties.VMIDC_USER + "=" + AgentAuthFilter.VMIDC_AGENT_LOGIN + "\n");
            configString.append(VirtualizationEnvironmentProperties.VMIDC_PASSWORD + "="
                    + EncryptionUtil.encrypt(AgentAuthFilter.VMIDC_AGENT_PASS) + "\n");
            configString.append(VirtualizationEnvironmentProperties.VIRTUAL_SYSTEM_ID + "=" + vs.getId() + "\n");
            configString.append(VirtualizationEnvironmentProperties.APPLIANCE_NAME + "=" + applianceName + "\n");

            if (bootstrapProperties != null) {
                for (Entry<String, String> configProperty : bootstrapProperties.entrySet()) {
                    configString.append(configProperty.getKey() + "=" + configProperty.getValue() + "\n");
                }
            }
            return new ApplianceBootstrapInformation("/tmp/vmidcAgent.conf", Base64.encode(configString.toString()));
        } else {
            return deviceApi
                    .getBootstrapinfo(new ApplianceBootStrap(applianceName, ImmutableMap.copyOf(bootstrapProperties)));
        }
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
