package org.osc.core.util;

import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.osc.core.util.VersionUtil.Version;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;

import static org.junit.Assert.assertTrue;

@RunWith(PowerMockRunner.class)
@PrepareForTest(VersionUtil.class)
public class VersionUtilTest {

    @Mock
    Package aPackage;

    @Mock
    URLClassLoader classLoader;

    @Mock
    URL mockedUrl;

    private String MANIFEST_CONTENT = "Manifest-Version: 1.0\nImplementation-Version: 1.2\nBuild-Time: 1476885882\nImplementation-Build: 11\n";

    @Test
    public void testCompareVersion() {
        // 1.1.1000
        Version currentVersion = new Version(1L, 1L, 1000L);

        // 1.1.1000
        Version otherVersion = new Version(1L, 1L, 1000L);
        assertTrue(currentVersion.compareTo(otherVersion) == 0);
        assertTrue(currentVersion.compareTo(otherVersion) == -(otherVersion.compareTo(currentVersion)));

        // 1.2.2000
        otherVersion = new Version(1L, 2L, 2000L);
        assertTrue(currentVersion.compareTo(otherVersion) < 0);

        // 1.0.2000
        otherVersion = new Version(1L, 0L, 2000L);
        assertTrue(currentVersion.compareTo(otherVersion) > 0);

        // 1.1.2000
        otherVersion = new Version(1L, 1L, 2000L);
        assertTrue(currentVersion.compareTo(otherVersion) < 0);

        // 2.1.2000
        otherVersion = new Version(2L, 1L, 2000L);
        assertTrue(currentVersion.compareTo(otherVersion) < 0);

        // 1.1.3000
        otherVersion = new Version(1L, 1L, 3000L);
        assertTrue(currentVersion.compareTo(otherVersion) < 0);

        // 1.1.999
        otherVersion = new Version(1L, 1L, 999L);
        assertTrue(currentVersion.compareTo(otherVersion) > 0);

        currentVersion.setVersionStr(VersionUtil.DEBUG_VERSION_STRING);
        // 9.9.9000
        otherVersion = new Version(9L, 9L, 9000L);
        assertTrue(currentVersion.compareTo(otherVersion) > 0);

        // 9.9.9000
        otherVersion.setVersionStr(VersionUtil.DEBUG_VERSION_STRING);
        assertTrue(currentVersion.compareTo(otherVersion) == 0);
        assertTrue(currentVersion.compareTo(otherVersion) == -(otherVersion.compareTo(currentVersion)));

    }

//    @Test
    public void testGetVersion_WithMockedManifest_ReturnsValidVersion() throws IOException {
        //  Arrange.
        PowerMockito.mockStatic(VersionUtil.class);
        PowerMockito.when(VersionUtil.getVersion()).thenCallRealMethod();
        PowerMockito.when(VersionUtil.class.getPackage()).thenReturn(aPackage);
        PowerMockito.when(VersionUtil.class.getClassLoader()).thenReturn(classLoader);
        PowerMockito.when(classLoader.findResource(Matchers.anyString())).thenReturn(mockedUrl);
        InputStream stubInputStream = IOUtils.toInputStream(MANIFEST_CONTENT);
        PowerMockito.when(mockedUrl.openStream()).thenReturn(stubInputStream);
        PowerMockito.when(aPackage.getImplementationVersion()).thenReturn("1.0.0");

        // Act.
        Version version = VersionUtil.getVersion();

        // Assert.
        Assert.assertTrue(11 == version.getBuild());
        Assert.assertEquals("1476885882", version.getBuildTime());
        Assert.assertTrue(1 == version.getMajor());
        Assert.assertTrue(2 == version.getMinor());
    }

//    @Test
    public void testGetVersion_WithoutImplementationVersion_ReturnsDebugVersion() throws IOException {
        //  Arrange.
        PowerMockito.mockStatic(VersionUtil.class);
        PowerMockito.when(VersionUtil.getVersion()).thenCallRealMethod();
        PowerMockito.when(VersionUtil.class.getPackage()).thenReturn(aPackage);
        PowerMockito.when(aPackage.getImplementationVersion()).thenReturn(null);

        // Act.
        Version version = VersionUtil.getVersion();

        // Assert.
        Assert.assertEquals("DEBUG", version.getVersionStr());
        Assert.assertNull(version.getMajor());
        Assert.assertNull(version.getMinor());
        Assert.assertNull(version.getBuild());
        Assert.assertNull(version.getBuildTime());
    }

}
