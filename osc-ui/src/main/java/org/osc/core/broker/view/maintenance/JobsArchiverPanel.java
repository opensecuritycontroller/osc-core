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
package org.osc.core.broker.view.maintenance;

import org.apache.log4j.Logger;
import org.osc.core.broker.service.api.ArchiveServiceApi;
import org.osc.core.broker.service.api.GetJobsArchiveServiceApi;
import org.osc.core.broker.service.api.UpdateJobsArchiveServiceApi;
import org.osc.core.broker.service.dto.JobsArchiveDto;
import org.osc.core.broker.service.request.BaseRequest;
import org.osc.core.broker.service.request.Request;
import org.osc.core.broker.service.response.BaseDtoResponse;
import org.osc.core.broker.view.common.StyleConstants;
import org.osc.core.broker.view.common.VmidcMessages;
import org.osc.core.broker.view.common.VmidcMessages_;
import org.osc.core.broker.view.util.ViewUtil;
import org.vaadin.risto.stepper.IntStepper;

import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Notification;
import com.vaadin.ui.OptionGroup;
import com.vaadin.ui.Panel;
import com.vaadin.ui.VerticalLayout;

public class JobsArchiverPanel extends CustomComponent {

    private static final long serialVersionUID = 1L;
    private static final Logger log = Logger.getLogger(JobsArchiverPanel.class);

    private VerticalLayout container = null;

    private Label triggerLabel = new Label("Triggering:");

    private String lastTriggerLabelText = "Last Triggered Timestamp: ";
    private Label lastTriggerTimestampLabel = null;
    private OptionGroup freqOpt = null;
    private CheckBox autoSchedChkBox = null;

    private Label thresholdLabel = new Label("Archive Jobs/Alerts older than the last: ");
    private OptionGroup thresOpt = null;
    private IntStepper archiveThreshold = null;

    private Panel panel = new Panel();

    private Button updateButton = null;
    private Button onDemandButton = null;

    private final String TEXT_FREQ_OPT_WEEKLY = "Weekly";
    private final String ID_FREQ_OPT_WEEKLY = "WEEKLY";
    private final String TEXT_FREQ_OPT_MONTHLY = "Monthly";
    private final String ID_FREQ_OPT_MONTHLY = "MONTHLY";

    private final String TEXT_THRES_OPT_MONTHS = "Months";
    private final String ID_THRES_OPT_MONTHS = "MONTHS";
    private final String TEXT_THRES_OPT_YEARS = "Years";
    private final String ID_THRES_OPT_YEARS = "YEARS";

    private final String UPDATE_SCHED_BUTTON_LABEL = "Update Schedule";
    private final String ON_DEMAND_BUTTON_LABEL = "On Demand";

    private String ARCHIVE_STYLE = "archive-options";
    private JobsArchiveDto dto;
    private final ArchiveLayout parentLayout;
    private final ArchiveServiceApi archiveService;
    private final GetJobsArchiveServiceApi getJobsArchiveService;
    private final UpdateJobsArchiveServiceApi updateJobsArchiveService;

    public JobsArchiverPanel(ArchiveLayout parentLayout, ArchiveServiceApi archiveService,
            GetJobsArchiveServiceApi getJobsArchiveService, UpdateJobsArchiveServiceApi updateJobsArchiveService) {
        super();
        this.parentLayout = parentLayout;
        this.archiveService = archiveService;
        this.getJobsArchiveService = getJobsArchiveService;
        this.updateJobsArchiveService = updateJobsArchiveService;

        try {
            this.dto = populateJobsArchiveDto().getDto();

            // create frequency layout component
            VerticalLayout freqLayout = new VerticalLayout();
            freqLayout.addComponent(createLastTriggerLabel());
            freqLayout.addComponent(createAutoSchedCheckBox());
            freqLayout.addComponent(createFrequencyOptionGroup());
            freqLayout.addStyleName(StyleConstants.COMPONENT_SPACING);
            freqLayout.setSpacing(true);

            // create triggering panel component
            Panel triggerPanel = new Panel();
            triggerPanel.setWidth("50%");
            triggerPanel.setContent(freqLayout);

            // create threshold layout component
            HorizontalLayout thresLayout = new HorizontalLayout();
            thresLayout.addComponent(this.thresholdLabel);
            thresLayout.addComponent(createArchiveThreshold());
            thresLayout.addComponent(createThresholdOptionGroup());
            thresLayout.addStyleName(StyleConstants.COMPONENT_SPACING);
            thresLayout.setSpacing(true);

            // add all components to container
            this.container = new VerticalLayout();
            this.container.addStyleName(StyleConstants.COMPONENT_SPACING);
            this.container.addComponent(this.triggerLabel);
            this.container.addComponent(triggerPanel);
            this.container.addComponent(thresLayout);

            // add buttons component to container
            HorizontalLayout buttonLayout = new HorizontalLayout();
            buttonLayout.addComponent(createOnDemandScheduleButton());
            buttonLayout.addComponent(createUpdateScheduleButton());
            buttonLayout.setSpacing(true);
            this.container.addComponent(buttonLayout);

            // add container to root panel
            this.container.setSpacing(true);
            this.panel.setWidth("100%");
            this.panel.setContent(this.container);
            setCompositionRoot(this.panel);

        } catch (Exception ex) {
            log.error("Failed to init archiver panel", ex);
        }
    }

