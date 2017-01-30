package org.osc.core.broker.rest.server;

import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.core.Application;

import org.osc.core.broker.rest.server.api.AgentApis;
import org.osc.core.broker.rest.server.api.AlarmApis;
import org.osc.core.broker.rest.server.api.AlertApis;
import org.osc.core.broker.rest.server.api.ApplianceApis;
import org.osc.core.broker.rest.server.api.DistributedApplianceApis;
import org.osc.core.broker.rest.server.api.DistributedApplianceInstanceApis;
import org.osc.core.broker.rest.server.api.JobApis;
import org.osc.core.broker.rest.server.api.ManagerApis;
import org.osc.core.broker.rest.server.api.ManagerConnectorApis;
import org.osc.core.broker.rest.server.api.ServerDebugApis;
import org.osc.core.broker.rest.server.api.ServerMgmtApis;
import org.osc.core.broker.rest.server.api.VirtualSystemApis;
import org.osc.core.broker.rest.server.api.VirtualizationConnectorApis;
import org.osc.core.broker.rest.server.api.proprietary.NsmMgrApis;
import org.osc.core.broker.rest.server.api.proprietary.NsxApis;

/**
 * The Main rest servlet class which dispatches requests to the API classes based on their path parameters
 */
public class IscRestServlet extends Application {

    // Because we started with versions in the URL, we will continue to have v1 in the URL
    public static final String SERVER_API_PATH_PREFIX = "/api/server/v1";
    public static final String AGENT_API_PATH_PREFIX = "/api/agent/v1";
    public static final String MANAGER_API_PATH_PREFIX = "/api/manager/v1";

    // Legacy/Proprietary API
    public static final String NSX_API_PATH_PREFIX = "/api/nsx/vmware/2.0";
    public static final String MGR_NSM_API_PATH_PREFIX = "/api/nsm/v1";

    @Override
    public Set<Class<?>> getClasses() {
        Set<Class<?>> classes = new HashSet<>();
        classes.add(AgentApis.class);
        classes.add(NsxApis.class);
        classes.add(NsmMgrApis.class);
        classes.add(ManagerApis.class);

        // Server APIs
        classes.add(ServerMgmtApis.class);
        classes.add(JobApis.class);
        classes.add(VirtualizationConnectorApis.class);
        classes.add(ManagerConnectorApis.class);
        classes.add(ApplianceApis.class);
        classes.add(DistributedApplianceApis.class);
        classes.add(DistributedApplianceInstanceApis.class);
        classes.add(VirtualSystemApis.class);
        classes.add(AlarmApis.class);
        classes.add(AlertApis.class);
        classes.add(ServerDebugApis.class);

        return classes;
    }
}
