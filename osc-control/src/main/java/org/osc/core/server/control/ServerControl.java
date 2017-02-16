package org.osc.core.server.control;

import java.io.FileInputStream;
import java.net.ConnectException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.log4j.Logger;
import org.osc.core.broker.rest.server.model.ServerStatusResponse;
import org.osc.core.broker.view.maintenance.SslConfigurationLayout;
import org.osc.core.rest.client.VmidcServerRestClient;
import org.osc.core.rest.client.crypto.SslCertificateResolver;
import org.osc.core.rest.client.crypto.X509TrustManagerFactory;
import org.osc.core.rest.client.crypto.model.CertificateResolverModel;
import org.osc.core.rest.client.exception.RestClientException;
import org.osc.core.util.LogUtil;
import org.osc.core.util.ServerUtil;
import org.osc.core.util.ServerUtil.ServerServiceChecker;
import org.osc.core.util.VersionUtil;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;

public class ServerControl {
    private static final Logger log = Logger.getLogger(ServerControl.class);

    private static final Integer DEFAULT_API_PORT = 8090;
    private static final String CONFIG_PROPERTIES_FILE = "vmidcServer.conf";
    private static final String SERVER_PID_FILE = "server.pid";

    private static final String PRODUCT_NAME = "Open Security Controller";
    private static final String ISC_PUBLIC_IP = "publicIP";

    private static Integer apiPort = DEFAULT_API_PORT;

    public static final String VMIDC_DEFAULT_LOGIN = "admin";
    public static final String VMIDC_DEFAULT_PASS = "admin123";

    public static void main(final String[] args) throws Exception {

        LogUtil.initLog4j();

        loadServerProps();

        final Options options = new Options();
        options.addOption("s", "stop", false, "Stop " + PRODUCT_NAME + " Server.");
        options.addOption("t", "start", false, "Start " + PRODUCT_NAME + " Server.");
        options.addOption("u", "status", false, "Report " + PRODUCT_NAME + " Server status.");
        options.addOption("c", "check", false, "Check if server is running.");
        options.addOption("v", "version", false, "Print server version.");
        options.addOption("h", "help", false, "Print options help.");
        options.addOption("l", "lock", false, "Prints Currently acquired Locks.");

        @SuppressWarnings("static-access")
        Option exec = OptionBuilder.withArgName("sql").hasArg().withDescription("Execute given SQL").withLongOpt("exec")
                .create("e");
        options.addOption(exec);

        @SuppressWarnings("static-access")
        Option query = OptionBuilder.withArgName("sql").hasArg().withDescription("Query given SQL").withLongOpt("query")
                .create("q");
        options.addOption(query);

        @SuppressWarnings("static-access")
        Option restLogging = OptionBuilder.withArgName("true").hasArg().withDescription("Enable/Disable REST logging")
                .withLongOpt("restlog").create("rl");
        options.addOption(restLogging);

        int exitStatus = 0;
        final CommandLineParser parser = new GnuParser();
        try {
            CommandLine cmd = parser.parse(options, args, true);

            if (cmd.hasOption("stop")) {
                ServerUtil.stopServerProcess(SERVER_PID_FILE);
            } else if (cmd.hasOption("start")) {
                ServerUtil.startServerProcess(180000, new ServerServiceChecker() {
                    @Override
                    public boolean isRunning() {
                        return ServerControl.isRunningServer();
                    }
                }, false);
            } else if (cmd.hasOption("status")) {
                reportStatus();
            } else if (cmd.hasOption("check")) {
                // --check is used by vmidc.sh
                if (isRunningServer()) {
                    System.out.println(ServerControl.PRODUCT_NAME + " Server is already running.");
                    System.out.println(getServerVersion());
                    exitStatus = 2; // already running
                }
            } else if (cmd.hasOption("version")) {
                reportVersion();
            } else if (cmd.hasOption("help")) {
                HelpFormatter formatter = new HelpFormatter();
                formatter.printHelp("vmiDC", options);
            } else if (cmd.hasOption("exec")) {
                exec(cmd.getOptionValue("exec"));
            } else if (cmd.hasOption("query")) {
                query(cmd.getOptionValue("query"));
            } else if (cmd.hasOption("lock")) {
                getLockInfomation();
            } else if (cmd.hasOption("restlog")) {
                enableRestLogging(cmd.getOptionValue("restlog"));
            } else {
                throw new ParseException("no option specified");
            }
        } catch (ParseException e) {
            log.info("Error parsing command line arguments", e);
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("vmiDC", options);
            System.exit(1);
        }
        System.exit(exitStatus);
    }

