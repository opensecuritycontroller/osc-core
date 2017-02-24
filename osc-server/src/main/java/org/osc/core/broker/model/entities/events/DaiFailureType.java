package org.osc.core.broker.model.entities.events;

public enum DaiFailureType {

    DAI_STATUS_CHANGE("DAI Status Change"),
    DAI_TIMEOUT("DAI Timeout"),
    DAI_NSX_STATUS_UPDATE("DAI NSX status update"),
    DAI_MGR_INITIAL_CONFIG("DAI Manager Initial Config"),
    DAI_MGR_CHECK("DAI Manager Check");

    private final String text;

    private DaiFailureType(final String text) {
        this.text = text;
    }

    public static DaiFailureType fromText(String text) {
        for (DaiFailureType type : DaiFailureType.values()) {
            if (type.getText().equals(text)) {
                return type;
            }
        }
        throw new IllegalArgumentException(
                "No enum constant " + DaiFailureType.class.getCanonicalName() + " Found for " + text);
    }

    private String getText() {
        return this.text;
    }

    @Override
    public String toString() {
        return text;
    }
}
