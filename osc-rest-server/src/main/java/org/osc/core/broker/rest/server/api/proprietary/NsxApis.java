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
package org.osc.core.broker.rest.server.api.proprietary;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import org.apache.log4j.Logger;
import org.osc.core.broker.rest.server.OscAuthFilter;
import org.osc.core.broker.rest.server.ServerRestConstants;
import org.osc.core.broker.rest.server.annotations.NsxAuth;
import org.osc.core.broker.service.api.AlertGeneratorApi;
import org.osc.core.broker.service.api.NsxDeleteAgentsServiceApi;
import org.osc.core.broker.service.api.NsxUpdateAgentsServiceApi;
import org.osc.core.broker.service.api.NsxUpdateProfileContainerServiceApi;
import org.osc.core.broker.service.api.NsxUpdateProfileServiceApi;
import org.osc.core.broker.service.api.server.ServerApi;
import org.osc.core.broker.service.api.server.UserContextApi;
import org.osc.core.broker.service.request.Attribute;
import org.osc.core.broker.service.request.ContainerSet;
import org.osc.core.broker.service.request.FabricAgents;
import org.osc.core.broker.service.request.NsxDeleteAgentsRequest;
import org.osc.core.broker.service.request.NsxUpdateAgentsRequest;
import org.osc.core.broker.service.request.NsxUpdateProfileContainerRequest;
import org.osc.core.broker.service.request.NsxUpdateProfileRequest;
import org.osc.core.broker.service.request.ServiceInstance;
import org.osc.core.broker.service.request.ServiceProfile;
import org.osc.core.broker.service.response.BaseJobResponse;
import org.osc.core.broker.service.response.NsxUpdateAgentsResponse;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;


@Component(service = NsxApis.class)
@Path(ServerRestConstants.NSX_API_PATH_PREFIX)
@NsxAuth
public class NsxApis {

    private static final Logger log = Logger.getLogger(NsxApis.class);

    @Reference
    NsxDeleteAgentsServiceApi nsxDeleteAgentsService;

    @Reference
    NsxUpdateAgentsServiceApi nsxUpdateAgentsService;

    @Reference
    NsxUpdateProfileServiceApi nsxUpdateProfileService;

    @Reference
    NsxUpdateProfileContainerServiceApi nsxUpdateProfileContainerService;

    @Reference
    ServerApi server;

    @Reference
    private AlertGeneratorApi alertGenerator;

    @Reference
    UserContextApi userContext;

    @XmlRootElement
    @XmlAccessorType(XmlAccessType.FIELD)
    static class ServiceInstanceResponse {
        static class InstanceResponseAttributes {
            public List<Attribute> attribute;
        }

        public InstanceResponseAttributes instanceResponseAttributes;
    }

    @XmlRootElement
    @XmlAccessorType(XmlAccessType.FIELD)
    static class ServiceProfileResponse {
        public String message;

        public ServiceProfileResponse() {
        }

        public ServiceProfileResponse(String message) {
            this.message = message;
        }
    }

    @Path("/agents")
    @PUT
    @Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    public Response putAgents(@Context HttpHeaders headers, @Context HttpServletRequest request,
                              FabricAgents fabricAgents) throws Exception {

        log.info("putAgents: " + fabricAgents.toString());

        this.userContext.setUser(OscAuthFilter.getUsername(headers));
        String nsxIpAddress = request.getRemoteAddr();

        NsxUpdateAgentsRequest serviceRequest = new NsxUpdateAgentsRequest();
        serviceRequest.nsxIpAddress = nsxIpAddress;
        serviceRequest.fabricAgents = fabricAgents;
        NsxUpdateAgentsResponse response = new NsxUpdateAgentsResponse();
        try {
            response = this.nsxUpdateAgentsService.dispatch(serviceRequest);
        } catch (Exception ex) {
            log.error("Fail to update agents.", ex);
            this.alertGenerator.processNsxFailureEvent("Fail to update NSX Agents (" + ex.getMessage() + ")");
        }

        return Response.status(Status.OK).entity(response.updatedAgents).build();
    }

    @Path("/agents/{agentIds}")
    @DELETE
    @Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    public Response deleteAgents(@Context HttpHeaders headers, @Context HttpServletRequest request,
                                 @PathParam("agentIds") String agentIds) {

        log.info("deleteAgents(): " + agentIds);

        String nsxIpAddress = request.getRemoteAddr();
        this.userContext.setUser(OscAuthFilter.getUsername(headers));

        NsxDeleteAgentsRequest serviceRequest = new NsxDeleteAgentsRequest();
        serviceRequest.nsxIpAddress = nsxIpAddress;
        serviceRequest.agentIds = agentIds;
        try {
            this.nsxDeleteAgentsService.dispatch(serviceRequest);
        } catch (Exception ex) {
            log.error("Fail to delete agent " + agentIds, ex);
            this.alertGenerator.processNsxFailureEvent("Fail to delete NSX Agents (" + ex.getMessage() + ")");
        }

        return Response.status(Status.OK).build();
    }

