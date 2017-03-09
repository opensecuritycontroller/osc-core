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

import java.util.ArrayList;
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
import org.osc.core.broker.model.entities.events.SystemFailureType;
import org.osc.core.broker.rest.client.nsx.model.Attribute;
import org.osc.core.broker.rest.client.nsx.model.ContainerSet;
import org.osc.core.broker.rest.client.nsx.model.FabricAgents;
import org.osc.core.broker.rest.client.nsx.model.ServiceInstance;
import org.osc.core.broker.rest.client.nsx.model.ServiceProfile;
import org.osc.core.broker.rest.server.IscRestServlet;
import org.osc.core.broker.rest.server.NsxAuthFilter;
import org.osc.core.broker.service.NsxDeleteAgentsService;
import org.osc.core.broker.service.NsxUpdateAgentsService;
import org.osc.core.broker.service.NsxUpdateProfileContainerService;
import org.osc.core.broker.service.NsxUpdateProfileService;
import org.osc.core.broker.service.alert.AlertGenerator;
import org.osc.core.broker.service.request.NsxDeleteAgentsRequest;
import org.osc.core.broker.service.request.NsxUpdateAgentsRequest;
import org.osc.core.broker.service.request.NsxUpdateProfileContainerRequest;
import org.osc.core.broker.service.request.NsxUpdateProfileRequest;
import org.osc.core.broker.service.response.BaseJobResponse;
import org.osc.core.broker.service.response.NsxUpdateAgentsResponse;
import org.osc.core.broker.util.SessionUtil;
import org.osgi.service.component.annotations.Component;

import com.mcafee.vmidc.server.Server;
import com.sun.jersey.spi.container.ResourceFilters;

@Component(service = NsxApis.class)
@Path(IscRestServlet.NSX_API_PATH_PREFIX)
@ResourceFilters({ NsxAuthFilter.class })
public class NsxApis {

    private static final Logger log = Logger.getLogger(NsxApis.class);

    @XmlRootElement
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class UpdatedAgents {
        public List<UpdatedAgent> updatedAgent = new ArrayList<UpdatedAgent>();
    }

    @XmlRootElement
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class UpdatedAgent {
        public String agentId;
        public String responseString = "{data to be written back to SVM}";

    }

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
    @Consumes(MediaType.APPLICATION_XML)
    @Produces(MediaType.APPLICATION_XML)
    public Response putAgents(@Context HttpHeaders headers, @Context HttpServletRequest request,
            FabricAgents fabricAgents) throws Exception {

        log.info("putAgents: " + fabricAgents.toString());

        SessionUtil.setUser(SessionUtil.getUsername(headers));
        String nsxIpAddress = request.getRemoteAddr();

        NsxUpdateAgentsRequest serviceRequest = new NsxUpdateAgentsRequest();
        serviceRequest.nsxIpAddress = nsxIpAddress;
        serviceRequest.fabricAgents = fabricAgents;
        NsxUpdateAgentsService service = new NsxUpdateAgentsService();
        NsxUpdateAgentsResponse response = new NsxUpdateAgentsResponse();
        try {
            response = service.dispatch(serviceRequest);
        } catch (Exception ex) {
            log.error("Fail to update agents.", ex);
            AlertGenerator.processSystemFailureEvent(SystemFailureType.NSX_NOTIFICATION,
                    "Fail to update NSX Agents (" + ex.getMessage() + ")");
        }

        return Response.status(Status.OK).entity(response.updatedAgents).build();
    }

    @Path("/agents/{agentIds}")
    @DELETE
    @Consumes(MediaType.APPLICATION_XML)
    public Response deleteAgents(@Context HttpHeaders headers, @Context HttpServletRequest request,
            @PathParam("agentIds") String agentIds) {

        log.info("deleteAgents(): " + agentIds);

        String nsxIpAddress = request.getRemoteAddr();
        SessionUtil.setUser(SessionUtil.getUsername(headers));

        NsxDeleteAgentsRequest serviceRequest = new NsxDeleteAgentsRequest();
        serviceRequest.nsxIpAddress = nsxIpAddress;
        serviceRequest.agentIds = agentIds;
        NsxDeleteAgentsService service = new NsxDeleteAgentsService();
        try {
            service.dispatch(serviceRequest);
        } catch (Exception ex) {
            log.error("Fail to delete agent " + agentIds, ex);
            AlertGenerator.processSystemFailureEvent(SystemFailureType.NSX_NOTIFICATION,
                    "Fail to delete NSX Agents (" + ex.getMessage() + ")");
        }

        return Response.status(Status.OK).build();
    }

