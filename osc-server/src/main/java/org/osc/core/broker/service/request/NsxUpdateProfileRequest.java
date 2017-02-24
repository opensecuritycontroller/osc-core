package org.osc.core.broker.service.request;

import org.osc.core.broker.rest.client.nsx.model.ServiceProfile;

public class NsxUpdateProfileRequest implements Request {
    public ServiceProfile serviceProfile;
}
