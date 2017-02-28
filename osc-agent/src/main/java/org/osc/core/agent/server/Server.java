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
package org.osc.core.agent.server;

import com.sun.jersey.api.core.DefaultResourceConfig;
import com.sun.jersey.spi.container.servlet.ServletContainer;
import org.apache.catalina.Context;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.LifecycleState;
import org.apache.catalina.Service;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.startup.Tomcat;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.log4j.Logger;
import org.osc.core.agent.dpaipc.DpaIpcClient;
import org.osc.core.agent.rest.server.AgentApis;
import org.osc.core.agent.rest.server.AgentAuthFilter;
import org.osc.core.broker.model.virtualization.VirtualizationEnvironmentProperties;
import org.osc.core.rest.client.VmidcAgentServerRestClient;
import org.osc.core.rest.client.agent.model.output.AgentStatusResponse;
import org.osc.core.util.EncryptionUtil;
import org.osc.core.util.FileUtil;
import org.osc.core.util.LogUtil;
import org.osc.core.util.ServerStatusResponseInjection;
import org.osc.core.util.ServerUtil;
import org.osc.core.util.ServerUtil.ServerServiceChecker;
import org.osc.core.util.ServerUtil.TimeChangeCommand;
import org.osc.core.util.VersionUtil;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.impl.StdSchedulerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

public class Server {
    private static Logger log = Logger.getLogger(Server.class);

    public static final String CONFIG_PROPERTIES_FILE = "vmidcAgent.conf";
    private static final String SERVER_PORT = "server.port";
    private static final String APPLIANCE_TYPE = "applianceType";
    private static final Integer DEFAULT_PORT = 8090;

    public static final String PRODUCT_NAME = "Open Security Controller";
    public static final String SHORT_PRODUCT_NAME = "OSC";

    private static final long TIME_CHANGE_THREAD_SLEEP_INTERVAL = 1000; // 1 sec
    private static final long SERVER_TIME_CHANGE_THRESHOLD = 1000 * 60 * 2; // 2 mins

    public static boolean upgradeInProgress = false;

    public static Tomcat tomcat = null;
    public static DpaIpcClient dpaipc = null;
    public static ApplianceUtils applianceUtils = null;
    public static Integer port = DEFAULT_PORT;

    public static String applianceIp = null;
    public static String applianceName = null;
    public static String sharedSecretKey = null;
    public static String managerIp = null;
    private static String vmidcServerIp = null;
    private static String vmidcServerPassword = null;
    private static String applianceType;
    public static String applianceGateway = null;
    public static String applianceSubnetMask = null;
    public static String applianceMtu = null;

    public static long serverProcessStart = System.nanoTime();

    public static long getUptimeMilli() {
        long endTime = System.nanoTime();
        long elapsedTime = endTime - serverProcessStart;
        return (long) (elapsedTime / 1.0E06);
    }

    public static String getVmidcServerIp() {
        return vmidcServerIp;
    }

    public static void setVmidcServerIp(String vmidcServerIp) {
        Server.vmidcServerIp = vmidcServerIp;
    }

    public static String getVmidcServerPassword() {
        return vmidcServerPassword;
    }

    public static void setVmidcServerPassword(String vmidcServerPassword) {
        Server.vmidcServerPassword = vmidcServerPassword;
    }

