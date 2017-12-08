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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.Date;
import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.StreamingOutput;

import org.osc.core.broker.rest.server.ApiUtil;
import org.osc.core.broker.rest.server.OscAuthFilter;
import org.osc.core.broker.rest.server.ServerRestConstants;
import org.osc.core.broker.rest.server.annotations.LocalHostAuth;
import org.osc.core.broker.rest.server.annotations.OscAuth;
import org.osc.core.broker.service.api.AddSslCertificateServiceApi;
import org.osc.core.broker.service.api.BackupServiceApi;
import org.osc.core.broker.service.api.DBConnectionManagerApi;
import org.osc.core.broker.service.api.DeleteSslCertificateServiceApi;
import org.osc.core.broker.service.api.ListSslCertificatesServiceApi;
import org.osc.core.broker.service.api.ReplaceInternalKeypairServiceApi;
import org.osc.core.broker.service.api.server.ServerApi;
import org.osc.core.broker.service.api.server.UserContextApi;
import org.osc.core.broker.service.dto.SslCertificateDto;
import org.osc.core.broker.service.exceptions.ErrorCodeDto;
import org.osc.core.broker.service.request.AddSslEntryRequest;
import org.osc.core.broker.service.request.BackupRequest;
import org.osc.core.broker.service.request.BaseRequest;
import org.osc.core.broker.service.request.DeleteSslEntryRequest;
import org.osc.core.broker.service.request.UploadRequest;
import org.osc.core.broker.service.response.BaseResponse;
import org.osc.core.broker.service.response.CertificateBasicInfoModel;
import org.osc.core.broker.service.response.ListResponse;
import org.osc.core.broker.service.response.ServerStatusResponse;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.Authorization;

@Component(service = ServerMgmtApis.class)
@Api(tags = "Operations for OSC server", authorizations = { @Authorization(value = "Basic Auth") })
@Path(ServerRestConstants.SERVER_API_PATH_PREFIX + "/serverManagement")
@Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
public class ServerMgmtApis {
    private static final Logger logger = LoggerFactory.getLogger(ServerMgmtApis.class);

    @Reference
    ServerApi server;

    @Reference
    AddSslCertificateServiceApi addSSlCertificateService;

    @Reference
    private ApiUtil apiUtil;

    @Reference
    private BackupServiceApi backupService;

    @Reference
    private DeleteSslCertificateServiceApi deleteSslCertificateService;

    @Reference
    private ListSslCertificatesServiceApi listSslCertificateService;

    @Reference
    private UserContextApi userContext;

    @Reference
    private ReplaceInternalKeypairServiceApi replaceInternalKeypairServiceApi;

