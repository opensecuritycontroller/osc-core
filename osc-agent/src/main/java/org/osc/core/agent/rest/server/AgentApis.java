package org.osc.core.agent.rest.server;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Date;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.osc.core.agent.dpaipc.InterfaceEndpointMap;
import org.osc.core.agent.server.Register;
import org.osc.core.agent.server.Server;
import org.osc.core.rest.annotations.AgentAuth;
import org.osc.core.rest.client.agent.model.input.AgentSetInterfaceEndpointMapRequest;
import org.osc.core.rest.client.agent.model.input.AgentUpdateConsolePasswordRequest;
import org.osc.core.rest.client.agent.model.input.AgentUpdateInterfaceEndpointMapRequest;
import org.osc.core.rest.client.agent.model.input.AgentUpdateVmidcPasswordRequest;
import org.osc.core.rest.client.agent.model.input.AgentUpdateVmidcServerRequest;
import org.osc.core.rest.client.agent.model.input.AgentUpgradeRequest;
import org.osc.core.rest.client.agent.model.input.dpaipc.InterfaceEntry;
import org.osc.core.rest.client.agent.model.output.AgentCurrentVmidcPasswordResponse;
import org.osc.core.rest.client.agent.model.output.AgentCurrentVmidcServerResponse;
import org.osc.core.rest.client.agent.model.output.AgentStatusResponse;
import org.osc.core.rest.client.agent.model.output.AgentSupportBundle;
import org.osc.core.rest.client.agent.model.output.AgentUpgradeResponse;
import org.osc.core.rest.client.crypto.SslContextProvider;
import org.osc.core.util.ArchiveUtil;
import org.osc.core.util.EncryptionUtil;
import org.osc.core.rest.annotations.LocalHostAuth;
import org.osc.core.util.PKIUtil;
import org.osc.core.util.ServerUtil;
import org.osc.core.util.VersionUtil;



import org.osc.core.util.encryption.EncryptionException;

@Path("/v1")
public class AgentApis {

    private Logger log = Logger.getLogger(AgentApis.class);

    @Path("/status")
    @GET
    @Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    public Response getBasicStatus() {
        this.log.info("getBasicStatus() called");

        AgentStatusResponse agentStatusResponse = getAgentBasicStatus();
        this.log.info("Response agentStatusResponse: " + agentStatusResponse);

        return Response.status(Status.OK).entity(agentStatusResponse).build();
    }

    @AgentAuth
    @Path("/fullstatus")
    @GET
    @Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    public Response getFullStatus() {
        this.log.info("getFullStatus() called");

        AgentStatusResponse agentStatusResponse = getAgentFullStatus();
        this.log.info("Response agentStatusResponse: " + agentStatusResponse);

        Thread upgradeThread = new Thread("Register-Thread") {
            @Override
            public void run() {
                Register.registerAppliance(false);
            }
        };
        upgradeThread.start();

        return Response.status(Status.OK).entity(agentStatusResponse).build();
    }

    private AgentStatusResponse getAgentBasicStatus() {
        AgentStatusResponse agentStatusResponse = new AgentStatusResponse();

        agentStatusResponse.setVersion(VersionUtil.getVersion());
        agentStatusResponse.setCurrentServerTime(new Date());
        agentStatusResponse.setApplianceIp(Server.applianceIp);
        agentStatusResponse.setApplianceName(Server.applianceName);
        agentStatusResponse.setManagerIp(Server.managerIp);
        agentStatusResponse.setBrokerIp(Server.getVmidcServerIp());
        agentStatusResponse.setCpaPid(ServerUtil.getCurrentPid());
        agentStatusResponse.setCpaUptime(Server.getUptimeMilli());
        agentStatusResponse.setApplianceGateway(Server.applianceGateway);
        agentStatusResponse.setApplianceSubnetMask(Server.applianceSubnetMask);

        return agentStatusResponse;
    }

