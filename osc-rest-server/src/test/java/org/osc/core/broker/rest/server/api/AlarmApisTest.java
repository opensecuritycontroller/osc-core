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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.sling.testing.mock.osgi.junit.OsgiContext;
import org.glassfish.jersey.server.ResourceConfig;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;
import org.osc.core.broker.rest.server.ApiUtil;
import org.osc.core.broker.rest.server.api.test.BaseJerseyTest;
import org.osc.core.broker.service.api.AddAlarmServiceApi;
import org.osc.core.broker.service.api.DeleteAlarmServiceApi;
import org.osc.core.broker.service.api.GetDtoFromEntityServiceFactoryApi;
import org.osc.core.broker.service.api.ListAlarmServiceApi;
import org.osc.core.broker.service.api.UpdateAlarmServiceApi;
import org.osc.core.broker.service.api.server.UserContextApi;
import org.osc.core.broker.service.dto.AlarmDto;
import org.osc.core.broker.service.response.ListResponse;
import org.osc.core.common.alarm.AlarmAction;
import org.osc.core.common.alarm.EventType;
import org.osc.core.common.alarm.Severity;

public class AlarmApisTest extends BaseJerseyTest {

    private ListResponse expectedResponseList;

    private Response expectedResponse;

    @Rule
    public OsgiContext context;

    @Override
    protected Application configure() {
        baseTestConfiguration();

        //configure services
        this.context = new OsgiContext();
        ApiUtil apiUtil = Mockito.mock(ApiUtil.class);
        this.context.registerService(apiUtil);
        AddAlarmServiceApi addAlarm = Mockito.mock(AddAlarmServiceApi.class);
        this.context.registerService(AddAlarmServiceApi.class, addAlarm);
        UpdateAlarmServiceApi updateAlarm = Mockito.mock(UpdateAlarmServiceApi.class);
        this.context.registerService(UpdateAlarmServiceApi.class, updateAlarm);
        DeleteAlarmServiceApi deleteAlarm = Mockito.mock(DeleteAlarmServiceApi.class);
        this.context.registerService(DeleteAlarmServiceApi.class, deleteAlarm);
        ListAlarmServiceApi listAlarm = Mockito.mock(ListAlarmServiceApi.class);
        this.context.registerService(ListAlarmServiceApi.class, listAlarm);

        GetDtoFromEntityServiceFactoryApi getDtoFactory = Mockito.mock(GetDtoFromEntityServiceFactoryApi.class);
        this.context.registerService(GetDtoFromEntityServiceFactoryApi.class, getDtoFactory);

        UserContextApi userContext = Mockito.mock(UserContextApi.class);
        this.context.registerService(UserContextApi.class, userContext);

        AlarmApis service = new AlarmApis();
        this.context.registerInjectActivateService(service);

        ResourceConfig application = getBaseResourceConfiguration()
                .register(service);

        //configure responses
        this.expectedResponseList = new ListResponse<AlarmDto>();
        this.expectedResponse = Response.status(Response.Status.ACCEPTED).build();
        when(apiUtil.getListResponse(any(), any())).thenReturn(this.expectedResponseList);
        when(apiUtil.getResponseForBaseRequest(any(), any())).thenReturn(this.expectedResponse);

        return application;
    }

    @Test
    public void testGetAlarms_expectListOfAlarmsAndStatusOk() {
        // Assume.
        Response response = null;
        try {
            List<AlarmDto> list = new ArrayList<>();
            list.add(new AlarmDto());
            this.expectedResponseList.setList(list);

            // Act.
            response = target("/api/server/v1/alarms")
                    .request()
                    .header(this.authorizationHeader, this.authorizationCreds)
                    .get();
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
            response = target("/api/server/v1/alarms")
                    .request()
                    .header(this.authorizationHeader, this.authorizationCreds)
                    .post(alarmEntity);
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
                    .header(this.authorizationHeader, this.authorizationCreds)
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
                    .header(this.authorizationHeader, this.authorizationCreds)
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
    public void testDeleteAlarm_withBadPathParam_expectErrorCode() {
        // Assume.
        Response response = null;
        try {
            String badParam = "textButShouldBeNumber";

            // Act.
            response = target("/api/server/v1/alarms/" + badParam)
                    .request()
                    .header(this.authorizationHeader, this.authorizationCreds)
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
