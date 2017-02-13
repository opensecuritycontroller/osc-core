package org.osc.core.broker.rest.server.api;

import org.glassfish.jersey.server.ResourceConfig;
import org.junit.Test;
import org.osc.core.broker.di.OSC;
import org.osc.core.broker.rest.server.api.test.BaseJerseyTest;
import org.osc.core.broker.rest.server.exception.ErrorCodeDto;
import org.osc.core.broker.service.dto.DistributedApplianceInstanceDto;
import org.osc.core.broker.service.request.DistributedApplianceInstancesRequest;
import org.osc.core.broker.service.response.AgentStatusResponseDto;
import org.osc.core.broker.service.response.BaseDtoResponse;
import org.osc.core.broker.service.response.BaseJobResponse;
import org.osc.core.broker.service.response.DownloadAgentLogResponse;
import org.osc.core.broker.service.response.GetAgentStatusResponseDto;
import org.osc.core.broker.service.response.ListResponse;
import org.osc.core.rest.client.agent.model.output.AgentStatusResponse;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

public class DistributedApplianceInstancesApisTest extends BaseJerseyTest {

    @Override
    protected Application configure() {
        baseTestConfiguration();

        ResourceConfig application = getBaseResourceConfiguration();
        application.register(DistributedApplianceInstanceApis.class);

        return application;
    }

    @Test
    public void testGetDistributedApplianceInstances_ReturnsOK() throws Exception {
        // Assume.
        Response response = null;
        try {
            ListResponse<DistributedApplianceInstanceDto> listResponse = new ListResponse<>();

            listResponse.getList().add(new DistributedApplianceInstanceDto());

            mockSessionFactory(OSC.get().listDistributedApplianceInstanceService());
            callRealMethods(OSC.get().apiUtil());
            when(OSC.get().listDistributedApplianceInstanceService().dispatch(any())).thenCallRealMethod();
            when(OSC.get().listDistributedApplianceInstanceService().exec(any(),any())).thenReturn(listResponse);

            // Act.
            response = target("/api/server/v1/distributedApplianceInstances")
                    .request()
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
                    .get();
            List<DistributedApplianceInstanceDto> alertDtos = response.readEntity(new GenericType<List<DistributedApplianceInstanceDto>>() {
            });

            // Assert.
            assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
            assertThat(alertDtos.size()).isEqualTo(1);
        } finally {
            if (response != null) {
                response.close();
            }
        }
    }

    @Test
    public void testGetDistributedApplianceInstance_withPathParam_ReturnsNotFOundErrorCode() throws Exception {
        // Assume.
        Response response = null;
        try {
            mockSessionFactory(OSC.get().dtoFromEntityService());
            callRealMethods(OSC.get().apiUtil());
            callRealMethods(OSC.get().dtoFromEntityService());

            // Act.
            response = target("/api/server/v1/distributedApplianceInstances/1")
                    .request()
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
                    .get();
            ErrorCodeDto error = response.readEntity(new GenericType<ErrorCodeDto>() {
            });

            // Assert.
            assertThat(response.getStatus()).isEqualTo(Response.Status.NOT_FOUND.getStatusCode());
            assertThat(error.getErrorMessages().size()).isEqualTo(1);
        } finally {
            if (response != null) {
                response.close();
            }
        }
    }

    @Test
    public void testGetDistributedApplianceInstance_ReturnsOK() throws Exception {
        // Assume.
        Response response = null;
        try {
            BaseDtoResponse responseDto = new BaseDtoResponse();
            responseDto.setDto(new DistributedApplianceInstanceDto());

            mockSessionFactory(OSC.get().dtoFromEntityService());
            callRealMethods(OSC.get().apiUtil());
            when(OSC.get().dtoFromEntityService().dispatch(any())).thenCallRealMethod();
            when(OSC.get().dtoFromEntityService().exec(any(),any())).thenReturn(responseDto);

            // Act.
            response = target("/api/server/v1/distributedApplianceInstances/1")
                    .request()
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
                    .get();
            DistributedApplianceInstanceDto dto = response.readEntity(new GenericType<DistributedApplianceInstanceDto>() {
            });

            // Assert.
            assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
            assertThat(dto).isNotEqualTo(null);
        } finally {
            if (response != null) {
                response.close();
            }
        }
    }


