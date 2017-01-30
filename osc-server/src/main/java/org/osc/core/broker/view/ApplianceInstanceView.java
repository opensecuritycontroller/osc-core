package org.osc.core.broker.view;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.model.entities.appliance.AgentType;
import org.osc.core.broker.service.DownloadAgentLogService;
import org.osc.core.broker.service.ListDistributedApplianceInstanceService;
import org.osc.core.broker.service.RegisterAgentService;
import org.osc.core.broker.service.SyncAgentService;
import org.osc.core.broker.service.UpgradeAgentsService;
import org.osc.core.broker.service.dto.BaseDto;
import org.osc.core.broker.service.dto.DistributedApplianceInstanceDto;
import org.osc.core.broker.service.request.BaseIdRequest;
import org.osc.core.broker.service.request.BaseRequest;
import org.osc.core.broker.service.request.DistributedApplianceInstancesRequest;
import org.osc.core.broker.service.response.BaseJobResponse;
import org.osc.core.broker.service.response.DownloadAgentLogResponse;
import org.osc.core.broker.service.response.ListResponse;
import org.osc.core.broker.view.common.VmidcMessages;
import org.osc.core.broker.view.common.VmidcMessages_;
import org.osc.core.broker.view.util.ToolbarButtons;
import org.osc.core.broker.view.util.ViewUtil;
import org.osc.core.broker.window.status.AgentStatusWindow;

import com.vaadin.data.util.BeanContainer;
import com.vaadin.data.util.BeanItem;
import com.vaadin.navigator.ViewChangeListener;
import com.vaadin.server.FileDownloader;
import com.vaadin.server.StreamResource;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.CustomTable;
import com.vaadin.ui.CustomTable.ColumnGenerator;
import com.vaadin.ui.Notification;

public class ApplianceInstanceView extends CRUDBaseView<DistributedApplianceInstanceDto, BaseDto> {

    private static final String DAI_HELP_GUID = "GUID-E4986D6E-C481-43C4-8801-670CAF8C1581.html";

    /**
     *
     */
    private static final long serialVersionUID = 1605215705219327527L;

    private static final Logger log = Logger.getLogger(ApplianceInstanceView.class);

    public ApplianceInstanceView() {
        super();
        createView(VmidcMessages.getString(VmidcMessages_.DAI_TITLE), Arrays.asList(
                ToolbarButtons.APPLIANCE_AGENT_STATUS, ToolbarButtons.CONFORM, ToolbarButtons.APPLIANCE_AGENT_UPGRADE,
                ToolbarButtons.APPLIANCE_AGENT_REGISTER, ToolbarButtons.APPLIANCE_AGENT_LOG_DOWNLOAD), true);
        setInfoText(VmidcMessages.getString(VmidcMessages_.PAGE_INFO_HELP_TITLE),
                VmidcMessages.getString(VmidcMessages_.DAI_HELP_MESSAGE));
    }

    @SuppressWarnings("serial")
    @Override
    public void initParentTable() {
        this.parentContainer = new BeanContainer<Long, DistributedApplianceInstanceDto>(
                DistributedApplianceInstanceDto.class);

        this.parentTable.setContainerDataSource(this.parentContainer);
        this.parentTable.setVisibleColumns("name", "ipAddress", "discovered", "inspectionReady", "lastStatus",
                "agentVersionStr", "agentTypeStr", "hostname", "virtualConnectorName", "applianceManagerConnectorName",
                "distributedApplianceName", "applianceModel", "swVersion");

        this.parentTable.addGeneratedColumn("applianceManagerConnectorName", new ColumnGenerator() {
            @Override
            public Object generateCell(CustomTable source, Object itemId, Object columnId) {
                DistributedApplianceInstanceDto daiDto = ApplianceInstanceView.this.parentContainer
                        .getItem(itemId).getBean();
                return ViewUtil.generateObjectLink(new LockObjectReference(daiDto.getMcId(),
                        daiDto.getApplianceManagerConnectorName(),
                        LockObjectReference.ObjectType.APPLIANCE_MANAGER_CONNECTOR));
            }
        });

        this.parentTable.addGeneratedColumn("virtualConnectorName", new ColumnGenerator() {
            @Override
            public Object generateCell(CustomTable source, Object itemId, Object columnId) {
                DistributedApplianceInstanceDto daiDto = ApplianceInstanceView.this.parentContainer
                        .getItem(itemId).getBean();
                return ViewUtil.generateObjectLink(new LockObjectReference(daiDto.getVcId(),
                        daiDto.getVirtualConnectorName(),
                        LockObjectReference.ObjectType.VIRTUALIZATION_CONNECTOR));
            }
        });

        this.parentTable.addGeneratedColumn("distributedApplianceName", new ColumnGenerator() {
            @Override
            public Object generateCell(CustomTable source, Object itemId, Object columnId) {
                DistributedApplianceInstanceDto daiDto = ApplianceInstanceView.this.parentContainer
                        .getItem(itemId).getBean();
                return ViewUtil.generateObjectLink(new LockObjectReference(daiDto.getVirtualsystemId(),
                        daiDto.getDistributedApplianceName(),
                        LockObjectReference.ObjectType.VIRTUAL_SYSTEM));
            }
        });

        this.parentTable.setColumnHeader("name", "Name");
        this.parentTable.setColumnHeader("ipAddress", "IP-Address");
        this.parentTable.setColumnHeader("discovered", "Discovered");
        this.parentTable.setColumnHeader("inspectionReady", "Inspection-Ready");
        this.parentTable.setColumnHeader("lastStatus", "Last Status");
        this.parentTable.setColumnHeader("agentVersionStr", "Agent Version");
        this.parentTable.setColumnHeader("agentTypeStr", "Agent Type");
        this.parentTable.setColumnHeader("hostname", "V.Server");
        this.parentTable.setColumnHeader("virtualConnectorName", "V.Connector");
        this.parentTable.setColumnHeader("applianceManagerConnectorName", "Manager");
        this.parentTable.setColumnHeader("distributedApplianceName", "Distributed Appliance");
        this.parentTable.setColumnHeader("applianceModel", "Model");
        this.parentTable.setColumnHeader("swVersion", "Version");

        // adding stream resource to download button to query support bundle
        // real time
        StreamResource zipStream = getZipStream();
        FileDownloader fileDownloader = new FileDownloader(zipStream);
        fileDownloader.extend(ViewUtil.getButtonById(this.parentToolbar,
                ToolbarButtons.APPLIANCE_AGENT_LOG_DOWNLOAD.getId()));
    }

