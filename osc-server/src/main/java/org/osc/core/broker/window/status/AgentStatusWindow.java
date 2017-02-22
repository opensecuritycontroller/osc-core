package org.osc.core.broker.window.status;

import java.util.List;

import org.apache.log4j.Logger;
import org.osc.core.broker.service.GetAgentStatusService;
import org.osc.core.broker.service.dto.DistributedApplianceInstanceDto;
import org.osc.core.broker.service.request.DistributedApplianceInstancesRequest;
import org.osc.core.broker.service.response.GetAgentStatusResponseDto;
import org.osc.core.broker.view.util.ViewUtil;
import org.osc.core.rest.client.agent.model.output.AgentStatusResponse;
import org.osc.core.util.ServerUtil;

import com.vaadin.event.ShortcutAction.KeyCode;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Table;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;
import com.vaadin.ui.themes.ValoTheme;

public class AgentStatusWindow extends Window {
    private static final Logger log = Logger.getLogger(AgentStatusWindow.class);
    private List<DistributedApplianceInstanceDto> daiList = null;

    /**
     *
     */
    private static final long serialVersionUID = 1L;
    protected FormLayout form;
    private VerticalLayout statusPane;

    @SuppressWarnings("serial")
    public AgentStatusWindow(List<DistributedApplianceInstanceDto> daiList) {
        super();
        this.daiList = daiList;
        setModal(true);
        setClosable(true);
        setResizable(true);
        setHeight("500px");
        setWidth("475px");
        setCaption("Appliance Instance Status");

        // creating top level layout for every window
        VerticalLayout content = new VerticalLayout();
        content.setWidthUndefined();
        // creating form layout shared by all derived classes
        this.form = new FormLayout();
        this.form.setMargin(true);

        HorizontalLayout windowToolbar = new HorizontalLayout();
        windowToolbar.setSpacing(true);
        windowToolbar.addStyleName("buttonToolbar");

        Button close = new Button("Close");
        close.setClickShortcut(KeyCode.ESCAPE, null);
        close.addClickListener(new ClickListener() {
            @Override
            public void buttonClick(ClickEvent event) {
                close();
            }
        });
        close.focus();

        Button refresh = new Button("Refresh");
        refresh.addClickListener(new ClickListener() {
            @Override
            public void buttonClick(ClickEvent event) {
                refreshTable();
            }
        });

        windowToolbar.addComponents(refresh, close);

        this.form.addComponent(windowToolbar);
        this.form.setComponentAlignment(windowToolbar, Alignment.TOP_RIGHT);

        this.statusPane = new VerticalLayout();
        this.form.addComponent(this.statusPane);
        refreshTable();
        content.addComponent(this.form);
        setContent(content);
    }

    private void refreshTable() {
        this.statusPane.removeAllComponents();

        DistributedApplianceInstancesRequest req = new DistributedApplianceInstancesRequest(this.daiList);
        GetAgentStatusService statusService = new GetAgentStatusService();

        try {
            GetAgentStatusResponseDto response = statusService.dispatch(req);
            for (AgentStatusResponse status : response.getAgentStatusList()) {
                // TODO emanoel: For now assuming that if the dpa info is null the status is not supported by the manager.
                // may change the status response for an enum: DISCOVERED, INSPECTION_READY, NOT_PROVIDED, etc.
                if (status.getAgentDpaInfo() != null){
                    this.statusPane.addComponent(createTableStatusProvided(status));
                } else {
                    this.statusPane.addComponent(createTableStatusNotProvided(status));
                }
            }
        } catch (Exception e) {
            log.error("Fail to get DAI status", e);
            ViewUtil.iscNotification("Fail to get Appliance Instance status (" + e.getMessage() + ")",
                    Notification.Type.ERROR_MESSAGE);
        }
    }

    @SuppressWarnings("unchecked")
    private Table createTableStatusNotProvided(AgentStatusResponse res) {
        Table statusTable = new Table();
        // initializing network table with empty values
        addCommonTableItems(statusTable);
        addCommonTableItemValues(res, statusTable);
        statusTable.addItem(new Object[] { "Status Not Available: ", "" }, new Integer(6));
        statusTable.getItem(6).getItemProperty("Value").setValue(res.getStatusLines().get(0));
        return statusTable;
    }

