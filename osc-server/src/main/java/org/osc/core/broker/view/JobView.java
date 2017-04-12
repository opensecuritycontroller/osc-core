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
package org.osc.core.broker.view;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.NullInputStream;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.osc.core.broker.job.JobEngine;
import org.osc.core.broker.model.entities.job.TaskGuard;
import org.osc.core.broker.model.entities.job.TaskRecord;
import org.osc.core.broker.model.entities.job.TaskState;
import org.osc.core.broker.model.entities.job.TaskStatus;
import org.osc.core.broker.service.ListJobService;
import org.osc.core.broker.service.ListTaskService;
import org.osc.core.broker.service.dto.JobRecordDto;
import org.osc.core.broker.service.dto.TaskRecordDto;
import org.osc.core.broker.service.persistence.TaskEntityMgr;
import org.osc.core.broker.service.request.ListJobRequest;
import org.osc.core.broker.service.request.ListTaskRequest;
import org.osc.core.broker.service.response.ListResponse;
import org.osc.core.broker.util.BroadcastMessage;
import org.osc.core.broker.util.SessionUtil;
import org.osc.core.broker.util.db.HibernateUtil;
import org.osc.core.broker.view.common.VmidcMessages;
import org.osc.core.broker.view.common.VmidcMessages_;
import org.osc.core.broker.view.util.ToolbarButtons;
import org.osc.core.broker.view.util.ViewUtil;
import org.osc.core.rest.client.util.LoggingUtil;
import org.osgi.service.transaction.control.ScopedWorkException;

import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.vaadin.data.util.BeanContainer;
import com.vaadin.data.util.BeanItem;
import com.vaadin.navigator.ViewChangeListener;
import com.vaadin.server.StreamResource;
import com.vaadin.server.StreamResource.StreamSource;
import com.vaadin.shared.ui.window.WindowMode;
import com.vaadin.ui.AbstractSelect.ItemDescriptionGenerator;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.Component;
import com.vaadin.ui.CustomTable;
import com.vaadin.ui.CustomTable.ColumnGenerator;
import com.vaadin.ui.Embedded;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Notification;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;

import elemental.events.KeyboardEvent.KeyCode;

public class JobView extends CRUDBaseView<JobRecordDto, TaskRecordDto> {

    private static final String TASK_PREDECESSORS_COLUMN_ID = "predecessors";
    private static final String TASK_DEPENDENCY_ORDER_COLUMN_ID = "dependencyOrder";
    private static final String TASK_FAIL_REASON_COLUMN_ID = "failReason";
    private static final String JOB_QUEUED_COLUMN_ID = "queued";
    private static final String JOB_COMPLETED_COLUMN_ID = "completed";
    private static final String JOB_STARTED_COLUMN_ID = "started";
    private static final String JOB_STATUS_COLUMN_ID = "status";
    private static final String JOB_STATE_COLUMN_ID = "state";
    private static final String JOB_OBJECTS_COLUMN_ID = "objects";
    private static final String JOB_NAME_COLUMN_ID = "name";
    private static final String JOB_FAIL_REASON_COLUMN_ID = "failureReason";
    private static final String JOB_ID_COLUMN_ID = "id";
    private static final String JOB_SUBMITTED_BY_COLUMN_ID = "submittedBy";

    private static final String JOB_HELP_GUID = "GUID-005C8EBC-FABB-4F94-B82A-8F760EFDE69F.html";

    private static final Logger log = Logger.getLogger(JobView.class);

    private static final long serialVersionUID = 1L;

    private File dotFile;
    private Embedded embeddedImage;

    public JobView() {
        createView("Jobs", Arrays.asList(ToolbarButtons.JOB_VIEW, ToolbarButtons.JOB_ABORT), "Tasks", null);
    }

    @Override
    public void buttonClicked(ClickEvent event) {
        if (event.getButton().getId().equals(ToolbarButtons.JOB_VIEW.getId())) {
            buildGraph();
        } else if (event.getButton().getId().equals(ToolbarButtons.JOB_ABORT.getId())) {
            JobEngine.getEngine().abortJob(getParentItemId(),
                    VmidcMessages.getString(VmidcMessages_.JOB_ABORT_USER, SessionUtil.getCurrentUser()));
        }
    }