    @Override
    public void populateParentTable() {

        this.parentContainer.removeAllItems();
        ListDistributedApplianceInstanceService listService = new ListDistributedApplianceInstanceService();

        try {
            ListResponse<DistributedApplianceInstanceDto> res = listService.dispatch(new BaseRequest<>());
            List<DistributedApplianceInstanceDto> listResponse = res.getList();
            for (DistributedApplianceInstanceDto dto : listResponse) {
                this.parentContainer.addItem(dto.getId(), dto);
            }

        } catch (Exception e) {
            log.error("Fail to populate DAI table", e);
            ViewUtil.iscNotification("Fail to populate Appliance Instance table (" + e.getMessage() + ")",
                    Notification.Type.ERROR_MESSAGE);
        }
    }

    @Override
    public void initChildTable() {
    }

    @Override
    public void populateChildTable(BeanItem<DistributedApplianceInstanceDto> parentItem) {
    }

    @Override
    public void buttonClicked(ClickEvent event) {
        if (event.getButton().getId().equals(ToolbarButtons.APPLIANCE_AGENT_STATUS.getId())) {
            checkAgentStatus();
        } else if (event.getButton().getId().equals(ToolbarButtons.APPLIANCE_AGENT_UPGRADE.getId())) {
        	upgradeAgent();
        } else if (event.getButton().getId().equals(ToolbarButtons.APPLIANCE_AGENT_REGISTER.getId())) {
            registerAgent();
        } else if (event.getButton().getId().equals(ToolbarButtons.CONFORM.getId())) {
            syncAgent();
        }
    }

    private void checkAgentStatus() {
        try {
            ViewUtil.addWindow(new AgentStatusWindow(this.itemList));
        } catch (Exception e) {
            log.error("Failed to get status from Agent(s)", e);
            ViewUtil.iscNotification("Failed to get status from Agent(s).", Notification.Type.WARNING_MESSAGE);
        }
    }

    private void registerAgent() {
        try {
            log.info("Calling register agent service");
            DistributedApplianceInstancesRequest req = new DistributedApplianceInstancesRequest(this.itemList);
            RegisterAgentService registerService = new RegisterAgentService();
            BaseJobResponse response = registerService.dispatch(req);
            log.info("Agent Registration Successful!");
            ViewUtil.showJobNotification(response.getJobId());
        } catch (Exception e) {
            log.error("Failed to register Agent(s)", e);
            ViewUtil.iscNotification("Failed to register Agent(s). (" + e.getMessage() + ")",
                    Notification.Type.WARNING_MESSAGE);
        }

    }

    public void upgradeAgent() {
        try {
            log.info("Calling upgrade agent service");
            DistributedApplianceInstancesRequest req = new DistributedApplianceInstancesRequest(this.itemList);
            UpgradeAgentsService upgradeService = new UpgradeAgentsService();
            BaseJobResponse response = upgradeService.dispatch(req);
            log.info("Agent Upgrade Successful!");
            ViewUtil.showJobNotification(response.getJobId());
        } catch (Exception e) {
            log.error("Failed to upgrade Agent(s)", e);
            ViewUtil.iscNotification("Failed to upgrade Agent(s). (" + e.getMessage() + ")", Notification.Type.WARNING_MESSAGE);
        }
    }

    @Override
    protected String getParentHelpGuid() {
        return DAI_HELP_GUID;
    }

