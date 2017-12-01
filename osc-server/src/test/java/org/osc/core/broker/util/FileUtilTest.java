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
package org.osc.core.broker.util;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

public class FileUtilTest {

    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();

    @Rule
    public ExpectedException exception = ExpectedException.none();

    private File homeDirectory;
    private File regularFile;
    private File zipFile;
    private final int numberOfFilesInDirectory = 4;
    private final static String sampleConfigFile = "server.port=8666\nh2db.connection.url.extraArgs=AUTO_SERVER=TRUE;\ndevMode=true\nserver.reboots=0";
    private final static String CONFIG_FILE = "vmidcServerMocked.conf";
    private final static String ZIP_FILE_NAME = "testzip.zip";

    private final static File AA_DIR = new File("aa");
    private final static File CC_DIR = new File("cc");
    private final static File CC_BB_DIR = Paths.get("cc", "bb").toFile();
    private final static File A_TXT = new File("a.txt");
    private final static File B_TXT = new File("b.txt");
    private final static File F_TXT = new File("f.txt");
    private final static File AA_A_TXT = Paths.get("aa", "a.txt").toFile();
    private final static File CC_A_TXT = Paths.get("cc", "a.txt").toFile();
    private final static File CC_BB_A_TXT = Paths.get("cc", "bb", "a.txt").toFile();
    private final static File CC_BB_D_TXT = Paths.get("cc", "bb", "f.txt").toFile();
    private final static File[] ZIP_CONTENTS = {AA_DIR, CC_DIR, CC_BB_DIR, A_TXT, B_TXT, F_TXT, AA_A_TXT, CC_A_TXT,
                                                CC_BB_A_TXT, CC_BB_D_TXT};

    @Before
    public void init() throws IOException {
        this.homeDirectory = this.testFolder.newFolder("fakeHomeDir");

        if (!this.homeDirectory.exists()) {
            Assert.fail("Failed to create new folder for tests");
        }

        this.regularFile = new File(this.homeDirectory, this.CONFIG_FILE);
        if (!this.regularFile.createNewFile() || !this.regularFile.exists()) {
            Assert.fail("Failed to create new file for test");
        }
    }

    @After
    public void teardown(){
        cleanRegularFile();
    }

    private void cleanRegularFile() {
        this.regularFile = new File(this.homeDirectory, this.CONFIG_FILE);
        if(this.regularFile.exists()){
            boolean isDeleted = this.regularFile.delete();
            Assert.assertTrue(isDeleted);
        }
    }

    private void cleanZipFile() {
        if (this.zipFile.exists()) {
            Assert.assertTrue(this.zipFile.delete());
        }
    }

    @Test
    public void testGetFileListFromDirectory_WithNullPath_ThrowsFileNotFound() throws FileNotFoundException {
        // Arrange.
        this.exception.expect(FileNotFoundException.class);
        this.exception.expectMessage("Cannot obtain list of files from directory - null given");

        // Act.
        FileUtil.getFileListFromDirectory(null);
    }

    @Test
    public void testGetFileListFromDirectory_WithAvailablePath_ReturnsList() throws IOException {
        // Arrange.
        cleanRegularFile();
        populateTemporaryFolder();

        // Act.
        File[] fileList = FileUtil.getFileListFromDirectory(this.homeDirectory.getAbsolutePath());

        // Assert.
        Assert.assertEquals("File list should contain all files", this.numberOfFilesInDirectory, fileList.length);
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
        Files.write(this.regularFile.toPath(), this.sampleConfigFile.getBytes(), StandardOpenOption.APPEND);

        // Act.
        Properties prop = FileUtil.loadProperties(this.regularFile.getAbsolutePath());

        // Assert.
        Assert.assertEquals("Different size of loaded properties file", 4, prop.size());
        Assert.assertEquals("Improper value obtained from properties", "8666", prop.getProperty("server.port"));
        Assert.assertEquals("Improper value obtained from properties", "true", prop.getProperty("devMode"));
        Assert.assertEquals("Improper value obtained from properties", "0", prop.getProperty("server.reboots"));
        Assert.assertEquals("Improper value obtained from properties", "AUTO_SERVER=TRUE;", prop.getProperty("h2db.connection.url.extraArgs"));
    }

    @Test
    public void testLoadProperties_WithUnavailableConfigFile_ThrowsIOException() throws IOException {
        // Arrange.
        File regularFile = new File(this.homeDirectory, this.CONFIG_FILE);
        if(this.regularFile.exists()){
            boolean isDeleted = this.regularFile.delete();
            Assert.assertTrue(isDeleted);
        }

        this.exception.expect(FileNotFoundException.class);

        // Act.
        Properties prop = FileUtil.loadProperties(regularFile.getAbsolutePath());

        // Assert.
        Assert.assertEquals("Different size of loaded properties file", 0, prop.size());
    }

    @Test
    public void testUnzip_WithZipFile_ExtractsAll() throws IOException {

        // Arrange.
        this.zipFile = new File(this.ZIP_FILE_NAME);
        Arrays.stream(ZIP_CONTENTS).forEach(FileUtils::deleteQuietly);

        InputStream tmpInputStream = getClass().getClassLoader().getResourceAsStream(this.ZIP_FILE_NAME);
        FileUtils.copyToFile(tmpInputStream, this.zipFile);

        // Act.
        List<File> files = FileUtil.unzip(this.zipFile);

        // Assert.
        assertTrue("Some files were not unzipped!", Arrays.stream(ZIP_CONTENTS).allMatch(f -> f.exists()));

        Arrays.stream(ZIP_CONTENTS).forEach(FileUtils::deleteQuietly);
        cleanZipFile();
    }

    private void populateTemporaryFolder() throws IOException {
        for (int i = 0; i < this.numberOfFilesInDirectory; i++) {
            File regularFile = new File(this.homeDirectory, "test_" + i + ".txt");
            if (!regularFile.createNewFile() || !regularFile.exists()) {
                Assert.fail("Failed to create new file for test");
            } else {
                Files.write(regularFile.toPath(), "test".getBytes(), StandardOpenOption.APPEND);
            }
        }
    }

}
