package org.osc.core.broker.rest.server.api;

import org.glassfish.jersey.server.ResourceConfig;
import org.junit.Test;
import org.osc.core.broker.di.OSC;
import org.osc.core.broker.rest.server.api.test.BaseJerseyTest;
import org.osc.core.broker.rest.server.exception.ErrorCodeDto;
import org.osc.core.broker.service.dto.DistributedApplianceInstanceDto;
import org.osc.core.broker.service.response.BaseDtoResponse;
import org.osc.core.broker.service.response.ListResponse;

import javax.ws.rs.core.Application;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

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
}
