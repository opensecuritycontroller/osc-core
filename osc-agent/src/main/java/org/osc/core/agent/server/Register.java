package org.osc.core.agent.server;

import org.apache.log4j.Logger;
import org.osc.core.agent.rest.server.AgentAuthFilter;
import org.osc.core.broker.model.virtualization.VirtualizationEnvironmentProperties;
import org.osc.core.rest.client.VmidcServerAgentRestClient;
import org.osc.core.rest.client.agent.model.input.AgentRegisterRequest;
import org.osc.core.rest.client.agent.model.output.AgentRegisterResponse;
import org.osc.core.util.EncryptionUtil;
import org.osc.core.util.FileUtil;
import org.osc.core.util.ServerUtil;
import org.osc.core.util.VersionUtil;
import org.osc.core.util.encryption.EncryptionException;

import java.io.IOException;
import java.util.Date;
import java.util.Properties;

public class Register {
    private static Logger log = Logger.getLogger(Register.class);

    static final String MANAGER_IP = "managerIp";
    static final String APPLIANCE_IP = "applianceIp";
    static final String APPLIANCE_MTU = "applianceMtu";
    static final String APPLIANCE_SUBNET_MASK = "applianceSubnetMask";
    static final String APPLIANCE_GATEWAY = "applianceGateway";
    static final String SHARED_SECRET_KEY = "sharedSecretKey";
    static final String VMIDC_SERVER_PORT = "vmidcPort";

    public static final String NSM_PUB_KEY = "nsmPubKey";
    public static final String VMIDC_AGENT_KS = "vmidcApplianceKeyStore.jks";

    public static synchronized void registerAppliance(boolean forced) {
        log.info("================= Register appliance started ... ==============================");

        try {
            Server.applianceUtils.enableFirewallVmiDCPort();
            Server.applianceUtils.mountCdRom();

            AgentEnv agentEnvironment = VirtualizationUtils.getAgentEnvironment();

            if (agentEnvironment.getApplianceIp() == null) {
                log.info("Missing network information in Agent environment");
                return;
            }

            /*
             * If the vmiDCServer IP or vmiDCServer Password has changed since
             * the time the appliance got deployed, we cannot read it from the
             * mounted XML.
             */
            if (Server.getVmidcServerIp() == null) {
                Server.setVmidcServerIp(agentEnvironment.getVmidcIp());
            }

            if (Server.getVmidcServerPassword() == null) {
                Server.setVmidcServerPassword(agentEnvironment.getVmidcPassword());
            }

            /*
             * If there is no stored appliance IP or if there is one but it has
             * changed, set network information.
             */
            if (Server.applianceIp == null || !Server.applianceIp.equals(agentEnvironment.getApplianceIp())) {
                setApplianceNetworkInfo(agentEnvironment);
                persistAgentInfo(agentEnvironment.getApplianceIp(), agentEnvironment.getApplianceName(), null, null,
                        agentEnvironment.getVmidcIp(), agentEnvironment.getVmidcPassword(),
                        agentEnvironment.getApplianceGateway(), agentEnvironment.getApplianceNetmask(),
                        agentEnvironment.getApplianceMtu());
                Server.applianceIp = agentEnvironment.getApplianceIp();
            }

            /*
             * Match which ever password is set - NsxEnv XML file or overridden value
             */
            if (Server.getVmidcServerPassword() != null) {
                AgentAuthFilter.AGENT_DEFAULT_PASS = EncryptionUtil.decryptAESCTR(Server.getVmidcServerPassword());
            }

            AgentRegisterResponse registrationResponse = registerAgent(Server.getVmidcServerIp(),
                    agentEnvironment.getVsId(), agentEnvironment.getApplianceIp(), agentEnvironment.getVmidcUser(),
                    Server.getVmidcServerPassword(), agentEnvironment.getApplianceName(),
                    agentEnvironment.getApplianceGateway(), agentEnvironment.getApplianceNetmask(), forced);

            Server.applianceUtils.persistConfig(registrationResponse.getApplianceConfig1(),
                    registrationResponse.getApplianceConfig2(), forced);

            if (Server.applianceUtils.isAuthenticationNeeded() || isManagmentAttributeChange(registrationResponse)
                    || forced) {
                /*
                 * Manager IP, or certificate, or shared-secret-key changed - need to re-authenticate command.
                 */
                boolean deinstall = Server.managerIp != null
                        && !Server.managerIp.equals(registrationResponse.getMgrIp()) || forced;

                persistAgentInfo(agentEnvironment.getApplianceIp(), registrationResponse.getApplianceName(),
                        registrationResponse.getMgrIp(), registrationResponse.getSharedSecretKey(),
                        Server.getVmidcServerIp(), Server.getVmidcServerPassword(),
                        agentEnvironment.getApplianceGateway(), agentEnvironment.getApplianceNetmask(),
                        agentEnvironment.getApplianceMtu());

                Server.applianceName = registrationResponse.getApplianceName();
                Server.managerIp = registrationResponse.getMgrIp();
                Server.sharedSecretKey = registrationResponse.getSharedSecretKey();

                authenticateAppliance(registrationResponse, deinstall);
                Server.applianceUtils.enableFirewallVmiDCPort();
            }

        } catch (Exception e) {

            log.error("Failed to register the appliance", e);
        }
    }