    @ApiOperation(value = "Get server status",
            notes = "Returns server status information",
            response = ServerStatusResponse.class)
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 400, message = "In case of any error", response = ErrorCodeDto.class)})
    @Path("/status")
    @GET
    public Response getStatus() {

        ServerStatusResponse serverStatusResponse = new ServerStatusResponse();
        serverStatusResponse.setVersion(this.server.getVersionStr());
        serverStatusResponse.setDbVersion(DBConnectionManagerApi.TARGET_DB_VERSION);
        serverStatusResponse.setCurrentServerTime(new Date());
        serverStatusResponse.setPid(this.server.getCurrentPid());

        return Response.status(Status.OK).entity(serverStatusResponse).build();
    }

    @ApiOperation(value = "Backs up server database",
            notes = "Trigger database backup, place backup on server and make it avaliable for download",
            response = ServerStatusResponse.class)
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 400, message = "In case of any error", response = ErrorCodeDto.class) })
    @OscAuth
    @Path("/backup-db")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    @POST
    public Response getDbBackupFile(@Context HttpHeaders headers, @ApiParam(required = true) BackupRequest request) {

        this.userContext.setUser(OscAuthFilter.getUsername(headers));
        logger.info(this.userContext.getCurrentUser()+" is generating a backap of the database");
        StreamingOutput fileStream = new StreamingOutput() {
            @Override
            public void write(OutputStream output) throws IOException, WebApplicationException {
                try {
                    ServerMgmtApis.this.backupService.dispatch(request);
                    File encryptedBackupFile = ServerMgmtApis.this.backupService.getEncryptedBackupFile(request.getBackupFileName());
                    output.write(Files.readAllBytes(encryptedBackupFile.toPath()));
                    output.flush();
                } catch (Exception e) {
                    throw new InternalServerErrorException("Backup could not be generated",e);
                }
            }
        };
        return Response
                .ok(fileStream, MediaType.APPLICATION_OCTET_STREAM)
                .header("content-disposition", "attachment; filename = oscServerDBBackup.zip")
                .build();
    }

    @LocalHostAuth
    @Path("/upgradecomplete")
    @PUT
    public Response upgradeServerReady() {

        logger.info("upgradedServerReady (pid:" + this.server.getCurrentPid() + "): Check pending upgrade server.");
        if (!this.server.isUnderMaintenance()) {
            logger.info("upgradedServerReady (pid:" + this.server.getCurrentPid() + "): No pending upgrade.");
            return Response.status(Status.BAD_REQUEST).build();
        }

        logger.info("upgradedServerReady (pid:" + this.server.getCurrentPid()
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
                    ServerMgmtApis.this.server.stopServer();
                } catch (Exception e) {
                    logger.error("upgradedServerReady (pid:" + ServerMgmtApis.this.server.getCurrentPid()
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
    @OscAuth
    @GET
    public List<CertificateBasicInfoModel> getSslCertificatesList(@Context HttpHeaders headers) {

        logger.info("Listing ssl certificates from trust store");
        this.userContext.setUser(OscAuthFilter.getUsername(headers));

        @SuppressWarnings("unchecked")
        ListResponse<CertificateBasicInfoModel> response = (ListResponse<CertificateBasicInfoModel>) this.apiUtil
                .getListResponse(this.listSslCertificateService, new BaseRequest<>(true));

        return response.getList();
    }

    /**
     * Add a SSL certificate entry
     */
    @ApiOperation(value = "Add SSL certificate",
            notes = "Adds a SSL certificate entry with custom alias provided by the user. Caution: As JSON string certificate needs to have all break lines converted to \\n.")
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 400, message = "In case of any error", response = ErrorCodeDto.class)})
    @Path("/sslcertificate")
    @OscAuth
    @POST
    public Response addSslCertificate(@Context HttpHeaders headers, @ApiParam(required = true) SslCertificateDto sslEntry) {
        logger.info("Adding new SSL certificate to truststore");
        this.userContext.setUser(OscAuthFilter.getUsername(headers));
        AddSslEntryRequest addSslEntryRequest = new AddSslEntryRequest(sslEntry.getAlias(), sslEntry.getCertificate());
        return this.apiUtil.getResponse(this.addSSlCertificateService, addSslEntryRequest);
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
    @OscAuth
    @DELETE
    public Response deleteSslCertificate(@Context HttpHeaders headers, @ApiParam(value = "SSL certificate alias") @PathParam("alias") String alias) {
        logger.info("Deleting SSL certificate from trust store with alias: " + alias);
        this.userContext.setUser(OscAuthFilter.getUsername(headers));
        return this.apiUtil.getResponse(this.deleteSslCertificateService, new DeleteSslEntryRequest(alias));
    }

    @ApiOperation(value = "Upload a private/public certificate zip file. ",
            notes = "Overwrites the entry marked &quot;internal&quot; in the truststore. "
            + "That is a private/public keypair used for secure connections by OSC. "
            + "The zip file should contain a  &quot;key.pem&quot; in PKCS8+PEM format (private key) and "
            + "certchain.pem or certchain.pkipath (the certificate chain). This results in the server restart ! ! !",
            response = BaseResponse.class)
    @ApiResponses(value = { @ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 400, message = "In case of any error", response = ErrorCodeDto.class) })
    @Path("/internalkeypair")
    @POST
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    public Response uploadKeypair(@Context HttpHeaders headers,
            @ApiParam(value = "The imported keypair zip file name",
            required = true) @PathParam("fileName") String fileName,
            @ApiParam(required = true) InputStream uploadedInputStream) {
        logger.info("Started uploading file " + fileName);
        this.userContext.setUser(OscAuthFilter.getUsername(headers));
        return this.apiUtil.getResponseForBaseRequest(this.replaceInternalKeypairServiceApi,
                new UploadRequest(fileName, uploadedInputStream));
    }
}
