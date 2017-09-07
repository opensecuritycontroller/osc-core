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

import static org.osc.core.common.version.Version.DEBUG_VERSION_STRING;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.jar.Manifest;

import org.apache.log4j.Logger;
import org.osc.core.common.version.Version;

public class VersionUtil {
    private static final Logger log = Logger.getLogger(VersionUtil.class);

    public static Version getVersion() {
        try {
            return getVersion(getManifest("Implementation-Build"));
        } catch (Exception ex) {
            if (ex instanceof IllegalArgumentException) {
                // for expected exception dont print stack trace
                log.warn(ex.getMessage());
            } else {
                log.error("Failed to load version information: ", ex);
            }
            Version version = new Version();
            version.setVersionStr(DEBUG_VERSION_STRING);
            return version;
        }
    }

    public static Version getVersion(Manifest manifest) {
        Version version = new Version();
        String versionStr = manifest.getMainAttributes().getValue("Implementation-Version");

        String[] versionParts = versionStr.split("-|\\.");

        Long major = Long.parseLong(versionParts[0]);
        Long minor =  versionParts.length > 1 ? Long.parseLong(versionParts[1]) : 0L;
        Long patch =  versionParts.length > 2 ? Long.parseLong(versionParts[2]) : 0L;
        String buildStr = manifest.getMainAttributes().getValue("Implementation-Build");

        if (buildStr == null || buildStr.isEmpty()) {
            throw new IllegalArgumentException("The provided build string should not be null or empty.");
        }

        String[] buildParts = buildStr.split("-");
        buildStr = buildParts[buildParts.length - 2] + "-" + buildParts[buildParts.length - 1];

        String buildTime = manifest.getMainAttributes().getValue("Build-Time");

        version.setMajor(major);
        version.setMinor(minor);
        version.setPatch(patch);
        version.setBuild(buildStr);
        version.setBuildTime(buildTime);

        versionStr += " (Build:" + buildStr + ", " + version.getBuildTime() + ")";

        version.setVersionStr(versionStr);
        return version;
    }

    /*
     * get first manifest containing specified header
     */
    private static Manifest getManifest(final String header) throws IOException {
        ClassLoader cl = VersionUtil.class.getClassLoader();
        Enumeration<URL> manifests = cl.getResources("META-INF/MANIFEST.MF");

        while (manifests.hasMoreElements()) {
            try (InputStream in = manifests.nextElement().openStream()) {
                Manifest m = new Manifest(in);
                if (m.getMainAttributes().getValue(header) != null) {
                    return m;
                }
            }
        }
        throw new IOException("Can't find Manifest containing header: " + header);
    }
}