    private static boolean isManagmentAttributeChange(AgentRegisterResponse registrationResponse) {
        return Server.managerIp == null || !Server.managerIp.equals(registrationResponse.getMgrIp())
                || Server.applianceName == null
                || !Server.applianceName.equals(registrationResponse.getApplianceName());
    }

    private static AgentRegisterResponse registerAgent(String vmidcServerIp, long virtualSystemId, String applianceIp,
            String vmidcUser, String vmidcPassword, String applianceName, String applianceGateway,
            String applianceSubnetMask, boolean forced) throws Exception {

        log.info("Start CPA registration with ISC server");

        boolean discovered = Server.applianceUtils.isDiscovered();
        // If this is a forced re-authentication request from server, turn off discovery state to
        // ensure appliance configuration will get regenerated.
        if (forced) {
            discovered = false;
        }
        boolean inspectionReady = Server.applianceUtils.isInspectionReady();

        VmidcServerAgentRestClient client = new VmidcServerAgentRestClient(vmidcServerIp, vmidcUser,
                EncryptionUtil.decryptAESCTR(vmidcPassword));

        AgentRegisterRequest input = new AgentRegisterRequest();
        input.setApplianceIp(applianceIp);
        input.setApplianceGateway(applianceGateway);
        input.setApplianceSubnetMask(applianceSubnetMask);
        input.setVirtualSystemId(virtualSystemId);
        input.setName(applianceName);
        input.setAgentVersion(VersionUtil.getVersion());
        input.setDiscovered(discovered);
        input.setInspectionReady(inspectionReady);

        input.setCurrentServerTime(new Date());
        input.setCpaPid(ServerUtil.getCurrentPid());
        input.setCpaUptime(Server.getUptimeMilli());
        input.setAgentDpaInfo(Server.dpaipc.getAgentDpaInfo());

        AgentRegisterResponse registrationResponse = client.postResource("agentregister", AgentRegisterResponse.class,
                input);

        log.info("Successfully registered agent. Response: " + registrationResponse);

        return registrationResponse;
    }

    public static void persistAgentInfo(String applianceIp, String applianceName, String managerIp,
            String sharedSecretKey, String vmidcServerIp, String vmidcServerPassword, String applianceGateway,
            String applianceSubnetMask, Integer applianceMtu) {

        Properties prop = new Properties();
        try {
            prop = FileUtil.loadProperties(Server.CONFIG_PROPERTIES_FILE);
        } catch (IOException e) {
            log.error("Failed to read to the properties file", e);
        }

        // Write to the properties file
        if (applianceIp != null) {
            prop.setProperty(APPLIANCE_IP, applianceIp);
        }
        if (applianceName != null) {
            prop.setProperty(VirtualizationEnvironmentProperties.APPLIANCE_NAME, applianceName);
        }
        if (managerIp != null) {
            prop.setProperty(MANAGER_IP, managerIp);
        }
        if (sharedSecretKey != null) {
            prop.setProperty(SHARED_SECRET_KEY, sharedSecretKey);
        }
        if (vmidcServerIp != null) {
            prop.setProperty(VirtualizationEnvironmentProperties.VMIDC_IP, Server.getVmidcServerIp());
        }
        if (vmidcServerPassword != null) {
            prop.setProperty(VirtualizationEnvironmentProperties.VMIDC_PASSWORD, Server.getVmidcServerPassword());
        }
        if (applianceGateway != null) {
            prop.setProperty(APPLIANCE_GATEWAY, applianceGateway);
        }
        if (applianceSubnetMask != null) {
            prop.setProperty(APPLIANCE_SUBNET_MASK, applianceSubnetMask);
        }
        if (applianceMtu != null) {
            prop.setProperty(APPLIANCE_MTU, applianceMtu.toString());
        }

        Server.saveProperties(prop);
    }

    private static int setApplianceNetworkInfo(AgentEnv agentEnv) {
        return Server.applianceUtils.setApplianceNetworkInfo(agentEnv.getApplianceIp(), agentEnv.getApplianceNetmask(),
                agentEnv.getApplianceGateway(), agentEnv.getApplianceMtu(), agentEnv.getVmidcIp());
    }

    private static int authenticateAppliance(AgentRegisterResponse registrationResponse, boolean deinstall) throws EncryptionException {
        String decrypted = EncryptionUtil.decryptAESCTR(registrationResponse.getSharedSecretKey());
        return Server.applianceUtils.authenticateAppliance(registrationResponse.getApplianceName(),
                registrationResponse.getMgrIp(), decrypted, null, deinstall);
    }

}
