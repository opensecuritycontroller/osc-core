package org.osc.core.broker.model.virtualization;

import java.util.Comparator;

public enum VmwareSoftwareVersion {
    VMWARE_V5_5("5.5"),
    VMWARE_V6("6");

    private final String text;

    private VmwareSoftwareVersion(final String text) {
        this.text = text;
    }

    public static VmwareSoftwareVersion fromText(String text) {
        for (VmwareSoftwareVersion version : VmwareSoftwareVersion.values()) {
            if (version.getText().equals(text)) {
                return version;
            }
        }
        throw new IllegalArgumentException("No enum constant " + VmwareSoftwareVersion.class.getCanonicalName() + " Found for " + text);
    }

    private String getText() {
        return this.text;
    }

    @Override
    public String toString() {
        return this.text;
    }

    public static class VmwareSoftwareVersionOrdinalComparator implements Comparator<VmwareSoftwareVersion> {
        // Orders by Enum ordinal or position in the enum
        @Override
        public int compare(VmwareSoftwareVersion m1, VmwareSoftwareVersion m2) {
            return new Integer(m1.ordinal()).compareTo(new Integer(m2.ordinal()));
        }
    }
}


