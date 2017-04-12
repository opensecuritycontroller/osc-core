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
package org.osc.core.broker.service.dto.job;

public enum ObjectType {
    VIRTUALIZATION_CONNECTOR("Virtualization Connector"),
    APPLIANCE_MANAGER_CONNECTOR("Manager Connector"),
    DISTRIBUTED_APPLIANCE("Distributed Appliance"),
    VIRTUAL_SYSTEM("Virtual System"),
    DEPLOYMENT_SPEC("Deployment Specification"),
    DISTRIBUTED_APPLIANCE_INSTANCE("Distributed Appliance Instance"),
    SECURITY_GROUP("Security Group"),
    SECURITY_GROUP_INTERFACE("Security Group Interface"),
    SSL_CONFIGURATION("SSL Configuration"),
    JOB("Job"),
    EMAIL(""),
    NETWORK(""),
    ARCHIVE(""),
    ALERT("Alert");

    private String name;

    private ObjectType(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return this.name;
    }
}