    private void addCommonTableItems(Table statusTable) {
        statusTable.setImmediate(true);
        statusTable.setStyleName(ValoTheme.TABLE_COMPACT);

        statusTable.addContainerProperty("Property", String.class, "");
        statusTable.addContainerProperty("Value", String.class, "");
        statusTable.setColumnHeaderMode(Table.ColumnHeaderMode.HIDDEN);
        statusTable.setPageLength(0);
        statusTable.setReadOnly(true);

        statusTable.addItem(new Object[] { "Name: ", "" }, new Integer(1));
        statusTable.addItem(new Object[] { "Local IP: ", "" }, new Integer(2));
        statusTable.addItem(new Object[] { "Public IP: ", "" }, new Integer(3));
        statusTable.addItem(new Object[] { "V.Server: ", "" }, new Integer(4));
        statusTable.addItem(new Object[] { "Manager IP: ", "" }, new Integer(5));
    }

    @SuppressWarnings("unchecked")
    private Table createTableStatusProvided(AgentStatusResponse res) {

        Table statusTable = new Table();

        // initializing network table with empty values
        addCommonTableItems(statusTable);
        statusTable.addItem(new Object[] { "DPA PID: ", "" }, new Integer(6));
        statusTable.addItem(new Object[] { "DPA Info: ", "" }, new Integer(7));
        statusTable.addItem(new Object[] { "DPA Stats: ", "" }, new Integer(8));
        statusTable.addItem(new Object[] { "Discovered: ", "" }, new Integer(9));
        statusTable.addItem(new Object[] { "Inspection Ready: ", "" }, new Integer(10));

        try {
            addCommonTableItemValues(res, statusTable);

            if (null != res.getVersion() && null != res.getVersion().getVersionStr()) {
                statusTable.getItem(6).getItemProperty("Value")
                .setValue(res.getAgentDpaInfo().netXDpaRuntimeInfo.dpaPid);
                statusTable.getItem(7).getItemProperty("Value")
                .setValue(
                        "IPC Ver:" + res.getAgentDpaInfo().dpaStaticInfo.ipcVersion + ", Name:"
                                + res.getAgentDpaInfo().dpaStaticInfo.dpaName + ", Version:"
                                + res.getAgentDpaInfo().dpaStaticInfo.dpaVersion);

                Long dropped = 0L;
                if (res.getAgentDpaInfo().netXDpaRuntimeInfo.dropResource != null) {
                    dropped += res.getAgentDpaInfo().netXDpaRuntimeInfo.dropResource;
                }
                if (res.getAgentDpaInfo().netXDpaRuntimeInfo.dropSva != null) {
                    dropped += res.getAgentDpaInfo().netXDpaRuntimeInfo.dropSva;
                }
                if (res.getAgentDpaInfo().netXDpaRuntimeInfo.dropError != null) {
                    dropped += res.getAgentDpaInfo().netXDpaRuntimeInfo.dropError;
                }

                statusTable
                .getItem(8)
                .getItemProperty("Value")
                .setValue(
                        "Rx:" + res.getAgentDpaInfo().netXDpaRuntimeInfo.rx + ", Tx:"
                                + res.getAgentDpaInfo().netXDpaRuntimeInfo.txSva + ", Dropped:" + dropped
                                + ", Insp-If:" + res.getAgentDpaInfo().netXDpaRuntimeInfo.workloadInterfaces);
                statusTable.getItem(9).getItemProperty("Value")
                .setValue(Boolean.valueOf(res.isDiscovered()).toString());
                statusTable.getItem(10).getItemProperty("Value")
                .setValue(Boolean.valueOf(res.isInspectionReady()).toString());
            }
        } catch (Exception e) {
            log.error("Fail to retrieve agent info", e);

            statusTable.getItem(6).getItemProperty("Value").setValue("Not Available due to communication error.");
        }

        return statusTable;

    }

    @SuppressWarnings("unchecked")
    private void addCommonTableItemValues(AgentStatusResponse res, Table statusTable) {
        statusTable.getItem(1).getItemProperty("Value").setValue(res.getApplianceName());
        statusTable.getItem(2).getItemProperty("Value").setValue(res.getApplianceIp());
        statusTable.getItem(3).getItemProperty("Value").setValue(res.getPublicIp());
        statusTable.getItem(4).getItemProperty("Value").setValue(res.getVirtualServer());

        if (null != res.getVersion() && null != res.getVersion().getVersionStr()) {
            statusTable.getItem(5).getItemProperty("Value").setValue(res.getManagerIp());
            statusTable.getItem(6).getItemProperty("Value").setValue(res.getCurrentServerTime().toString());
        } else {
            statusTable.getItem(5).getItemProperty("Value").setValue("Not Available due to communication error.");
        }
    }

}
