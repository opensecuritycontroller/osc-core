package org.osc.core.broker.service.request;

import org.osc.core.broker.rest.client.nsx.model.ContainerSet;


public class NsxUpdateProfileContainerRequest implements Request {
    public String serviceProfileId;
    public String nsxIpAddress;
    public ContainerSet containerSet;
}