    private static void enableRestLogging(String enable) throws Exception {
        VmidcServerRestClient restClient = new VmidcServerRestClient(apiPort);
        String restLogging = restClient.postResource("rest-logging", String.class, enable);
        System.out.println("Logging of Rest API's: " + restLogging);
    }

    private static void getLockInfomation() throws Exception {
        VmidcServerRestClient restClient = new VmidcServerRestClient(apiPort);
        String lockInfo = restClient.getResource("lock", String.class);
        System.out.println(lockInfo);
    }

    private static void query(String sql) throws Exception {
        if (isRunningServer()) {
            VmidcServerRestClient restClient = new VmidcServerRestClient(apiPort);
            String queryOutput = restClient.postResource("query", String.class, sql);
            System.out.println(queryOutput);
        } else {
            //            System.out.println(ServerDebugApis.query(sql).toString());
        }
    }

    private static void exec(String sql) throws Exception {
        if (isRunningServer()) {
            VmidcServerRestClient restClient = new VmidcServerRestClient(apiPort);
            String queryOutput = restClient.postResource("exec", String.class, sql);
            System.out.println(queryOutput);
        } else {
            //            System.out.println(ServerDebugApis.exec(sql).toString());
        }
    }

    private static void reportVersion() {
        System.out.println(ServerControl.PRODUCT_NAME + " version: " + VersionUtil.getVersion().getVersionStr());
    }

    private static void reportStatus() {
        if (isRunningServer()) {
            System.out.println(ServerControl.PRODUCT_NAME + " Server is running.");
        } else {
            System.out.println(ServerControl.PRODUCT_NAME + " Server is not running.");
        }
    }

    private static void loadServerProps() {
        Properties prop = new Properties();
        try {
            prop.load(new FileInputStream(ServerControl.CONFIG_PROPERTIES_FILE));
            ServerControl.apiPort = Integer.valueOf(prop.getProperty("server.port", DEFAULT_API_PORT.toString()));
            //set ISC public IP in Server Util
            ServerUtil.setServerIP(prop.getProperty(ISC_PUBLIC_IP, ""));
        } catch (Exception ex) {
            System.out.println("Warning: Failed to load server configuration file "
                    + ServerControl.CONFIG_PROPERTIES_FILE + " (Error:" + ex.getMessage() + ")");
        }
    }

    private static String getServerVersion() {
        VmidcServerRestClient restClient = new VmidcServerRestClient("localhost", apiPort, VMIDC_DEFAULT_LOGIN,
                VMIDC_DEFAULT_PASS, true);
        try {
            ServerStatusResponse res = getServerStatusResponse(restClient);
            String format = "version: %s\n(dbVersion:%s, pid=%s, serverTime=%s)";
            return String.format(format, res.getVersion(), res.getDbVersion(), res.getPid(),
                    res.getCurrentServerTime());
        } catch (Exception e) {
            return null;
        }
    }

