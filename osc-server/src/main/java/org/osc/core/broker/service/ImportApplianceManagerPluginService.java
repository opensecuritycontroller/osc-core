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
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import javax.persistence.EntityManager;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.log4j.Logger;
import org.osc.core.broker.model.plugin.manager.ManagerApiFactory;
import org.osc.core.broker.service.api.ImportApplianceManagerPluginServiceApi;
import org.osc.core.broker.service.exceptions.VmidcBrokerValidationException;
import org.osc.core.broker.service.exceptions.VmidcException;
import org.osc.core.broker.service.request.ImportFileRequest;
import org.osc.core.broker.service.response.BaseResponse;
import org.osc.core.broker.view.common.VmidcMessages;
import org.osc.core.util.FileUtil;
import org.osc.core.util.ServerUtil;
import org.osgi.service.component.annotations.Component;

@Component
public class ImportApplianceManagerPluginService extends ServiceDispatcher<ImportFileRequest, BaseResponse>
        implements ImportApplianceManagerPluginServiceApi {

    private static final Logger log = Logger.getLogger(ImportApplianceManagerPluginService.class);

    private File barFile = null;
    private String deploymentName;

    @Override
    public BaseResponse exec(ImportFileRequest request, EntityManager em) throws Exception {
        BaseResponse response = new BaseResponse();

        File tmpUploadFolder = new File(request.getUploadPath());

		validate(em, request, tmpUploadFolder);

		File pluginTarget = new File(ManagerApiFactory.MANAGER_PLUGINS_DIRECTORY, this.deploymentName);

		FileUtils.copyFile(this.barFile, pluginTarget);

		cleanTmpFolder(tmpUploadFolder);

        return response;
    }

    private void cleanTmpFolder(File tmpUploadFolder) {
        log.info("Cleaning temp folder: " + tmpUploadFolder.getPath());
        try {
            FileUtils.deleteDirectory(tmpUploadFolder);
        } catch (Exception e) {
            log.error("Failed to cleaning temp folder: " + tmpUploadFolder.getPath(), e);
            // Not throwing exception since AddApplianceSoftwareVersionService succeeded
        }
    }

    private void validate(EntityManager em, ImportFileRequest request, File tmpUploadFolder) throws Exception {

        if (!ServerUtil.isEnoughSpace()) {
            throw new VmidcException(VmidcMessages.getString("upload.plugin.manager.nospace"));
        }

		File[] tmpFolderList = FileUtil.getFileListFromDirectory(tmpUploadFolder.getPath());
        for (File tmpFolderFile : tmpFolderList) {
            String fileName = FilenameUtils.getName(tmpFolderFile.getName());
            if (FilenameUtils.getExtension(fileName).equals("bar")) {
                this.barFile = tmpFolderFile;
                break;
            }
        }

        if (this.barFile == null) {
            throw new VmidcBrokerValidationException(
                    "Invalid upload folder. Should contain single .bar file.");
        }

        try (JarFile jar = new JarFile(this.barFile)) {
			Manifest manifest = jar.getManifest();
			Attributes attributes = manifest.getMainAttributes();
			String symbolicName = attributes.getValue("Deployment-SymbolicName");

			if (symbolicName == null) {
				throw new VmidcBrokerValidationException("uploaded file does not contain Deployment-SymbolicName: " + this.barFile);
			}

			this.deploymentName = symbolicName + ".bar";
        }

    }

}
