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
package org.osc.core.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.apache.log4j.Logger;
import org.osc.core.rest.client.util.LoggingUtil;

public class ArchiveUtil {

    private static final Logger log = Logger.getLogger(ArchiveUtil.class);

    /**
     * @param inputDir   Input Directory name
     * @param outputFile Desired output file name
     * @return returns created zip file
     * @throws IOException
     */
    public static File archive(String inputDir, String outputFile) throws IOException {
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
        byte[] buffer = new byte[1024];

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
    public static void unzip(String inputFile, String destination) throws IOException {
        log.info("Extracting " + inputFile + " into " + destination);
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(inputFile))) {
            ZipEntry zipEntry = zis.getNextEntry();
            while (zipEntry != null) {
                String fileName = zipEntry.getName();
                log.info("Extracting " + LoggingUtil.removeCRLF(fileName));
                File file = new File(destination + File.separator + fileName);
                // create folder as needed
                file.getParentFile().mkdirs();
                try (FileOutputStream fos = new FileOutputStream(file)) {
                    int length;
                    byte[] buffer = new byte[1024];
                    while ((length = zis.read(buffer)) > 0) {
                        fos.write(buffer, 0, length);
                    }
                }
                zipEntry = zis.getNextEntry();
            }
            zis.closeEntry();
            log.info("Extraction successful! " + inputFile);
        }
    }

    /**
     * Returns the files included within the zip file
     *
     * @param inputFile the zip file
     * @return the list of files within the zip file
     * @throws IOException
     */
    public static List<String> peekFileNames(String inputFile) throws IOException {
        List<String> files = new ArrayList<>();
        try (FileInputStream inputStream = new FileInputStream(inputFile);
             ZipInputStream zis = new ZipInputStream(inputStream)) {
            ZipEntry zipEntry = zis.getNextEntry();
            while (zipEntry != null) {
                files.add(zipEntry.getName());
                zipEntry = zis.getNextEntry();
            }
            zis.closeEntry();
        }
        return files;
    }

}
