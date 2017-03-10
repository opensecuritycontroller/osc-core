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
package org.osc.core.broker.rest.server.api;

import com.mcafee.vmidc.server.Server;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.log4j.Logger;
import org.osc.core.broker.job.lock.LockInformationDto;
import org.osc.core.broker.job.lock.LockManager;
import org.osc.core.broker.rest.server.OscRestServlet;
import org.osc.core.broker.util.db.DBConnectionParameters;
import org.osc.core.broker.util.db.HibernateUtil;
import org.osc.core.rest.client.RestBaseClient;
import org.osc.core.rest.client.util.LoggingUtil;
import org.osc.core.util.KeyStoreProvider.KeyStoreProviderException;
import org.osc.core.rest.annotations.LocalHostAuth;

import com.mcafee.vmidc.server.Server;
import com.sun.jersey.spi.container.ResourceFilters;

@Component(service = ServerDebugApis.class)
@Path(OscRestServlet.SERVER_API_PATH_PREFIX + "/serverDebug")
@Consumes(MediaType.TEXT_PLAIN)
@Produces(MediaType.TEXT_PLAIN)
@LocalHostAuth
public class ServerDebugApis {
    private static final Logger logger = Logger.getLogger(ServerDebugApis.class);

    @Path("/lock")
    @GET
    public Response getCurrentLockInfomation() {
        if(!Server.devMode) {
            return Response.status(Status.NOT_FOUND).build();
        }

        try {
            LockInformationDto lockInfo = new LockInformationDto(LockManager.getLockManager().getLockInformation());
            return Response.ok(LoggingUtil.pojoToJsonPrettyString(lockInfo)).build();
        } catch (Exception e) {

            logger.error("Failed to get Lock information.", e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Path("/query")
    @POST
    public Response queryDb(String sql) {
        if(!Server.devMode) {
            return Response.status(Status.NOT_FOUND).build();
        }
        try {
            StringBuilder output = query(sql);
            return Response.ok(output.toString()).build();

        } catch (Exception e) {

            logger.error("Failed to query.", e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Path("/exec")
    @POST
    public Response execDb(String sql) {
        if(!Server.devMode) {
            return Response.status(Status.NOT_FOUND).build();
        }
        try {
            StringBuilder output = exec(sql);
            return Response.ok(output.toString()).build();

        } catch (Exception e) {

            logger.error("Failed to execute.", e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Path("/reload")
    @PUT
    public Response reloadDevMode() {
        try {
            Server.devMode = Boolean.valueOf(Server.loadServerProp(Server.DEV_MODE_PROPERTY_KEY, "false"));
            return Response.ok(String.valueOf(Server.devMode)).build();

        } catch (Exception e) {

            logger.error("Failed to reload dev mode flag.", e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Path("/rest-logging")
    @POST
    public Response enableLogging(String enable) {
        if(!Server.devMode) {
            return Response.status(Status.NOT_FOUND).build();
        }
        try {

            boolean enableLogging = Boolean.valueOf(enable);
            RestBaseClient.enableDebugLogging = enableLogging;
            return Response.ok(String.valueOf(enableLogging)).build();

        } catch (Exception e) {

            logger.error("Failed to enable Rest Debug Logging", e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    public static StringBuilder query(String sql) throws IOException, KeyStoreProviderException {
        StringBuilder output = new StringBuilder();
        output.append("Query: ").append(sql).append("\n");

        DBConnectionParameters params = new DBConnectionParameters();
        params.setConnectionURL(params.getConnectionURL() + "AUTO_SERVER=TRUE;");

        try (Connection conn = HibernateUtil.getSQLConnection(params);
             Statement statement = conn.createStatement()) {
            try (ResultSet result = statement.executeQuery(sql)){
                processResultSetForQuery(output, result);
            }
        } catch (SQLException ex) {
            output.append(ExceptionUtils.getStackTrace(ex));
        }

        return output;
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

    public static StringBuilder exec(String sql) throws IOException, KeyStoreProviderException {
        StringBuilder output = new StringBuilder();
        output.append("Exec: ").append(sql).append("\n");

        DBConnectionParameters params = new DBConnectionParameters();
        params.setConnectionURL(params.getConnectionURL() + "AUTO_SERVER=TRUE;");

        try (Connection conn = HibernateUtil.getSQLConnection(params);
             Statement statement = conn.createStatement()) {
            boolean isResultSet = statement.execute(sql);
            if (!isResultSet) {
                output.append(statement.getUpdateCount()).append(" rows affected.\n");
            }
        } catch (SQLException ex) {
            output.append(ExceptionUtils.getStackTrace(ex));
        }
        return output;
    }

}