    private BaseDtoResponse<JobsArchiveDto> populateJobsArchiveDto() throws Exception {
        BaseDtoResponse<JobsArchiveDto> res = this.getJobsArchiveService.dispatch(new Request() {
        });
        return res;
    }

    Integer convertToPositiveIntegerOrNull(String strVal) {
        Integer val = null;

        try {
            val = Integer.parseInt(strVal);
        } catch (Exception ex) {
            return null;
        }

        if (val < 0) {
            return null;
        }

        return val;
    }

    private Label createLastTriggerLabel() {
        String lastTriggerTimestamp = "None";
        this.lastTriggerTimestampLabel = new Label();

        if (this.dto != null && this.dto.getLastTriggerTimestamp() != null) {
            lastTriggerTimestamp = this.dto.getLastTriggerTimestamp().toString();
        }

        this.lastTriggerTimestampLabel.setCaption(this.lastTriggerLabelText + lastTriggerTimestamp);

        return this.lastTriggerTimestampLabel;
    }

    private CheckBox createAutoSchedCheckBox() {
        this.autoSchedChkBox = new CheckBox("Auto Schedule");
        this.autoSchedChkBox.setImmediate(true);

        if (this.dto == null) {
            this.autoSchedChkBox.setValue(false);
        } else {
            boolean isAuto = this.dto.getAutoSchedule();
            this.autoSchedChkBox.setValue(isAuto);
        }

        return this.autoSchedChkBox;
    }

    private OptionGroup createFrequencyOptionGroup() {

        this.freqOpt = new OptionGroup();
        this.freqOpt.addItem(this.ID_FREQ_OPT_WEEKLY);
        this.freqOpt.setItemCaption(this.ID_FREQ_OPT_WEEKLY, this.TEXT_FREQ_OPT_WEEKLY);
        this.freqOpt.addItem(this.ID_FREQ_OPT_MONTHLY);
        this.freqOpt.setItemCaption(this.ID_FREQ_OPT_MONTHLY, this.TEXT_FREQ_OPT_MONTHLY);
        this.freqOpt.addStyleName(this.ARCHIVE_STYLE);
        this.freqOpt.setImmediate(true);

        if (this.dto == null) {
            this.freqOpt.select(this.ID_FREQ_OPT_MONTHLY);
        } else {
            this.freqOpt.select(this.dto.getFrequency());
        }

        return this.freqOpt;
    }

    private IntStepper createArchiveThreshold() {

        this.archiveThreshold = new IntStepper();

        if (this.dto == null) {
            this.archiveThreshold.setValue(3);
        } else {
            this.archiveThreshold.setValue(this.dto.getThresholdValue());
        }

        this.archiveThreshold.setStepAmount(1);
        this.archiveThreshold.setMinValue(1);
        this.archiveThreshold.setRequiredError("Archive threshold cannot be empty");

        return this.archiveThreshold;
    }

