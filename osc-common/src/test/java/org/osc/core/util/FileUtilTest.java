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
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.osc.core.util.encryption.SecurityException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.Properties;

public class FileUtilTest {

    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();

    @Rule
    public ExpectedException exception = ExpectedException.none();

    private File homeDirectory;
    private File regularFile;
    private final int numberOfFilesInDirectory = 5;
    private final String sampleConfigFile = "server.port=8666\nnsxExtraAutomation=true\nh2db.connection.url.extraArgs=AUTO_SERVER=TRUE;\ndevMode=true\nserver.reboots=0";
    private final String CONFIG_FILE = "vmidcServerMocked.conf";

    @Before
    public void init() throws IOException {
        homeDirectory = testFolder.newFolder("fakeHomeDir");

        if (!homeDirectory.exists()) {
            Assert.fail("Failed to create new folder for tests");
        }

        this.regularFile = new File(homeDirectory, CONFIG_FILE);
        if (!this.regularFile.createNewFile() || !this.regularFile.exists()) {
            Assert.fail("Failed to create new file for test");
        }
    }

    @After
    public void teardown(){
        cleanRegularFile();
    }

    private void cleanRegularFile() {
        this.regularFile = new File(homeDirectory, CONFIG_FILE);
        if(this.regularFile.exists()){
            boolean isDeleted = this.regularFile.delete();
            Assert.assertTrue(isDeleted);
        }
    }

    @Test
    public void testGetFileListFromDirectory_WithNullPath_ThrowsFileNotFound() throws FileNotFoundException {
        // Arrange.
        exception.expect(FileNotFoundException.class);
        exception.expectMessage("Cannot obtain list of files from directory - null given");

        // Act.
        FileUtil.getFileListFromDirectory(null);
    }

    @Test
    public void testGetFileListFromDirectory_WithAvailablePath_ReturnsList() throws IOException {
        // Arrange.
        cleanRegularFile();
        populateTemporaryFolder();

        // Act.
        File[] fileList = FileUtil.getFileListFromDirectory(homeDirectory.getAbsolutePath());

        // Assert.
        Assert.assertEquals("File list should contain all files", numberOfFilesInDirectory, fileList.length);
        for (File loadedFile : fileList) {
            Assert.assertTrue(loadedFile.getName().contains("test_"));
        }
    }

    @Test
    public void testGetFileListFromDirectory_WithInvalidPath_ReturnsEmptyList() throws IOException {
        // Act.
        File[] fileList = FileUtil.getFileListFromDirectory("testInvalidDir");

        // Assert.
        Assert.assertEquals("File list should contain empty list", 0, fileList.length);
    }

    @Test
    public void testLoadProperties_WithAvailableConfigFile_ReturnsLoadedConfigFile() throws IOException {
        // Arrange.
        Files.write(this.regularFile.toPath(), sampleConfigFile.getBytes(), StandardOpenOption.APPEND);

        // Act.
        Properties prop = FileUtil.loadProperties(this.regularFile.getAbsolutePath());

        // Assert.
        Assert.assertEquals("Different size of loaded properties file", 5, prop.size());
        Assert.assertEquals("Improper value obtained from properties", "true", prop.getProperty("nsxExtraAutomation"));
        Assert.assertEquals("Improper value obtained from properties", "8666", prop.getProperty("server.port"));
        Assert.assertEquals("Improper value obtained from properties", "true", prop.getProperty("devMode"));
        Assert.assertEquals("Improper value obtained from properties", "0", prop.getProperty("server.reboots"));
        Assert.assertEquals("Improper value obtained from properties", "AUTO_SERVER=TRUE;", prop.getProperty("h2db.connection.url.extraArgs"));
    }

    @Test
    public void testLoadProperties_WithUnavailableConfigFile_ThrowsIOException() throws IOException {
        // Arrange.
        File regularFile = new File(homeDirectory, CONFIG_FILE);
        if(this.regularFile.exists()){
            boolean isDeleted = this.regularFile.delete();
            Assert.assertTrue(isDeleted);
        }

        exception.expect(FileNotFoundException.class);

        // Act.
        Properties prop = FileUtil.loadProperties(regularFile.getAbsolutePath());

        // Assert.
        Assert.assertEquals("Different size of loaded properties file", 0, prop.size());
    }

    @Test
    public void testUploadFile_WithPathTraversalVulnerability1_ThrowsSecurityException() throws IOException, SecurityException {
        // Arrange.

        String dir = System.getProperty("user.dir");
        String filename = "../traversal/file.txt";

        this.exception.expect(SecurityException.class);

        // Act.
        FileUtil.preventPathTraversal(filename,dir);

    }

    @Test
    public void testUploadFile_WithPathTraversalVulnerability2_ThrowsSecurityException() throws IOException, SecurityException {
        // Arrange.

        String dir = System.getProperty("user.dir");
        String filename = "../\\file.txt";

        this.exception.expect(SecurityException.class);

        // Act.
        FileUtil.preventPathTraversal(filename,dir);

    }

    private void populateTemporaryFolder() throws IOException {
        for (int i = 0; i < numberOfFilesInDirectory; i++) {
            File regularFile = new File(homeDirectory, "test_" + i + ".txt");
            if (!regularFile.createNewFile() || !regularFile.exists()) {
                Assert.fail("Failed to create new file for test");
            } else {
                Files.write(regularFile.toPath(), "test".getBytes(), StandardOpenOption.APPEND);
            }
        }
    }

}