    @SuppressWarnings("serial")
    @Override
    public void initParentTable() {
        this.parentContainer = new BeanContainer<Long, JobRecordDto>(JobRecordDto.class);
        this.parentTable.setContainerDataSource(this.parentContainer);
        this.parentTable.setVisibleColumns(JOB_ID_COLUMN_ID, JOB_NAME_COLUMN_ID, JOB_OBJECTS_COLUMN_ID,
                JOB_STATE_COLUMN_ID, JOB_STATUS_COLUMN_ID, JOB_STARTED_COLUMN_ID, JOB_COMPLETED_COLUMN_ID,
                JOB_FAIL_REASON_COLUMN_ID, JOB_QUEUED_COLUMN_ID, JOB_SUBMITTED_BY_COLUMN_ID);
        // Hide job fail reason column by default
        this.parentTable.setColumnCollapsed(JOB_FAIL_REASON_COLUMN_ID, true);

        this.parentTable.addGeneratedColumn(JOB_OBJECTS_COLUMN_ID, new ColumnGenerator() {
            @Override
            public Object generateCell(CustomTable source, Object itemId, Object columnId) {
                JobRecordDto jobDto = JobView.this.parentContainer.getItem(itemId).getBean();
                return ViewUtil.generateObjectLink(jobDto.getObjects());
            }
        });

        // Add a tooltip to the error column so the user is able to see the
        // complete error message
        this.parentTable.setItemDescriptionGenerator(new ItemDescriptionGenerator() {

            @Override
            public String generateDescription(Component source, Object itemId, Object propertyId) {
                Object errorMessage = getParentContainer().getContainerProperty(itemId, JOB_FAIL_REASON_COLUMN_ID)
                        .getValue();
                if (errorMessage != null && errorMessage instanceof String) {
                    return SafeHtmlUtils.fromString(errorMessage.toString()).asString();
                } else {
                    return null;
                }

            }
        });

        this.parentTable.setColumnHeader(JOB_ID_COLUMN_ID, "Id");
        this.parentTable.setColumnHeader(JOB_NAME_COLUMN_ID, "Name");
        this.parentTable.setColumnHeader(JOB_OBJECTS_COLUMN_ID, "Objects");
        this.parentTable.setColumnHeader(JOB_STATE_COLUMN_ID, "State");
        this.parentTable.setColumnHeader(JOB_STATUS_COLUMN_ID, "Status");
        this.parentTable.setColumnHeader(JOB_STARTED_COLUMN_ID, "Started");
        this.parentTable.setColumnHeader(JOB_COMPLETED_COLUMN_ID, "Completed");
        this.parentTable.setColumnHeader(JOB_QUEUED_COLUMN_ID, "Queued");
        this.parentTable.setColumnHeader(JOB_FAIL_REASON_COLUMN_ID, "Failure Reason");
        this.parentTable.setColumnHeader(JOB_SUBMITTED_BY_COLUMN_ID, "Submitted By");
    }

    @Override
    public void populateParentTable() {

        ListJobRequest listRequest = null;
        ListResponse<JobRecordDto> res;
        ListJobService listService = new ListJobService();
        try {
            res = listService.dispatch(listRequest);
            List<JobRecordDto> listResponse = res.getList();
            this.parentContainer.removeAllItems();
            // creating table with list of jobs
            for (JobRecordDto j : listResponse) {
                this.parentContainer.addItem(j.getId(), j);
            }

        } catch (Exception e) {
            log.error("Fail to populate Jobs table", e);
            ViewUtil.iscNotification("Fail to populate Job table (" + e.getMessage() + ")",
                    Notification.Type.ERROR_MESSAGE);
        }

    }

    @Override
    public void parentTableClicked(long parentItemId) {
        super.parentTableClicked(parentItemId);
        if (parentItemId != CRUDBaseView.NULL_SELECTION_ITEM_ID) {
            JobRecordDto jobRecordDto = this.parentContainer.getItem(parentItemId).getBean();
            updateAbortButtonState(jobRecordDto);
        } else {
            updateAbortButtonState(null);
        }
    }

    // This is also needed since Abort button should change to disabled if job state
    // changes before user clicks on the table
    @Override
    protected void syncParentTable(BroadcastMessage msg) throws Exception {
        super.syncParentTable(msg);
        BeanItem<JobRecordDto> item = this.parentContainer.getItem(msg.getEntityId());
        JobRecordDto jobRecordDto = null;
        if (item != null) {
            jobRecordDto = item.getBean();
        }
        updateAbortButtonState(jobRecordDto);
    }

