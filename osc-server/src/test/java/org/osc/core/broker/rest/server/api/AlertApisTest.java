package org.osc.core.broker.rest.server.api;

import org.glassfish.jersey.server.ResourceConfig;
import org.junit.Test;
import org.osc.core.broker.di.OSC;
import org.osc.core.broker.model.entities.events.EventType;
import org.osc.core.broker.model.entities.events.Severity;
import org.osc.core.broker.rest.server.api.test.BaseJerseyTest;
import org.osc.core.broker.rest.server.exception.ErrorCodeDto;
import org.osc.core.broker.service.alert.AlertDto;
import org.osc.core.broker.service.response.BaseDtoResponse;
import org.osc.core.broker.service.response.ListResponse;

import javax.ws.rs.core.Application;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;

public class AlertApisTest extends BaseJerseyTest {

    private ListResponse expectedResponseList;
    private BaseDtoResponse<AlertDto> expectedResponse;

    @Override
    protected Application configure() {
        baseTestConfiguration();

        expectedResponseList = new ListResponse<AlertDto>();
        expectedResponse = new BaseDtoResponse<>();
        when(OSC.get().apiUtil().getListResponse(any(), any())).thenReturn(expectedResponseList);
        when(OSC.get().apiUtil().submitBaseRequestToService(any(),any())).thenReturn(expectedResponse);

        ResourceConfig application = getBaseResourceConfiguration();
        application.register(AlertApis.class);

        return application;
    }

    @Test
    public void testGetAlertsReturnsOK() {
        // Assume.
        Response response = null;
        try {
            List<AlertDto> list = new ArrayList<>();
            list.add(new AlertDto());
            expectedResponseList.setList(list);

            // Act.
            response = target("/api/server/v1/alerts").request().get();
            final List<AlertDto> alertDtos = response.readEntity(new GenericType<List<AlertDto>>() {
            });
            response.close();

            // Assert.
            assertThat(response.getStatus()).isEqualTo(200);
            assertThat(alertDtos.size()).isEqualTo(1);
        } finally {
            if (response != null) {
                response.close();
            }
        }
    }

    @Test
    public void testGetAlertReturnsOK() {
        // Assume.
        Response response = null;
        try {
            expectedResponse.setDto(getAlertDto());

            // Act.
            response = target("/api/server/v1/alerts/1").request().get();
            final AlertDto alertDto = response.readEntity(new GenericType<AlertDto>() {
            });
            response.close();

            // Assert.
            assertThat(response.getStatus()).isEqualTo(200);
        } finally {
            if (response != null) {
                response.close();
            }
        }
    }

    @Test
    public void testGetAlert_withBadParam_ReturnsErrorCode() {
        // Assume.
        Response response = null;
        try {
            expectedResponse.setDto(getAlertDto());

            // Act.
            response = target("/api/server/v1/alerts/a").request()
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
                    .get();

            ErrorCodeDto error = response.readEntity(new GenericType<ErrorCodeDto>() {
            });
            response.close();

            // Assert.
            assertThat(response.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
        } finally {
            if (response != null) {
                response.close();
            }
        }

    }

    @Test
    public void testGetAlert_withBadQueryParam_ReturnsErrorCode() {
        // Assume.
        Response response = null;
        try {
            expectedResponse.setDto(getAlertDto());

            // Act.
            response = target("/api/server/v1/alerts/1?a=2").request()
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
                    .get();

            ErrorCodeDto error = response.readEntity(new GenericType<ErrorCodeDto>() {
            });
            response.close();

            // Assert.
            assertThat(response.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
        } finally {
            if (response != null) {
                response.close();
            }
        }
    }


    /**
     * Returns AlertDto instance fulfilled with proper data
     */
    private AlertDto getAlertDto() {
        AlertDto alert = new AlertDto();

        alert.setName("testAlert");
        alert.setEventType(EventType.JOB_FAILURE);
        alert.setSeverity(Severity.LOW);
        alert.setId(1L);

        return alert;
    }

}