    public static void main(final String[] args) {
        LogUtil.initLog4j();
        loadServerProps();

        Server.applianceUtils = ApplianceUtilFactory.createApplianceUtils(applianceType);

        final Options options = new Options();
        options.addOption("s", "stop", false, "Stop " + PRODUCT_NAME + " Agent service.");
        options.addOption("t", "start", false, "Start " + PRODUCT_NAME + " Agent service.");
        options.addOption("c", "console", false, "Start " + PRODUCT_NAME + " Agent in console mode.");
        options.addOption("u", "status", false, "Report " + PRODUCT_NAME + "r Agent service status.");
        options.addOption("v", "version", false, "Print " + PRODUCT_NAME + " version.");
        options.addOption("h", "help", false, "Print options help.");

        final CommandLineParser parser = new GnuParser();
        try {
            CommandLine cmd = parser.parse(options, args, true);

            if (cmd.hasOption("stop")) {
                ServerUtil.stopServerProcess(Server.applianceUtils.getPidFilePath());
            } else if (cmd.hasOption("start")) {
                ServerUtil.startAgentProcess(20000, new ServerServiceChecker() {
                    @Override
                    public boolean isRunning() {
                        return isRunningAgentServer();
                    }
                });
            } else if (cmd.hasOption("version")) {
                reportVersion();
            } else if (cmd.hasOption("status")) {
                reportStatus();
            } else if (cmd.hasOption("help")) {
                HelpFormatter formatter = new HelpFormatter();
                formatter.printHelp("vmiDCAgent", options);
            } else if (cmd.hasOption("console")) {
                startServer();
            } else {
                startServer();
            }
        } catch (ParseException e) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("vmiDCAgent", options);
        }

    }

    private static void reportVersion() {
        System.out.println(Server.PRODUCT_NAME + " version: " + VersionUtil.getVersion().getVersionStr());
    }

    private static void loadServerProps() {
        try {
            Properties prop = FileUtil.loadProperties(Server.CONFIG_PROPERTIES_FILE);

            Server.port = Integer.valueOf(prop.getProperty(SERVER_PORT, DEFAULT_PORT.toString()));
            Server.applianceType = prop.getProperty(Server.APPLIANCE_TYPE);
            if (Server.applianceType == null) {
                Server.applianceType = discoverApplianceType();
                persistApplianceType();
            }
            Server.sharedSecretKey = prop.getProperty(Register.SHARED_SECRET_KEY);
            Server.managerIp = prop.getProperty(Register.MANAGER_IP);
            Server.applianceIp = prop.getProperty(Register.APPLIANCE_IP);
            Server.applianceGateway = prop.getProperty(Register.APPLIANCE_GATEWAY);
            Server.applianceSubnetMask = prop.getProperty(Register.APPLIANCE_SUBNET_MASK);
            Server.applianceMtu = prop.getProperty(Register.APPLIANCE_MTU);
            Server.applianceName = prop.getProperty(VirtualizationEnvironmentProperties.APPLIANCE_NAME);
            Server.vmidcServerIp = prop.getProperty(VirtualizationEnvironmentProperties.VMIDC_IP);
            Server.vmidcServerPassword = prop.getProperty(VirtualizationEnvironmentProperties.VMIDC_PASSWORD);
            if (Server.vmidcServerPassword != null) {
                AgentAuthFilter.AGENT_DEFAULT_PASS = EncryptionUtil.decryptAESCTR(Server.vmidcServerPassword);
            }
        } catch (Exception e) {
            log.error("Unexpected behavior occurred in loading server properties", e);
        }
    }

    private static String discoverApplianceType() {
        if (new File("/tftpboot/vmidc/bin/vmiDCAgent.jar").exists()) {
            return "vnsp";
        } else if (new File("/spool/cpa/bin/vmiDCAgent.jar").exists()) {
            return "ngfw";
        } else {
            return "generic";
        }
    }

    static void saveProperties(Properties prop) {
        try (FileOutputStream out = new FileOutputStream(Server.CONFIG_PROPERTIES_FILE)){
            prop.store(out, null);

            // Persist config file for next reboot
            Server.applianceUtils.persistFile(Server.CONFIG_PROPERTIES_FILE);
        } catch (Exception e) {
            log.error("Failed to write to the properties file", e);
        }
    }

    private static void persistApplianceType() throws Exception {
        if (applianceType == null) {
            throw new Exception("Unknown Appliance Type");
        }

        // Now we need to initialize based on discovered appliance type so we can persist file for next reboot
        Server.applianceUtils = ApplianceUtilFactory.createApplianceUtils(applianceType);

        Properties prop;
        try {
            prop = FileUtil.loadProperties(Server.CONFIG_PROPERTIES_FILE);
        } catch (IOException e) {
            log.warn("Failed to update properties file - properties file was not loaded");
            return;
        }

        prop.setProperty(APPLIANCE_TYPE, applianceType);
        saveProperties(prop);
    }

    private static void reportStatus() {
        System.out.print(Server.PRODUCT_NAME + " Agent is ");
        if (isRunningAgentServer()) {
            System.out.println("running.");
        } else {
            System.out.println("not running.");
        }
    }

    private static void startServer() {
        Server.applianceUtils.cpaInitOnce();

        // Checking if an agent is already running.
        if (isRunningAgentServer()) {
            System.out.println(Server.PRODUCT_NAME + " Agent is already running.");
            System.exit(2); // already running
        }

        log.info("\n");
        log.info("############ Starting " + Server.PRODUCT_NAME + " Agent (pid:" + ServerUtil.getCurrentPid()
                + "). ############");
        log.info("############ Version: " + VersionUtil.getVersion().getVersionStr() + ". ############");
        log.info("\n");
        addShutdownHook();
        ServerUtil.writePIDToFile(Server.applianceUtils.getPidFilePath());

        dpaipc = new DpaIpcClient();
        startScheduler();
        startTomcatThread();

        Thread timeMonitorThread = ServerUtil.getTimeMonitorThread(new TimeChangeCommand() {

            @Override
            public void execute(long timeDifference) {
                stopScheduler();
                startScheduler();
            }
        }, SERVER_TIME_CHANGE_THRESHOLD, TIME_CHANGE_THREAD_SLEEP_INTERVAL);
        timeMonitorThread.start();
    }

    public static void stopServer() {
        System.out.print(Server.PRODUCT_NAME + " Agent: shutdown ...");
        log.info(Server.PRODUCT_NAME + " Agent: Shutdown agent... (pid:" + ServerUtil.getCurrentPid() + ")");
        stopScheduler();
        stopTomcat();
        ServerUtil.deletePidFileIfOwned(Server.applianceUtils.getPidFilePath());

        log.info("\n");
        log.info("************ " + Server.PRODUCT_NAME + ": Agent shutdown completed (pid:"
                + ServerUtil.getCurrentPid() + "). ************");
        log.info("************ Version: " + VersionUtil.getVersion().getVersionStr() + ". ************");
        log.info("\n");
        /*
         * Terminate process as last resort to ensure dangling process
         * termination
         */
        ServerUtil.execWithLog("kill " + ServerUtil.getCurrentPid());
        System.out.println("Shutdown completed");
        Runtime.getRuntime().halt(0);
    }

    private static void startScheduler() {
        log.info("Starting Scheduler (pid:" + ServerUtil.getCurrentPid() + ")");
        try {
            // Grab the Scheduler instance from the Factory
            Scheduler scheduler = StdSchedulerFactory.getDefaultScheduler();
            scheduler.start();
            scheduleAgentRegistrationJob(scheduler);
        } catch (SchedulerException se) {
            log.fatal("Scheduler fail to start", se);
            // Stop the server, assume watchdog restarts process
            stopServer();
        }
    }

    private static void scheduleAgentRegistrationJob(Scheduler scheduler) throws SchedulerException {
        JobDetail job = JobBuilder.newJob(RegisterJob.class).build();

        Trigger trigger = TriggerBuilder.newTrigger().startNow()
                .withSchedule(SimpleScheduleBuilder.simpleSchedule().withIntervalInSeconds(3 * 60).repeatForever())
                .build();

        scheduler.scheduleJob(job, trigger);
    }

    private static void stopScheduler() {
        log.info("Stopping Scheduler (pid:" + ServerUtil.getCurrentPid() + ")");
        try {
            Scheduler scheduler = StdSchedulerFactory.getDefaultScheduler();
            scheduler.shutdown();

        } catch (SchedulerException se) {
            log.error("Scheduler fail to start", se);
        }
    }

    public static void startTomcatThread() {
        log.info("Starting Tomcat (pid:" + ServerUtil.getCurrentPid() + ")");

        Thread tomcatThread = new Thread("Tomcat Thread") {
            @Override
            public void run() {
                try {
                    upgradeInProgress = false;

                    tomcat = new Tomcat();

                    String currentDir = "";
                    try {
                        currentDir = new java.io.File(".").getCanonicalPath();
                        tomcat.setBaseDir(currentDir);

                    } catch (IOException e) {
                        log.error("Error getting current tomcat base directory");
                    }

                    deployResfulServlet("/api/agent", AgentApis.class, "restfulAgentApisServlet");

                    // support for https
                    enableSsl(currentDir);

                    log.info("Starting Tomcat server");
                    tomcat.start();
                    tomcat.getServer().await();

                    log.info("Tomcat server stopped.");

                } catch (LifecycleException ex) {
                    log.error("Failed to start tomcat server.", ex);
                }
            }

        };

        tomcatThread.start();
    }

    private static void addShutdownHook() {
        log.info(Server.PRODUCT_NAME + " Agent: Shutdown Hook...");
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                log.info("addShutdownHook() called.");
                stopServer();
            }
        });
    }

    private static synchronized void stopTomcat() {
        if (tomcat == null) {
            return;
        }
        log.info("Stopping Tomcat (pid:" + ServerUtil.getCurrentPid() + ")");
        try {
            tomcat.stop();
            // wait for the server to stop.
            while (LifecycleState.STOPPING == tomcat.getServer().getState()
                    || LifecycleState.STOPPING_PREP == tomcat.getServer().getState()) {
                Thread.sleep(500);
            }
            tomcat.destroy();
            tomcat = null;
            log.info("Tomcat stopped.");
        } catch (Exception ex) {
            log.error("Shutting down web server failed.", ex);
        }
    }

    private static boolean isRunningAgentServer() {
        log.info("Check if agent is running ...");
        final VmidcAgentServerRestClient restClient = new VmidcAgentServerRestClient(
                "localhost", port, AgentAuthFilter.AGENT_DEFAULT_LOGIN, AgentAuthFilter.AGENT_DEFAULT_PASS, true);

        AgentStatusResponse res;
        try {
            res = restClient.getResource("status", AgentStatusResponse.class);
        } catch (Exception e) {
            log.warn("Fail to connect to running agent. Assuming not running. " + e.getMessage(), e);
            return false;
        }

        if(res == null) {
            return false;
        }

        String oldPid = res.getCpaPid();

        return ServerUtil.terminateProcessInRunningServer(restClient, oldPid, new ServerStatusResponseInjection() {
            @Override
            public String getProcessId() throws Exception {
                AgentStatusResponse resource = restClient.getResource("status", AgentStatusResponse.class);
                return (resource != null) ? resource.getCpaPid() : null;
            }
        });
    }

    private static void deployResfulServlet(String path, Class<?> servletClass, String servletName) {
        Context agentCtx = tomcat.addContext(path, new File(".").getAbsolutePath());
        DefaultResourceConfig resourceConfig = new DefaultResourceConfig(servletClass);
        Tomcat.addServlet(agentCtx, servletName, new ServletContainer(resourceConfig));
        agentCtx.addServletMapping("/*", servletName);
    }

    private static void enableSsl(String currentDir) {
        Connector httpsConnector = new Connector();
        httpsConnector.setPort(port);
        httpsConnector.setSecure(true);
        httpsConnector.setScheme("https");
        httpsConnector.setAttribute("keyAlias", "vmidcKeyStore");
        httpsConnector.setAttribute("keystorePass", "abc12345");
        httpsConnector.setAttribute("keystoreFile", currentDir + File.separator + "agentKeyStore.jks");
        httpsConnector.setAttribute("clientAuth", "false");
        httpsConnector.setAttribute("sslProtocol", "TLS");
        httpsConnector.setAttribute("SSLEnabled", true);

        Service service = tomcat.getService();
        service.addConnector(httpsConnector);
        tomcat.setConnector(httpsConnector);
        Connector defaultConnector = tomcat.getConnector();
        defaultConnector.setRedirectPort(port);
    }

}
