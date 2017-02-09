package org.osc.core.broker.rest.server.api;

import org.glassfish.jersey.server.ResourceConfig;
import org.junit.Test;
import org.osc.core.broker.di.OSC;
import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.model.entities.events.AcknowledgementStatus;
import org.osc.core.broker.model.entities.events.EventType;
import org.osc.core.broker.model.entities.events.Severity;
import org.osc.core.broker.rest.server.api.test.BaseJerseyTest;
import org.osc.core.broker.rest.server.exception.ErrorCodeDto;
import org.osc.core.broker.service.alert.AlertDto;
import org.osc.core.broker.service.response.BaseDtoResponse;
import org.osc.core.broker.service.response.EmptySuccessResponse;
import org.osc.core.broker.service.response.ListResponse;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;

public class AlertApisTest extends BaseJerseyTest {

    @Override
    protected Application configure() {
        baseTestConfiguration();

        ResourceConfig application = getBaseResourceConfiguration();
        application.register(AlertApis.class);

        return application;
    }

    @Test
    public void testGetAlertsReturnsOK() throws Exception {
        // Assume.
        Response response = null;
        try {
            ListResponse<AlertDto> listResponse = new ListResponse<>();

            listResponse.getList().add(new AlertDto());

            mockSessionFactory(OSC.get().listAlertService());
            callRealMethods(OSC.get().apiUtil());
            when(OSC.get().listAlertService().dispatch(any())).thenCallRealMethod();
            when(OSC.get().listAlertService().exec(any(),any())).thenReturn(listResponse);

            // Act.
            response = target("/api/server/v1/alerts")
                    .request()
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
                    .get();
            final List<AlertDto> alertDtos = response.readEntity(new GenericType<List<AlertDto>>() {
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
    public void testGetAlert_withGoodRequest_ReturnsNotFoundAndErrorCode() throws Exception {
        // Assume.
        Response response = null;
        try {
            mockSessionFactory(OSC.get().dtoFromEntityService());
            callRealMethods(OSC.get().apiUtil());
            callRealMethods(OSC.get().dtoFromEntityService());

            // Act.
            response = target("/api/server/v1/alerts/1")
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
    public void testGetAlert_withGoodRequest_ServiceDispacherThrowsNullPointer_ReturnsErrorCode() throws Exception {
        // Assume.
        Response response = null;
        try {
            mockSessionFactory(OSC.get().dtoFromEntityService());
            callRealMethods(OSC.get().apiUtil());
            when(OSC.get().dtoFromEntityService().dispatch(any())).thenCallRealMethod();
            when(OSC.get().dtoFromEntityService().exec(any(),any())).thenThrow(NullPointerException.class);

            // Act.
            response = target("/api/server/v1/alerts/1")
                    .request()
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
                    .get();
            ErrorCodeDto error = response.readEntity(new GenericType<ErrorCodeDto>() {
            });

            // Assert.
            assertThat(response.getStatus()).isEqualTo(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
            assertThat(error.getErrorMessages().size()).isEqualTo(1);
        } finally {
            if (response != null) {
                response.close();
            }
        }
    }


    @Test
    public void testGetAlert_withGoodRequest_ReturnsOk() throws Exception {
        // Assume.
        Response response = null;
        try {

            mockSessionFactory(OSC.get().dtoFromEntityService());
            callRealMethods(OSC.get().apiUtil());
            when(OSC.get().dtoFromEntityService().dispatch(any())).thenCallRealMethod();
            BaseDtoResponse<AlertDto> alertResponse = new BaseDtoResponse<>();
            alertResponse.setDto(new AlertDto());
            when(OSC.get().dtoFromEntityService().exec(any(),any())).thenReturn(alertResponse);

            // Act.
            response = target("/api/server/v1/alerts/1")
                    .request()
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
                    .get();
            final AlertDto alertDto = response.readEntity(new GenericType<AlertDto>() {
            });

            // Assert.
            assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
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

            // Act.
            response = target("/api/server/v1/alerts/a").request()
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
                    .get();

            ErrorCodeDto error = response.readEntity(new GenericType<ErrorCodeDto>() {
            });

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

            // Act.
            response = target("/api/server/v1/alerts/1?a=2").request()
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
                    .get();

            ErrorCodeDto error = response.readEntity(new GenericType<ErrorCodeDto>() {
            });

            // Assert.
            assertThat(response.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
        } finally {
            if (response != null) {
                response.close();
            }
        }
    }

    @Test
    public void testPutAlert_withGoodRequest_DbThrowsNull_ReturnsErrorCode() throws Exception {
        // Assume.
        Response response = null;
        try {

            mockSessionFactory(OSC.get().acknowledgeAlertService());
            callRealMethods(OSC.get().apiUtil());
            callRealMethods(OSC.get().acknowledgeAlertService());

            AlertDto alert = getAlertDto();

            Entity<AlertDto> alarmEntity = Entity.entity(alert, MediaType.APPLICATION_JSON);

            // Act.
            response = target("/api/server/v1/alerts/"+alert.getId())
                    .request()
                    .put(alarmEntity);
            ErrorCodeDto error = response.readEntity(new GenericType<ErrorCodeDto>() {
            });

            // Assert.
            assertThat(response.getStatus()).isEqualTo(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
            assertThat(error.getErrorMessages().size()).isEqualTo(1);
        } finally {
            if (response != null) {
                response.close();
            }
        }
    }

    @Test
    public void testPutAlert_withBadRequestParamAndId_ReturnsErrorCode() throws Exception {
        // Assume.
        Response response = null;
        try {

            mockSessionFactory(OSC.get().acknowledgeAlertService());
            callRealMethods(OSC.get().apiUtil());
            callRealMethods(OSC.get().acknowledgeAlertService());

            AlertDto alert = getAlertDto();
            alert.setId(1L);

            Entity<AlertDto> alarmEntity = Entity.entity(alert, MediaType.APPLICATION_JSON);

            // Act.
            response = target("/api/server/v1/alerts/2")
                    .request()
                    .put(alarmEntity);
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
    public void testPutAlert_withGoodRequest_ReturnsOk() throws Exception {
        // Assume.
        Response response = null;
        try {
            mockSessionFactory(OSC.get().acknowledgeAlertService());
            callRealMethods(OSC.get().apiUtil());
            when(OSC.get().acknowledgeAlertService().dispatch(any())).thenCallRealMethod();
            when(OSC.get().acknowledgeAlertService().exec(any(),any())).thenReturn(new EmptySuccessResponse());
            AlertDto alert = getAlertDto();

            Entity<AlertDto> alarmEntity = Entity.entity(alert, MediaType.APPLICATION_JSON);

            // Act.
            response = target("/api/server/v1/alerts/"+alert.getId())
                    .request()
                    .put(alarmEntity);

            // Assert.
            assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
        } finally {
            if (response != null) {
                response.close();
            }
        }
    }

    @Test
    public void testPutAlertWithObject_withGoodRequest_ReturnsErrorCode() {
        // Assume.
        Response response = null;
        when(OSC.get().apiUtil().getResponseForBaseRequest(any(),any())).thenReturn(Response.accepted().build());
        try {
            AlertDto alert = getAlertDto();
            alert.setObject(new LockObjectReference());

            Entity<AlertDto> alarmEntity = Entity.entity(alert, MediaType.APPLICATION_JSON);

            // Act.
            response = target("/api/server/v1/alerts/2")
                    .request()
                    .put(alarmEntity);
            ErrorCodeDto error = response.readEntity(new GenericType<ErrorCodeDto>() {
            });

            // Assert.
            assertThat(response.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
            assertThat(error.getErrorMessages().size()).isEqualTo(3);
        } finally {
            if (response != null) {
                response.close();
            }
        }
    }

    @Test
    public void testPutAlertWithObject_withGoodRequest_ReturnsOk() {
        // Assume.
        Response response = null;
        when(OSC.get().apiUtil().getResponseForBaseRequest(any(),any())).thenReturn(Response.accepted().build());
        try {
            AlertDto alert = getAlertDto();
            alert.setObject(getLockObjectRefernce());

            Entity<AlertDto> alarmEntity = Entity.entity(alert, MediaType.APPLICATION_JSON);

            // Act.
            response = target("/api/server/v1/alerts/2")
                    .request()
                    .put(alarmEntity);

            // Assert.
            assertThat(response.getStatus()).isEqualTo(Response.Status.ACCEPTED.getStatusCode());
        } finally {
            if (response != null) {
                response.close();
            }
        }
    }

    @Test
    public void testPutAlertWithObject_withDiffernetIdAndParamId_ReturnsErrorCode() throws Exception {
        // Assume.
        Response response = null;
        when(OSC.get().apiUtil().getResponseForBaseRequest(any(),any())).thenReturn(Response.accepted().build());
        try {
            AlertDto alert = getAlertDto();
            alert.setObject(getLockObjectRefernce());
            alert.setId(2L);

            mockSessionFactory(OSC.get().dtoFromEntityService());
            callRealMethods(OSC.get().apiUtil());

            Entity<AlertDto> alarmEntity = Entity.entity(alert, MediaType.APPLICATION_JSON);

            // Act.
            response = target("/api/server/v1/alerts/1")
                    .request()
                    .put(alarmEntity);
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
    public void testDeleteAlert_withGoodRequest_ReturnsOk() throws Exception {
        // Assume.
        Response response = null;
        try {
            mockSessionFactory(OSC.get().deleteAlertService());
            callRealMethods(OSC.get().apiUtil());
            when(OSC.get().deleteAlertService().dispatch(any())).thenCallRealMethod();
            EmptySuccessResponse deleteResponse = new EmptySuccessResponse();
            when(OSC.get().deleteAlertService().exec(any(),any())).thenReturn(deleteResponse);

            // Act.
            response = target("/api/server/v1/alerts/2")
                    .request()
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
                    .delete();

            // Assert.
            assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
        } finally {
            if (response != null) {
                response.close();
            }
        }
    }

    @Test
    public void testDeleteAlert_withBadRequest_ReturnsErrorCode() throws Exception {
        // Assume.
        Response response = null;
        try {
            mockSessionFactory(OSC.get().deleteAlertService());
            callRealMethods(OSC.get().apiUtil());
            callRealMethods(OSC.get().deleteAlertService());

            // Act.
            response = target("/api/server/v1/alerts/2")
                    .request()
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
                    .delete();
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


    /**
     * Returns AlertDto instance fulfilled with proper data
     */
    private AlertDto getAlertDto() {
        AlertDto alert = new AlertDto();

        alert.setName("testAlert");
        alert.setEventType(EventType.JOB_FAILURE);
        alert.setSeverity(Severity.LOW);
        alert.setId(1L);
        alert.setStatus(AcknowledgementStatus.ACKNOWLEDGED);

        return alert;
    }

    /**
     * Returns LockObjectReference instance fulfilled with proper data
     */
    private LockObjectReference getLockObjectRefernce() {
        LockObjectReference lor = new LockObjectReference();

        lor.setId(1L);
        lor.setName("test");
        lor.setType(LockObjectReference.ObjectType.ALERT);

        return lor;
    }

}
