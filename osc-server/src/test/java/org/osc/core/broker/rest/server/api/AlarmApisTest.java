package org.osc.core.broker.rest.server.api;

import org.glassfish.jersey.server.ResourceConfig;
import org.junit.Test;
import org.osc.core.broker.di.OSC;
import org.osc.core.broker.model.entities.events.AlarmAction;
import org.osc.core.broker.model.entities.events.EventType;
import org.osc.core.broker.model.entities.events.Severity;
import org.osc.core.broker.rest.server.api.test.BaseJerseyTest;
import org.osc.core.broker.rest.server.exception.ErrorCodeDto;
import org.osc.core.broker.service.alarm.AlarmDto;
import org.osc.core.broker.service.response.ListResponse;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;

public class AlarmApisTest extends BaseJerseyTest {

    private ListResponse expectedResponseList;

    private Response expectedResponse;

    @Override
    protected Application configure() {
        baseTestConfiguration();

        expectedResponseList = new ListResponse<AlarmDto>();
        expectedResponse = Response.status(Response.Status.ACCEPTED).build();

        when(OSC.get().apiUtil().getListResponse(any(), any())).thenReturn(expectedResponseList);
        when(OSC.get().apiUtil().getResponseForBaseRequest(any(), any())).thenReturn(expectedResponse);

        ResourceConfig application = getBaseResourceConfiguration()
                .register(AlarmApis.class);

        return application;
    }

    @Test
    public void testGetAlarms_expectListOfAlarmsAndStatusOk() {
        // Assume.
        Response response = null;
        try {
            List<AlarmDto> list = new ArrayList<>();
            list.add(new AlarmDto());
            expectedResponseList.setList(list);

            // Act.
            response = target("/api/server/v1/alarms").request().get();
            final List<AlarmDto> alarmDtos = response.readEntity(new GenericType<List<AlarmDto>>() {
            });
            response.close();

            // Assert.
            assertThat(response.getStatus()).isEqualTo(200);
            assertThat(alarmDtos.size()).isEqualTo(1);
        } finally {
            if (response != null) {
                response.close();
            }
        }
    }

    @Test
    public void testPostAlarm_withGoodRequest_expectStatusAccepted() {
        // Assume.
        Response response = null;
        try {
            AlarmDto alarm = getAlarmDto();

            Entity<AlarmDto> alarmEntity = Entity.entity(alarm, MediaType.APPLICATION_JSON);

            // Act.
            response = target("/api/server/v1/alarms").request().post(alarmEntity);
            response.close();

            // Assert.
            assertThat(response.getStatus()).isEqualTo(202);
        } finally {
            if (response != null) {
                response.close();
            }
        }
    }

    @Test
    public void testPutAlarm_withGoodRequest_expectStatusAccepted() {
        // Assume.
        Response response = null;
        try {
            AlarmDto alarm = getAlarmDto();

            Entity<AlarmDto> alarmEntity = Entity.entity(alarm, MediaType.APPLICATION_JSON);

            // Act.
            response = target("/api/server/v1/alarms/2")
                    .request()
                    .put(alarmEntity);
            response.close();

            // Assert.
            assertThat(response.getStatus()).isEqualTo(202);
        } finally {
            if (response != null) {
                response.close();
            }
        }
    }

