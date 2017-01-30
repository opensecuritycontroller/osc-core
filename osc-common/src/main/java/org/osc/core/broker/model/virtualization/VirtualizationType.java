package org.osc.core.broker.model.virtualization;


public enum VirtualizationType {
    VMWARE("VMWARE"),
    OPENSTACK("OPENSTACK");

    private final String text;

    private VirtualizationType(final String text) {
        this.text = text;
    }

    public static VirtualizationType fromText(String text) {
        for (VirtualizationType type : VirtualizationType.values()) {
            if (type.getText().equals(text)) {
                return type;
            }
        }
        throw new IllegalArgumentException("No enum constant " + VirtualizationType.class.getCanonicalName() + " Found for " + text);
    }

    private String getText() {
        return this.text;
    }

    public boolean isVmware() {
        return this.equals(VirtualizationType.VMWARE);
    }

    public boolean isOpenstack() {
        return this.equals(VirtualizationType.OPENSTACK);
    }

    @Override
    public String toString() {
        return this.text;
    }
}
