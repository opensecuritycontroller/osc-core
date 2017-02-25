/*******************************************************************************
 * Copyright (c) 2017 Intel Corporation
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

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class ServiceInstanceRuntimeInfo {
    protected Long id;
    protected Long revision;
    protected String status;
    protected String installState;
    protected DeploymentScope deploymentScope;

    public Long getId() {
        return id;
    }

    public void setId(Long value) {
        this.id = value;
    }

    public Long getRevision() {
        return revision;
    }

    public void setRevision(Long value) {
        this.revision = value;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String value) {
        this.status = value;
    }

    public String getInstallState() {
        return installState;
    }

    public void setInstallState(String value) {
        this.installState = value;
    }

    public DeploymentScope getDeloymentScope() {
        return deploymentScope;
    }

    public void setDeloymentScope(DeploymentScope value) {
        this.deploymentScope = value;
    }

}
