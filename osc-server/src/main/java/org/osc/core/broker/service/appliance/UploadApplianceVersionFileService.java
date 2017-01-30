package org.osc.core.broker.service.appliance;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.osc.core.broker.service.ServiceDispatcher;
import org.osc.core.broker.service.exceptions.VmidcBrokerValidationException;
import org.osc.core.broker.service.response.BaseResponse;
import org.osc.core.broker.view.maintenance.ApplianceUploader;
import org.osc.core.util.ArchiveUtil;

public class UploadApplianceVersionFileService extends ServiceDispatcher<UploadRequest, BaseResponse> {

    private static final Logger log = Logger.getLogger(UploadApplianceVersionFileService.class);

    @Override
    public BaseResponse exec(UploadRequest request, Session session) throws Exception {
        String fileName = request.getFileName();
        String uploadFolder = ApplianceUploader.getUploadPath(true);

        try {
            validate(uploadFolder, fileName);

            // save the zip file
            String uploadedFilePath = uploadFolder + fileName;
            writeToFile(request.getUploadedInputStream(), uploadedFilePath);
            log.info("Uploaded file " + uploadedFilePath);

			boolean isZip = FilenameUtils.getExtension(fileName).equals("zip");
			if (isZip) {
				// If we are here we are handling a zip file
				ArchiveUtil.unzip(uploadedFilePath, uploadFolder);
				// After extraction, we don't need the zip file. Delete the zip file
				log.info("Delete temporary uploaded zip file after extraction " + uploadedFilePath);
				new File(uploadedFilePath).delete();
			}

            ImportApplianceSoftwareVersionService importService = new ImportApplianceSoftwareVersionService();

            ImportFileRequest importRequest = new ImportFileRequest(uploadFolder);

            return importService.dispatch(importRequest);
        } finally {
            try {
                FileUtils.deleteDirectory(new File(uploadFolder));
            } catch (Exception e) {
                log.error("Failed to cleaning temp folder: " + uploadFolder, e);
                // Not throwing exception since only cleaning failed
            }
        }
    }

    void validate(String extractdir, String fileName) throws VmidcBrokerValidationException {
        if (fileName == null || fileName.isEmpty()) {
            throw new VmidcBrokerValidationException("Invalid request, no file name");
        }

        String extension = FilenameUtils.getExtension(fileName);

        if (!(extension.equals("zip") || extension.equals("bar"))) {
        	// in the future we can support files that are not zip file as long as parallel to NSX will be able to work with them
            throw new VmidcBrokerValidationException("Invalid request, not a valid file - valid file types are '.bar' and '.zip'");
        }

        String uploadedFilePath = extractdir + fileName;
        if (new File(uploadedFilePath).exists()) {

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
