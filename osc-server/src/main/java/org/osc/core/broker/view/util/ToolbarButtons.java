package org.osc.core.broker.view.util;



public enum ToolbarButtons {
    ADD("Add", "Add", "img/add.png"),
    EDIT("Edit", "Edit", "img/edit.png"),
    DELETE("Delete", "Delete", "img/delete.png"),
    CONFORM("Sync", "Sync", "img/conform.png"),
    JOB_VIEW("Job Graph", "Job_Graph", "img/job-graph.png"),
    JOB_ABORT("Abort", "Abort", "img/cancel.png"),
    APPLIANCE_AGENT_UPGRADE("Upgrade Agent", "Upgrade_Agent", "img/upgrade.png"),
    APPLIANCE_AGENT_STATUS("Agent Status", "Agent_Status", "img/status.png"),
    APPLIANCE_AGENT_REGISTER("Appliance Re-authentication", "Appliance_Re-authentication", "img/register.png"),
    APPLIANCE_AGENT_LOG_DOWNLOAD("Download Agent Log", "Download_Agent_Log", "img/download.png"),
    VSS_DEPLOY("Deploy", "Deploy", "img/deploy.png"),
    AUTO_IMPORT_APPLIANCE("Auto Import", "Auto_Import", "img/deploy.png"),
    BACK("Back", "Back_to_Previous_View", "img/back.png"),
    ADD_CHILD("Add", "Add_Child", "img/add.png"),
    EDIT_CHILD("Edit", "Edit_Child", "img/edit.png"),
    DELETE_CHILD("Delete", "Delete_Child", "img/delete.png"),
    DEPLOYMENTS("Deployments...", "Deployments", "img/drilldown.png"),
    SECURITY_GROUP_INTERFACES("Traffic Policy Mappings ...", "Security_Group_Interfaces", "img/drilldown.png"),
    BIND_SECURITY_GROUP("Bind", "bind"),
    ACKNOWLEDGE_ALERT("Acknowledge", "acknowledge_alert", "img/acknowledge.png"),
    UNACKNOWLEDGE_ALERT("Unacknowledge", "unacknowledge_alert", "img/unacknowledge.png"),
    SHOW_PENDING_ACKNOWLEDGE_ALERTS("Show Pending", "show_pending_alerts", null, "Show Pending Un-Acknowledged Alerts", HorizontalAlignment.RIGHT),
    SHOW_ALL_ALERTS("Show All", "show_all_alerts", null, "Remove All Filters and Show All Alerts", HorizontalAlignment.RIGHT);

    private String text;
    private String tooltip;
    private String id;
    private String imageLocation;
    private HorizontalAlignment alignment;

    private ToolbarButtons(final String text, final String id) {
        this(text, id, null, null);
    }

    private ToolbarButtons(final String text, final String id, final String imageLocation) {
        this(text, id, imageLocation, null);
    }

    private ToolbarButtons(final String text, String id, String imageLocation, final String tooltip) {
        this(text, id, imageLocation, tooltip, HorizontalAlignment.LEFT);
    }

    private ToolbarButtons(final String text, String id, String imageLocation, final String tooltip, HorizontalAlignment alignment) {
        this.text = text;
        this.id = id;
        this.imageLocation = imageLocation;
        this.tooltip = tooltip;
        this.alignment = alignment;
    }

    public String getId() {
        return this.id;
    }

    public String getText() {
        return this.text;
    }

    public String getImageLocation() {
        return this.imageLocation;
    }

    public String getTooltip() {
        if (this.tooltip == null) {
            return this.text;
        } else {
            return this.tooltip;
        }
    }

    public HorizontalAlignment getAlignment() {
        return this.alignment;
    }

    @Override
    public String toString() {
        return this.text;
    }

    public enum HorizontalAlignment {
        LEFT,
        RIGHT;
    }

}