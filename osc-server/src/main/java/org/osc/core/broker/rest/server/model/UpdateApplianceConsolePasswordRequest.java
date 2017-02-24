package org.osc.core.broker.rest.server.model;

import java.util.HashSet;
import java.util.Set;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class UpdateApplianceConsolePasswordRequest {

    public String newPassword;
    public Set<String> applianceInstance = new HashSet<String>();
}
