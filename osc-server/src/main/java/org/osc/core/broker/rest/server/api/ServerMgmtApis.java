package org.osc.core.broker.rest.server.api;

import com.mcafee.vmidc.server.Server;
import com.sun.jersey.api.JResponse;
import com.sun.jersey.spi.container.ResourceFilters;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.Authorization;
import org.apache.log4j.Logger;
import org.osc.core.broker.rest.server.IscRestServlet;
import org.osc.core.broker.rest.server.VmidcAuthFilter;
import org.osc.core.broker.rest.server.exception.ErrorCodeDto;
import org.osc.core.broker.rest.server.model.ServerStatusResponse;
import org.osc.core.broker.service.AddSslCertificateService;
import org.osc.core.broker.service.BackupService;
import org.osc.core.broker.service.DeleteSslCertificateService;
import org.osc.core.broker.service.ListSslCertificatesService;
import org.osc.core.broker.service.dto.SslCertificateDto;
import org.osc.core.broker.service.request.AddSslEntryRequest;
import org.osc.core.broker.service.request.BackupRequest;
import org.osc.core.broker.service.request.BaseRequest;
import org.osc.core.broker.service.request.DeleteSslEntryRequest;
import org.osc.core.broker.service.response.ListResponse;
import org.osc.core.broker.util.SessionUtil;
import org.osc.core.broker.util.db.upgrade.ReleaseUpgradeMgr;
import org.osc.core.util.LocalHostAuthFilter;
import org.osc.core.rest.annotations.LocalHostAuth;
import org.osc.core.rest.client.crypto.model.CertificateBasicInfoModel;
import org.osc.core.util.LocalHostAuthFilter;
import org.osc.core.rest.client.crypto.model.CertificateBasicInfoModel;
import org.osc.core.util.PKIUtil;
import org.osc.core.util.ServerUtil;
import org.osc.core.util.VersionUtil;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.io.File;
import java.util.Date;
import java.util.List;

@Api(tags = "Operations for OSC server", authorizations = {@Authorization(value = "Basic Auth")})
@Path(IscRestServlet.SERVER_API_PATH_PREFIX + "/serverManagement")
@Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
public class ServerMgmtApis {
    private static final Logger logger = Logger.getLogger(ServerMgmtApis.class);

