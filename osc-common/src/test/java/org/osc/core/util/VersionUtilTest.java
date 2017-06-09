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
        // 1.1.11-abc
        Version currentVersion = new Version(1L, 1L, "11-abc");

        // 1.1.11-abc
        Version otherVersion = new Version(1L, 1L, "11-abc");
        assertTrue(currentVersion.compareTo(otherVersion) == 0);
        assertTrue(currentVersion.compareTo(otherVersion) == -(otherVersion.compareTo(currentVersion)));

        // 1.2.11-abc
        otherVersion = new Version(1L, 2L, "11-abc");
        assertTrue(currentVersion.compareTo(otherVersion) < 0);

        // 1.0.11-abc
        otherVersion = new Version(1L, 0L, "11-abc");
        assertTrue(currentVersion.compareTo(otherVersion) > 0);

        // 1.1.12-abc
        otherVersion = new Version(1L, 1L, "12-abc");
        assertTrue(currentVersion.compareTo(otherVersion) < 0);

        // 2.1.11-abc
        otherVersion = new Version(2L, 1L, "11-abc");
        assertTrue(currentVersion.compareTo(otherVersion) < 0);

        // 1.1.13-abc
        otherVersion = new Version(1L, 1L, "13-abc");
        assertTrue(currentVersion.compareTo(otherVersion) < 0);

        // 1.1.9-abc
        otherVersion = new Version(1L, 1L, "9-abc");
        assertTrue(currentVersion.compareTo(otherVersion) > 0);

        currentVersion.setVersionStr(VersionUtil.DEBUG_VERSION_STRING);
        // 9.9.11-abc
        otherVersion = new Version(9L, 9L, "11-abc");
        assertTrue(currentVersion.compareTo(otherVersion) > 0);

        // 9.9.11-abc
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
        Assert.assertTrue("11" == version.getBuild());
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
