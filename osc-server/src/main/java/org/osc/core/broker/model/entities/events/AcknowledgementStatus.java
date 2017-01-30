package org.osc.core.broker.model.entities.events;

public enum AcknowledgementStatus {

    PENDING_ACKNOWLEDGEMENT("Pending Acknowledgement"), ACKNOWLEDGED("Acknowledged");

    private final String text;

    private AcknowledgementStatus(final String text) {
        this.text = text;
    }

    public static AcknowledgementStatus fromText(String text) {
        for (AcknowledgementStatus status : AcknowledgementStatus.values()) {
            if (status.getText().equals(text)) {
                return status;
            }
        }
        throw new IllegalArgumentException("No enum constant " + AcknowledgementStatus.class.getCanonicalName()
                + " Found for " + text);
    }

    private String getText() {
        return this.text;
    }

    @Override
    public String toString() {
        return text;
    }
}
