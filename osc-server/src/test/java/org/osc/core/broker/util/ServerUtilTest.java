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

import static org.powermock.api.support.membermodification.MemberMatcher.method;
import static org.powermock.api.support.membermodification.MemberModifier.replace;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.osc.core.broker.rest.client.VmidcServerRestClient;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ServerUtil.class, Runtime.class})
public class ServerUtilTest {

    private final String oldProcessId = "10000";
    private final String linuxProcessExec = "ps ";

    private Runtime runtimeMock;
    private Process processMock;
    private File homeDirectory;
    private File regularFile;
    private String PID_FILE = "appPid";

    @Mock
    VmidcServerRestClient vmidcServerRestClientMock;

    @Mock
    ServerStatusResponseInjection serverStatusResponseInjectionMock;

    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();

    @Rule
    public ExpectedException exception = ExpectedException.none();
    private List<String> cmdOutput;

    @Before
    public void init() throws InterruptedException, IOException {
        PowerMockito.mockStatic(Runtime.class);
        this.runtimeMock = PowerMockito.mock(Runtime.class);
        this.processMock = PowerMockito.mock(Process.class);
        PowerMockito.when(this.processMock.waitFor()).thenReturn(0);
        Mockito.when(Runtime.getRuntime()).thenReturn(this.runtimeMock);

        this.homeDirectory = this.testFolder.newFolder("fakeHomeDir");

        if (!this.homeDirectory.exists()) {
            Assert.fail("Failed to create new folder for tests");
        }

        this.regularFile = new File(this.homeDirectory, this.PID_FILE);
        if (this.regularFile.exists()) {
            boolean isDeleted = this.regularFile.delete();
            Assert.assertTrue(isDeleted);
        }

        this.regularFile = new File(this.homeDirectory, this.PID_FILE);

        if (!this.regularFile.createNewFile() || !this.regularFile.exists()) {
            Assert.fail("Failed to create new file for test");
        }

        this.cmdOutput = new ArrayList<>();
        this.cmdOutput.add(" PID TTY STAT TIME COMMAND");
        this.cmdOutput.add(" 10 ? Ss 0:07 /test/test");
        this.cmdOutput.add(" 9990 ? Ss 0:07 /test/java");
        this.cmdOutput.add(" " + this.oldProcessId + " ? Ss 0:07 /osc/java");
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testKillProcess_WithLongRunningProcessId_UsesForcedShutdown() throws Exception {
        // Arrange.
        final boolean[] testPassed = new boolean[1];
        final List<String> gentleShutdownIterationsCounter = new ArrayList<>();

        replace(method(ServerUtil.class, "execWithLines", String.class, List.class)).with(
                new InvocationHandler() {
                    @Override
                    public Object invoke(Object object, Method method, Object[] arguments) throws Throwable {
                        if (arguments[0].equals("ps " + ServerUtilTest.this.oldProcessId)) {
                            List<String> test = (List<String>) arguments[1];
                            test.addAll(ServerUtilTest.this.cmdOutput);
                            gentleShutdownIterationsCounter.add("invocation");
                            return 0;
                        } else {
                            return method.invoke(object, arguments);
                        }
                    }
                });

        replaceExecWithLogMethod(this.oldProcessId, testPassed);

        // Act.
        ServerUtil.killProcess(this.oldProcessId);

        // Assert.
        Assert.assertTrue(gentleShutdownIterationsCounter.size() == 10);
        Assert.assertTrue(testPassed[0]);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testKillProcess_WithShortRunningProcessId_UsesGentleShutdown() throws Exception {
        // Arrange.
        final boolean[] testPassed = new boolean[1];
        final List<String> gentleShutdownIterationsCounter = new ArrayList<>();

        replace(method(ServerUtil.class, "execWithLines", String.class, List.class)).with(
                new InvocationHandler() {
                    @Override
                    public Object invoke(Object object, Method method, Object[] arguments) throws Throwable {
                        if (arguments[0].equals("ps " + ServerUtilTest.this.oldProcessId)) {
                            if (gentleShutdownIterationsCounter.size() < 3) {
                                List<String> test = (List<String>) arguments[1];
                                test.addAll(ServerUtilTest.this.cmdOutput);
                            }
                            gentleShutdownIterationsCounter.add("invocation");
                            return 0;
                        } else {
                            return method.invoke(object, arguments);
                        }
                    }
                });

        replaceExecWithLogMethod(this.oldProcessId, testPassed);

        // Act.
        ServerUtil.killProcess(this.oldProcessId);

        // Assert.
        Assert.assertTrue(gentleShutdownIterationsCounter.size() == 4);
        Assert.assertFalse(testPassed[0]);
    }

    @Test
    public void testGetPidByProcessName_WithProperData_ReturnsProperProcessId() throws IOException, InterruptedException {
        // Arrange.
        InputStream stubInputStream = IOUtils.toInputStream(
                " PID TTY STAT TIME COMMAND\n 9990 ? Ss 0:07 /test/java\n " + this.oldProcessId + " ? Ss 0:07 /osc/java",
                Charset.defaultCharset());
        PowerMockito.when(this.processMock.getInputStream()).thenReturn(stubInputStream);
        PowerMockito.when(this.runtimeMock.exec(Matchers.anyString())).thenReturn(this.processMock);

        Mockito.when(Runtime.getRuntime()).thenReturn(this.runtimeMock);

        // Act.
        String foundPid = ServerUtil.getPidByProcessName("/osc/java");

        // Assert.
        Assert.assertEquals(this.oldProcessId, foundPid);
    }

    @Test
    public void testGetPidByProcessName_WithImproperData_ReturnsNothing() throws IOException, InterruptedException {
        // Arrange.
        InputStream stubInputStream = IOUtils.toInputStream(
                " PID TTY STAT TIME COMMAND\n 9990 ? Ss 0:07 /test/java\n " + this.oldProcessId + " ? Ss 0:07 /osc/java",
                Charset.defaultCharset());

        PowerMockito.when(this.processMock.getInputStream()).thenReturn(stubInputStream);
        PowerMockito.when(this.runtimeMock.exec(this.linuxProcessExec)).thenReturn(this.processMock);

        Mockito.when(Runtime.getRuntime()).thenReturn(this.runtimeMock);

        // Act.
        String foundPid = ServerUtil.getPidByProcessName("/osc/test");

        // Assert.
        Assert.assertNull(foundPid);
    }

    @Test
    public void testGetPidByProcessName_WithImproperData_ThrowsInputStreamException() throws IOException, InterruptedException {
        // Arrange.
        PowerMockito.when(this.processMock.getInputStream()).thenReturn(null);
        PowerMockito.when(this.runtimeMock.exec(this.linuxProcessExec)).thenReturn(this.processMock);

        Mockito.when(Runtime.getRuntime()).thenReturn(this.runtimeMock);

        // Act.
        String foundPid = ServerUtil.getPidByProcessName("/osc/java");

        // Assert.
        Assert.assertNull(foundPid);
    }

    @Test
    public void testGetPidByProcessName_WithWindowsMachineFlag_ReturnsProperProcessId() throws IOException, InterruptedException {
        // Arrange.
        InputStream stubInputStream = IOUtils.toInputStream(
                " PID TTY STAT TIME COMMAND\n 9990 ? Ss 0:07 /test/java\n " + this.oldProcessId + " ? Ss 0:07 /osc/java",
                Charset.defaultCharset());

        PowerMockito.when(this.processMock.getInputStream()).thenReturn(stubInputStream);
        PowerMockito.when(this.runtimeMock.exec(this.linuxProcessExec + "-W")).thenReturn(this.processMock);
        replace(method(ServerUtil.class, "isWindows")).with(
                new InvocationHandler() {
                    @Override
                    public Object invoke(Object object, Method method, Object[] arguments) throws Throwable {
                        return true;
                    }
                });

        Mockito.when(Runtime.getRuntime()).thenReturn(this.runtimeMock);

        // Act.
        String foundPid = ServerUtil.getPidByProcessName("/osc/java");

        // Assert.
        Assert.assertEquals("Process id is different than expected", this.oldProcessId, foundPid);
    }

    @Test
    public void testReadPIDFromFile_WithValidPIDFile_ReturnsNumericPID() throws IOException, CorruptedPidException {
        // Arrange.
        Files.write(this.regularFile.toPath(), "1000".getBytes(), StandardOpenOption.APPEND);

        // Act.
        Integer pidFromFile = ServerUtil.readPIDFromFile(this.regularFile.getPath());

        // Assert.
        Assert.assertTrue(Integer.valueOf(1000).equals(pidFromFile));
    }

    @Test
    public void testReadPIDFromFile_WithOverflowedPID_ThrowsCorruptedPidException() throws IOException, CorruptedPidException {
        // Arrange.
        Files.write(this.regularFile.toPath(), String.valueOf(Integer.MAX_VALUE).getBytes(), StandardOpenOption.APPEND);
        this.exception.expect(CorruptedPidException.class);

        // Act.
        ServerUtil.readPIDFromFile(this.regularFile.getPath());
    }

    @Test
    public void testReadPIDFromFile_WithCorruptedPID_ThrowsCorruptedPidException() throws IOException, CorruptedPidException {
        // Arrange.
        Files.write(this.regularFile.toPath(), "notnumber".getBytes(), StandardOpenOption.APPEND);
        this.exception.expect(CorruptedPidException.class);

        // Act.
        ServerUtil.readPIDFromFile(this.regularFile.getPath());
    }

    @Test
    public void testReadPIDFromFile_WithoutFile_ReturnsNull() throws IOException, CorruptedPidException {
        // Arrange.
        this.regularFile = new File(this.homeDirectory, this.PID_FILE);
        if (this.regularFile.exists()) {
            boolean isDeleted = this.regularFile.delete();
            Assert.assertTrue(isDeleted);
        }

        // Act.
        Integer pidFromFile = ServerUtil.readPIDFromFile(this.regularFile.getPath());

        // Assert.
        Assert.assertNull(pidFromFile);
    }

    @Test
    public void testDeletePidFileIfOwned_WithProperPidFile_RemovesPIDFile() throws IOException {
        // Arrange.
        mockGetCurrentPid("1000");
        Files.write(this.regularFile.toPath(), "1000".getBytes(), StandardOpenOption.APPEND);
        Assert.assertTrue(this.regularFile.exists());

        // Act.
        ServerUtil.deletePidFileIfOwned(this.regularFile.getPath());

        // Assert.
        Assert.assertFalse(this.regularFile.exists());
    }

    @Test
    public void testDeletePidFileIfOwned_WithDifferentCurrentPid_PreservesPIDFile() throws IOException {
        // Arrange.
        mockGetCurrentPid("1100");
        Files.write(this.regularFile.toPath(), "1000".getBytes(), StandardOpenOption.APPEND);
        Assert.assertTrue(this.regularFile.exists());

        // Act.
        ServerUtil.deletePidFileIfOwned(this.regularFile.getPath());

        // Assert.
        Assert.assertTrue(this.regularFile.exists());
    }

    private void replaceExecWithLogMethod(final String oldProcessId, final boolean[] testPassed) {
        replace(method(ServerUtil.class, "execWithLog", String.class)).with(
                new InvocationHandler() {
                    @Override
                    public Object invoke(Object object, Method method, Object[] arguments) throws Throwable {
                        if (arguments[0].equals("kill -9 " + oldProcessId)) {
                            testPassed[0] = true;
                            return 0;
                        } else {
                            testPassed[0] = false;
                            return method.invoke(object, arguments);
                        }
                    }
                });
    }

    private void mockGetCurrentPid() {
        mockGetCurrentPid("0");
    }

    private void mockGetCurrentPid(final String currentPid) {
        replace(method(ServerUtil.class, "getCurrentPid")).with(
                new InvocationHandler() {
                    @Override
                    public Object invoke(Object object, Method method, Object[] arguments) throws Throwable {
                        return currentPid;
                    }
                });
    }

}
