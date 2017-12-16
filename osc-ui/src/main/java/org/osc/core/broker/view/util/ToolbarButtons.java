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
package org.osc.core.broker.view.util;



public enum ToolbarButtons {
    ADD("Add", "Add", "img/add.png"),
    EDIT("Edit", "Edit", "img/edit.png"),
    DELETE("Delete", "Delete", "img/delete.png"),
    CONFORM("Sync", "Sync", "img/conform.png"),
    JOB_VIEW("Job Graph", "Job_Graph", "img/job-graph.png"),
    JOB_ABORT("Abort", "Abort", "img/cancel.png"),
    APPLIANCE_STATUS("Appliance Status", "Appliance_Status", "img/status.png"),
    VSS_DEPLOY("Deploy", "Deploy", "img/deploy.png"),
    AUTO_IMPORT_APPLIANCE("Auto Import", "Auto_Import", "img/deploy.png"),
    BACK("Back", "Back_to_Previous_View", "img/back.png"),
    ADD_CHILD("Add", "Add_Child", "img/add.png"),
    EDIT_CHILD("Edit", "Edit_Child", "img/edit.png"),
    DELETE_CHILD("Delete", "Delete_Child", "img/delete.png"),
    DEPLOYMENTS("Deployments...", "Deployments", "img/drilldown.png"),
    SECURITY_GROUP_INTERFACES("Traffic Policy Mappings ...", "Security_Group_Interfaces", "img/drilldown.png"),
    BIND_SECURITY_GROUP("Bind", "bind"),
    SECURITY_GROUP_MEMBERSHIP("Membership", "membership"),
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