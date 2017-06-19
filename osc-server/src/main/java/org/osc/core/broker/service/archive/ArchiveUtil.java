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
package org.osc.core.broker.service.archive;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.apache.log4j.Logger;
import org.osc.core.broker.service.api.server.ArchiveApi;
import org.osc.core.broker.service.api.server.LoggingApi;
import org.osc.core.broker.util.FileUtil;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component
public class ArchiveUtil implements ArchiveApi {

    @Reference
    private LoggingApi logging;

    private static final Logger log = Logger.getLogger(ArchiveUtil.class);
    static final int BUFFER_SIZE = 1024;
    static final long FILE_SIZE = 4*1024*1024*1024L;//size 4GB
    static final int FILE_LIMIT = 2048;
    /**
     * @param inputDir   Input Directory name
     * @param outputFile Desired output file name
     * @return returns created zip file
     * @throws IOException
     */
    @Override
    public File archive(String inputDir, String outputFile) throws IOException {
        try (FileOutputStream out = new FileOutputStream(outputFile);
                ZipOutputStream zos = new ZipOutputStream(out)) {
            addToArchive(new File(inputDir), zos);
            return new File(outputFile);
        }
    }

    /**
     * @param inputDir Input Directory
     * @param zos      Zip Output Stream
     * @throws IOException
     */
    private static void addToArchive(File inputDir, ZipOutputStream zos) throws IOException {
        File[] fileList = FileUtil.getFileListFromDirectory(inputDir.getPath());
        byte[] buffer = new byte[BUFFER_SIZE];

        for (File element : fileList) {

            if (element.isDirectory()) {
                zos.putNextEntry(new ZipEntry(element.getName()));
                addToArchive(element, zos);
                continue;
            }

            try (FileInputStream fis = new FileInputStream(element.getPath())) {
                log.info("Adding: " + element.getPath() + " to log bundle");
                zos.putNextEntry(new ZipEntry(element.getPath()));
                int len;
                while ((len = fis.read(buffer)) > 0) {
                    zos.write(buffer, 0, len);
                }
                zos.closeEntry();
            }
        }
    }

    /**
     * @param inputFile   ZIP file
     * @param destination directory to extract
     * @throws IOException
     */
    @Override
    public void unzip(String inputFile, String destination) throws IOException {
        // TODO: barteks - use system unzip instead of java zip stream.
        FileInputStream fis = new FileInputStream(inputFile);
        ZipEntry entry;
        int entries = 0;
        long total = BUFFER_SIZE;
        log.info("Extracting " + this.logging.removeCRLF(inputFile) + " into " + destination);

        File zipParentDir = new File(inputFile).getParentFile();

        try(ZipInputStream zis = new ZipInputStream(new BufferedInputStream(fis));) {
            while ((entry = zis.getNextEntry()) != null) {
                int count;
                byte[] data = new byte[BUFFER_SIZE];
                // Write the files to the disk, but ensure that the filename is valid,
                // and that the file is not insanely big

                String filename = entry.getName();
                if (zipParentDir.isDirectory()) {
                    filename = Paths.get(zipParentDir.toString(), entry.getName()).toString();
                }

                String name = preventPathTraversal(filename, destination);
                if (entry.isDirectory()) {
                    new File(name).mkdir();
                    continue;
                }
                log.info("Extracting " + this.logging.removeCRLF(name));
                FileOutputStream fos = new FileOutputStream(name);
                BufferedOutputStream dest = new BufferedOutputStream(fos, BUFFER_SIZE);
                count = zis.read(data, 0, BUFFER_SIZE);
                while (total <= FILE_SIZE && count != -1) {
                    dest.write(data, 0, count);
                    total += count;
                    count = zis.read(data, 0, BUFFER_SIZE);
                }
                dest.flush();
                dest.close();
                zis.closeEntry();
                entries++;
                if (entries > FILE_LIMIT) {
                    throw new IllegalStateException("Archive has too many files.");
                }
                if (total > FILE_SIZE) {
                    throw new IllegalStateException("Archive is too big.");
                }
            }
        }
    }

    /**
     * Returns the files included within the zip file
     *
     * @param filename the zip file
     * @param intendedDir the zip potential location
     * @return name of the file
     * @throws IllegalStateException if name is incorrect
     */
    private static String preventPathTraversal(String filename, String intendedDir)
            throws java.io.IOException {
        File f = new File(filename);
        File iD = new File(intendedDir);
        String canPath = f.getCanonicalPath();
        String canID = iD.getCanonicalPath();

        if (canPath.startsWith(canID)) {
            return canPath;
        } else {
            throw new IllegalStateException("File is not inside extract directory.");
        }
    }

}