    @Test
    public void testGetDistributedApplianceInstanceLog_ReturnsOK() throws Exception {
        // Assume.
        Response response = null;
        Path path = null;
        String fileContent = "File content example!";
        try {
            path = Files.createTempFile("AgentSupportBundle",".zip");
            DownloadAgentLogResponse responseDto = new DownloadAgentLogResponse();
            Files.write(path, fileContent.getBytes());
            responseDto.setSupportBundle(path.toFile());

            mockSessionFactory(OSC.get().downloadAgentLogService());
            callRealMethods(OSC.get().apiUtil());
            when(OSC.get().downloadAgentLogService().dispatch(any())).thenCallRealMethod();
            when(OSC.get().downloadAgentLogService().exec(any(),any())).thenReturn(responseDto);

            // Act.
            response = target("/api/server/v1/distributedApplianceInstances/1/log")
                    .request()
                    .get();
            String responseFileContent = "";
            try (BufferedReader buffer = new BufferedReader(new InputStreamReader(response.readEntity(InputStream.class)))) {
                responseFileContent = buffer.lines().collect(Collectors.joining("\n"));
            }
            // Assert.
            assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
            assertThat(responseFileContent).contains(fileContent);
        } finally {
            if (response != null) {
                response.close();
            }
            if (path != null) {
                Files.delete(path);
            }
        }
    }

