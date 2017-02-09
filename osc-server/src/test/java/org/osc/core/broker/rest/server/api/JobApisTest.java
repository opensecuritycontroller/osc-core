package org.osc.core.broker.rest.server.api;

import org.glassfish.jersey.server.ResourceConfig;
import org.junit.Test;
import org.mockito.ArgumentMatcher;
import org.osc.core.broker.di.OSC;
import org.osc.core.broker.rest.server.OscRestServlet;
import org.osc.core.broker.rest.server.api.test.BaseJerseyTest;
import org.osc.core.broker.service.GetDtoFromEntityService;
import org.osc.core.broker.service.dto.JobRecordDto;
import org.osc.core.broker.service.request.BaseRequest;
import org.osc.core.broker.service.request.GetDtoFromEntityRequest;
import org.osc.core.broker.service.response.BaseDtoResponse;
import org.osc.core.broker.service.response.ListResponse;

import javax.ws.rs.core.Application;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.when;

public class JobApisTest extends BaseJerseyTest {
    private static final String JOBS_URL = OscRestServlet.SERVER_API_PATH_PREFIX + "/jobs";

    @Override
    protected Application configure() {
        baseTestConfiguration();

        ResourceConfig application = getBaseResourceConfiguration()
                .register(JobApis.class);

        return application;
    }

    @Test
    public void testGetJobs_withValidRequest_returnsOK() throws Exception {
        // Assume.
        List<JobRecordDto> list = new ArrayList<>(Arrays.asList(new JobRecordDto()));
        ListResponse<JobRecordDto> listResponse = new ListResponse<>(list);
        when(OSC.get().listJobService().dispatch(any())).thenReturn(listResponse);

        // Act.
        Response response = target(JOBS_URL).request().get();
        final List<JobRecordDto> jobDtos = response.readEntity(new GenericType<List<JobRecordDto>>() {
        });

        // Assert.
        assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
        assertThat(jobDtos.size()).isEqualTo(1);
    }

    @Test
    public void testGetJob_withValidJobId_returnsOK() throws Exception {
        // Assume.
        long jobId = 666;

        JobRecordDto expectedDto = new JobRecordDto();
        expectedDto.setName("TEST_JOB_DTO");
        expectedDto.setId(jobId);
        BaseDtoResponse<JobRecordDto> dtoResponse = new BaseDtoResponse<>();
        dtoResponse.setDto(expectedDto);

        GetDtoFromEntityRequest getDtoRequest = new GetDtoFromEntityRequest();
        getDtoRequest.setEntityId(jobId);
        getDtoRequest.setEntityName("JobRecord");

        GetDtoFromEntityRequestMatcher t = new GetDtoFromEntityRequestMatcher(getDtoRequest);
        when(OSC.get().apiUtil().submitBaseRequestToService(any(GetDtoFromEntityService.class),
                argThat(new GetDtoFromEntityRequestMatcher(getDtoRequest))))
                .thenReturn(dtoResponse);

        // Act.
        Response response = target(JOBS_URL + "/" + jobId).request().get();
        final JobRecordDto dtoFromResponse = response.readEntity(JobRecordDto.class);

        // Assert.
        assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
        assertThat(dtoFromResponse).isEqualToComparingFieldByField(expectedDto);
    }

    @Test
    public void testGetJob_withInvalidJobIdFormat_returnsBadRequest() throws Exception {
        // Assume.
        String jobIdInInvalidFormat = 666 + "InvalidCharacters";

        // Act.
        Response response = target(JOBS_URL + "/" + jobIdInInvalidFormat).request().get();


        // Assert.
        assertThat(response.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
    }

    @Test
    public void testGetJob_withNotExistingJobId_returnsNotFound() throws Exception {
        // Assume.
        Response response = null;
        try {
            long notExistingJobId = 667;

            mockSessionFactory(OSC.get().dtoFromEntityService());
            callRealMethods(OSC.get().apiUtil());
            callRealMethods(OSC.get().dtoFromEntityService());

            // Act.
            response = target(JOBS_URL + "/" + notExistingJobId).request().get();

            // Assert.
            assertThat(response.getStatus()).isEqualTo(Response.Status.NOT_FOUND.getStatusCode());
        } finally {
            if (response != null) {
                response.close();
            }
        }
    }

    private class GetDtoFromEntityRequestMatcher extends ArgumentMatcher<BaseRequest<?>> {
        private GetDtoFromEntityRequest expected;

        public GetDtoFromEntityRequestMatcher(GetDtoFromEntityRequest expected) {
            this.expected = expected;
        }

        @Override
        public boolean matches(Object o) {
            if (!(o instanceof GetDtoFromEntityRequest)) {
                return false;
            }

            GetDtoFromEntityRequest actual = (GetDtoFromEntityRequest) o;

            return actual.getEntityId() == this.expected.getEntityId() &&
                    actual.getEntityName() == this.expected.getEntityName();
        }
    }
}
