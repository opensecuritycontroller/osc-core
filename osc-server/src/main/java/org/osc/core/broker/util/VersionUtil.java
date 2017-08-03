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

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.jar.Manifest;

import org.apache.log4j.Logger;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class VersionUtil {
    private static final Logger log = Logger.getLogger(VersionUtil.class);

    public static final String DEBUG_VERSION_STRING = "DEBUG";

    public static class Version implements Comparable<Version> {
        private Long major;
        private Long minor;
        private Long patch;
        private String build;
        // TODO emanoel: Remove the buildtime as it might be no longer needed
        private String buildTime;
        private String versionStr;

        public Version() {

        }

        public Version(Long major, Long minor, String build) {
            this.major = major;
            this.minor = minor;
            this.patch = 0L; // Default to 0 if not provided.
            this.build = build;
        }

        public Long getMajor() {
            return this.major;
        }

        public void setMajor(Long major) {
            this.major = major;
        }

        public Long getMinor() {
            return this.minor;
        }

        public void setMinor(Long minor) {
            this.minor = minor;
        }

        public Long getPatch() {
            return this.patch;
        }

        public void setPatch(Long patch) {
            this.patch = patch;
        }

        public String getBuild() {
            return this.build;
        }

        @JsonIgnore
        public Long getBuildNumber() {
            return Long.parseLong(this.build.split("-")[0]);
        }

        public void setBuild(String build) {
            this.build = build;
        }

        public String getBuildTime() {
            return this.buildTime;
        }

        public void setBuildTime(String buildTime) {
            this.buildTime = buildTime;
        }

        public String getVersionStr() {
            return this.versionStr;
        }

        @JsonIgnore
        public String getShortVersionStr() {
            if (this.major == null || this.minor == null || this.patch == null) {
                return null;
            }

            return this.major.toString() + "." + this.minor.toString() + "." + this.patch.toString();
        }

        @JsonIgnore
        public String getShortVersionStrWithBuild() {
            if (this.major == null || this.minor == null || this.build == null) {
                return null;
            }

            return getShortVersionStr() + "." + this.build;
        }

        public void setVersionStr(String versionStr) {
            this.versionStr = versionStr;
        }

        @Override
        public String toString() {
            return "Version [major=" + this.major + ", minor=" + this.minor + ", build=" + this.build + ", buildTime="
                    + this.buildTime + ", versionStr=" + this.versionStr + "]";
        }

        @Override
        public int compareTo(Version other) {
            String versionStr = getVersionStr();
            String otherVersionStr = other.getVersionStr();

            // TODO emanoel: Replace DEBUG version with SNAPSHOT
            // Debug versions
            if (versionStr != null && otherVersionStr != null && versionStr.equals(DEBUG_VERSION_STRING)
                    && otherVersionStr.equals(DEBUG_VERSION_STRING)) {
                return 0;
            }
            if (versionStr != null && versionStr.equals(DEBUG_VERSION_STRING)) {
                return 1;
            }
            if (otherVersionStr != null && otherVersionStr.equals(DEBUG_VERSION_STRING)) {
                return -1;
            }

            if (!getMajor().equals(other.getMajor())) {
                return getMajor().compareTo(other.getMajor());
            }
            if (!getMinor().equals(other.getMinor())) {
                return getMinor().compareTo(other.getMinor());
            }
            if (!getPatch().equals(other.getPatch())) {
                return getPatch().compareTo(other.getPatch());
            }
            if (!getBuild().equals(other.getBuild())) {
                long buildNumber = getBuildNumber();
                long otherBuildNumber = other.getBuildNumber();
                return (int)(buildNumber - otherBuildNumber);
            }
            return 0;
        }

    }

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
