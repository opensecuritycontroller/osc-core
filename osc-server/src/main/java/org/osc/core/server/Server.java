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
package org.osc.core.server;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.time.Instant;
import java.util.Date;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CopyOnWriteArrayList;
import org.apache.commons.lang.time.DurationFormatUtils;
import org.osc.core.broker.job.JobEngine;
import org.osc.core.broker.model.entities.events.SystemFailureType;
import org.osc.core.broker.model.plugin.ApiFactoryService;
import org.osc.core.broker.rest.client.RestBaseClient;
import org.osc.core.broker.rest.client.openstack.vmidc.notification.runner.OsDeploymentSpecNotificationRunner;
import org.osc.core.broker.rest.client.openstack.vmidc.notification.runner.OsSecurityGroupNotificationRunner;
import org.osc.core.broker.rest.client.openstack.vmidc.notification.runner.RabbitMQRunner;
import org.osc.core.broker.service.DeploymentSpecConformJobFactory;
import org.osc.core.broker.service.DistributedApplianceConformJobFactory;
import org.osc.core.broker.service.ManagerConnectorConformJobFactory;
import org.osc.core.broker.service.SecurityGroupConformJobFactory;
import org.osc.core.broker.service.alert.AlertGenerator;
import org.osc.core.broker.service.api.ArchiveServiceApi;
import org.osc.core.broker.service.api.GetJobsArchiveServiceApi;
import org.osc.core.broker.service.api.RestConstants;
import org.osc.core.broker.service.api.server.EncryptionApi;
import org.osc.core.broker.service.api.server.ServerApi;
import org.osc.core.broker.service.api.server.ServerTerminationListener;
import org.osc.core.broker.service.persistence.DatabaseUtils;
import org.osc.core.broker.service.ssl.X509TrustManagerApi;
import org.osc.core.broker.util.FileUtil;
import org.osc.core.broker.util.NetworkUtil;
import org.osc.core.broker.util.PasswordUtil;
import org.osc.core.broker.util.ServerUtil;
import org.osc.core.broker.util.TransactionalBroadcastUtil;
import org.osc.core.broker.util.VersionUtil;
import org.osc.core.broker.util.db.DBConnectionManager;
import org.osc.core.broker.util.db.DBConnectionParameters;
import org.osc.core.broker.util.db.upgrade.ReleaseUpgradeMgr;
import org.osc.core.broker.util.log.LogUtil;
import org.osc.core.server.scheduler.SyncDistributedApplianceJob;
import org.osc.core.server.scheduler.SyncSecurityGroupJob;
import org.osc.core.server.websocket.WebSocketRunner;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentServiceObjects;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferenceScope;
import org.osgi.util.promise.Deferred;
import org.osgi.util.promise.Promise;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.impl.StdSchedulerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This component exposes both the API and the implementation so that
 * numerous types can access {@link #getActiveRabbitMQRunner()}. Making
 * this part of the {@link ServerApi} would expose a lot of the server
 * internals through the API.
 */
@Component(immediate = true, service = {Server.class, ServerApi.class})
public class Server implements ServerApi {
    // Need to change the package name of Server class to org.osc.core.server

    private static final int SERVER_FATAL_ERROR_REBOOT_TIMEOUT = 15 * 1000;
    private static final int SERVER_TIME_CHANGE_THRESHOLD = 1000 * 60 * 10; // 10 mins
    private static final int TIME_CHANGE_THREAD_SLEEP_INTERVAL = 1000 * 10; // 10 secs
    private static final int SERVER_SYNC_DELAY = 60 * 3; // 3 mins
    private static final String REPLACEMENT_SSL_KEYPAIR_ZIP = "internal.keypair.startup.location";

    private static final Logger log = LoggerFactory.getLogger(Server.class);

    private static final Integer DEFAULT_API_PORT = 8090;
    public static final String CONFIG_PROPERTIES_FILE = "data/vmidcServer.conf";
    private static final String SERVER_PID_FILE = "server.pid";

    public static final String PRODUCT_NAME = "Open Security Controller";
    public static final String SHORT_PRODUCT_NAME = "OSC";
    public static final String ISC_PUBLIC_IP = "publicIP";

    private WebSocketRunner wsRunner = null;
    private RabbitMQRunner rabbitMQRunner = null;

    // static to avoid many trivial references
    private static Integer apiPort = DEFAULT_API_PORT;
    private static volatile boolean inMaintenance = false;

    private int scheduledSyncInterval = 60; // 60 minutes
    private boolean devMode = false;

    @Reference
    private DistributedApplianceConformJobFactory daConformJobFactory;

    @Reference
    private DeploymentSpecConformJobFactory dsConformJobFactory;

    @Reference
    private SecurityGroupConformJobFactory sgConformJobFactory;

    @Reference
    private ManagerConnectorConformJobFactory mcConformJobFactory;

    @Reference
    private ApiFactoryService apiFactoryService;

    @Reference
    private PasswordUtil passwordUtil;

    @Reference
    private ArchiveServiceApi archiveService;

    @Reference
    private GetJobsArchiveServiceApi jobsArchiveService;

    @Reference(service=WebSocketRunner.class,
            scope=ReferenceScope.PROTOTYPE_REQUIRED)
    private ComponentServiceObjects<WebSocketRunner> webSocketFactory;

    @Reference(service=RabbitMQRunner.class,
            scope=ReferenceScope.PROTOTYPE_REQUIRED)
    private ComponentServiceObjects<RabbitMQRunner> rabbitRunnerFactory;

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
    private volatile ComponentServiceObjects<OsSecurityGroupNotificationRunner> securityGroupRunnerCSO;
    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
    private volatile ComponentServiceObjects<OsDeploymentSpecNotificationRunner> deploymentSpecRunnerCSO;

    @Reference(cardinality=ReferenceCardinality.MULTIPLE, policy=ReferencePolicy.DYNAMIC)
    private volatile List<ServerTerminationListener> terminationListeners = new CopyOnWriteArrayList<>();


    @Reference
    private EncryptionApi encryption;

    @Reference
    private DBConnectionParameters dbParams;

    @Reference
    private DBConnectionManager dbMgr;

    @Reference
    private TransactionalBroadcastUtil txBroadcastUtil;

    @Reference
    private AlertGenerator alertGenerator;

    @Reference
    private X509TrustManagerApi x509TrustManager;

    private Thread thread;
    private BundleContext context;
    private ServiceRegistration<RabbitMQRunner> rabbitMQRegistration;

    @Activate
    void activate(BundleContext context) {
        this.context = context;

        if (doReplaceSslKeysAndReboot()) {
            return;
        }

        Runnable server = () -> {
            try {
                startServer();
            } catch (Exception e) {
                log.error("startServer failed", e);
            }
        };

        this.thread = new Thread(server, "Start-Server");
        this.thread.start();
    }

    private void startServer() throws Exception {
        LogUtil.redirectConsoleMessagesToLog();
        loadServerProps();

        try {
            log.warn("\n");
            log.warn("############ Starting " + Server.PRODUCT_NAME + " Server (pid:" + ServerUtil.getCurrentPid()
            + "). ############");
            log.warn("############ Version: " + VersionUtil.getVersion().getVersionStr() + ". ############");
            log.warn("\n");
            ServerUtil.writePIDToFile(SERVER_PID_FILE);
            ReleaseUpgradeMgr.initDb(this.encryption, this.dbParams, this.dbMgr);
            DatabaseUtils.createDefaultDB(this.dbMgr, this.txBroadcastUtil);
            DatabaseUtils.markRunningJobAborted(this.dbMgr, this.txBroadcastUtil);

            this.passwordUtil.initPasswordFromDb(RestConstants.OSC_DEFAULT_LOGIN);

            JobEngine.getEngine().addJobCompletionListener(this.alertGenerator);

            startRabbitMq();
            startWebsocket();

            addShutdownHook();
            startScheduler();

            Thread timeMonitorThread = ServerUtil.getTimeMonitorThread(timeDifference -> {
                String timeDifferenceString = DurationFormatUtils.formatDuration(Math.abs(timeDifference),
                        "d 'Days' H 'Hours' m 'Minutes' s 'Seconds'");
                if (timeDifference < 0) {
                    Server.this.alertGenerator.processSystemFailureEvent(SystemFailureType.SYSTEM_CLOCK,
                            "System Clock Moved Back by " + timeDifferenceString);
                } else {
                    Server.this.alertGenerator.processSystemFailureEvent(SystemFailureType.SYSTEM_CLOCK,
                            "System Clock Moved Forward by " + timeDifferenceString);
                }
                stopScheduler();
                try {
                    startScheduler();
                } catch (SchedulerException se) {
                    log.error("Cannot start scheduler (pid:" + ServerUtil.getCurrentPid()
                    + ") due to system time change. Will reboot in 15 seconds", se);
                    handleFatalSystemError(se);
                }
            }, SERVER_TIME_CHANGE_THRESHOLD, TIME_CHANGE_THREAD_SLEEP_INTERVAL);
            timeMonitorThread.start();

            Long reboots = Long.valueOf(loadServerProp("server.reboots", "0"));
            if (reboots != 0) {
                saveServerProp("server.reboots", "0");
            }
        } catch (Throwable e) {
            log.error("Cannot start Server (pid:" + ServerUtil.getCurrentPid() + "). Will reboot in 15 seconds", e);
            handleFatalSystemError(e);
        }
    }

    public static class EnvironmentProperties {

        private boolean dhcp;
        private String hostIpAddress;
        private String hostSubnetMask;
        private String hostDefaultGateway;
        private String hostDnsServer1;
        private String hostDnsServer2;
        private String hostname;
        private String defaultCliPassword;
        private String defaultGuiPassword;

        @Override
        public String toString() {
            return "EnvProp [dhcp=" + this.dhcp + ", hostIpAddress=" + this.hostIpAddress + ", hostSubnetMask="
                    + this.hostSubnetMask + ", hostDefaultGateway=" + this.hostDefaultGateway + ", hostDnsServer1="
                    + this.hostDnsServer1 + ", hostDnsServer2=" + this.hostDnsServer2 + ", hostname=" + this.hostname
                    + ", defaultCliPassword=" + this.defaultCliPassword + ", defaultGuiPassword="
                    + this.defaultGuiPassword + "]";
        }

    }

    private void startScheduler() throws SchedulerException {
        log.warn("Starting scheduler (pid:" + ServerUtil.getCurrentPid() + ")");
        Scheduler scheduler = StdSchedulerFactory.getDefaultScheduler();
        scheduler.start();

        JobDataMap jobDataMap = new JobDataMap();
        jobDataMap.put(ApiFactoryService.class.getName(), this.apiFactoryService);
        jobDataMap.put(DistributedApplianceConformJobFactory.class.getName(), this.daConformJobFactory);
        jobDataMap.put(DeploymentSpecConformJobFactory.class.getName(), this.dsConformJobFactory);
        jobDataMap.put(SecurityGroupConformJobFactory.class.getName(), this.sgConformJobFactory);
        jobDataMap.put(ManagerConnectorConformJobFactory.class.getName(), this.mcConformJobFactory);


        JobDetail syncDaJob = JobBuilder.newJob(SyncDistributedApplianceJob.class).usingJobData(jobDataMap).build();
        JobDetail syncSgJob = JobBuilder.newJob(SyncSecurityGroupJob.class).usingJobData(jobDataMap).build();

		// TODO: Remove the delay, once plugin state listener is implemented.
        // Related issue: https://github.com/opensecuritycontroller/osc-core/issues/545
		Trigger syncDaJobTrigger = TriggerBuilder.newTrigger()
				.startAt(Date.from(Instant.now().plusSeconds(SERVER_SYNC_DELAY))).withSchedule(SimpleScheduleBuilder
				.simpleSchedule().withIntervalInMinutes(this.scheduledSyncInterval).repeatForever()).build();

		Trigger syncSgJobTrigger = TriggerBuilder.newTrigger()
				.startAt(Date.from(Instant.now().plusSeconds(SERVER_SYNC_DELAY))).withSchedule(SimpleScheduleBuilder
				.simpleSchedule().withIntervalInMinutes(this.scheduledSyncInterval).repeatForever()).build();

        scheduler.scheduleJob(syncDaJob, syncDaJobTrigger);
        scheduler.scheduleJob(syncSgJob, syncSgJobTrigger);

        this.archiveService.maybeScheduleArchiveJob();
    }

    private void stopScheduler() {
        log.warn("Stopping scheduler (pid:" + ServerUtil.getCurrentPid() + ")");
        try {
            // Grab the Scheduler instance from the Factory
            Scheduler scheduler = StdSchedulerFactory.getDefaultScheduler();

            // and start it off
            scheduler.shutdown();

        } catch (SchedulerException se) {
            log.error("Scheduler fail to stop", se);
        }
    }

    void loadServerProps() {
        Properties prop;
        try {
            prop = FileUtil.loadProperties(Server.CONFIG_PROPERTIES_FILE);
        } catch (IOException e) {
            log.error("Failed to write to the properties file - properties file was not loaded", e);
            return;
        }

        try {
            setApiPort(Integer.valueOf(prop.getProperty("server.port", DEFAULT_API_PORT.toString())));
            this.scheduledSyncInterval = Integer.parseInt(prop.getProperty("server.syncJobport", "60"));
            this.devMode = Boolean.valueOf(prop.getProperty(DEV_MODE_PROPERTY_KEY, "false"));
            //set ISC public IP in Server Util
            ServerUtil.setServerIP(prop.getProperty(ISC_PUBLIC_IP, ""));
            JobEngine.setJobThreadPoolSize(prop.getProperty("server.jobThreadPoolSize"));
            JobEngine.setTaskThreadPoolSize(prop.getProperty("server.taskThreadPoolSize"));
        } catch (Exception e) {
            log.error("Warning: Parsing file failed " + Server.CONFIG_PROPERTIES_FILE + " (Error:" + e.getMessage()
                    + ")");
        }
    }

    @Override
    public String loadServerProp(String propName, String defaultValue) {
        Properties properties = new Properties();
        try {
            properties = FileUtil.loadProperties(Server.CONFIG_PROPERTIES_FILE);
        } catch (IOException e) {
            log.error("Failed to open to the properties file - properties file was not loaded", e);
        }

        return properties.getProperty(propName, defaultValue);
    }

    @Override
    public void saveServerProp(String propName, String value) {
        Properties prop;

        try {
            prop = FileUtil.loadProperties(Server.CONFIG_PROPERTIES_FILE);
        } catch (IOException e) {
            log.error("Failed to open to the properties file - properties file was not loaded", e);
            return;
        }

        // Write to the properties file
        prop.setProperty(propName, value);
        try (FileOutputStream out = new FileOutputStream(Server.CONFIG_PROPERTIES_FILE)) {
            prop.store(out, null);
        } catch (Exception e) {
            log.error("Failed to write to the properties file", e);
        }
    }

    private boolean doReplaceSslKeysAndReboot() {
        if (!ReleaseUpgradeMgr.isLastUpgradeSucceeded()) {
            return false;
        }

        // This method is just before the startup. It cannot be allowed to throw.
        try {
            Properties prop = FileUtil.loadProperties(Server.CONFIG_PROPERTIES_FILE);
            String replacementKeypairLocation = prop.getProperty(REPLACEMENT_SSL_KEYPAIR_ZIP);
            if (replacementKeypairLocation != null) {
                File zipFile = new File(replacementKeypairLocation);
                if (zipFile.exists()) {
                    log.info("New ssl key pair file located at " + replacementKeypairLocation
                            + ". Replacing and rebooting !!");
                    this.x509TrustManager.replaceInternalCertificate(zipFile, true);
                    return true;
                }
            }
        } catch (Exception e) {
            log.error("Exception replacing internal ssl key pair: ", e);
        }

        return false;
    }

    private void addShutdownHook() {
        log.warn(Server.PRODUCT_NAME + " Server: Shutdown Hook...");
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                stopServer();
            }
        });
    }

    @Override
    public void stopServer() {
        System.out.print(Server.PRODUCT_NAME + ": Server shutdown...");
        log.warn(Server.PRODUCT_NAME + ": Shutdown server...(pid:" + ServerUtil.getCurrentPid() + ")");

        doStop();

        ServerUtil.deletePidFileIfOwned(SERVER_PID_FILE);

        log.warn("\n");
        log.warn("************ " + Server.PRODUCT_NAME + ": Server shutdown completed (pid:"
                + ServerUtil.getCurrentPid() + "). ************");
        log.warn("************ Version: " + VersionUtil.getVersion().getVersionStr() + ". ************");
        log.warn("\n");

        /*
         * Terminate process as last resort to ensure dangling threads terminate
         */
        System.out.println("Shutdown completed");
        Runtime.getRuntime().halt(0);
    }

    @Deactivate
    void deactivateServer() {
    	System.out.print(Server.PRODUCT_NAME + ": Server dynamically restarting...");
        log.warn(Server.PRODUCT_NAME + ": Restarting server component...(pid:" + ServerUtil.getCurrentPid() + ")");
        doStop();
    }

	private void doStop() {
		// Shutdown Scheduler
        stopScheduler();

        // Shutdown Job Engine
        try {
        	JobEngine.getEngine().shutdown();
        } catch (Exception e) {
        	log.warn(Server.PRODUCT_NAME + ": Error shutting down the JobEngine", e);
        }

        // gracefully closing all RabbitMQ clients before shutting down server
        shutdownRabbitMq();

        // Gracefully closing all web socket clients before shutting down server
        shutdownWebsocket();

        // invalidate all Vaadin sessions
        this.terminationListeners.forEach(ServerTerminationListener::serverStopping);
	}

    public static Integer getApiPort() {
        return Server.apiPort;
    }

    private void setApiPort(Integer port) {
        Server.apiPort = port;
    }

    @Override
    public boolean getDevMode() {
        return this.devMode;
    }

    @Override
    public void setDevMode(boolean devMode) {
        this.devMode = devMode;
    }

    public int getScheduledSyncInterval(int ssInterval) {
        return this.scheduledSyncInterval;
    }

    public void setScheduledSyncInterval(int ssInterval) {
        this.scheduledSyncInterval = ssInterval;
    }

    @Override
    public Promise<Void> restart() {

        Deferred<Void> future = new Deferred<>();
        Thread restartThread = new Thread("Restart-Server-Thread") {
            @Override
            public void run() {
                Server.inMaintenance = true;
                try {
                    log.info("Restarting server.");
                    boolean successStarted = startNewServer();
                    if (!successStarted) {
                        throw new Exception("Fail to start new server process.");
                    }
                    log.info("Restart server (pid:" + ServerUtil.getCurrentPid() + ")");

                    future.resolve(null);
                } catch (Exception ex) {
                    future.fail(ex);
                }
            }
        };
        restartThread.start();
        return future.getPromise();
    }

    public void startRabbitMq() {
        Dictionary<String, Object> props = new Hashtable<>();
        props.put("active", "true");
        this.rabbitMQRunner = this.rabbitRunnerFactory.getService();
        this.rabbitMQRegistration = this.context.registerService(RabbitMQRunner.class, this.rabbitMQRunner, props);
        this.rabbitMQRunner.setDeploymentSpecRunner(this.deploymentSpecRunnerCSO.getService());
        this.rabbitMQRunner.setsecurityGroupRunner(this.securityGroupRunnerCSO.getService());
        log.info("Started RabbitMQ Runner");
    }

    public void shutdownRabbitMq() {
        try {
            this.rabbitMQRegistration.unregister();
        } catch (IllegalStateException ise) {
            // No problem - this means the service was
            // already unregistered (e.g. by bundle stop)
        }
        ComponentServiceObjects<OsDeploymentSpecNotificationRunner> cso = this.deploymentSpecRunnerCSO;
        if(cso != null) {
        	cso.ungetService(this.rabbitMQRunner.getOsDeploymentSpecNotificationRunner());
        }

        ComponentServiceObjects<OsSecurityGroupNotificationRunner> cso2 = this.securityGroupRunnerCSO;
		if(cso2 != null) {
			cso2.ungetService(this.rabbitMQRunner.getSecurityGroupRunner());
		}

        this.rabbitRunnerFactory.ungetService(this.rabbitMQRunner);
        log.info("Shutdown of RabbitMQ succeeded");
        this.rabbitMQRunner = null;
    }

    public void startWebsocket() {
        /*
         * Calling web Socket Runner here.
         * This class is responsible for creating web socket communication with all existing SMCs upon server
         * start/restart
         */
        this.wsRunner = this.webSocketFactory.getService();
        log.info("Started Web Socket Runner");
    }

    public void shutdownWebsocket() {
        this.webSocketFactory.ungetService(this.wsRunner);
        log.info("Shutdown of WebSocket succeeded");
        this.wsRunner = null;
    }

    @Override
    public boolean isUnderMaintenance() {
        return Server.inMaintenance;
    }

    public static boolean isInMaintenance() {
        return Server.inMaintenance;
    }

    public static void setInMaintenance(boolean inMaintenance) {
        Server.inMaintenance = inMaintenance;
    }

    private boolean startNewServer() {
        log.info("Restart (pid:" + ServerUtil.getCurrentPid() + "): Start new vmidc server.");
        return ServerUtil.startServerProcess(2000, null, true);
    }

    private void handleFatalSystemError(Throwable e) {
        if (!ServerUtil.isWindows()) {
            Long reboots = Long.valueOf(loadServerProp("server.reboots", "0"));
            log.warn("Reboot count " + reboots);
            if (reboots < 1) {
                try {
                    Thread.sleep(SERVER_FATAL_ERROR_REBOOT_TIMEOUT);
                } catch (InterruptedException e1) {
                    log.warn("Reboot time out interrupted.", e1);
                }
                reboots++;
                saveServerProp("server.reboots", reboots.toString());

                ServerUtil.execWithLog("/sbin/reboot");
            }
        }
    }

    @Override
    public String getProductName() {
        return Server.PRODUCT_NAME;
    }

    @Override
    public String getCurrentPid() {
        return ServerUtil.getCurrentPid();
    }

    @Override
    public String getVersionStr() {
        return VersionUtil.getVersion().getVersionStr();
    }

    @Override
    public void setDebugLogging(boolean on) {
        RestBaseClient.setDebugLogging(on);
    }

    @Override
    public String getHostIpAddress() throws SocketException, UnknownHostException {
        return NetworkUtil.getHostIpAddress();
    }

    @Override
    public String getServerIpAddress() {
        return ServerUtil.getServerIP();
    }

    @Override
    public boolean isEnoughSpace() throws IOException {
        return ServerUtil.isEnoughSpace();
    }

    @Override
    public String uptimeToString() {
        return ServerUtil.uptimeToString();
    }
}