    private void syncAgent() {
        try {
            log.info("Calling sync agent service");
            DistributedApplianceInstancesRequest req = new DistributedApplianceInstancesRequest(this.itemList);
            SyncAgentService syncAgentService = new SyncAgentService();
            BaseJobResponse response = syncAgentService.dispatch(req);
            log.info("Agent Sync Successful!");
            ViewUtil.showJobNotification(response.getJobId());
        } catch (Exception e) {
            log.error("Failed to sync Agent(s)", e);
            ViewUtil.iscNotification("Failed to sync Agent(s). (" + e.getMessage() + ")", Notification.Type.WARNING_MESSAGE);
        }
    }

    private File downloadAgentLog() {
        DownloadAgentLogResponse res = null;
        try {
            log.info("Downloading agent log file");
            BaseIdRequest downloadRequest = new BaseIdRequest();
            downloadRequest.setId(this.itemList.get(0).getId());
            DownloadAgentLogService downloadLogService = new DownloadAgentLogService();
            res = downloadLogService.dispatch(downloadRequest);
        } catch (Exception e) {
            log.error("Failed to download support bundle", e);
            ViewUtil.iscNotification("Failed to download support bundle. (" + e.getMessage() + ")", Notification.Type.ERROR_MESSAGE);
            return null;
        }
        return res.getSupportBundle();
    }

    private StreamResource getZipStream() {

        if (new File("AgentSupportBundle.zip").exists()) {
            new File("AgentSupportBundle.zip").delete();
        }
        @SuppressWarnings("serial")
        StreamResource.StreamSource source = new StreamResource.StreamSource() {
            @Override
            public InputStream getStream() {
                if (ApplianceInstanceView.this.itemList.size() != 1) {
                    Notification
                            .show("Warning!",
                                    "Multiple selection is not allowed for this action. Please retry by selecting a single Appliance Instance.",
                                    Notification.Type.WARNING_MESSAGE);
                    return null;
                }
                if (ApplianceInstanceView.this.itemList.get(0).getAgentTypeStr().equals(AgentType.AGENTLESS.toString())){
                    ViewUtil.iscNotification("Failed to download support bundle. (This action is not applicable for the selection since it contains Agentless Instance(s))",
                            Notification.Type.WARNING_MESSAGE);
                    return null;
                }

                InputStream fin = null;
                try {
                    // creating a zip file resource to download
                    fin = new FileInputStream(downloadAgentLog());
                } catch (Exception exception) {
                    log.error("Failed! to receive zip file from Agent", exception);
                    ViewUtil.iscNotification("Failed to download support bundle.", Notification.Type.ERROR_MESSAGE);
                }
                return fin;
            }
        };
        StreamResource resource = new StreamResource(source, "AgentSupportBundle.zip");
        return resource;

    }

    @SuppressWarnings("unchecked")
    @Override
    protected void updateParentContainer(DistributedApplianceInstanceDto dto) {
        DistributedApplianceInstanceDto updateDto = dto;
        this.parentContainer.getItem(dto.getId()).getItemProperty("ipAddress").setValue(updateDto.getIpAddress());
        this.parentContainer.getItem(dto.getId()).getItemProperty("discovered").setValue(updateDto.getDiscovered());
        this.parentContainer.getItem(dto.getId()).getItemProperty("inspectionReady")
                .setValue(updateDto.getInspectionReady());
        this.parentContainer.getItem(dto.getId()).getItemProperty("lastStatus").setValue(updateDto.getLastStatus());
        this.parentContainer.getItem(dto.getId()).getItemProperty("agentVersionStr")
                .setValue(updateDto.getAgentVersionStr());
        this.parentContainer.getItem(dto.getId()).getItemProperty("agentTypeStr")
                .setValue(updateDto.getAgentTypeStr());
        this.parentContainer.getItem(dto.getId()).getItemProperty("hostname").setValue(updateDto.getHostname());
        this.parentContainer.getItem(dto.getId()).getItemProperty("virtualConnectorName")
                .setValue(updateDto.getVirtualConnectorName());
        this.parentContainer.getItem(dto.getId()).getItemProperty("applianceManagerConnectorName")
                .setValue(updateDto.getApplianceManagerConnectorName());
        this.parentContainer.getItem(dto.getId()).getItemProperty("distributedApplianceName")
                .setValue(updateDto.getDistributedApplianceName());
        this.parentContainer.getItem(dto.getId()).getItemProperty("applianceModel").setValue(updateDto.getApplianceModel());
        this.parentContainer.getItem(dto.getId()).getItemProperty("swVersion").setValue(updateDto.getSwVersion());
    }

    @Override
    public void enter(ViewChangeListener.ViewChangeEvent event) {
        super.enter(event);
        String parameters = event.getParameters();
        if (!StringUtils.isEmpty(parameters)) {
            Map<String, String> paramMap = ViewUtil.stringToMap(parameters);
            try {
                Long daiId = Long.parseLong(paramMap.get(ViewUtil.DAI_ID_PARAM_KEY));
                log.info("Entered DAI View with Id:" + daiId);
                this.parentTable.setValue(null);
                this.parentTable.select(daiId);
                this.parentTable.setCurrentPageFirstItemIndex(getParentContainer().indexOfId(daiId));
            } catch (NumberFormatException ne) {
                log.warn("Invalid Parameters for DAI View. " + parameters);
            }
        }
    }

}
