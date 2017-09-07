package org.osc.core.common.version;


public class Version implements Comparable<Version> {

    public static final String DEBUG_VERSION_STRING = "DEBUG";

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

    public String getShortVersionStr() {
        if (this.major == null || this.minor == null || this.patch == null) {
            return null;
        }

        return this.major.toString() + "." + this.minor.toString() + "." + this.patch.toString();
    }

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
