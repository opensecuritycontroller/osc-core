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

import java.util.Comparator;
import java.util.List;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.log4j.Logger;
import org.openstack4j.model.identity.v3.Project;

public class Openstack4jKeystone extends BaseOpenstack4jApi {

    private static final Logger log = Logger.getLogger(Openstack4JNova.class);

    public Openstack4jKeystone(Endpoint endPoint) {
        super(endPoint);
    }

    public List<? extends Project> listProjects() {
        List<? extends Project> projectsList = getOs().identity().projects().list();
        if (CollectionUtils.isEmpty(projectsList)) {
            log.warn("No projects found!");
        }
        projectsList.sort(Comparator.comparing(Project::getName));
        return projectsList;
    }

    public Project getProjectById(String projectId) {
        return getOs().identity().projects().get(projectId);
    }
}