    @Path("/si/serviceinstance")
    @POST
    @Consumes(MediaType.APPLICATION_XML)
    @Produces(MediaType.APPLICATION_XML)
    public Response postServiceInstance(ServiceInstance serviceInstance) {
        log.info("postServiceInstance(): " + serviceInstance.toString());
        return Response.status(Status.OK).entity(new ServiceInstanceResponse()).build();
    }

    @Path("/si/serviceinstance/{serviceInstanceId}")
    @DELETE
    @Consumes(MediaType.APPLICATION_XML)
    public Response deleteServiceInstance(@PathParam("serviceInstanceId") String serviceInstanceId) {
        log.info("deleteServiceInstance(): " + serviceInstanceId);
        return Response.status(Status.OK).entity(new ServiceInstanceResponse()).build();
    }

    @Path("/si/serviceinstance/{serviceInstanceId}")
    @PUT
    @Consumes(MediaType.APPLICATION_XML)
    public Response putServiceInstance(@PathParam("serviceInstanceId") String serviceInstanceId) {
        log.info("putServiceInstance(): " + serviceInstanceId);
        return Response.status(Status.OK).entity(new ServiceInstanceResponse()).build();
    }

    @Path("/si/serviceprofile/")
    @POST
    @Consumes(MediaType.APPLICATION_XML)
    @Produces(MediaType.APPLICATION_XML)
    public Response postServiceProfile(ServiceProfile serviceProfile) {
        log.info("postServiceProfile(): " + serviceProfile);
        return Response.status(Status.OK).entity(new ServiceProfileResponse()).build();
    }

    @Path("/si/serviceprofile/{serviceProfileId}")
    @DELETE
    @Consumes(MediaType.APPLICATION_XML)
    public Response deleteServiceProfile(@PathParam("serviceProfileId") String serviceProfileId) {
        log.info("deleteServiceProfile(): " + serviceProfileId);
        return Response.status(Status.OK).entity(new ServiceProfileResponse()).build();
    }

    @Path("/si/serviceprofile/{serviceProfileId}")
    @PUT
    @Consumes(MediaType.APPLICATION_XML)
    public Response putServiceProfile(@Context HttpHeaders headers,
            @PathParam("serviceProfileId") String serviceProfileId, ServiceProfile serviceProfile) {

        log.info("putServiceProfile(): " + serviceProfileId);
        SessionUtil.setUser(SessionUtil.getUsername(headers));

        NsxUpdateProfileRequest request = new NsxUpdateProfileRequest();
        request.serviceProfile = serviceProfile;
        NsxUpdateProfileService service = new NsxUpdateProfileService();
        ServiceProfileResponse response = new ServiceProfileResponse();
        try {
            service.dispatch(request);
            response.message = Server.PRODUCT_NAME + " started Service Profile Synchronization Job";
        } catch (Exception ex) {
            log.error("Error while updating service profile", ex);
            response.message = Server.PRODUCT_NAME + ": Fail to trigger Synchronization Job for NSX Service Profile '"
                    + serviceProfile.getName() + "' (" + ex.getMessage() + ")";
            AlertGenerator.processSystemFailureEvent(SystemFailureType.NSX_NOTIFICATION, response.message);
        }

        return Response.status(Status.OK).entity(response).build();
    }

    @Path("/si/serviceprofile/{serviceProfileId}/containerset")
    @PUT
    @Consumes(MediaType.APPLICATION_XML)
    public Response putServiceProfileContainerset(@Context HttpHeaders headers, @Context HttpServletRequest httpRequest,
            @PathParam("serviceProfileId") String serviceProfileId, ContainerSet containerSet) {

        log.info("putServiceProfileContainerset(): " + serviceProfileId + ", ContainerSet " + containerSet);
        SessionUtil.setUser(SessionUtil.getUsername(headers));
        String nsxIpAddress = httpRequest.getRemoteAddr();

        NsxUpdateProfileContainerRequest request = new NsxUpdateProfileContainerRequest();
        request.serviceProfileId = serviceProfileId;
        request.nsxIpAddress = nsxIpAddress;
        request.containerSet = containerSet;

        NsxUpdateProfileContainerService service = new NsxUpdateProfileContainerService();
        ServiceProfileResponse response = new ServiceProfileResponse();
        try {
            BaseJobResponse serviceResponse = service.dispatch(request);
            response.message = Server.PRODUCT_NAME + " started Service Profile Cotainer Synchronization Job (id:"
                    + serviceResponse.getJobId() + ").";

        } catch (Exception ex) {
            log.error("Error while updating service profile container set", ex);
            response.message = Server.PRODUCT_NAME
                    + ": Fail to trigger Synchronization Job for NSX Service Profile Container Set ID '"
                    + serviceProfileId + "' (" + ex.getMessage() + ")";
            AlertGenerator.processSystemFailureEvent(SystemFailureType.NSX_NOTIFICATION, response.message);
        }

        return Response.status(Status.OK).build();
    }
}
