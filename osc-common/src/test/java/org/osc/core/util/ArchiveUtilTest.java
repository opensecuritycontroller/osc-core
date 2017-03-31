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

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ArchiveUtilTest {


    private static final String PATH = System.getProperty("user.dir");
    private final static String FILE_ABSOLUTE = PATH + File.separator+"test.zip";

    private  Map<String, String> archiveMap = new HashMap<String, String>(){{
       put("mytext.txt","f");
       put("dir"+File.separator,"d");
       put("dir"+File.separator+"mytext.txt","f");
    }};

    private void prepareValidZipFile() throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("Test String");

        File f = new File(FILE_ABSOLUTE);
        getZipContent(sb, f);
    }

    private void getZipContent(StringBuilder sb, File f) throws IOException {
        try(FileOutputStream fos = new FileOutputStream(f);
            ZipOutputStream out = new ZipOutputStream(fos)) {
            archiveMap.entrySet().stream()
                    .sorted(Map.Entry.<String, String>comparingByValue())
                    .forEachOrdered(k ->addEntryToZip(sb, out, k));
        }
    }

    private void prepareInvalidZipFile() throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("Test String");

        File f = new File(FILE_ABSOLUTE);
        getZipContent(sb, f);
    }

    private void addEntryToZip(StringBuilder sb, ZipOutputStream out, Map.Entry<String, String> k) {
        try {
            if ("f".equals(k.getValue())) {
                ZipEntry e = new ZipEntry(k.getKey());
                out.putNextEntry(e);
                byte[] data = sb.toString().getBytes();
                out.write(data, 0, data.length);
            } else if ("d".equals(k.getValue())) {
                ZipEntry e = new ZipEntry(k.getKey());
                out.putNextEntry(e);
            }
        } catch (IOException e1) {
            e1.printStackTrace();
        }
    }

    @After
    public void clearFiles() throws IOException {
        Files.deleteIfExists(Paths.get(FILE_ABSOLUTE));
        final List<String> dirsToRetry = new ArrayList<>();
        archiveMap.entrySet()
                .stream()
                .sorted(Map.Entry.<String, String>comparingByKey())
                .forEach(k -> removeFilesAndEmptyDirs(dirsToRetry, k));
        dirsToRetry
                .stream()
                .forEach(d -> removeDirs(d));
    }

    private void removeDirs(String d) {
        try {
            Files.deleteIfExists(Paths.get(PATH + File.separator + d));
        } catch (IOException e) {
            if(!(e instanceof DirectoryNotEmptyException)) {
                e.printStackTrace();
            }
        }
    }

    private void removeFilesAndEmptyDirs(List<String> dirsToRetry, Map.Entry<String, String> k) {
        try {
             Files.deleteIfExists(Paths.get(PATH + File.separator + k.getKey()));
        } catch (IOException e) {
            if(e instanceof DirectoryNotEmptyException) {
                dirsToRetry.add(k.getKey());
            } else {
                e.printStackTrace();
            }
        }
    }

    @Test
    public void testUnzip_withValidZip_expectedSuccess() throws IOException {
        //Arrange
        prepareValidZipFile();
        //Act.
        ArchiveUtil.unzip(FILE_ABSOLUTE,PATH);
        //Assert.
        archiveMap.entrySet()
                .stream()
                .forEach(k ->
                        Assert.assertTrue("File should exist: " + PATH + File.separator + k.getKey(), Files.exists(Paths.get(PATH + File.separator + k.getKey())))
                );

    }

}