    @Test
    public void testPutAlarm_withBadPathParam_expectErrorCode() {
        // Assume.
        Response response = null;
        try {
            AlarmDto alarm = new AlarmDto();

            Entity<AlarmDto> alarmEntity = Entity.entity(alarm, MediaType.APPLICATION_JSON);
            String badParam = "textButShouldBeNumber";

            // Act.
            response = target("/api/server/v1/alarms/" + badParam)
                    .request()
                    .put(alarmEntity);
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
    public void testPostAlarms_withBadRegex_expectErrorCode() {
        // Assume.
        Response response = null;
        try {
            AlarmDto alarm = getAlarmDto();
            alarm.setRegexMatch("\\");

            Entity<AlarmDto> alarmEntity = Entity.entity(alarm, MediaType.APPLICATION_JSON);

            // Act.
            response = target("/api/server/v1/alarms").request().post(alarmEntity);
            ErrorCodeDto error = response.readEntity(new GenericType<ErrorCodeDto>() {
            });
            response.close();

            // Assert.
            assertThat(response.getStatus()).isEqualTo(400);
            assertThat(error.getErrorMessages().size()).isEqualTo(1);
        } finally {
            if (response != null) {
                response.close();
            }
        }
    }

    @Test
    public void testPostAlarms_withBadEmail_expectErrorCode() {
        // Assume.
        Response response = null;
        try {
            AlarmDto alarm = getAlarmDto();
            alarm.setReceipientEmail("email@email");

            Entity<AlarmDto> alarmEntity = Entity.entity(alarm, MediaType.APPLICATION_JSON);

            // Act.
            response = target("/api/server/v1/alarms").request().post(alarmEntity);
            ErrorCodeDto error = response.readEntity(new GenericType<ErrorCodeDto>() {
            });
            response.close();

            // Assert.
            assertThat(response.getStatus()).isEqualTo(400);
            assertThat(error.getErrorMessages().size()).isEqualTo(1);
        } finally {
            if (response != null) {
                response.close();
            }
        }
    }

    @Test
    public void testDeleteAlarm_withGoodRequest_expectStatusAccepted() {
        // Assume.
        Response response = null;
        try {
            String goodParam = "2";

            // Act.
            response = target("/api/server/v1/alarms/" + goodParam)
                    .request()
                    .delete();
            response.close();

            // Assert.
            assertThat(response.getStatus()).isEqualTo(202);
        } finally {
            if (response != null) {
                response.close();
            }
        }
    }

    @Test
    public void testDeleteAlarm_withBadPathParam_expectErrorCode() {
        // Assume.
        Response response = null;
        try {
            String badParam = "textButShouldBeNumber";

            // Act.
            response = target("/api/server/v1/alarms/" + badParam)
                    .request()
                    .delete();
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
    public void testPutAlarm_withBadRequest_expectErrorCode() {
        // Assume.
        Response response = null;
        try {
            AlarmDto alarm = new AlarmDto();

            Entity<AlarmDto> alarmEntity = Entity.entity(alarm, MediaType.APPLICATION_JSON);

            // Act.
            response = target("/api/server/v1/alarms/2")
                    .request()
                    .put(alarmEntity);
            response.close();

            // Assert.
            assertThat(response.getStatus()).isEqualTo(400);
        } finally {
            if (response != null) {
                response.close();
            }
        }
    }

    @Test
    public void testPostAlarms_withBadRequest_expectErrorCode() {
        // Assume.
        Response response = null;
        try {
            AlarmDto alarm = new AlarmDto();

            Entity<AlarmDto> alarmEntity = Entity.entity(alarm, MediaType.APPLICATION_JSON);

            // Act.
            response = target("/api/server/v1/alarms").request().post(alarmEntity);
            ErrorCodeDto error = response.readEntity(new GenericType<ErrorCodeDto>() {
            });
            response.close();

            // Assert.
            assertThat(response.getStatus()).isEqualTo(400);
        } finally {
            if (response != null) {
                response.close();
            }
        }
    }


    @Test
    public void testPostAlarms_withAlarmActionEmail_expectErrorCode() {
        // Assume.
        Response response = null;
        try {
            AlarmDto alarm = getAlarmDto();
            alarm.setAlarmAction(AlarmAction.EMAIL);
            alarm.setReceipientEmail(null);

            Entity<AlarmDto> alarmEntity = Entity.entity(alarm, MediaType.APPLICATION_JSON);

            // Act.
            response = target("/api/server/v1/alarms").request().post(alarmEntity);
            ErrorCodeDto error = response.readEntity(new GenericType<ErrorCodeDto>() {
            });
            response.close();

            // Assert.
            assertThat(response.getStatus()).isEqualTo(400);
            assertThat(error.getErrorMessages().size()).isEqualTo(1);
        } finally {
            if (response != null) {
                response.close();
            }
        }
    }

    @Test
    public void testPostAlarms_withToBigNameEmailAndRegex_expectErrorCode() {
        // Assume.
        Response response = null;
        try {
            AlarmDto alarm = getAlarmDto();
            alarm.setName(getString155() + "1");
            alarm.setRegexMatch(getString155() + "1");
            alarm.setReceipientEmail(getString155() + "1");


            Entity<AlarmDto> alarmEntity = Entity.entity(alarm, MediaType.APPLICATION_JSON);

            // Act.
            response = target("/api/server/v1/alarms").request().post(alarmEntity);
            ErrorCodeDto error = response.readEntity(new GenericType<ErrorCodeDto>() {
            });
            response.close();

            // Assert.
            assertThat(response.getStatus()).isEqualTo(400);
            assertThat(error.getErrorMessages().size()).isEqualTo(4);
        } finally {
            if (response != null) {
                response.close();
            }
        }

    }

    /**
     * Returns alarmDto instance fulfilled with proper data
     */
    private AlarmDto getAlarmDto() {
        AlarmDto alarm = new AlarmDto();

        alarm.setName("testAlarm");
        alarm.setAlarmAction(AlarmAction.NONE);
        alarm.setEnabledAlarm(false);
        alarm.setEventType(EventType.JOB_FAILURE);
        alarm.setRegexMatch("\\d");
        alarm.setSeverity(Severity.LOW);
        alarm.setId(1L);

        return alarm;
    }

}
