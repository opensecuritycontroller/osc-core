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

import org.osc.core.broker.service.api.ListSecurityGroupMembersBySgServiceApi;
import org.osc.core.broker.service.dto.PortDto;
import org.osc.core.broker.service.dto.SecurityGroupDto;
import org.osc.core.broker.service.dto.SecurityGroupMemberItemDto;
import org.osc.core.broker.service.request.BaseIdRequest;
import org.osc.core.broker.window.VmidcWindow;
import org.osc.core.broker.window.button.CloseButtonModel;

import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.ui.Component;
import com.vaadin.ui.TreeTable;
import com.vaadin.ui.VerticalLayout;

public class SecurityGroupMembershipInfoWindow extends VmidcWindow<CloseButtonModel> {

    private static final long serialVersionUID = 1L;

    private final String CAPTION = "Member Information";

    private final SecurityGroupDto currentSecurityGroup;
    private ListSecurityGroupMembersBySgServiceApi listSecurityGroupMembersBySgService;
    private TreeTable treeTable;

    public SecurityGroupMembershipInfoWindow(SecurityGroupDto sg,
            ListSecurityGroupMembersBySgServiceApi listSecurityGroupMembersBySgService) throws Exception {
        super(new CloseButtonModel());
        this.currentSecurityGroup = sg;
        this.listSecurityGroupMembersBySgService = listSecurityGroupMembersBySgService;
        setCaption(sg.getName() + " - " + this.CAPTION);
        setContent(getTreeTable());
    }

    public Component getTreeTable() throws Exception {
        VerticalLayout content = new VerticalLayout();
        content.setMargin(new MarginInfo(true, true, false, true));

        this.treeTable = new TreeTable();
        this.treeTable.setPageLength(10);
        this.treeTable.setSelectable(false);
        this.treeTable.setSizeFull();

        this.treeTable.addContainerProperty("name", String.class, "");
        this.treeTable.addContainerProperty("type", String.class, "");
        this.treeTable.addContainerProperty("mac", String.class, "");
        this.treeTable.addContainerProperty("ip", String.class, "");

        this.treeTable.setColumnHeader("name", "Name");
        this.treeTable.setColumnHeader("type", "Type");
        this.treeTable.setColumnHeader("mac", "Mac Address");
        this.treeTable.setColumnHeader("ip", "IP Address");

        this.treeTable.setColumnWidth("name", 150);
        this.treeTable.setColumnWidth("type", 50);
        this.treeTable.setColumnWidth("mac", 100);
        this.treeTable.setColumnWidth("ip", -1);

        populateData(this.treeTable);
        content.addComponent(this.treeTable);
        return content;
    }

    private void populateData(final TreeTable treeTable) throws Exception {
        Set<SecurityGroupMemberItemDto> members = this.listSecurityGroupMembersBySgService
                .dispatch(new BaseIdRequest(this.currentSecurityGroup.getId())).getSet();

        for (SecurityGroupMemberItemDto member : members) {
            Object memberItem = treeTable.addItem(new Object[] { member.getName(), member.getType(), "", "" },
                    null);
            treeTable.setCollapsed(memberItem, member.getPorts().isEmpty());
            treeTable.setChildrenAllowed(memberItem, !member.getPorts().isEmpty());

            for (PortDto port : member.getPorts()) {
                Object portItem = treeTable.addItem(new Object[] { port.getOpenstackId(), "", port.getMacAddress(),
                        String.join(", ", port.getIpAddresses()) },null);
                treeTable.setChildrenAllowed(portItem, false);
                treeTable.setParent(portItem, memberItem);
            }
        }
    }

}
