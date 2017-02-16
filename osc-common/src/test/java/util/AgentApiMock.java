package util;

import org.osc.core.rest.client.agent.model.output.AgentStatusResponse;
import org.osc.core.util.VersionUtil;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.InputStream;

/**
 * Mocked rest Api
 * */
@Path("/api/agent/v1")
public class AgentApiMock {

    @Path("/status")
    @GET
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    public AgentStatusResponse getStatus() {
        return new AgentStatusResponse();
    }

    @Path("/fullstatus")
    @GET
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    public AgentStatusResponse getFullStatus() {
        AgentStatusResponse response = new AgentStatusResponse();
        response.setVersion(new VersionUtil.Version());
        return response;
    }

    @Path("/uploadMgrFile/{fileName}")
    @PUT
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    public Response uploadMgrFile(final @PathParam("fileName") String mgrFileName, final InputStream uploadedInputStream) {
        return Response.ok().build();
    }
}