    private AgentStatusResponse getAgentFullStatus() {
        // Get basic status
        AgentStatusResponse agentStatusResponse = getAgentBasicStatus();

        // Add DPA info and stats
        agentStatusResponse.setAgentDpaInfo(Server.dpaipc.getAgentDpaInfo());

        // Add Appliance status
        agentStatusResponse.setDiscovered(Server.applianceUtils.isDiscovered());
        agentStatusResponse.setInspectionReady(Server.applianceUtils.isInspectionReady());

        return agentStatusResponse;
    }

    @AgentAuth
    @Path("/getSupportBundle")
    @GET
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    public Response getAgentSupportBundle() {
        try {
            String supportFileName = "AgentSupportBundle.zip";
            // deleting previous log bundle
            if (new File(supportFileName).exists()) {
                new File(supportFileName).delete();
            }
            this.log.info("Creating support bundle");
            AgentSupportBundle res = new AgentSupportBundle();
            res.setSupportLogBundle(PKIUtil.readBytesFromFile(ArchiveUtil.archive("log", supportFileName)));
            return Response.ok(res).build();
        } catch (Exception e) {
            this.log.error("Failed to zip log folder ", e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @AgentAuth
    @Path("/uploadMgrFile/{fileName}")
    @PUT
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    public Response uploadMgrFile(final @PathParam("fileName") String mgrFileName, final InputStream uploadedInputStream) {

        this.log.info("Start updating mgr file: " + mgrFileName);

        if (uploadedInputStream == null) {
            this.log.error("Invalid request, null input stream");
            return Response.status(Status.BAD_REQUEST).entity("Invalid request. No input stream.")
                    .type(MediaType.TEXT_PLAIN).build();
        }

        if (mgrFileName == null || mgrFileName.isEmpty()) {
            this.log.error("Invalid mgrfile name");
            return Response.status(Status.BAD_REQUEST).entity("Invalid request. No mgrfile name.")
                    .type(MediaType.TEXT_PLAIN).build();
        }

        try {
            ExecutorService executor = Executors.newCachedThreadPool();

            Callable<Boolean> task = new Callable<Boolean>() {
                @Override
                public Boolean call() throws Exception {
                    // Executor will execute task in a separate thread.
                    Boolean status = Server.applianceUtils.processMgrFile(uploadedInputStream, mgrFileName);

                    return status;
                }
            };

            Future<Boolean> future = executor.submit(task);
            Boolean response = null;

            try {
                // Wait 3 mins for response from python execution task.
                response = future.get(180, TimeUnit.SECONDS);

                if (response) {
                    this.log.info("mgrfile updated successfully.");
                    return Response.status(Status.OK).build();
                } else {
                    this.log.info("Existing marker file detected. Prevent overlapping requests as Manager File processing is in progress");
                    return Response
                            .status(Status.NOT_ACCEPTABLE)
                            .entity("Existing marker file detected. Prevent overlapping requests as Manager File processing is in progress.")
                            .type(MediaType.TEXT_PLAIN).build();
                }
            } catch (InterruptedException e) {
                this.log.error("Error invoking python script to notify vsensor. Execution thread is interrupted", e);
                return Response.status(Status.INTERNAL_SERVER_ERROR)
                        .entity("Error invoking python script to notify vsensor. Execution thread is interrupted.")
                        .type(MediaType.TEXT_PLAIN).build();
            } catch (ExecutionException e) {
                this.log.error("Error invoking python script to notify vsensor", e);
                return Response.status(Status.INTERNAL_SERVER_ERROR)
                        .entity("Error invoking python script to notify vsensor.").type(MediaType.TEXT_PLAIN).build();
            } catch (TimeoutException e) {
                this.log.error("Timeout while invoking python script to notify vsensor");
                return Response.status(Status.INTERNAL_SERVER_ERROR)
                        .entity("Timeout while invoking python script to notify vsensor.").type(MediaType.TEXT_PLAIN)
                        .build();
            } finally {
                executor.shutdownNow();
                try {
                    executor.awaitTermination(1, TimeUnit.MINUTES);
                } catch (InterruptedException e) {
                    this.log.warn("Couldn't wait any longer for REST call completion. Shutting down request thread.", e);
                }
            }

        } catch (Exception ex) {
            this.log.error("Failed to update mgrfile", ex);
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity(ex.getMessage()).type(MediaType.TEXT_PLAIN)
                    .build();
        }
    }

    @AgentAuth
    @Path("/register")
    @POST
    @Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    public Response triggerRegistration() {
        this.log.info("triggerRegistration() called");

        Thread upgradeThread = new Thread("Register-Thread") {
            @Override
            public void run() {
                Register.registerAppliance(true);
            }
        };
        upgradeThread.start();

        return Response.status(Status.OK).entity("Registration triggered.").type(MediaType.TEXT_PLAIN).build();
    }

    @LocalHostAuth
    @Path("/upgradecomplete")
    @PUT
    @Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    public Response upgradedAgentReady() {
        this.log.info("upgradedAgentReady (pid:" + ServerUtil.getCurrentPid() + "): Check pending upgrade agent");
        if (!Server.upgradeInProgress) {
            return Response.status(Status.BAD_REQUEST).build();
        }

        this.log.info("upgradedAgentReady (pid:" + ServerUtil.getCurrentPid()
                + "): Upgraded agent is up. Start shutdown...");
        Thread shutdownThread = new Thread("Shutdown-Thread") {
            @Override
            public void run() {
                try {
                    /*
                     * Introduce a slight delay so REST response can be
                     * completed
                     */
                    Thread.sleep(500);
                    Server.stopServer();
                } catch (Exception e) {
                    AgentApis.this.log.error("upgradedAgentReady (pid:" + ServerUtil.getCurrentPid()
                            + "): Shutting down Tomcat after upgrade experienced failures", e);
                }
            }
        };
        shutdownThread.start();

        return Response.status(Status.OK).build();
    }

    @AgentAuth
    @Path("/get-agent-vmidcserver-ip")
    @GET
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    public Response getCurrentServerIp() {

        AgentCurrentVmidcServerResponse agentCurrentVmidcServerResponse = new AgentCurrentVmidcServerResponse();
        agentCurrentVmidcServerResponse.setVmidcServerIp(Server.getVmidcServerIp());

        return Response.status(Status.OK).entity(agentCurrentVmidcServerResponse).build();
    }

    @AgentAuth
    @Path("/update-agent-vmidcserver-ip")
    @PUT
    @Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    public Response updateServerIp(AgentUpdateVmidcServerRequest agentUpdateVmidcServerRequest) {

        Server.setVmidcServerIp(agentUpdateVmidcServerRequest.getVmidcServerIp());
        /*
         * Need to immediately persist the vmidcServerIp to the properties file
         */
        Register.persistAgentInfo(null, null, null, null, Server.getVmidcServerIp(), null, null, null, null);

        return Response.status(Status.OK).build();
    }

    @AgentAuth
    @Path("/get-agent-vmidcserver-password")
    @GET
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    public Response getCurrentServerPassword() {

        AgentCurrentVmidcPasswordResponse agentCurrentVmidcPasswordResponse = new AgentCurrentVmidcPasswordResponse();
        agentCurrentVmidcPasswordResponse.setVmidcServerPassword(Server.getVmidcServerPassword());

        return Response.status(Status.OK).entity(agentCurrentVmidcPasswordResponse).build();
    }

    @AgentAuth
    @Path("/update-agent-vmidcserver-password")
    @PUT
    @Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    public Response updateServerPassword(AgentUpdateVmidcPasswordRequest agentUpdateVmidcPasswordRequest) {
        Server.setVmidcServerPassword(agentUpdateVmidcPasswordRequest.getVmidcServerPassword());
        try {
            AgentAuthFilter.AGENT_DEFAULT_PASS = EncryptionUtil.decryptAESCTR(Server.getVmidcServerPassword());
        } catch (EncryptionException e) {
            log.error("Failed to get default password", e);
            //TODO : handle in exception mapper (after Jersey2 validation changes)
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }
        /*
         * Need to immediately persist the vmidcPassword to the properties file
         */
        Register.persistAgentInfo(null, null, null, null, null, Server.getVmidcServerPassword(), null, null, null);

        return Response.status(Status.OK).build();
    }

    @AgentAuth
    @Path("/update-interface-endpoint-map")
    @PUT
    @Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    public Response updateInterfaceEndpointMap(AgentUpdateInterfaceEndpointMapRequest request) {
        this.log.info("InterfaceEndpointMap update request: " + request);

        Server.dpaipc.updateProfileServiceContainer(request.interfaceTag, request.endpoints);

        return Response.status(Status.OK).build();
    }

    @AgentAuth
    @Path("/set-interface-endpoint-map")
    @PUT
    @Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    public Response setInterfaceEndpointMap(AgentSetInterfaceEndpointMapRequest request) throws IOException {
        this.log.info("InterfaceEndpointMap set request: " + request);

        InterfaceEndpointMap interfaceEndpointMap = new InterfaceEndpointMap();
        for (InterfaceEntry entry : request.getInterfaceEndpointMap().map.values()) {
            interfaceEndpointMap.updateProfileServiceContainer(entry.getInterfaceTag(), entry.getEndpointSet());
        }

        Server.dpaipc.setProfileServiceContainers(interfaceEndpointMap);

        return Response.status(Status.OK).build();
    }

    @AgentAuth
    @Path("/upgrade")
    @POST
    @Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    public Response upgradeAgent(AgentUpgradeRequest upgradeRequest) {
        this.log.info("================= Start agent upgrade =====================");
        this.log.info("Upgrade Request (pid:" + ServerUtil.getCurrentPid() + "): " + upgradeRequest);
        boolean successRename = false;
        File newAgentFile = new File("vmiDCAgent.jar");
        File originalAgentfile = new File("vmiDCAgent.org");

        AgentUpgradeResponse upgradeResponse = new AgentUpgradeResponse();

        if (Server.upgradeInProgress) {
            this.log.info("Upgrade Request (pid:" + ServerUtil.getCurrentPid() + "). update in progress.");
            upgradeResponse.setUpgradeStatus("Upgrade already in progress");
            return Response.status(Status.OK).entity(upgradeResponse).build();
        }

        try {
            String downloadedFilePath = "/tmp/agentUpgradeBundle.zip";

            this.log.info("Upgrade (pid:" + ServerUtil.getCurrentPid() + "): Removing existing upgrade bundle from");
            ServerUtil.execWithLog("rm -rf /tmp/agentUpgradeBundle/");

            this.log.info("Upgrade (pid:" + ServerUtil.getCurrentPid() + "): Downloading new agent upgrade Bundle.");
            downloadFileSecure(upgradeRequest.getUpgradePackageUrl(), new File(downloadedFilePath));

            this.log.info("Upgrade (pid:" + ServerUtil.getCurrentPid()
                    + ") + Extracting upgrade package received from Server from " + downloadedFilePath
                    + " to /tmp/agentUpgradeBundle/");
            ServerUtil.execWithLog("unzip -q -o " + downloadedFilePath + " -d /tmp/");

            // Temporary rename existing file to make room for new file
            this.log.info("Upgrade (pid:" + ServerUtil.getCurrentPid() + "): Renaming current file.");
            successRename = newAgentFile.renameTo(originalAgentfile);
            if (!successRename) {
                // File was not successfully renamed
                this.log.error("Fail to rename original agent file.");
            }

            this.log.info("Upgrade (pid:" + ServerUtil.getCurrentPid() + ")+ Upgrade Bundle downloaded successfully");
            originalAgentfile.delete();

            Server.upgradeInProgress = true;

            this.log.info("Executing upgrade script");

            String cpaDir = new File("../.").getAbsolutePath();

            ServerUtil.execWithLog("chmod +x /tmp/agentUpgradeBundle/vmidc/bin/scripts/upgrade.sh");
            ServerUtil.execWithLog("/tmp/agentUpgradeBundle/vmidc/bin/scripts/upgrade.sh "
                    + Server.applianceUtils.getName() + " " + VersionUtil.getVersion().getMajor() + " "
                    + VersionUtil.getVersion().getMinor() + " " + VersionUtil.getVersion().getBuild() + " "
                    + cpaDir);

            Thread shutdownThread = null;
            if (!Server.applianceUtils.isCpaWatchDogEnabled()) {
                this.log.info("CPA Watchdog is not enabled. Upgrade (pid:" + ServerUtil.getCurrentPid() + ") Starting new agent process");
                boolean successStarted = startNewAgent();
                if (!successStarted) {
                    throw new Exception("Fail to verify newly upgraded agent is running.");
                }
            } else {
                shutdownThread = new Thread("Shutdown-Thread") {
                    @Override
                    public void run() {
                        try {
                            /*
                             * Introduce a slight delay so REST response can be
                             * completed
                             */
                            Thread.sleep(500);
                            Server.stopServer();
                        } catch (Exception e) {
                            AgentApis.this.log.error(
                                    "Error shutting down server as part of upgrade (pid:" + ServerUtil.getCurrentPid()
                                            + ")", e);
                        }
                    }
                };
            }

            this.log.info("Upgrade (pid:" + ServerUtil.getCurrentPid() + "): Deleting original file.");
            originalAgentfile.delete();

            this.log.info("Upgrade (pid:" + ServerUtil.getCurrentPid() + "): Cleaning up /tmp/");
            ServerUtil.execWithLog("rm -rf /tmp/agentUpgradeBundle");
            ServerUtil.execWithLog("rm -rf /tmp/agentUpgradeBundle.zip");

            upgradeResponse.setUpgradeStatus("Upgrade process started successfully (PID: " + ServerUtil.getCurrentPid()
                    + ")");
            this.log.info("================= End agent upgrade =====================");
            if (shutdownThread != null) {
                this.log.info("Shutting down server as part of upgrade. Assuming CPA watchdog will start the upgraded server.");
                shutdownThread.start();
            }

        } catch (Exception ex) {
            Server.upgradeInProgress = false;
            // Restore back original agent file.
            if (successRename) {
                if (originalAgentfile.exists()) {
                    newAgentFile.delete();
                }
                originalAgentfile.renameTo(newAgentFile);
            }

            this.log.error("Upgrade (pid:" + ServerUtil.getCurrentPid() + "): Error upgrading agent.", ex);
            upgradeResponse.setUpgradeStatus(ex.getMessage());
        }
        return Response.status(Status.OK).entity(upgradeResponse).build();
    }

    private void downloadFileSecure(URL url, File outFile) throws Exception {

        final SSLContext sslContext = new SslContextProvider().getSSLContext();
        HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());

        // Create all-trusting host name verifier
        HostnameVerifier allHostsValid = new HostnameVerifier() {
            @Override
            public boolean verify(String hostname, SSLSession session) {
                return true;
            }
        };
        // Install the all-trusting host verifier
        HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);

        URLConnection connection = url.openConnection();

        InputStream input = connection.getInputStream();
        try {
            FileOutputStream output = new FileOutputStream(outFile);
            try {
                byte[] buffer = new byte[1024 * 4];
                int n = 0;
                while (-1 != (n = input.read(buffer))) {
                    output.write(buffer, 0, n);
                }
            } finally {
                IOUtils.closeQuietly(output);
            }
        } finally {
            IOUtils.closeQuietly(input);
        }
    }

    private boolean startNewAgent() {
        this.log.info("Upgrade (pid:" + ServerUtil.getCurrentPid() + "): Start new agent.");
        return ServerUtil.startAgentProcess();
    }

    @AgentAuth
    @Path("/update-console-password")
    @PUT
    @Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    public Response updateApplianceConsolePassword(AgentUpdateConsolePasswordRequest agentUpdateConsolePasswordRequest)
            throws Exception {
        this.log.info("updateApplianceConsolePassword() called");
        int errorCode = Server.applianceUtils.updateApplianceConsolePassword(
                agentUpdateConsolePasswordRequest.getOldPassword(), agentUpdateConsolePasswordRequest.getNewPassword());
        if (errorCode != 0) {
            return Response.status(Status.INTERNAL_SERVER_ERROR)
                    .entity("Password update failed. Error code " + errorCode).type(MediaType.TEXT_PLAIN).build();
        }
        return Response.status(Status.OK).build();
    }

}
