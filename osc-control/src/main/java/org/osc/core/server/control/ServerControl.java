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
package org.osc.core.server.control;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.ConnectException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

import javax.ws.rs.ProcessingException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.io.output.StringBuilderWriter;
import org.osc.core.broker.rest.client.VmidcServerRestClient;
import org.osc.core.broker.service.response.ServerStatusResponse;
import org.osc.core.broker.util.ServerUtil;
import org.osc.core.broker.util.ServerUtil.ServerServiceChecker;
import org.osc.core.broker.util.VersionUtil;
import org.osc.core.broker.util.db.DBConnectionParameters;
import org.osc.core.broker.util.log.LogUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServerControl {

    private static Logger log = LoggerFactory.getLogger(ServerControl.class);

    private static final Integer DEFAULT_API_PORT = 8090;
    private static final String CONFIG_PROPERTIES_FILE = "data/vmidcServer.conf";
    private static final String SERVER_PID_FILE = "server.pid";

    private static final String PRODUCT_NAME = "Open Security Controller";
    private static final String ISC_PUBLIC_IP = "publicIP";

    private static Integer apiPort = DEFAULT_API_PORT;

    public static final String VMIDC_DEFAULT_LOGIN = "admin";
    public static final String VMIDC_DEFAULT_PASS = "admin123";


    public static void main(final String[] args) throws Exception {



        LogUtil.redirectConsoleMessagesToLog();
        loadServerProps();

        final Options options = new Options();
        options.addOption("s", "stop", false, "Stop " + PRODUCT_NAME + " Server.");
        options.addOption("t", "start", false, "Start " + PRODUCT_NAME + " Server.");
        options.addOption("u", "status", false, "Report " + PRODUCT_NAME + " Server status.");
        options.addOption("c", "check", false, "Check if server is running.");
        options.addOption("v", "version", false, "Print server version.");
        options.addOption("h", "help", false, "Print options help.");
        options.addOption("l", "lock", false, "Prints Currently acquired Locks.");

        Option exec = Option.builder("e").argName("sql").hasArg().desc("Execute given SQL").longOpt("exec")
                .build();
        options.addOption(exec);

        Option query = Option.builder("q").argName("sql").hasArg().desc("Query given SQL").longOpt("query")
                .build();
        options.addOption(query);

        Option restLogging = Option.builder("rl").argName("true").hasArg().desc("Enable/Disable REST logging")
                .longOpt("restlog").build();
        options.addOption(restLogging);

        int exitStatus = 0;
        final CommandLineParser parser = new DefaultParser();
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
            System.err.println("Error parsing command line arguments" + e.getMessage());
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
            offlineQuery(sql);
        }
    }

    private static void exec(String sql) throws Exception {
        if (isRunningServer()) {
            VmidcServerRestClient restClient = new VmidcServerRestClient(apiPort);
            String queryOutput = restClient.postResource("exec", String.class, sql);
            System.out.println(queryOutput);
        } else {
            offlineExec(sql);
        }
    }

    private static void offlineQuery(String sql) throws IOException {
        StringBuilder output = new StringBuilder();
        output.append("Query: ").append(sql).append("\n");

        DBConnectionParameters params = new DBConnectionParameters();

        try (Connection conn = DriverManager.getConnection(params.getConnectionURL(),
                params.getLogin(), params.getPassword());
             Statement statement = conn.createStatement()) {
            try (ResultSet result = statement.executeQuery(sql)){
                processResultSetForQuery(output, result);
            }
        } catch (Exception ex) {
            try (PrintWriter pw = new PrintWriter(new StringBuilderWriter(output), true)) {
                ex.printStackTrace(pw);
            }
        }

        System.out.println(output.toString());
    }

    private static void offlineExec(String sql) throws IOException {
        StringBuilder output = new StringBuilder();
        output.append("Exec: ").append(sql).append("\n");

        DBConnectionParameters params = new DBConnectionParameters();

        try (Connection conn = DriverManager.getConnection(params.getConnectionURL(),
                params.getLogin(), params.getPassword());
             Statement statement = conn.createStatement()) {
            boolean isResultSet = statement.execute(sql);
            if (!isResultSet) {
                output.append(statement.getUpdateCount()).append(" rows affected.\n");
            }
        } catch (Exception ex) {
            try (PrintWriter pw = new PrintWriter(new StringBuilderWriter(output), true)) {
                ex.printStackTrace(pw);
            }
        }
        System.out.println(output.toString());
    }

    private static void processResultSetForQuery(StringBuilder output, ResultSet result) throws SQLException {
        ResultSetMetaData meta = result.getMetaData();
        int cols = meta.getColumnCount();

        output.append("Metadata information for columns (name,type,display-size):\n");
        for (int i = 1; i <= cols; i++) {
            output.append("Column ").append(i).append(":  ")
                    .append(meta.getColumnName(i)).append(",").append(meta.getColumnTypeName(i)).append(",").append(meta.getColumnDisplaySize(i))
                    .append("\n");
        }

        output.append("\nResult set dump:\n");
        int cnt = 1;
        while (result.next()) {
            output.append("\nRow ").append(cnt).append(" : ");
            for (int i = 1; i <= cols; i++) {
                output.append(result.getString(i)).append("\t");
            }
            cnt++;
        }
        output.append("\n");
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
        try(FileInputStream fis = new FileInputStream(ServerControl.CONFIG_PROPERTIES_FILE)) {
            prop.load(fis);
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

            return true;
        } catch (ProcessingException | ConnectException e1) {
            log.warn("Fail to connect to running server: "+ e1.getMessage());
        } catch (Exception ex) {
            log.warn("Fail to connect to running server. Assuming not running: " + ex);
        }

        return false;
    }

    private static ServerStatusResponse getServerStatusResponse(VmidcServerRestClient restClient) throws Exception {
        return restClient.getResource("status", ServerStatusResponse.class);
    }

}
