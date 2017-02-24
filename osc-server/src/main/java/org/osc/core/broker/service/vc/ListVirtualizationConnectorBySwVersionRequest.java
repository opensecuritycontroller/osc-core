package org.osc.core.broker.service.vc;

import org.osc.core.broker.service.request.Request;



public class ListVirtualizationConnectorBySwVersionRequest implements Request {

    String swVersion;

    /**
     * @return the swVersion
     */
    public String getSwVersion() {
        return swVersion;
    }

    /**
     * @param swVersion
     *            the swVersion to set
     */
    public void setSwVersion(String swVersion) {
        this.swVersion = swVersion;
    }
}