    @ApiOperation(value = "Get server status",
            notes = "Returns server status information",
            response = ServerStatusResponse.class)
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 400, message = "In case of any error", response = ErrorCodeDto.class)})
    @Path("/status")
    @GET
    public Response getStatus() {

        logger.info("getStatus()");

        ServerStatusResponse serverStatusResponse = new ServerStatusResponse();
        serverStatusResponse.setVersion(VersionUtil.getVersion().getVersionStr());
        serverStatusResponse.setDbVersion(ReleaseUpgradeMgr.TARGET_DB_VERSION);
        serverStatusResponse.setCurrentServerTime(new Date());
        serverStatusResponse.setPid(ServerUtil.getCurrentPid());

        return Response.status(Status.OK).entity(serverStatusResponse).build();
    }

    @ApiOperation(value = "Backs up server database",
            notes = "Trigger database backup, place backup on server and make it avaliable for download",
            response = ServerStatusResponse.class)
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 400, message = "In case of any error", response = ErrorCodeDto.class)})
    @ResourceFilters({VmidcAuthFilter.class})
    @Path("/backup-db")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    @POST
    public Response getDbBackupFile(BackupRequest request) {

        try {
            BackupService bkpservice = new BackupService();
            bkpservice.dispatch(request);
            byte[] docStream = PKIUtil.readBytesFromFile(bkpservice.getEncryptedBackupFile());

            return Response.ok(docStream, MediaType.APPLICATION_OCTET_STREAM)
                    .header("content-disposition", "attachment; filename = vmiDCServerDBBackup.zip").build();

        } catch (Exception e) {

            logger.error("Failed to backup vmiDCServer Database ", e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @ResourceFilters({LocalHostAuthFilter.class})
    @Path("/upgradecomplete")
    @PUT
    public Response upgradeServerReady() {

        logger.info("upgradedServerReady (pid:" + ServerUtil.getCurrentPid() + "): Check pending upgrade server.");
        if (!Server.isInMaintenance()) {
            logger.info("upgradedServerReady (pid:" + ServerUtil.getCurrentPid() + "): No pending upgrade.");
            return Response.status(Status.BAD_REQUEST).build();
        }

        logger.info("upgradedServerReady (pid:" + ServerUtil.getCurrentPid()
                + "): Upgraded server is up. Start shutdown...");
        Thread shutdownThread = new Thread("Shutdown-Thread") {
            @Override
            public void run() {
                try {
                    /*
                     * Introduce a slight delay so REST response can be
                     * completed
                     */
                    Thread.sleep(500);
                    File originalServerFile = new File("vmiDCServer.org");
                    if (originalServerFile.exists()) {
                        logger.info("Original Server file exists, deleting original server.");
                        originalServerFile.delete();
                    }
                    Server.stopServer();
                } catch (Exception e) {
                    logger.error("upgradedServerReady (pid:" + ServerUtil.getCurrentPid()
                            + "): Shutting down Tomcat after upgrade experienced failures", e);
                }
            }
        };
        shutdownThread.start();

        return Response.status(Status.OK).build();
    }

    @ApiOperation(value = "Get SSL certificates list", response = SslCertificateDto.class, responseContainer = "Set")
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 400, message = "In case of any error", response = ErrorCodeDto.class)})
    @Path("/sslcertificates")
    @ResourceFilters({ VmidcAuthFilter.class })
    @GET
    public JResponse<List<CertificateBasicInfoModel>> getSslCertificatesList(@Context HttpHeaders headers) {

        logger.info("Listing ssl certificates from trust store");
        SessionUtil.setUser(SessionUtil.getUsername(headers));

        @SuppressWarnings("unchecked")
        ListResponse<CertificateBasicInfoModel> response = (ListResponse<CertificateBasicInfoModel>) ApiUtil
                .getListResponse(new ListSslCertificatesService(), new BaseRequest<>(true));

        return JResponse.ok(response.getList()).build();
    }

    /**
     * Add a SSL certificate entry
     */
    @ApiOperation(value = "Add SSL certificate")
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 400, message = "In case of any error", response = ErrorCodeDto.class)})
    @Path("/sslcertificate")
    @ResourceFilters({ VmidcAuthFilter.class })
    @POST
    public Response addSslCertificate(@Context HttpHeaders headers, @ApiParam(required = true) SslCertificateDto sslEntry) {
        logger.info("Adding new SSL certificate to truststore");
        SessionUtil.setUser(SessionUtil.getUsername(headers));
        AddSslEntryRequest addSslEntryRequest = new AddSslEntryRequest(sslEntry.getAlias(), sslEntry.getCertificate());
        return ApiUtil.getResponse(new AddSslCertificateService(), addSslEntryRequest);
    }

    /**
     * Delete SSL certificate entry
     */
    @ApiOperation(value = "Deletes a SSL certificate entry",
            notes = "Deletes a SSL certificate entry if not referenced by any available connector or manager")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 400, message = "In case of any error", response = ErrorCodeDto.class)
    })
    @Path("/sslcertificate/{alias}")
    @ResourceFilters({ VmidcAuthFilter.class })
    @DELETE
    public Response deleteSslCertificate(@Context HttpHeaders headers, @ApiParam(value = "SSL certificate alias") @PathParam("alias") String alias) {
        logger.info("Deleting SSL certificate from trust store with alias: " + alias);
        SessionUtil.setUser(SessionUtil.getUsername(headers));
        return ApiUtil.getResponse(new DeleteSslCertificateService(), new DeleteSslEntryRequest(alias));
    }
}
