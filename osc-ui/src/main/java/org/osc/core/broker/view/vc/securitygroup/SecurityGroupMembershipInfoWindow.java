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
package org.osc.core.broker.view.vc.securitygroup;

import java.util.Set;

import org.apache.log4j.Logger;
import org.osc.core.broker.service.api.ListSecurityGroupMembersBySgServiceApi;
import org.osc.core.broker.service.dto.PortDto;
import org.osc.core.broker.service.dto.SecurityGroupDto;
import org.osc.core.broker.service.dto.SecurityGroupMemberItemDto;
import org.osc.core.broker.service.request.BaseIdRequest;
import org.osc.core.broker.view.common.VmidcMessages;
import org.osc.core.broker.view.common.VmidcMessages_;
import org.osc.core.broker.view.util.ViewUtil;
import org.osc.core.broker.window.VmidcWindow;
import org.osc.core.broker.window.button.CloseButtonModel;

import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.ui.Component;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Notification.Type;
import com.vaadin.ui.TreeTable;
import com.vaadin.ui.VerticalLayout;

public class SecurityGroupMembershipInfoWindow extends VmidcWindow<CloseButtonModel> {

    private static final long serialVersionUID = 1L;

    private static final int OPENSTACK_ID_SHORTENED_LENGTH = 13;

    private static final int NAME_COLUMN_WIDTH = 150;
    private static final int TYPE_COLUMN_WIDTH = 60;
    private static final int IP_COLUMN_WIDTH = 155;
    private static final int MAC_COLUMN_WIDTH = 100;

    private static final int WINDOW_WIDTH = NAME_COLUMN_WIDTH + TYPE_COLUMN_WIDTH + IP_COLUMN_WIDTH + MAC_COLUMN_WIDTH
            + 150;

    private static final String PROPERTY_ID_MEMBER_NAME = "name";
    private static final String PROPERTY_ID_MEMBER_TYPE = "type";
    private static final String PROPERTY_ID_MEMBER_IP = "ip";
    private static final String PROPERTY_ID_MEMBER_MAC = "mac";

    private static final Logger log = Logger.getLogger(SecurityGroupMembershipInfoWindow.class);

    private final SecurityGroupDto currentSecurityGroup;
    private ListSecurityGroupMembersBySgServiceApi listSecurityGroupMembersBySgService;
    private TreeTable treeTable;

    public SecurityGroupMembershipInfoWindow(SecurityGroupDto sg,
            ListSecurityGroupMembersBySgServiceApi listSecurityGroupMembersBySgService) throws Exception {
        super(new CloseButtonModel());
        setWidth(WINDOW_WIDTH, Unit.PIXELS);
        this.currentSecurityGroup = sg;
        this.listSecurityGroupMembersBySgService = listSecurityGroupMembersBySgService;
        setCaption(sg.getName() + " - " + VmidcMessages.getString(VmidcMessages_.SG_MEMBERSHIP_CAPTION));
        setContent(getTreeTable());
    }

    public Component getTreeTable() throws Exception {
        VerticalLayout content = new VerticalLayout();
        content.setMargin(new MarginInfo(true, true, false, true));

        this.treeTable = new TreeTable();
        this.treeTable.setPageLength(10);
        this.treeTable.setSelectable(false);
        this.treeTable.setSizeFull();

        this.treeTable.addContainerProperty(PROPERTY_ID_MEMBER_NAME, String.class, "");
        this.treeTable.addContainerProperty(PROPERTY_ID_MEMBER_TYPE, String.class, "");
        this.treeTable.addContainerProperty(PROPERTY_ID_MEMBER_IP, String.class, "");
        this.treeTable.addContainerProperty(PROPERTY_ID_MEMBER_MAC, String.class, "");

        this.treeTable.setColumnHeader(PROPERTY_ID_MEMBER_NAME, VmidcMessages.getString(VmidcMessages_.NAME));
        this.treeTable.setColumnHeader(PROPERTY_ID_MEMBER_TYPE, VmidcMessages.getString(VmidcMessages_.OS_MEMBER_TYPE));
        this.treeTable.setColumnHeader(PROPERTY_ID_MEMBER_MAC, VmidcMessages.getString(VmidcMessages_.GENERAL_MACADDR));
        this.treeTable.setColumnHeader(PROPERTY_ID_MEMBER_IP, VmidcMessages.getString(VmidcMessages_.GENERAL_IPADDR));

        this.treeTable.setColumnWidth(PROPERTY_ID_MEMBER_NAME, NAME_COLUMN_WIDTH);
        this.treeTable.setColumnWidth(PROPERTY_ID_MEMBER_TYPE, TYPE_COLUMN_WIDTH);
        this.treeTable.setColumnWidth(PROPERTY_ID_MEMBER_MAC, MAC_COLUMN_WIDTH);
        this.treeTable.setColumnWidth(PROPERTY_ID_MEMBER_IP, IP_COLUMN_WIDTH);

        populateData();
        content.addComponent(this.treeTable);
        return content;
    }

    private void populateData() {
        try {
            Set<SecurityGroupMemberItemDto> members = this.listSecurityGroupMembersBySgService
                    .dispatch(new BaseIdRequest(this.currentSecurityGroup.getId())).getSet();

            if (members.isEmpty()) {
                ViewUtil.iscNotification(null, VmidcMessages.getString(VmidcMessages_.SG_NO_MEMBERS),
                        Type.WARNING_MESSAGE);
            } else {
                for (SecurityGroupMemberItemDto member : members) {
                    Object memberItem = this.treeTable
                            .addItem(new Object[] { member.getName(), member.getType(), "", "" }, null);
                    this.treeTable.setCollapsed(memberItem, member.getPorts().isEmpty());
                    this.treeTable.setChildrenAllowed(memberItem, !member.getPorts().isEmpty());

                    for (PortDto port : member.getPorts()) {
                        String ipAddressString = String.join(", ", port.getIpAddresses());
                        String shortId = port.getOpenstackId().length() > OPENSTACK_ID_SHORTENED_LENGTH ?
                                port.getOpenstackId().substring(0, OPENSTACK_ID_SHORTENED_LENGTH) :
                                    port.getOpenstackId();
                                Object portItem = this.treeTable.addItem(
                                        new Object[] { shortId, "",
                                                ipAddressString, port.getMacAddress() },
                                        null);
                                this.treeTable.setChildrenAllowed(portItem, false);
                                this.treeTable.setParent(portItem, memberItem);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Unable to list security group members for SG ID: " + this.currentSecurityGroup.getId(), e);
            ViewUtil.iscNotification(e.getMessage(), Notification.Type.ERROR_MESSAGE);
        }
    }

}
