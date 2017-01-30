package org.osc.core.broker.rest.client.nsx.model;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class ServicePolicy {

    @XmlRootElement
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class ServiceRule {
        public String id;
        public String name;
    }

    @XmlRootElement
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class Rules {
        public List<ServiceRule> serviceRule = new ArrayList<ServiceRule>();
    }

    @XmlRootElement
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class ServiceRuleSet {
        public String id;
        public String name;
        public String description;
        public String enabled;
        public Rules rules = new Rules();
    }

    @XmlRootElement
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class RuleSets {
        public List<ServiceRuleSet> serviceRuleSet = new ArrayList<ServiceRuleSet>();
    }

    public String id;
    public String name;
    public String description;
    public RuleSets ruleSets = new RuleSets();
}