    private void updateAbortButtonState(JobRecordDto jobRecordDto) {
        if (jobRecordDto == null || jobRecordDto.getState().isTerminalState()) {
            ViewUtil.enableToolBarButtons(false, this.parentToolbar, Arrays.asList(ToolbarButtons.JOB_ABORT.getId()));
        }
    }

    @SuppressWarnings("serial")
    @Override
    public void initChildTable() {
        this.childContainer = new BeanContainer<Long, TaskRecordDto>(TaskRecordDto.class);
        this.childTable.setContainerDataSource(this.childContainer);
        this.childTable.setVisibleColumns(TASK_DEPENDENCY_ORDER_COLUMN_ID, JOB_NAME_COLUMN_ID, JOB_OBJECTS_COLUMN_ID,
                JOB_STATE_COLUMN_ID, JOB_STATUS_COLUMN_ID, JOB_STARTED_COLUMN_ID, JOB_COMPLETED_COLUMN_ID,
                TASK_FAIL_REASON_COLUMN_ID, TASK_PREDECESSORS_COLUMN_ID, JOB_ID_COLUMN_ID);

        this.childTable.addGeneratedColumn(JOB_OBJECTS_COLUMN_ID, new ColumnGenerator() {
            @Override
            public Object generateCell(CustomTable source, Object itemId, Object columnId) {
                TaskRecordDto taskDto = JobView.this.childContainer.getItem(itemId).getBean();
                return ViewUtil.generateObjectLink(taskDto.getObjects());
            }
        });

        this.childTable.setColumnHeader(TASK_DEPENDENCY_ORDER_COLUMN_ID, "Order");
        this.childTable.setColumnHeader(JOB_NAME_COLUMN_ID, "Name");
        this.childTable.setColumnHeader(JOB_OBJECTS_COLUMN_ID, "Objects");
        this.childTable.setColumnHeader(JOB_STATE_COLUMN_ID, "State");
        this.childTable.setColumnHeader(JOB_STATUS_COLUMN_ID, "Status");
        this.childTable.setColumnHeader(JOB_STARTED_COLUMN_ID, "Started");
        this.childTable.setColumnHeader(JOB_COMPLETED_COLUMN_ID, "Completed");
        this.childTable.setColumnHeader(TASK_FAIL_REASON_COLUMN_ID, "Error");
        this.childTable.setColumnHeader(TASK_PREDECESSORS_COLUMN_ID, "Predecessors");
        this.childTable.setColumnHeader(JOB_ID_COLUMN_ID, "Id");

        // Add a tooltip to the error column so the user is able to see the
        // complete error message
        this.childTable.setItemDescriptionGenerator(new ItemDescriptionGenerator() {

            @Override
            public String generateDescription(Component source, Object itemId, Object propertyId) {
                Object errorMessage = getChildContainer().getContainerProperty(itemId, TASK_FAIL_REASON_COLUMN_ID)
                        .getValue();
                if (errorMessage != null && errorMessage instanceof String) {
                    return SafeHtmlUtils.fromString(errorMessage.toString()).asString();
                } else {
                    return null;
                }

            }
        });
    }

    @Override
    public void populateChildTable(BeanItem<JobRecordDto> parentItem) {
        if (parentItem != null) {
            try {
                ListTaskRequest listRequest = new ListTaskRequest();
                listRequest.setJobId(getParentItemId());
                ListTaskService listService = new ListTaskService();
                ListResponse<TaskRecordDto> res = listService.dispatch(listRequest);

                this.childContainer.removeAllItems();
                for (TaskRecordDto task : res.getList()) {
                    this.childContainer.addItem(task.getId(), task);
                }

            } catch (Exception e) {
                log.error("Fail to populate Task Table", e);
                ViewUtil.iscNotification("Fail to populate Task table (" + e.getMessage() + ")",
                        Notification.Type.ERROR_MESSAGE);
            }
        } else {
            this.childContainer.removeAllItems();
            ViewUtil.setButtonsEnabled(false, this.childToolbar);
        }
    }

    private void refreshGraph() throws Exception {
        StreamResource imageResource = buildImageResource();
        this.embeddedImage.setIcon(imageResource);
    }