    private OptionGroup createThresholdOptionGroup() {

        this.thresOpt = new OptionGroup();
        this.thresOpt.addItem(this.ID_THRES_OPT_MONTHS);
        this.thresOpt.setItemCaption(this.ID_THRES_OPT_MONTHS, this.TEXT_THRES_OPT_MONTHS);
        this.thresOpt.addItem(this.ID_THRES_OPT_YEARS);
        this.thresOpt.setItemCaption(this.ID_THRES_OPT_YEARS, this.TEXT_THRES_OPT_YEARS);

        this.thresOpt.addStyleName(this.ARCHIVE_STYLE);
        this.thresOpt.setImmediate(true);

        if (this.dto == null) {
            this.thresOpt.select(this.ID_THRES_OPT_MONTHS);
        } else {
            this.thresOpt.select(this.dto.getThresholdUnit());
        }

        return this.thresOpt;
    }

    @SuppressWarnings("serial")
    private Button createUpdateScheduleButton() {

        this.updateButton = new Button(this.UPDATE_SCHED_BUTTON_LABEL);

        this.updateButton.addClickListener(new ClickListener() {
            @Override
            public void buttonClick(ClickEvent event) {

                try {
                    BaseRequest<JobsArchiveDto> request = new BaseRequest<JobsArchiveDto>();
                    request.setDto(new JobsArchiveDto());
                    request.getDto().setId(JobsArchiverPanel.this.dto.getId());
                    request.getDto().setAutoSchedule(JobsArchiverPanel.this.autoSchedChkBox.getValue());
                    request.getDto().setFrequency(JobsArchiverPanel.this.freqOpt.getValue().toString());
                    request.getDto().setThresholdUnit(JobsArchiverPanel.this.thresOpt.getValue().toString());
                    request.getDto().setThresholdValue(JobsArchiverPanel.this.archiveThreshold.getValue());

                    JobsArchiverPanel.this.updateJobsArchiveService.dispatch(request);
                    JobsArchiverPanel.this.archiveService.maybeScheduleArchiveJob();

                    ViewUtil.iscNotification(
                            VmidcMessages.getString(VmidcMessages_.MAINTENANCE_JOBSARCHIVE_SUBMIT_SUCCESSFUL), null,
                            Notification.Type.TRAY_NOTIFICATION);

                } catch (Exception e) {

                    log.error("Fail to update archive schedule", e);
                    ViewUtil.iscNotification(
                            VmidcMessages.getString(VmidcMessages_.MAINTENANCE_JOBSARCHIVE_SUBMIT_FAILED),
                            Notification.Type.ERROR_MESSAGE);
                }
            }
        });

        return this.updateButton;
    }

    @SuppressWarnings("serial")
    private Button createOnDemandScheduleButton() {

        this.onDemandButton = new Button(this.ON_DEMAND_BUTTON_LABEL);

        this.onDemandButton.addClickListener(new ClickListener() {
            @Override
            public void buttonClick(ClickEvent event) {

                JobsArchiverPanel.this.onDemandButton.setEnabled(false);
                ViewUtil.iscNotification("On demand archiving started in the background", null,
                        Notification.Type.TRAY_NOTIFICATION);

                Thread thread = new Thread(new Runnable() {

                    @Override
                    public void run() {
                        BaseRequest<JobsArchiveDto> request = new BaseRequest<JobsArchiveDto>();
                        request.setDto(new JobsArchiveDto());
                        request.getDto().setFrequency(JobsArchiverPanel.this.freqOpt.getValue().toString());
                        request.getDto().setThresholdUnit(JobsArchiverPanel.this.thresOpt.getValue().toString());
                        request.getDto().setThresholdValue(JobsArchiverPanel.this.archiveThreshold.getValue());

                        try {
                            JobsArchiverPanel.this.archiveService.dispatch(request);

                            ViewUtil.iscNotification(
                                    VmidcMessages.getString(VmidcMessages_.MAINTENANCE_JOBSARCHIVE_ONDEMAND_SUCCESSFUL), null,
                                    Notification.Type.TRAY_NOTIFICATION);

                            JobsArchiverPanel.this.parentLayout.buildArchivesTable();
                            JobsArchiverPanel.this.onDemandButton.setEnabled(true);

                        } catch (Exception e) {
                            ViewUtil.showError("Error starting on demand archiving", e);
                        }
                    }
                });
                thread.start();

            }
        });

        return this.onDemandButton;
    }
}