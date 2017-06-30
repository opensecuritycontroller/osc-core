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
package org.osc.core.broker.rest.client.openstack.openstack4j;

import org.openstack4j.model.identity.v3.Project;

import java.util.Comparator;
import java.util.List;

public class Openstack4jKeystone extends BaseOpenstack4jApi {

    public Openstack4jKeystone(Endpoint endPoint) {
        super(endPoint);
    }

    public List<? extends Project> listProjects() {
        List<? extends Project> tenantsList = this.getOs().identity().projects().list();
        tenantsList.sort(Comparator.comparing(Project::getName));
        return tenantsList;
    }

    public Project getProjectById(String projectId) {
        return this.getOs().identity().projects().get(projectId);
    }
}