    private static boolean isRunningServer() {
        try {
            log.info("Check if server is running ...");
            VmidcServerRestClient restClient = new VmidcServerRestClient("localhost", apiPort, VMIDC_DEFAULT_LOGIN, VMIDC_DEFAULT_PASS, true);

            ServerStatusResponse res = getServerStatusResponse(restClient);

            String oldPid = res.getPid();

            log.warn("Current pid:" + ServerUtil.getCurrentPid() + ". Running server (pid:" + oldPid + ") version: "
                    + res.getVersion() + " with db version: " + res.getDbVersion());

            // If we're here, server is running. Check for active pending
            // upgrade
            log.warn("Checking pending server upgrade.");

            try {
                restClient.putResource("upgradecomplete", Entity.entity(null, MediaType.APPLICATION_JSON));
                // If we made it to here, running server is in active upgrade mode
                // It should be shutting down now. We'll wait till the old process
                // goes away first by testing if we can get status.
                log.warn("Pending active server upgrade. Waiting for old process (" + oldPid
                        + ") to exit... (current pid:" + ServerUtil.getCurrentPid() + ")");
                int oldServerStatusCheckCount = 5; // 5 x 500ms = 2.5 seconds.
                while (oldServerStatusCheckCount > 0) {
                    try {
                        oldServerStatusCheckCount--;
                        Thread.sleep(500);
                        restClient.getResource("status", ServerStatusResponse.class);
                    } catch (Exception e) {
                        // If we got an exception, we assume old process
                        // terminated
                        break;
                    }
                }

                if (oldPid != null) {
                    // Making sure that old process also went away.
                    int retries = 10; // 10 x 500ms = 5 seconds.
                    boolean pidFound = false;
                    while (retries > 0) {
                        List<String> lines = new ArrayList<String>();
                        ServerUtil.execWithLines("ps " + oldPid, lines);
                        pidFound = false;
                        for (String line : lines) {
                            if (line.contains(oldPid)) {
                                pidFound = true;
                                break;
                            }
                        }
                        if (!pidFound) {
                            break;
                        }
                        log.info("Old process (" + oldPid + ") is still running. Retry (" + retries
                                + ") wait for graceful termination.");
                        retries--;
                        Thread.sleep(500);
                    }
                    // If process is still there, we'll force termination.
                    if (pidFound) {
                        log.warn("Old process (" + oldPid + ") is still running. Triggering forceful termination.");
                        ServerUtil.execWithLog("kill -9 " + oldPid);
                    }
                }

                // We'll proceed normal startup as the newly upgraded server.
                return false;
            } catch (Exception ex) {
                log.warn("No active pending upgrade.");
            }

            return true;
        } catch (ProcessingException | ConnectException e1) {
            log.warn("Fail to connect to running server: "+ e1.getMessage());
        } catch (Exception ex) {
            log.warn("Fail to connect to running server. Assuming not running: " + ex);
        }

        return false;
    }

    private static ServerStatusResponse getServerStatusResponse(VmidcServerRestClient restClient) throws Exception {
        ServerStatusResponse res;
        try {
            res = restClient.getResource("status", ServerStatusResponse.class);
        } catch (RestClientException e) {
            if (e.isCredentialError() || e.isConnectException()) {
                throw e;
            }

            log.warn("Failed to connect to running server.", e);
            res = getSslCertificates(restClient, e);
        }
        return res;
    }

    private static ServerStatusResponse getSslCertificates(VmidcServerRestClient restClient, RestClientException e) throws Exception {

        if (e == null) {
            throw new IllegalArgumentException("Rest client exception is empty");
        }

        SslCertificateResolver sslCertificateResolver = new SslCertificateResolver();

        if (!sslCertificateResolver.checkExceptionTypeForSSL(e)) {
            throw e;
        }

        log.info("Trying to fetch SSL certificate from server");

        String internalAlias = SslConfigurationLayout.INTERNAL_CERTIFICATE_ALIAS;
        sslCertificateResolver.fetchCertificatesFromURL(new URL(e.getResourcePath()), internalAlias);

        if (sslCertificateResolver.getCertificateResolverModels().isEmpty()) {
            throw e;
        }

        X509TrustManagerFactory trustManagerFactory = X509TrustManagerFactory.getInstance();
        for (CertificateResolverModel model : sslCertificateResolver.getCertificateResolverModels()) {
            trustManagerFactory.addEntry(model.getCertificate(), internalAlias);
            log.info("Added new certificate with alias: " + internalAlias + " and SHA1: " + model.getSha1());
        }
        log.warn("Retrying connection with server");
        return restClient.getResource("status", ServerStatusResponse.class);
    }

}
