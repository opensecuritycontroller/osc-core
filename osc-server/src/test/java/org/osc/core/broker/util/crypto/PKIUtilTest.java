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
package org.osc.core.broker.util.crypto;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.osc.core.broker.util.log.LogProvider;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.slf4j.Logger;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ Files.class, LogProvider.class })
public class PKIUtilTest {

    private final static String FILE_TEXT = "Some file input.";

    private final static String DIR_PATH = System.getProperty("user.dir");

    private final static String INVALID_DIR_PATH = "\\%&^";

    private final static String FILE_NAME = "temporaryFile";

    private final static String FILE_NAME_WITH_TRAVERSAL = FILE_NAME + "/asd";

    private final String succesMessageBytes = "Successfully wrote " + FILE_TEXT.getBytes().length + " bytes to file '"
            + Paths.get(DIR_PATH + File.separator + FILE_NAME) + "'";

    private final String succesMessageInputStream = "Successfully wrote input stream to file '"
            + Paths.get(DIR_PATH + File.separator + FILE_NAME) + "'";

    private final String failedMessageBytes = "Failed to convert bytes to file";

    private final String failedMessageInputStream = "Failed to write input stream to file";

    private final String renameMesage = "Renaming/backup existing file";

    private final String traversalMessage = "Filename: " + FILE_NAME_WITH_TRAVERSAL + " is not valid";

    private static Writer writer = new StringWriter();

