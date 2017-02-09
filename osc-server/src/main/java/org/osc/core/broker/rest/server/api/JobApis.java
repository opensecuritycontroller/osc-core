package org.osc.core.broker.rest.server.api;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.log4j.Logger;
import org.osc.core.broker.di.OSC;
import org.osc.core.broker.rest.server.OscRestServlet;
import org.osc.core.rest.annotations.OscAuth;
import org.osc.core.broker.rest.server.exception.ErrorCodeDto;
import org.osc.core.broker.rest.server.exception.VmidcRestServerException;
import org.osc.core.broker.service.GetDtoFromEntityService;
import org.osc.core.broker.service.ListTaskService;
import org.osc.core.broker.service.dto.JobRecordDto;
import org.osc.core.broker.service.dto.TaskRecordDto;
import org.osc.core.broker.service.persistence.JobEntityManager;
import org.osc.core.broker.service.request.GetDtoFromEntityRequest;
import org.osc.core.broker.service.request.ListTaskRequest;
import org.osc.core.broker.service.response.BaseDtoResponse;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.Authorization;
import org.osc.core.broker.service.response.ListResponse;

import java.util.List;

@Api(tags = "Operations for Jobs", authorizations = { @Authorization(value = "Basic Auth") })
@Path(OscRestServlet.SERVER_API_PATH_PREFIX + "/jobs")
@Produces({ MediaType.APPLICATION_JSON })
@OscAuth
public class JobApis {

    private static final Logger logger = Logger.getLogger(JobApis.class);

    @ApiOperation(value = "Retrieves all jobs",
            notes = "Retrieves all jobs",
            response = JobRecordDto.class,
            responseContainer = "List")
    @ApiResponses(value = { @ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 400, message = "In case of any error", response = ErrorCodeDto.class) })
    @GET
    public List<JobRecordDto> getJobs() {

        logger.info("Listing job records");

        try {
            ListResponse<JobRecordDto> res = OSC.get().listJobService().dispatch(null);
            return res.getList();
        } catch (Exception e) {
            throw new VmidcRestServerException(Response.status(Status.INTERNAL_SERVER_ERROR), e.getMessage());
        }
    }

    @ApiOperation(value = "Retrieves a Job",
            notes = "Retrieves Job specified by the Job Id",
            response = JobRecordDto.class)
    @ApiResponses(value = { @ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 400, message = "In case of any error", response = ErrorCodeDto.class) })
    @GET
    @Path("/{jobId}")
    public Response getJob(@PathParam("jobId") Long jobId) {

            GetDtoFromEntityRequest getDtoRequest = new GetDtoFromEntityRequest();
            getDtoRequest.setEntityId(jobId);
            getDtoRequest.setEntityName("JobRecord");

            GetDtoFromEntityService<JobRecordDto> getDtoService = OSC.get().dtoFromEntityService();

            BaseDtoResponse<JobRecordDto> res = OSC.get().apiUtil().submitBaseRequestToService(getDtoService, getDtoRequest);
            JobRecordDto jrd = res.getDto();

            jrd.setTaskCount(JobEntityManager.getTaskCount(jobId));
            jrd.setTaskCompleted(JobEntityManager.getCompletedTaskCount(jobId));

            return Response.status(Status.OK).entity(jrd).build();
    }

    @ApiOperation(value = "Retrieves Job's tasks",
            notes = "Retrieves a Job's Tasks for Job specified by the Job Id",
            response = TaskRecordDto.class,
            responseContainer = "List")
    @ApiResponses(value = { @ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 400, message = "In case of any error", response = ErrorCodeDto.class) })
    @GET
    @Path("/{jobId}/tasks")
    public List<TaskRecordDto> getJobTasks(@PathParam("jobId") Long jobId) {

        logger.info("Listing task records for job id " + jobId);

        try {
            ListTaskRequest listRequest = new ListTaskRequest();
            listRequest.setJobId(jobId);
            ListTaskService listService = new ListTaskService();
            ListResponse<TaskRecordDto> res = listService.dispatch(listRequest);

            return res.getList();
        } catch (Exception e) {
            throw new VmidcRestServerException(Response.status(Status.INTERNAL_SERVER_ERROR), e.getMessage());
        }
    }

}
