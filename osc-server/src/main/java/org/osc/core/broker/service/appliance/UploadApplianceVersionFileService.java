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
package org.osc.core.broker.service.appliance;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.persistence.EntityManager;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.osc.core.broker.service.ServiceDispatcher;
import org.osc.core.broker.service.api.ImportApplianceSoftwareVersionServiceApi;
import org.osc.core.broker.service.api.UploadApplianceVersionFileServiceApi;
import org.osc.core.broker.service.api.server.ArchiveApi;
import org.osc.core.broker.service.exceptions.VmidcBrokerValidationException;
import org.osc.core.broker.service.request.ImportFileRequest;
import org.osc.core.broker.service.request.UploadRequest;
import org.osc.core.broker.service.response.BaseResponse;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
@Component(configurationPid="org.osc.core.broker.upload",
    configurationPolicy=ConfigurationPolicy.REQUIRE)
public class UploadApplianceVersionFileService extends ServiceDispatcher<UploadRequest, BaseResponse>
        implements UploadApplianceVersionFileServiceApi {

    private static final Logger log = Logger.getLogger(UploadApplianceVersionFileService.class);

    @Reference
    private ImportApplianceSoftwareVersionServiceApi importApplianceSoftwareVersionService;

    @Reference
    private ArchiveApi archiver;

    String tmpFolderParent;

    @Activate
    void start(UploadConfig config) {
        this.tmpFolderParent = config.tmp_upload_parent();
    }

    @Override
    public BaseResponse exec(UploadRequest request, EntityManager em) throws Exception {
        String fileName = request.getFileName();
        Path uploadFolder = Files.createTempDirectory(Paths.get(this.tmpFolderParent), "uploadAppliance");

        try {
            validate(uploadFolder, fileName);

            // save the zip file
            String uploadedFilePath = uploadFolder + fileName;
            writeToFile(request.getUploadedInputStream(), uploadedFilePath);
            log.info("Uploaded file " + uploadedFilePath);

			boolean isZip = FilenameUtils.getExtension(fileName).equals("zip");
			if (isZip) {
				// If we are here we are handling a zip file
			    this.archiver.unzip(uploadedFilePath, uploadFolder.toString());
				// After extraction, we don't need the zip file. Delete the zip file
				log.info("Delete temporary uploaded zip file after extraction " + uploadedFilePath);
				new File(uploadedFilePath).delete();
			}

            ImportFileRequest importRequest = new ImportFileRequest(uploadFolder.toString());

            return this.importApplianceSoftwareVersionService.dispatch(importRequest);
        } finally {
            try {
                FileUtils.deleteDirectory(uploadFolder.toFile());
            } catch (Exception e) {
                log.error("Failed to cleaning temp folder: " + uploadFolder, e);
                // Not throwing exception since only cleaning failed
            }
        }
    }

    void validate(Path extractdir, String fileName) throws VmidcBrokerValidationException {
        if (fileName == null || fileName.isEmpty()) {
            throw new VmidcBrokerValidationException("Invalid request, no file name");
        }

        String extension = FilenameUtils.getExtension(fileName);

        if (!(extension.equals("zip") || extension.equals("bar"))) {
        	// in the future we can support files that are not zip file
            throw new VmidcBrokerValidationException("Invalid request, not a valid file - valid file types are '.bar' and '.zip'");
        }

        Path uploadedFilePath = extractdir.resolve(fileName);
        if (uploadedFilePath.toFile().exists()) {

            throw new VmidcBrokerValidationException("File " + uploadedFilePath + " already exist");
        } else {

            log.info("Valid file will upload it to " + uploadedFilePath + " ...");
        }
    }

    // save uploaded file to new location
    private void writeToFile(InputStream uploadedInputStream, String uploadedFileLocation) throws IOException {

        OutputStream out = null;

        try {
            File file = new File(uploadedFileLocation);
            file.getParentFile().mkdirs();
            file.createNewFile();

            out = new FileOutputStream(file);
            int read = 0;
            byte[] bytes = new byte[32 * 1024];
            int blockCounter = 0;
            int loggingFactor = 0;

            while ((read = uploadedInputStream.read(bytes)) != -1) {

                out.write(bytes, 0, read);

                if (++blockCounter > loggingFactor * 5000) {
                    log.info("== " + uploadedFileLocation + " == Uploading block " + blockCounter);
                    ++loggingFactor;
                }
            }

            out.flush();
        } catch (Exception e) {
            log.error("Error writing Input stream to File: " + uploadedFileLocation);
            throw e;
        } finally {
            IOUtils.closeQuietly(out);

        }
    }
}
