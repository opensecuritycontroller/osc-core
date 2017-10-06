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

import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.osc.core.broker.rest.server.LogProvider;
import org.osc.core.broker.rest.server.ServerRestConstants;
import org.osc.core.broker.rest.server.annotations.OscAuth;
import org.osc.core.broker.rest.server.exception.VmidcRestServerException;
import org.osc.core.broker.service.api.GetDtoFromEntityServiceApi;
import org.osc.core.broker.service.api.GetDtoFromEntityServiceFactoryApi;
import org.osc.core.broker.service.api.JobEntityManagerApi;
import org.osc.core.broker.service.api.ListJobServiceApi;
import org.osc.core.broker.service.api.ListTaskServiceApi;
import org.osc.core.broker.service.dto.JobRecordDto;
import org.osc.core.broker.service.dto.TaskRecordDto;
import org.osc.core.broker.service.exceptions.ErrorCodeDto;
import org.osc.core.broker.service.request.GetDtoFromEntityRequest;
import org.osc.core.broker.service.request.ListTaskRequest;
import org.osc.core.broker.service.response.BaseDtoResponse;
import org.osc.core.broker.service.response.ListResponse;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.Authorization;

@Component(service = JobApis.class)
@Api(tags = "Operations for Jobs", authorizations = { @Authorization(value = "Basic Auth") })
@Path(ServerRestConstants.SERVER_API_PATH_PREFIX + "/jobs")
@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
@OscAuth
public class JobApis {

    private static final Logger logger = LogProvider.getLogger(JobApis.class);

    @Reference
    private ListJobServiceApi listJobService;

    @Reference
    private ListTaskServiceApi listTaskService;

    @Reference
    private GetDtoFromEntityServiceFactoryApi getDtoFromEntityServiceFactory;

    @Reference
    private JobEntityManagerApi jobEntityManager;

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
            ListResponse<JobRecordDto> res = this.listJobService.dispatch(null);
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

        try {
            GetDtoFromEntityRequest getDtoRequest = new GetDtoFromEntityRequest();
            getDtoRequest.setEntityId(jobId);
            getDtoRequest.setEntityName("JobRecord");
            GetDtoFromEntityServiceApi<JobRecordDto> getDtoService = this.getDtoFromEntityServiceFactory.getService(JobRecordDto.class);
            BaseDtoResponse<JobRecordDto> res = getDtoService.dispatch(getDtoRequest);
            JobRecordDto jrd = res.getDto();

            jrd.setTaskCount(this.jobEntityManager.taskCount(jobId));
            jrd.setTaskCompleted(this.jobEntityManager.completedTaskCount(jobId));

            return Response.status(Status.OK).entity(jrd).build();

        } catch (Exception e) {

            logger.error("Failed to load job id " + jobId, e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }
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
            ListResponse<TaskRecordDto> res = this.listTaskService.dispatch(listRequest);

            return res.getList();
        } catch (Exception e) {
            throw new VmidcRestServerException(Response.status(Status.INTERNAL_SERVER_ERROR), e.getMessage());
        }
    }

}
