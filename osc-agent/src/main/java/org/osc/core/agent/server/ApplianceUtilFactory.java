package org.osc.core.agent.server;

public class ApplianceUtilFactory {

    public static ApplianceUtils createApplianceUtils(String applianceType) {
        if (applianceType.equals("vnsp")) {
            return new NspApplianceUtils();
        } else if (applianceType.equals("ngfw")) {
            return new NgfwApplianceUtils();
        } else if (applianceType.equals("generic")) {
            return new GenericApplianceUtils();
        }
        return null;
    }

}