    @SuppressWarnings("serial")
    private void buildGraph() {
        try {
            this.embeddedImage = new Embedded();
            this.embeddedImage.setSizeFull();
            refreshGraph();

            Button refresh = new Button("Refresh");
            refresh.addClickListener(new ClickListener() {
                @Override
                public void buttonClick(ClickEvent event) {
                    try {
                        refreshGraph();
                    } catch (Exception e) {
                        ViewUtil.showError("Error while building task graph DOT file.", e);
                    }
                }
            });

            final HorizontalLayout toolbarLayout = new HorizontalLayout();
            toolbarLayout.addComponent(refresh);
            toolbarLayout.setSizeFull();
            toolbarLayout.setMargin(true);

            final VerticalLayout imageLayout = new VerticalLayout();
            imageLayout.addComponent(this.embeddedImage);
            imageLayout.setComponentAlignment(this.embeddedImage, Alignment.MIDDLE_CENTER);
            imageLayout.setSizeUndefined();

            final VerticalLayout layout = new VerticalLayout();
            layout.addComponent(refresh);
            layout.setComponentAlignment(refresh, Alignment.TOP_LEFT);
            layout.addComponent(imageLayout);
            layout.setSizeUndefined();

            final Window window = new Window();
            window.setContent(layout);
            window.setModal(true);
            window.setHeight("80%");
            window.setWidth("80%");
            window.setClosable(true);
            window.setResizable(true);
            window.setCaption("Task Graph for Job " + getParentItemId());
            window.center();
            window.setWindowMode(WindowMode.MAXIMIZED);
            window.setCloseShortcut(KeyCode.ESC, null);

            ViewUtil.addWindow(window);

            window.focus();

        } catch (Exception e) {
            ViewUtil.showError("Error while building task graph DOT file.", e);
        }
    }

    @SuppressWarnings("resource")
    private StreamResource buildImageResource() throws Exception {
        try {
            this.dotFile = new File("job-" + getParentItemId() + System.currentTimeMillis() + ".dot");

            PrintWriter out = new PrintWriter(new FileWriter(this.dotFile));
            out.println("digraph G {");
            out.println();

            out.println("compound=true");
            out.println("rankdir=TB");
            out.println("bgcolor=white; fontcolor=black; fontname=Helvetica; fontsize=9.0");
            out.println();

            out.println("edge [color=black, fontcolor=black, fontname=Helvetica, fontsize=9.0]");
            out.println();

            out.println("node [color=black, fontcolor=black, fontname=\"Helvetica\", fontsize=11.0, shape=record, style=\"solid,filled\"]");
            out.println();

            EntityManager em = HibernateUtil.getTransactionalEntityManager();
            HibernateUtil.getTransactionControl().required(() -> {
                TaskEntityMgr emgr = new TaskEntityMgr(em);
                for (TaskRecord tr : emgr.getTasksByJobId(getParentItemId())) {
                    out.printf("node_%d [%n", tr.getId());
                    out.printf("  label=\"{%d) %s}\"%n", tr.getDependencyOrder(), tr.getName());
                    if(org.osc.core.broker.service.dto.job.TaskState.valueOf(tr.getState().name()).isTerminalState()) {
                        if (tr.getStatus().equals(TaskStatus.PASSED)) {
                            if (tr.getChildren().isEmpty()) {
                                out.printf("  fillcolor=%s fontcolor=white%n", "green4");
                            } else {
                                out.printf("  fillcolor=%s%n fontname=\"Helvetica-Bold\"", "green");
                            }
                        } else if (tr.getStatus().equals(TaskStatus.FAILED)) {
                            out.printf("  fillcolor=%s%n", "red");
                        } else if (tr.getStatus().equals(TaskStatus.SKIPPED)) {
                            out.printf("  fillcolor=%s%n", "gray");
                        } else {
                            out.printf("  fillcolor=%s%n", "white");
                        }
                    } else {
                        if (tr.getState().equals(TaskState.QUEUED)) {
                            out.printf("  fillcolor=%s%n", "orange");
                        } else if (tr.getState().equals(TaskState.PENDING)) {
                            out.printf("  fillcolor=%s%n", "lightblue");
                        } else if (tr.getState().equals(TaskState.NOT_RUNNING)) {
                            out.printf("  fillcolor=%s%n", "white");
                        } else {
                            out.printf("  fillcolor=%s%n", "yellow");
                        }
                    }
                    if (!tr.getChildren().isEmpty()) {
                        out.printf("  style=\"rounded,filled\"%n");
                    }
                    out.println("]");
                    out.println();
                }

                for (TaskRecord tr : emgr.getTasksByJobId(getParentItemId())) {
                    String executionDependencyAttr = "[color=black arrowhead=empty]";
                    if (tr.getTaskGaurd().equals(TaskGuard.ALL_ANCESTORS_SUCCEEDED)) {
                        executionDependencyAttr = "[color=magenta arrowhead=normal]";
                    } else if (tr.getTaskGaurd().equals(TaskGuard.ALL_PREDECESSORS_SUCCEEDED)) {
                        executionDependencyAttr = "[color=black arrowhead=normal]";
                    }
                    for (TaskRecord tr1 : tr.getPredecessors()) {
                        out.printf("node_%s -> node_%s %s", tr1.getId(), tr.getId(), executionDependencyAttr);
                    }

                    for (TaskRecord tr1 : tr.getChildren()) {
                        out.printf("node_%s -> node_%s %s", tr1.getId(), tr.getId(), "[color=gray arrowhead=none style=dashed]");
                    }
                }
                return null;
            });

            out.println("}");

            out.flush();
            out.close();

            @SuppressWarnings("serial")
            StreamSource streamSource = new StreamResource.StreamSource() {
                @Override
                public InputStream getStream() {
                    byte[] imageStream = getImageStream(JobView.this.dotFile, "png");
                    if (imageStream != null) {
                        ByteArrayInputStream bytes = new ByteArrayInputStream(imageStream);
                        JobView.this.dotFile.delete();
                        return bytes;
                    }
                    JobView.this.dotFile.delete();
                    return new NullInputStream(0);
                }
            };
            StreamResource imageResource = new StreamResource(streamSource, "job" + System.currentTimeMillis() + ".png");
            imageResource.setCacheTime(0);
            return imageResource;

        } catch (ScopedWorkException swe) {
            throw swe.as(Exception.class);
        }
    }

