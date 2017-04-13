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