    @Path("/si/serviceinstance")
    @POST
    @Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    public Response postServiceInstance(ServiceInstance serviceInstance) {
        log.info("postServiceInstance(): " + serviceInstance.toString());
        return Response.status(Status.OK).entity(new ServiceInstanceResponse()).build();
    }

    @Path("/si/serviceinstance/{serviceInstanceId}")
    @DELETE
    @Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    public Response deleteServiceInstance(@PathParam("serviceInstanceId") String serviceInstanceId) {
        log.info("deleteServiceInstance(): " + serviceInstanceId);
        return Response.status(Status.OK).entity(new ServiceInstanceResponse()).build();
    }

    @Path("/si/serviceinstance/{serviceInstanceId}")
    @PUT
    @Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    public Response putServiceInstance(@PathParam("serviceInstanceId") String serviceInstanceId) {
        log.info("putServiceInstance(): " + serviceInstanceId);
        return Response.status(Status.OK).entity(new ServiceInstanceResponse()).build();
    }

    @Path("/si/serviceprofile/")
    @POST
    @Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    public Response postServiceProfile(ServiceProfile serviceProfile) {
        log.info("postServiceProfile(): " + serviceProfile);
        return Response.status(Status.OK).entity(new ServiceProfileResponse()).build();
    }

    @Path("/si/serviceprofile/{serviceProfileId}")
    @DELETE
    @Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    public Response deleteServiceProfile(@PathParam("serviceProfileId") String serviceProfileId) {
        log.info("deleteServiceProfile(): " + serviceProfileId);
        return Response.status(Status.OK).entity(new ServiceProfileResponse()).build();
    }

    @Path("/si/serviceprofile/{serviceProfileId}")
    @PUT
    @Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    public Response putServiceProfile(@Context HttpHeaders headers,
                                      @PathParam("serviceProfileId") String serviceProfileId, ServiceProfile serviceProfile) {

        log.info("putServiceProfile(): " + serviceProfileId);
        this.userContext.setUser(OscAuthFilter.getUsername(headers));

        NsxUpdateProfileRequest request = new NsxUpdateProfileRequest();
        request.serviceProfile = serviceProfile;
        ServiceProfileResponse response = new ServiceProfileResponse();

        String productName = this.server.getProductName();
        try {
            this.nsxUpdateProfileService.dispatch(request);
            response.message = productName + " started Service Profile Synchronization Job";
        } catch (Exception ex) {
            log.error("Error while updating service profile", ex);
            response.message = productName + ": Fail to trigger Synchronization Job for NSX Service Profile '"
                    + serviceProfile.getName() + "' (" + ex.getMessage() + ")";
            this.alertGenerator.processNsxFailureEvent(response.message);
        }

        return Response.status(Status.OK).entity(response).build();
    }

    @Path("/si/serviceprofile/{serviceProfileId}/containerset")
    @PUT
    @Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    public Response putServiceProfileContainerset(@Context HttpHeaders headers, @Context HttpServletRequest httpRequest,
                                                  @PathParam("serviceProfileId") String serviceProfileId, ContainerSet containerSet) {

        log.info("putServiceProfileContainerset(): " + serviceProfileId + ", ContainerSet " + containerSet);
        this.userContext.setUser(OscAuthFilter.getUsername(headers));
        String nsxIpAddress = httpRequest.getRemoteAddr();

        NsxUpdateProfileContainerRequest request = new NsxUpdateProfileContainerRequest();
        request.serviceProfileId = serviceProfileId;
        request.nsxIpAddress = nsxIpAddress;
        request.containerSet = containerSet;

        ServiceProfileResponse response = new ServiceProfileResponse();

        String productName = this.server.getProductName();
        try {
            BaseJobResponse serviceResponse = this.nsxUpdateProfileContainerService.dispatch(request);
            response.message = productName + " started Service Profile Cotainer Synchronization Job (id:"
                    + serviceResponse.getJobId() + ").";

        } catch (Exception ex) {
            log.error("Error while updating service profile container set", ex);
            response.message = productName
                    + ": Fail to trigger Synchronization Job for NSX Service Profile Container Set ID '"
                    + serviceProfileId + "' (" + ex.getMessage() + ")";
            this.alertGenerator.processNsxFailureEvent(response.message);
        }

        return Response.status(Status.OK).build();
    }
}
