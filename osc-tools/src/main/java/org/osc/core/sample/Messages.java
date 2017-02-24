package org.osc.core.sample;

import java.util.MissingResourceException;
import java.util.ResourceBundle;

public final class Messages {
    public static final String BUNDLE_NAME = "org.osc.core.sample.messages"; //$NON-NLS-1$

    private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle.getBundle(BUNDLE_NAME);

    private Messages() {
    }

    public static String getString(String key) {
        try {
            return RESOURCE_BUNDLE.getString(key);
        } catch (MissingResourceException e) {
            return '!' + key + '!';
        }
    }
}
