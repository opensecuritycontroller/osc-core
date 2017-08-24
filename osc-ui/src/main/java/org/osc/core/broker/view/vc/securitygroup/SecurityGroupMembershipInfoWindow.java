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

import java.util.Arrays;
import java.util.Date;
import java.util.Random;

import org.apache.log4j.Logger;
import org.osc.core.broker.service.dto.SecurityGroupDto;
import org.osc.core.broker.window.VmidcWindow;
import org.osc.core.broker.window.button.CloseButtonModel;

import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.ui.Component;
import com.vaadin.ui.TreeTable;
import com.vaadin.ui.VerticalLayout;

public class SecurityGroupMembershipInfoWindow extends VmidcWindow<CloseButtonModel> {

    private static final long serialVersionUID = 1L;

    final String CAPTION = "Member Information";

    private static final Logger log = Logger.getLogger(SecurityGroupMembershipInfoWindow.class);

    private final SecurityGroupDto currentSecurityGroup;

    public SecurityGroupMembershipInfoWindow(SecurityGroupDto sg) {
        super(new CloseButtonModel());
        this.currentSecurityGroup = sg;
        setCaption(sg.getName() + " - " + this.CAPTION);
        setContent(getContent());
    }

    @Override
    public Component getContent() {
        VerticalLayout content = new VerticalLayout();
        content.setMargin(new MarginInfo(true, true, false, true));

	    TreeTable sample = new TreeTable();
        sample.setSizeFull();

        sample.addContainerProperty("NAME", String.class, "");
        sample.addContainerProperty("HOURS", Integer.class, 0);
        sample.addContainerProperty("MODIFIED", Date.class, new Date());

        populateWithRandomHierarchicalData(sample);
        content.addComponent(sample);
        return content;
    }

    @SuppressWarnings("unchecked")
    private void populateWithRandomHierarchicalData(final TreeTable sample) {
        final Random random = new Random();
        int hours = 0;
        final Object allProjects = sample.addItem(new Object[] {
                "All Projects", 0, new Date() }, null);
        for (final int year : Arrays.asList(2010, 2011, 2012, 2013)) {
            int yearHours = 0;
            final Object yearId = sample.addItem(new Object[] { "Year " + year,
                    yearHours, new Date() }, null);
            sample.setParent(yearId, allProjects);
            for (int project = 1; project < random.nextInt(4) + 2; project++) {
                int projectHours = 0;
                final Object projectId = sample.addItem(
                        new Object[] { "Customer Project " + project,
                                projectHours, new Date() }, null);
                sample.setParent(projectId, yearId);
                for (final String phase : Arrays.asList("Implementation",
                        "Planning", "Prototype")) {
                    final int phaseHours = random.nextInt(50);
                    final Object phaseId = sample.addItem(new Object[] { phase,
                            phaseHours, new Date() }, null);
                    sample.setParent(phaseId, projectId);
                    sample.setChildrenAllowed(phaseId, false);
                    sample.setCollapsed(phaseId, false);
                    projectHours += phaseHours;
                }
                yearHours += projectHours;
                sample.getItem(projectId).getItemProperty("HOURS")
                        .setValue(projectHours);
                sample.setCollapsed(projectId, false);
            }
            hours += yearHours;
            sample.getItem(yearId).getItemProperty("HOURS")
                    .setValue(yearHours);
            sample.setCollapsed(yearId, false);
        }
        sample.getItem(allProjects).getItemProperty("HOURS")
                .setValue(hours);
        sample.setCollapsed(allProjects, false);
    }

}
