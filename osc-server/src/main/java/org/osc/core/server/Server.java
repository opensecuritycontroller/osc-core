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
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.lang.time.DurationFormatUtils;
import org.apache.log4j.Logger;
import org.osc.core.broker.job.JobEngine;
import org.osc.core.broker.model.entities.events.SystemFailureType;
import org.osc.core.broker.model.plugin.ApiFactoryService;
import org.osc.core.broker.rest.client.openstack.vmidc.notification.runner.OsDeploymentSpecNotificationRunner;
import org.osc.core.broker.rest.client.openstack.vmidc.notification.runner.OsSecurityGroupNotificationRunner;
import org.osc.core.broker.rest.client.openstack.vmidc.notification.runner.RabbitMQRunner;
import org.osc.core.broker.service.ConformService;
import org.osc.core.broker.service.NsxUpdateAgentsService;
import org.osc.core.broker.service.alert.AlertGenerator;
import org.osc.core.broker.service.api.ArchiveServiceApi;
import org.osc.core.broker.service.api.GetJobsArchiveServiceApi;
import org.osc.core.broker.service.api.RestConstants;
import org.osc.core.broker.service.api.server.EncryptionApi;
import org.osc.core.broker.service.api.server.ServerApi;
import org.osc.core.broker.service.api.server.ServerTerminationListener;
import org.osc.core.broker.service.dto.NetworkSettingsDto;
import org.osc.core.broker.service.persistence.DatabaseUtils;
import org.osc.core.broker.util.PasswordUtil;
import org.osc.core.broker.util.TransactionalBroadcastUtil;
import org.osc.core.broker.util.db.DBConnectionManager;
import org.osc.core.broker.util.db.DBConnectionParameters;
import org.osc.core.broker.util.db.upgrade.ReleaseUpgradeMgr;
import org.osc.core.broker.util.network.NetworkSettingsApi;
import org.osc.core.rest.client.RestBaseClient;
import org.osc.core.server.scheduler.MonitorDistributedApplianceInstanceJob;
import org.osc.core.server.scheduler.SyncDistributedApplianceJob;
import org.osc.core.server.scheduler.SyncSecurityGroupJob;
import org.osc.core.server.websocket.WebSocketRunner;
import org.osc.core.util.FileUtil;
import org.osc.core.util.LogUtil;
import org.osc.core.util.NetworkUtil;
import org.osc.core.util.ServerUtil;
import org.osc.core.util.ServerUtil.TimeChangeCommand;
import org.osc.core.util.VersionUtil;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentServiceObjects;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
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
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

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
    private static final long SERVER_TIME_CHANGE_THRESHOLD = 1000 * 60 * 10; // 10 mins
    private static final long TIME_CHANGE_THREAD_SLEEP_INTERVAL = 1000 * 10; // 10 secs

    private static final Logger log = Logger.getLogger(Server.class);

    private static final Integer DEFAULT_API_PORT = 8090;
    public static final String CONFIG_PROPERTIES_FILE = "vmidcServer.conf";
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
    private ConformService conformService;

    @Reference
    private ApiFactoryService apiFactoryService;

    @Reference
    private NsxUpdateAgentsService nsxUpdateAgentsService;

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

    private Thread thread;
    private BundleContext context;
    private ServiceRegistration<RabbitMQRunner> rabbitMQRegistration;

    @Activate
    void activate(BundleContext context) {
        this.context = context;
        Runnable server = new Runnable() {
            @Override
            public void run() {
                try {
                    startServer();
                } catch (Exception e) {
                    log.error("startServer failed", e);
                }
            }
        };

        this.thread = new Thread(server, "Start-Server");
        this.thread.start();
    }

    private void startServer() throws Exception {
        LogUtil.initLog4j();
        loadServerProps();

        try {
            log.warn("\n");
            log.warn("############ Starting " + Server.PRODUCT_NAME + " Server (pid:" + ServerUtil.getCurrentPid()
            + "). ############");
            log.warn("############ Version: " + VersionUtil.getVersion().getVersionStr() + ". ############");
            log.warn("\n");
            ServerUtil.writePIDToFile(SERVER_PID_FILE);

            NetworkSettingsApi api = new NetworkSettingsApi();
            if (api.getNetworkSettings().isDhcp()) {
                NsxEnv nsxEnv = parseNsxEnvXml();
                if (nsxEnv != null) {
                    try {
                        log.info("Setting network info: " + nsxEnv.toString());
                        if (nsxEnv.hostIpAddress != null && !nsxEnv.hostIpAddress.isEmpty()) {
                            NetworkSettingsDto networkSettingsDto = new NetworkSettingsDto();
                            networkSettingsDto.setDhcp(false);
                            networkSettingsDto.setHostIpAddress(nsxEnv.hostIpAddress);
                            networkSettingsDto.setHostSubnetMask(nsxEnv.hostSubnetMask);
                            networkSettingsDto.setHostDefaultGateway(nsxEnv.hostDefaultGateway);
                            networkSettingsDto.setHostDnsServer1(nsxEnv.hostDnsServer1);
                            networkSettingsDto.setHostDnsServer2(nsxEnv.hostDnsServer2);
                            api.setNetworkSettings(networkSettingsDto);
                        }
                    } catch (Exception ex) {
                        log.error("Failed to read OVF attributes.", ex);
                    }
                }
            }

            ReleaseUpgradeMgr.initDb(this.encryption, this.dbParams, this.dbMgr);
            DatabaseUtils.createDefaultDB(this.dbMgr, this.txBroadcastUtil);
            DatabaseUtils.markRunningJobAborted(this.dbMgr, this.txBroadcastUtil);

            this.passwordUtil.initPasswordFromDb(RestConstants.VMIDC_NSX_LOGIN);
            this.passwordUtil.initPasswordFromDb(RestConstants.OSC_DEFAULT_LOGIN);

            JobEngine.getEngine().addJobCompletionListener(this.alertGenerator);

            startRabbitMq();
            startWebsocket();

            addShutdownHook();
            startScheduler();

            Thread timeMonitorThread = ServerUtil.getTimeMonitorThread(new TimeChangeCommand() {

                @Override
                public void execute(long timeDifference) {
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
                        log.fatal("Cannot start scheduler (pid:" + ServerUtil.getCurrentPid()
                        + ") due to system time change. Will reboot in 15 seconds", se);
                        handleFatalSystemError(se);
                    }
                }
            }, SERVER_TIME_CHANGE_THRESHOLD, TIME_CHANGE_THREAD_SLEEP_INTERVAL);
            timeMonitorThread.start();

            Long reboots = Long.valueOf(loadServerProp("server.reboots", "0"));
            if (reboots != 0) {
                saveServerProp("server.reboots", "0");
            }
        } catch (Throwable e) {
            log.fatal("Cannot start Server (pid:" + ServerUtil.getCurrentPid() + "). Will reboot in 15 seconds", e);
            handleFatalSystemError(e);
        }
    }

    public static class NsxEnv {
        private static final String OVF_ENV_XML_DIR = "/mnt/media";
        private static final String OVF_ENV_XML_FILE = "ovf-env.xml";

        public boolean dhcp;
        public String hostIpAddress;
        public String hostSubnetMask;
        public String hostDefaultGateway;
        public String hostDnsServer1;
        public String hostDnsServer2;
        public String hostname;
        public String defaultCliPassword;
        public String defaultGuiPassword;

        @Override
        public String toString() {
            return "NsxEnv [dhcp=" + this.dhcp + ", hostIpAddress=" + this.hostIpAddress + ", hostSubnetMask="
                    + this.hostSubnetMask + ", hostDefaultGateway=" + this.hostDefaultGateway + ", hostDnsServer1="
                    + this.hostDnsServer1 + ", hostDnsServer2=" + this.hostDnsServer2 + ", hostname=" + this.hostname
                    + ", defaultCliPassword=" + this.defaultCliPassword + ", defaultGuiPassword="
                    + this.defaultGuiPassword + "]";
        }

    }

    public static NsxEnv parseNsxEnvXml() throws ParserConfigurationException, SAXException, IOException {
        File vmwareConf = new File(NsxEnv.OVF_ENV_XML_DIR + File.separator + NsxEnv.OVF_ENV_XML_FILE);
        if (!vmwareConf.exists()) {
            log.info("VMware Virtual Appliance Configuration file missing.");
            return null;
        }

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document document = db.parse(vmwareConf);

        NsxEnv env = new NsxEnv();
        NodeList nodeList = document.getElementsByTagName("Property");
        for (int i = 0, size = nodeList.getLength(); i < size; i++) {
            String key = nodeList.item(i).getAttributes().getNamedItem("oe:key").getTextContent();
            String value = nodeList.item(i).getAttributes().getNamedItem("oe:value").getTextContent();

            if (value == null || value.isEmpty()) {
                continue;
            }

            if (key.equalsIgnoreCase("sbm_gui_passwd_0")) {
                env.defaultGuiPassword = value;
            } else if (key.equalsIgnoreCase("sbm_cli_en_passwd_0")) {
                env.defaultCliPassword = value;
            } else if (key.equalsIgnoreCase("sbm_hostname")) {
                env.hostname = value;
            } else if (key.equalsIgnoreCase("sbm_ip_0")) {
                env.hostIpAddress = value;
            } else if (key.equalsIgnoreCase("sbm_netmask_0")) {
                env.hostSubnetMask = value;
            } else if (key.equalsIgnoreCase("sbm_gateway_0")) {
                env.hostDefaultGateway = value;
            } else if (key.equalsIgnoreCase("sbm_dns1_0")) {
                env.hostDnsServer1 = value;
            } else if (key.equalsIgnoreCase("sbm_dns2_0")) {
                env.hostDnsServer2 = value;
            }
        }

        log.warn("Appliance Env Info= " + env.toString());

        return env;
    }

    private void startScheduler() throws SchedulerException {
        log.warn("Starting scheduler (pid:" + ServerUtil.getCurrentPid() + ")");
        Scheduler scheduler = StdSchedulerFactory.getDefaultScheduler();
        scheduler.start();

        JobDataMap jobDataMap = new JobDataMap();
        jobDataMap.put(ApiFactoryService.class.getName(), this.apiFactoryService);
        jobDataMap.put(ConformService.class.getName(), this.conformService);

        JobDetail syncDaJob = JobBuilder.newJob(SyncDistributedApplianceJob.class).usingJobData(jobDataMap).build();
        JobDetail syncSgJob = JobBuilder.newJob(SyncSecurityGroupJob.class).usingJobData(jobDataMap).build();
        Trigger syncDaJobTrigger = TriggerBuilder.newTrigger().startNow().withSchedule(SimpleScheduleBuilder
                .simpleSchedule().withIntervalInMinutes(this.scheduledSyncInterval).repeatForever()).build();

        Trigger syncSgJobTrigger = TriggerBuilder.newTrigger().startNow().withSchedule(SimpleScheduleBuilder
                .simpleSchedule().withIntervalInMinutes(this.scheduledSyncInterval).repeatForever()).build();

        scheduler.scheduleJob(syncDaJob, syncDaJobTrigger);
        scheduler.scheduleJob(syncSgJob, syncSgJobTrigger);

        MonitorDistributedApplianceInstanceJob.scheduleMonitorDaiJob(this.nsxUpdateAgentsService);

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

        // Shutdown Scheduler
        stopScheduler();

        // Shutdown Job Engine
        JobEngine.getEngine().shutdown();

        // gracefully closing all RabbitMQ clients before shutting down server
        shutdownRabbitMq();

        // Gracefully closing all web socket clients before shutting down server
        shutdownWebsocket();

        // invalidate all Vaadin sessions
        this.terminationListeners.forEach(ServerTerminationListener::serverStopping);

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
        this.deploymentSpecRunnerCSO.ungetService(this.rabbitMQRunner.getOsDeploymentSpecNotificationRunner());
        this.securityGroupRunnerCSO.ungetService(this.rabbitMQRunner.getSecurityGroupRunner());
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
