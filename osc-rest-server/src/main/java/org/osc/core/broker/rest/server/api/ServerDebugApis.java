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

import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.log4j.Logger;
import org.osc.core.broker.rest.server.ServerRestConstants;
import org.osc.core.broker.rest.server.annotations.LocalHostAuth;
import org.osc.core.broker.service.api.DBConnectionManagerApi;
import org.osc.core.broker.service.api.LockInfoServiceApi;
import org.osc.core.broker.service.api.server.ServerApi;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(service = ServerDebugApis.class)
@Path(ServerRestConstants.SERVER_API_PATH_PREFIX + "/serverDebug")
@Consumes(MediaType.TEXT_PLAIN)
@Produces(MediaType.TEXT_PLAIN)
@LocalHostAuth
public class ServerDebugApis {
    private static final Logger logger = Logger.getLogger(ServerDebugApis.class);

    @Reference
    ServerApi server;

    @Reference
    private DBConnectionManagerApi dbConnectionManager;

    @Reference
    private LockInfoServiceApi lockInfoServiceApi;

    @Path("/lock")
    @GET
    public Response getCurrentLockInfomation() {
        if(!this.server.getDevMode()) {
            return Response.status(Status.NOT_FOUND).build();
        }

        try {
            return Response.ok(this.lockInfoServiceApi.getLockInfo()).build();
        } catch (Exception e) {
            logger.error("Failed to get Lock information.", e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Path("/query")
    @POST
    public Response queryDb(String sql) {
        if(!this.server.getDevMode()) {
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
        if(!this.server.getDevMode()) {
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
            this.server.setDevMode(Boolean.valueOf(this.server.loadServerProp(ServerApi.DEV_MODE_PROPERTY_KEY, "false")));
            return Response.ok(String.valueOf(this.server.getDevMode())).build();

        } catch (Exception e) {

            logger.error("Failed to reload dev mode flag.", e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Path("/rest-logging")
    @POST
    public Response enableLogging(String enable) {
        if(!this.server.getDevMode()) {
            return Response.status(Status.NOT_FOUND).build();
        }
        try {

            boolean enableLogging = Boolean.valueOf(enable);
            this.server.setDebugLogging(enableLogging);
            return Response.ok(String.valueOf(enableLogging)).build();

        } catch (Exception e) {

            logger.error("Failed to enable Rest Debug Logging", e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    private StringBuilder query(String sql) throws IOException {
        StringBuilder output = new StringBuilder();
        output.append("Query: ").append(sql).append("\n");

        try (Connection conn = this.dbConnectionManager.getSQLConnection();
             Statement statement = conn.createStatement()) {
            try (ResultSet result = statement.executeQuery(sql)){
                processResultSetForQuery(output, result);
            }
        } catch (Exception ex) {
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

    private StringBuilder exec(String sql) throws IOException {
        StringBuilder output = new StringBuilder();
        output.append("Exec: ").append(sql).append("\n");

        try (Connection conn = this.dbConnectionManager.getSQLConnection();
             Statement statement = conn.createStatement()) {
            boolean isResultSet = statement.execute(sql);
            if (!isResultSet) {
                output.append(statement.getUpdateCount()).append(" rows affected.\n");
            }
        } catch (Exception ex) {
            output.append(ExceptionUtils.getStackTrace(ex));
        }
        return output;
    }

}