    @Test
    public void testPutDistributedApplianceInstanceAuthenticate_withEmptyListInRequest_ReturnsBadRequest() throws Exception {
        // Assume.
        Response response = null;
        try {

            DistributedApplianceInstancesRequest dai = new DistributedApplianceInstancesRequest();
            Entity<DistributedApplianceInstancesRequest> daiEntity = Entity.entity(dai, MediaType.APPLICATION_JSON);
            // Act.
            response = target("/api/server/v1/distributedApplianceInstances/authenticate")
                    .request()
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
                    .put(daiEntity);
            ErrorCodeDto error = response.readEntity(new GenericType<ErrorCodeDto>() {
            });

            // Assert.
            assertThat(response.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
            assertThat(error.getErrorMessages().size()).isEqualTo(1);
        } finally {
            if (response != null) {
                response.close();
            }
        }
    }


    @Test
    public void testPutDistributedApplianceInstanceAuthenticate_withGoodRequest_ReturnsOk() throws Exception {
        // Assume.
        Response response = null;
        try {
            BaseJobResponse responseDto = new BaseJobResponse();
            responseDto.setId(1L);
            responseDto.setJobId(1L);

            mockSessionFactory(OSC.get().registerAgentService());
            callRealMethods(OSC.get().apiUtil());
            when(OSC.get().registerAgentService().dispatch(any())).thenCallRealMethod();
            when(OSC.get().registerAgentService().exec(any(),any())).thenReturn(responseDto);

            DistributedApplianceInstancesRequest dai = new DistributedApplianceInstancesRequest();
            dai.getDtoIdList().add(1L);
            Entity<DistributedApplianceInstancesRequest> daiEntity = Entity.entity(dai, MediaType.APPLICATION_JSON);
            // Act.
            response = target("/api/server/v1/distributedApplianceInstances/authenticate")
                    .request()
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
                    .put(daiEntity);
            BaseJobResponse baseJobResponse = response.readEntity(new GenericType<BaseJobResponse>() {
            });

            // Assert.
            assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
            assertThat(baseJobResponse.getJobId()).isEqualTo(responseDto.getJobId());
            assertThat(baseJobResponse.getId()).isEqualTo(responseDto.getId());
        } finally {
            if (response != null) {
                response.close();
            }
        }
    }

    @Test
    public void testPutDistributedApplianceInstanceSync_withEmptyListInRequest_ReturnsBadRequest() throws Exception {
        // Assume.
        Response response = null;
        try {

            DistributedApplianceInstancesRequest dai = new DistributedApplianceInstancesRequest();
            Entity<DistributedApplianceInstancesRequest> daiEntity = Entity.entity(dai, MediaType.APPLICATION_JSON);
            // Act.
            response = target("/api/server/v1/distributedApplianceInstances/sync")
                    .request()
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
                    .put(daiEntity);
            ErrorCodeDto error = response.readEntity(new GenericType<ErrorCodeDto>() {
            });

            // Assert.
            assertThat(response.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
            assertThat(error.getErrorMessages().size()).isEqualTo(1);
        } finally {
            if (response != null) {
                response.close();
            }
        }
    }


    @Test
    public void testPutDistributedApplianceInstanceSync_withGoodRequest_ReturnsOk() throws Exception {
        // Assume.
        Response response = null;
        try {
            BaseJobResponse responseDto = new BaseJobResponse();
            responseDto.setId(1L);
            responseDto.setJobId(1L);

            mockSessionFactory(OSC.get().syncAgentService());
            callRealMethods(OSC.get().apiUtil());
            when(OSC.get().syncAgentService().dispatch(any())).thenCallRealMethod();
            when(OSC.get().syncAgentService().exec(any(),any())).thenReturn(responseDto);

            DistributedApplianceInstancesRequest dai = new DistributedApplianceInstancesRequest();
            dai.getDtoIdList().add(1L);
            Entity<DistributedApplianceInstancesRequest> daiEntity = Entity.entity(dai, MediaType.APPLICATION_JSON);
            // Act.
            response = target("/api/server/v1/distributedApplianceInstances/sync")
                    .request()
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
                    .put(daiEntity);
            BaseJobResponse baseJobResponse = response.readEntity(new GenericType<BaseJobResponse>() {
            });

            // Assert.
            assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
            assertThat(baseJobResponse.getJobId()).isEqualTo(responseDto.getJobId());
            assertThat(baseJobResponse.getId()).isEqualTo(responseDto.getId());
        } finally {
            if (response != null) {
                response.close();
            }
        }
    }


    @Test
    public void testPutDistributedApplianceInstanceStatus_withEmptyListInRequest_ReturnsBadRequest() throws Exception {
        // Assume.
        Response response = null;
        try {

            DistributedApplianceInstancesRequest dai = new DistributedApplianceInstancesRequest();
            Entity<DistributedApplianceInstancesRequest> daiEntity = Entity.entity(dai, MediaType.APPLICATION_JSON);
            // Act.
            response = target("/api/server/v1/distributedApplianceInstances/status")
                    .request()
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
                    .put(daiEntity);
            ErrorCodeDto error = response.readEntity(new GenericType<ErrorCodeDto>() {
            });

            // Assert.
            assertThat(response.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
            assertThat(error.getErrorMessages().size()).isEqualTo(1);
        } finally {
            if (response != null) {
                response.close();
            }
        }
    }


    @Test
    public void testPutDistributedApplianceInstanceStatus_withGoodRequest_ReturnsOk() throws Exception {
        // Assume.
        Response response = null;
        try {
            GetAgentStatusResponseDto responseDto = new GetAgentStatusResponseDto();
            AgentStatusResponseDto asrd = new AgentStatusResponseDto();
            asrd.setAgentType("type");
            asrd.setResponse(new AgentStatusResponse());
            responseDto.getAgentStatusDtoList().add(asrd);

            mockSessionFactory(OSC.get().getAgentStatusService());
            callRealMethods(OSC.get().apiUtil());
            when(OSC.get().getAgentStatusService().dispatch(any())).thenCallRealMethod();
            when(OSC.get().getAgentStatusService().exec(any(),any())).thenReturn(responseDto);

            DistributedApplianceInstancesRequest dai = new DistributedApplianceInstancesRequest();
            dai.getDtoIdList().add(1L);
            Entity<DistributedApplianceInstancesRequest> daiEntity = Entity.entity(dai, MediaType.APPLICATION_JSON);
            // Act.
            response = target("/api/server/v1/distributedApplianceInstances/status")
                    .request()
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
                    .put(daiEntity);
            GetAgentStatusResponseDto agentStatusResponse = response.readEntity(new GenericType<GetAgentStatusResponseDto>() {
            });

            // Assert.
            assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
            assertThat(agentStatusResponse.getAgentStatusDtoList().size()).isEqualTo(1);
        } finally {
            if (response != null) {
                response.close();
            }
        }
    }
}
