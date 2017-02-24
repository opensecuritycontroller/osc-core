package org.osc.core.broker.rest.server.api;

import com.mcafee.vmidc.server.Server;
import com.sun.jersey.spi.container.ResourceFilters;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.log4j.Logger;
import org.osc.core.broker.job.lock.LockInformationDto;
import org.osc.core.broker.job.lock.LockManager;
import org.osc.core.broker.rest.server.IscRestServlet;
import org.osc.core.broker.util.db.DBConnectionParameters;
import org.osc.core.broker.util.db.HibernateUtil;
import org.osc.core.rest.client.RestBaseClient;
import org.osc.core.rest.client.util.LoggingUtil;
import org.osc.core.util.KeyStoreProvider.KeyStoreProviderException;
import org.osc.core.util.LocalHostAuthFilter;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;

@Path(IscRestServlet.SERVER_API_PATH_PREFIX + "/serverDebug")
@ResourceFilters(LocalHostAuthFilter.class)
@Consumes(MediaType.TEXT_PLAIN)
@Produces(MediaType.TEXT_PLAIN)
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
