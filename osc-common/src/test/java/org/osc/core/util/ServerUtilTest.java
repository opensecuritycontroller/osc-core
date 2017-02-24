package org.osc.core.util;

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
import org.osc.core.rest.client.VmidcServerRestClient;
import org.osc.core.rest.client.exception.ClientResponseNotOkException;
import org.osc.core.rest.client.exception.CorruptedPidException;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

import static org.powermock.api.support.membermodification.MemberMatcher.method;
import static org.powermock.api.support.membermodification.MemberModifier.replace;

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
        runtimeMock = PowerMockito.mock(Runtime.class);
        processMock = PowerMockito.mock(Process.class);
        PowerMockito.when(processMock.waitFor()).thenReturn(0);
        Mockito.when(Runtime.getRuntime()).thenReturn(runtimeMock);

        this.homeDirectory = testFolder.newFolder("fakeHomeDir");

        if (!this.homeDirectory.exists()) {
            Assert.fail("Failed to create new folder for tests");
        }

        this.regularFile = new File(this.homeDirectory, PID_FILE);
        if (regularFile.exists()) {
            boolean isDeleted = regularFile.delete();
            Assert.assertTrue(isDeleted);
        }

        this.regularFile = new File(this.homeDirectory, PID_FILE);

        if (!regularFile.createNewFile() || !regularFile.exists()) {
            Assert.fail("Failed to create new file for test");
        }

        cmdOutput = new ArrayList<>();
        cmdOutput.add(" PID TTY STAT TIME COMMAND");
        cmdOutput.add(" 10 ? Ss 0:07 /test/test");
        cmdOutput.add(" 9990 ? Ss 0:07 /test/java");
        cmdOutput.add(" " + oldProcessId + " ? Ss 0:07 /osc/java");
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testKillProcess_WithLongRunningProcessId_UsesForcedShutdown() throws Exception {
        // Arrange.
        final boolean[] testPassed = new boolean[1];
        final List<String> gentleShutdownIterationsCounter = new ArrayList<>();

        replace(method(ServerUtil.class, "execWithLines", String.class, List.class)).with(
                new InvocationHandler() {
                    public Object invoke(Object object, Method method, Object[] arguments) throws Throwable {
                        if (arguments[0].equals("ps " + oldProcessId)) {
                            List<String> test = (List<String>) arguments[1];
                            test.addAll(cmdOutput);
                            gentleShutdownIterationsCounter.add("invocation");
                            return 0;
                        } else {
                            return method.invoke(object, arguments);
                        }
                    }
                });

        replaceExecWithLogMethod(oldProcessId, testPassed);

        // Act.
        ServerUtil.killProcess(oldProcessId);

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
                    public Object invoke(Object object, Method method, Object[] arguments) throws Throwable {
                        if (arguments[0].equals("ps " + oldProcessId)) {
                            if (gentleShutdownIterationsCounter.size() < 3) {
                                List<String> test = (List<String>) arguments[1];
                                test.addAll(cmdOutput);
                            }
                            gentleShutdownIterationsCounter.add("invocation");
                            return 0;
                        } else {
                            return method.invoke(object, arguments);
                        }
                    }
                });

        replaceExecWithLogMethod(oldProcessId, testPassed);

        // Act.
        ServerUtil.killProcess(oldProcessId);

        // Assert.
        Assert.assertTrue(gentleShutdownIterationsCounter.size() == 4);
        Assert.assertFalse(testPassed[0]);
    }

    @Test
    public void testTerminateProcessInRunningServer_WithNoActiveConnection_ThrowsClientResponseInPutResource() throws Exception {
        // Arrange.
        Mockito.doThrow(ClientResponseNotOkException.class)
                .when(vmidcServerRestClientMock).putResource(Matchers.anyString(), Matchers.any());
        Mockito.when(serverStatusResponseInjectionMock.getProcessId()).thenReturn(oldProcessId);

        // Act.
        boolean isServerRunning = ServerUtil.terminateProcessInRunningServer(vmidcServerRestClientMock, oldProcessId, serverStatusResponseInjectionMock);

        // Assert.
        Assert.assertTrue(isServerRunning);
    }

    @Test
    public void testTerminateProcessInRunningServer_WithNoActiveProcess_ReturnsNoRunningServer() throws Exception {
        // Arrange.
        Mockito.doNothing().when(vmidcServerRestClientMock).putResource(Matchers.anyString(), Matchers.any());
        Mockito.when(serverStatusResponseInjectionMock.getProcessId()).thenReturn(null);

        mockGetCurrentPid();

        // Act.
        boolean isServerRunning = ServerUtil.terminateProcessInRunningServer(vmidcServerRestClientMock, oldProcessId, serverStatusResponseInjectionMock);

        // Assert.
        Assert.assertFalse(isServerRunning);
    }

    @Test
    public void testTerminateProcessInRunningServer_WithExceptionInGetProcessId_ThrowsClientResponseException() throws Exception {
        // Arrange.
        Mockito.doNothing().when(vmidcServerRestClientMock).putResource(Matchers.anyString(), Matchers.any());
        Mockito.doThrow(ClientResponseNotOkException.class).when(serverStatusResponseInjectionMock).getProcessId();

        mockGetCurrentPid();

        // Act.
        boolean isServerRunning = ServerUtil.terminateProcessInRunningServer(vmidcServerRestClientMock, oldProcessId, serverStatusResponseInjectionMock);

        // Assert.
        Assert.assertFalse(isServerRunning);
    }

    @Test
    public void testTerminateProcessInRunningServer_WithPendingUpgrade_TerminatesProcessId() throws Exception {
        // Arrange.
        Mockito.doNothing().when(vmidcServerRestClientMock).putResource(Matchers.anyString(), Matchers.any());
        Mockito.when(serverStatusResponseInjectionMock.getProcessId()).thenReturn(oldProcessId);
        mockGetCurrentPid();

        replace(method(ServerUtil.class, "killProcess", String.class)).with(
                new InvocationHandler() {
                    public Object invoke(Object object, Method method, Object[] arguments) throws Throwable {
                        return "0";
                    }
                });

        // Act.
        boolean isServerRunning = ServerUtil.terminateProcessInRunningServer(vmidcServerRestClientMock, oldProcessId, serverStatusResponseInjectionMock);

        // Assert.
        Assert.assertFalse(isServerRunning);
    }

    @Test
    public void testTerminateProcessInRunningServer_InPendingUpgradeUnableToKillProcess_ThrowsInterruptedExceptionInKillProcess() throws Exception {
        // Arrange.
        Mockito.doNothing().when(vmidcServerRestClientMock).putResource(Matchers.anyString(), Matchers.any());
        Mockito.when(serverStatusResponseInjectionMock.getProcessId()).thenReturn(oldProcessId);
        mockGetCurrentPid();

        replace(method(ServerUtil.class, "killProcess", String.class)).with(
                new InvocationHandler() {
                    public Object invoke(Object object, Method method, Object[] arguments) throws Throwable {
                        throw new InterruptedException();
                    }
                });

        // Act.
        boolean isServerRunning = ServerUtil.terminateProcessInRunningServer(vmidcServerRestClientMock, oldProcessId, serverStatusResponseInjectionMock);

        // Assert.
        Assert.assertFalse(isServerRunning);
    }

    @Test
    public void testGetPidByProcessName_WithProperData_ReturnsProperProcessId() throws IOException, InterruptedException {
        // Arrange.
        InputStream stubInputStream = IOUtils.toInputStream(
                " PID TTY STAT TIME COMMAND\n 9990 ? Ss 0:07 /test/java\n " + oldProcessId + " ? Ss 0:07 /osc/java");
        PowerMockito.when(processMock.getInputStream()).thenReturn(stubInputStream);
        PowerMockito.when(runtimeMock.exec(Matchers.anyString())).thenReturn(processMock);

        Mockito.when(Runtime.getRuntime()).thenReturn(runtimeMock);

        // Act.
        String foundPid = ServerUtil.getPidByProcessName("/osc/java");

        // Assert.
        Assert.assertEquals(oldProcessId, foundPid);
    }

    @Test
    public void testGetPidByProcessName_WithImproperData_ReturnsNothing() throws IOException, InterruptedException {
        // Arrange.
        InputStream stubInputStream = IOUtils.toInputStream(
                " PID TTY STAT TIME COMMAND\n 9990 ? Ss 0:07 /test/java\n " + oldProcessId + " ? Ss 0:07 /osc/java");

        PowerMockito.when(processMock.getInputStream()).thenReturn(stubInputStream);
        PowerMockito.when(runtimeMock.exec(linuxProcessExec)).thenReturn(processMock);

        Mockito.when(Runtime.getRuntime()).thenReturn(runtimeMock);

        // Act.
        String foundPid = ServerUtil.getPidByProcessName("/osc/test");

        // Assert.
        Assert.assertNull(foundPid);
    }

    @Test
    public void testGetPidByProcessName_WithImproperData_ThrowsInputStreamException() throws IOException, InterruptedException {
        // Arrange.
        PowerMockito.when(processMock.getInputStream()).thenReturn(null);
        PowerMockito.when(runtimeMock.exec(linuxProcessExec)).thenReturn(processMock);

        Mockito.when(Runtime.getRuntime()).thenReturn(runtimeMock);

        // Act.
        String foundPid = ServerUtil.getPidByProcessName("/osc/java");

        // Assert.
        Assert.assertNull(foundPid);
    }

    @Test
    public void testGetPidByProcessName_WithWindowsMachineFlag_ReturnsProperProcessId() throws IOException, InterruptedException {
        // Arrange.
        InputStream stubInputStream = IOUtils.toInputStream(
                " PID TTY STAT TIME COMMAND\n 9990 ? Ss 0:07 /test/java\n " + oldProcessId + " ? Ss 0:07 /osc/java");

        PowerMockito.when(processMock.getInputStream()).thenReturn(stubInputStream);
        PowerMockito.when(runtimeMock.exec(linuxProcessExec + "-W")).thenReturn(processMock);
        replace(method(ServerUtil.class, "isWindows")).with(
                new InvocationHandler() {
                    public Object invoke(Object object, Method method, Object[] arguments) throws Throwable {
                        return true;
                    }
                });

        Mockito.when(Runtime.getRuntime()).thenReturn(runtimeMock);

        // Act.
        String foundPid = ServerUtil.getPidByProcessName("/osc/java");

        // Assert.
        Assert.assertEquals("Process id is different than expected", oldProcessId, foundPid);
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
        exception.expect(CorruptedPidException.class);

        // Act.
        ServerUtil.readPIDFromFile(this.regularFile.getPath());
    }

    @Test
    public void testReadPIDFromFile_WithCorruptedPID_ThrowsCorruptedPidException() throws IOException, CorruptedPidException {
        // Arrange.
        Files.write(this.regularFile.toPath(), "notnumber".getBytes(), StandardOpenOption.APPEND);
        exception.expect(CorruptedPidException.class);

        // Act.
        ServerUtil.readPIDFromFile(this.regularFile.getPath());
    }

    @Test
    public void testReadPIDFromFile_WithoutFile_ReturnsNull() throws IOException, CorruptedPidException {
        // Arrange.
        this.regularFile = new File(this.homeDirectory, PID_FILE);
        if (regularFile.exists()) {
            boolean isDeleted = regularFile.delete();
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
                    public Object invoke(Object object, Method method, Object[] arguments) throws Throwable {
                        return currentPid;
                    }
                });
    }

}