    private byte[] getImageStream(File dot, String type) {
        byte[] imgStream = null;
        FileInputStream in = null;

        try {
            File imgFile = getImageFile(dot, type);
            in = new FileInputStream(imgFile.getAbsolutePath());
            imgStream = new byte[in.available()];
            in.read(imgStream);
            in.close();

            if (!imgFile.delete()) {
                log.error("Warning: " + imgFile.getAbsolutePath() + " could not be deleted!");
            }

        } catch (Exception ex) {
            log.error("Fail to create graph", ex);
            ViewUtil.iscNotification("Fail to create Job graph (" + ex.getMessage() + ")",
                    Notification.Type.ERROR_MESSAGE);
        } finally {
            IOUtils.closeQuietly(in);
        }

        return imgStream;
    }

    private File getImageFile(File dot, String type) throws Exception {
        File imgFile = null;
        try {
            imgFile = File.createTempFile("graph_", "." + type, new File("."));
            Runtime rt = Runtime.getRuntime();

            String[] args = { "dot", "-T" + type, dot.getAbsolutePath(), "-o", imgFile.getAbsolutePath() };
            Process process = rt.exec(args);
            process.waitFor();

            try(InputStreamReader inp = new InputStreamReader(process.getInputStream());
                BufferedReader kbdInput = new BufferedReader(inp)){
                String line;
                while ((line = kbdInput.readLine()) != null) {
                    log.info(LoggingUtil.removeCRLF(line));
                }
            }

            if (!imgFile.exists()) {
                throw new Exception("Fail to generate image file!");
            }

            return imgFile;

        } catch (Exception e) {
            if (imgFile != null && imgFile.exists()) {
                imgFile.delete();
            }
            log.error("Fail to generate image file!", e);
            throw e;
        }

    }

    @Override
    protected String getParentHelpGuid() {
        return JOB_HELP_GUID;
    }

    @Override
    public void enter(ViewChangeListener.ViewChangeEvent event) {
        super.enter(event);
        String parameters = event.getParameters();
        if (!StringUtils.isEmpty(parameters)) {
            Map<String, String> paramMap = ViewUtil.stringToMap(parameters);
            try {
                Long jobId = Long.parseLong(paramMap.get(ViewUtil.JOB_ID_PARAM_KEY));
                log.info("Entered Job View with Id:" + jobId);
                this.parentTable.select(jobId);
                this.parentTable.setCurrentPageFirstItemIndex(getParentContainer().indexOfId(jobId));
            } catch (NumberFormatException ne) {
                log.warn("Invalid Parameters for Job View. " + parameters);
            }
        }
    }
}
