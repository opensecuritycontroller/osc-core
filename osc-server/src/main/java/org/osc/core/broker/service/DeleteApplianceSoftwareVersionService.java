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
package org.osc.core.broker.service;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.osc.core.broker.model.entities.appliance.ApplianceSoftwareVersion;
import org.osc.core.broker.service.exceptions.VmidcBrokerInvalidRequestException;
import org.osc.core.broker.service.exceptions.VmidcBrokerValidationException;
import org.osc.core.broker.service.persistence.DistributedApplianceEntityMgr;
import org.osc.core.broker.service.persistence.EntityManager;
import org.osc.core.broker.service.request.BaseIdRequest;
import org.osc.core.broker.service.response.EmptySuccessResponse;
import org.osc.core.broker.view.maintenance.ApplianceUploader;

public class DeleteApplianceSoftwareVersionService extends ServiceDispatcher<BaseIdRequest, EmptySuccessResponse> {

    private static final Logger log = Logger.getLogger(DeleteApplianceSoftwareVersionService.class);

    @Override
    public EmptySuccessResponse exec(BaseIdRequest request, Session session) throws Exception {

        ApplianceSoftwareVersion av = (ApplianceSoftwareVersion) session.get(ApplianceSoftwareVersion.class,
                request.getId());
        validate(session, request, av);

        try {
            File imageFolder = new File(ApplianceUploader.getImageFolderPath());
            FilenameFilter prefixFileFilter = FileFilterUtils.prefixFileFilter(FilenameUtils.getBaseName(av
                    .getImageUrl()));

            File[] files = imageFolder.listFiles(prefixFileFilter);
            if (files != null) {
                for (File fileToDelete : files) {
                    log.info("Deleting file: " + fileToDelete.getName());
                    FileUtils.forceDelete(fileToDelete);
                }
            } else {
                log.error("No files matching the file name: " + av.getImageUrl() + " found in " + imageFolder.getPath());
            }
        } catch (FileNotFoundException fnf) {
            log.error("File: " + av.getImageUrl() + " Not Found. Continuing with deleting the Record");
        }

        EntityManager.delete(session, av);

        EmptySuccessResponse response = new EmptySuccessResponse();
        return response;
    }

    void validate(Session session, BaseIdRequest request, ApplianceSoftwareVersion av) throws Exception {

        // entry must pre-exist in db
        if (av == null) { // note: we cannot use name here in error msg since
                          // del req does not have name, only ID

            throw new VmidcBrokerValidationException("Appliance Software Version entry with Id " + request.getId()
                    + " is not found.");
        }

        if (request.getParentId() != null) {

            // If we are here the call is from the Rest API and not through the UI - validating the url consistency
            if (!av.getAppliance().getId().equals(request.getParentId())) {

                throw new VmidcBrokerValidationException("Parent ID of the Appliance Software Version is "
                        + av.getAppliance().getId() + " but was expected to be " + request.getParentId());
            }
        }

        if (DistributedApplianceEntityMgr.isReferencedByDistributedAppliance(session, av)) {

            throw new VmidcBrokerInvalidRequestException(
                    "Cannot delete an Appliance Software Version that is referenced by a Distributed Appliance.");
        }
    }

}