    @BeforeClass
    public static void setUpClasses() {

        Logger logger = Mockito.mock(Logger.class, new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                String methodName = invocation.getMethod().getName();

                if (methodName.equals("info") || methodName.equals("warn") || methodName.equals("debug")
                        || methodName.equals("error") || methodName.equals("trace")) {
                    Object arg0 = invocation.getArguments()[0];
                    if (arg0 instanceof String) {
                        writer.append((String) arg0);
                        writer.append('\n');
                    }
                }
                return null;
            }
        });

        PowerMockito.mockStatic(LogProvider.class, new Answer<Logger>() {
            @Override
            public Logger answer(InvocationOnMock invocation) throws Throwable {
                String methodName = invocation.getMethod().getName();
                if (methodName.equals("getLogger")) {
                    return logger;
                }

                return null;
            }
        });
    }

    private Writer getLogs() {
        return writer;
    }

    @Before
    public void setUpTest() {
        writer = new StringWriter();
    }

    @After
    public void clearFile() throws IOException {
        Path file = Paths.get(DIR_PATH + File.separator + FILE_NAME);
        Files.deleteIfExists(file);
    }

    @Test
    public void writeBytesToFile_fileNotExists_expectedSuccess() {
        //Arrange.
        Path file = Paths.get(DIR_PATH + File.separator + FILE_NAME);
        Path backup = Paths.get(DIR_PATH + File.separator + FILE_NAME + ".org");
        Writer logs = getLogs();
        //Act.
        PKIUtil.writeBytesToFile(FILE_TEXT.getBytes(), DIR_PATH, FILE_NAME);
        //Assert.
        Assert.assertTrue("File should exist: " + file, Files.exists(file));
        Assert.assertFalse("File should not exist: " + file, Files.exists(backup));
        Assert.assertTrue("Logs should contatin message: " + this.succesMessageBytes,
                logs.toString().contains(this.succesMessageBytes));
        Assert.assertFalse("Logs should not contatin message: " + this.renameMesage,
                logs.toString().contains(this.renameMesage));
    }

    @Test
    public void writeBytesToFile_fileExists_expectedSuccess() throws IOException {
        //Arrange.
        Path file = Paths.get(DIR_PATH + File.separator + FILE_NAME);
        Path backup = Paths.get(DIR_PATH + File.separator + FILE_NAME + ".org");
        Writer logs = getLogs();
        //Act.
        Files.write(file, FILE_TEXT.getBytes());
        PKIUtil.writeBytesToFile(FILE_TEXT.getBytes(), DIR_PATH, FILE_NAME);
        //Assert.
        Assert.assertTrue("File should exist: " + file, Files.exists(file));
        Assert.assertFalse("File should not exist: " + file, Files.exists(backup));
        Assert.assertTrue("Logs should contatin message: " + this.succesMessageBytes,
                logs.toString().contains(this.succesMessageBytes));
        Assert.assertTrue("Logs should contatin message: " + this.renameMesage,
                logs.toString().contains(this.renameMesage));
    }

    @Test
    public void writeBytesToFile_fileNotExists_expectedFailOnMoveFileToBackup() throws Exception {
        //Arrange.
        Path file = Paths.get(DIR_PATH + File.separator + FILE_NAME);
        Path backup = Paths.get(INVALID_DIR_PATH + File.separator + FILE_NAME + ".org");
        Writer logs = getLogs();
        //Act.
        PKIUtil.writeBytesToFile(FILE_TEXT.getBytes(), INVALID_DIR_PATH, FILE_NAME);
        //Assert.
        Assert.assertFalse("File should not exist: " + file, Files.exists(file));
        Assert.assertFalse("File should not exist: " + file, Files.exists(backup));
        Assert.assertTrue("Logs should contatin message: " + this.failedMessageBytes,
                logs.toString().contains(this.failedMessageBytes));
        Assert.assertFalse("Logs should contatin message: " + this.renameMesage,
                logs.toString().contains(this.renameMesage));
    }

    @Test
    public void writeInputStreamToFile_fileNotExists_expectedSuccess() throws IOException {
        //Arrange.
        Path file = Paths.get(DIR_PATH + File.separator + FILE_NAME);
        Path backup = Paths.get(DIR_PATH + File.separator + FILE_NAME + ".org");
        Writer logs = getLogs();
        //Act.
        PKIUtil.writeInputStreamToFile(IOUtils.toInputStream(FILE_TEXT, "UTF-8"), DIR_PATH, FILE_NAME);
        //Assert.
        Assert.assertTrue("File should exist: " + file, Files.exists(file));
        Assert.assertFalse("File should not exist: " + file, Files.exists(backup));
        Assert.assertTrue("Logs should contatin message: " + this.succesMessageInputStream,
                logs.toString().contains(this.succesMessageInputStream));
        Assert.assertFalse("Logs should not contatin message: " + this.renameMesage,
                logs.toString().contains(this.renameMesage));
    }

    @Test
    public void writeInputStreamToFile_fileExists_expectedSuccess() throws IOException {
        //Arrange.
        Path file = Paths.get(DIR_PATH + File.separator + FILE_NAME);
        Path backup = Paths.get(DIR_PATH + File.separator + FILE_NAME + ".org");
        Writer logs = getLogs();
        //Act.
        Files.write(file, FILE_TEXT.getBytes());
        PKIUtil.writeInputStreamToFile(IOUtils.toInputStream(FILE_TEXT, "UTF-8"), DIR_PATH, FILE_NAME);
        //Assert.
        Assert.assertTrue("File should exist: " + file, Files.exists(file));
        Assert.assertFalse("File should not exist: " + file, Files.exists(backup));
        Assert.assertTrue("Logs should contatin message: " + this.succesMessageInputStream,
                logs.toString().contains(this.succesMessageInputStream));
        Assert.assertTrue("Logs should contatin message: " + this.renameMesage,
                logs.toString().contains(this.renameMesage));
    }

    @Test
    public void writeInputStreamToFile_fileNotExists_expectedFailOnMoveFileToBackup() throws IOException {
        //Arrange.
        Path file = Paths.get(DIR_PATH + File.separator + FILE_NAME);
        Path backup = Paths.get(INVALID_DIR_PATH + File.separator + FILE_NAME + ".org");
        Writer logs = getLogs();
        //Act.
        PKIUtil.writeInputStreamToFile(IOUtils.toInputStream(FILE_TEXT, "UTF-8"), INVALID_DIR_PATH, FILE_NAME);
        //Assert.
        Assert.assertFalse("File should not exist: " + file, Files.exists(file));
        Assert.assertFalse("File should not exist: " + file, Files.exists(backup));
        Assert.assertTrue("Logs should contatin message: " + this.failedMessageInputStream,
                logs.toString().contains(this.failedMessageInputStream));
        Assert.assertFalse("Logs should contatin message: " + this.renameMesage,
                logs.toString().contains(this.renameMesage));
    }

    @Test
    public void writeInputStreamToFile_fileNotExistsAndPathTraversalInFileName_expectedFail() throws IOException {
        //Arrange.
        Path file = Paths.get(DIR_PATH + File.separator + FILE_NAME_WITH_TRAVERSAL);
        Path backup = Paths.get(DIR_PATH + File.separator + FILE_NAME_WITH_TRAVERSAL + ".org");
        Writer logs = getLogs();
        //Act.
        PKIUtil.writeInputStreamToFile(IOUtils.toInputStream(FILE_TEXT, "UTF-8"), DIR_PATH, FILE_NAME_WITH_TRAVERSAL);
        //Assert.
        Assert.assertFalse("File should exist: " + file, Files.exists(file));
        Assert.assertFalse("File should not exist: " + file, Files.exists(backup));
        Assert.assertFalse("Logs should contatin message: " + this.succesMessageInputStream,
                logs.toString().contains(this.succesMessageInputStream));
        Assert.assertFalse("Logs should not contatin message: " + this.renameMesage,
                logs.toString().contains(this.renameMesage));
        Assert.assertTrue("Logs should contatin message: " + this.traversalMessage,
                logs.toString().contains(this.traversalMessage));
    }

    @Test
    public void writeBytesToFile_fileNotExistsAndPathTraversalInFileName_expectedFail() throws IOException {
        //Arrange.
        Path file = Paths.get(DIR_PATH + File.separator + FILE_NAME_WITH_TRAVERSAL);
        Path backup = Paths.get(DIR_PATH + File.separator + FILE_NAME_WITH_TRAVERSAL + ".org");
        Writer logs = getLogs();
        //Act.
        PKIUtil.writeBytesToFile(FILE_TEXT.getBytes(), DIR_PATH, FILE_NAME_WITH_TRAVERSAL);
        //Assert.
        Assert.assertFalse("File should exist: " + file, Files.exists(file));
        Assert.assertFalse("File should not exist: " + file, Files.exists(backup));
        Assert.assertFalse("Logs should contatin message: " + this.succesMessageInputStream,
                logs.toString().contains(this.succesMessageInputStream));
        Assert.assertFalse("Logs should not contatin message: " + this.renameMesage,
                logs.toString().contains(this.renameMesage));
        Assert.assertTrue("Logs should contatin message: " + this.traversalMessage,
                logs.toString().contains(this.traversalMessage));
    }
}