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

import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.osc.core.broker.service.api.ArchiveServiceApi;
import org.osc.core.broker.service.api.BackupServiceApi;
import org.osc.core.broker.service.api.CheckNetworkSettingsServiceApi;
import org.osc.core.broker.service.api.DeleteSslCertificateServiceApi;
import org.osc.core.broker.service.api.GetEmailSettingsServiceApi;
import org.osc.core.broker.service.api.GetJobsArchiveServiceApi;
import org.osc.core.broker.service.api.GetNATSettingsServiceApi;
import org.osc.core.broker.service.api.GetNetworkSettingsServiceApi;
import org.osc.core.broker.service.api.ListSslCertificatesServiceApi;
import org.osc.core.broker.service.api.RestoreServiceApi;
import org.osc.core.broker.service.api.SetEmailSettingsServiceApi;
import org.osc.core.broker.service.api.SetNATSettingsServiceApi;
import org.osc.core.broker.service.api.SetNetworkSettingsServiceApi;
import org.osc.core.broker.service.api.UpdateJobsArchiveServiceApi;
import org.osc.core.broker.service.api.UpgradeServiceApi;
import org.osc.core.broker.service.api.server.ArchiveApi;
import org.osc.core.broker.service.api.server.ServerApi;
import org.osc.core.broker.service.api.server.ValidationApi;
import org.osc.core.broker.service.ssl.X509TrustManagerApi;
import org.osc.core.broker.view.common.StyleConstants;
import org.osc.core.broker.view.common.VmidcMessages;
import org.osc.core.broker.view.common.VmidcMessages_;
import org.osc.core.broker.view.maintenance.ArchiveLayout;
import org.osc.core.broker.view.maintenance.EmailLayout;
import org.osc.core.broker.view.maintenance.ManageLayout;
import org.osc.core.broker.view.maintenance.NetworkLayout;
import org.osc.core.broker.view.maintenance.SslConfigurationLayout;
import org.osc.core.broker.view.maintenance.SummaryLayout;
import org.osc.core.broker.view.maintenance.SupportLayout;
import org.osc.core.broker.view.util.ViewUtil;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ServiceScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.Panel;
import com.vaadin.ui.TabSheet;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.ValoTheme;

@Component(service={MaintenanceView.class}, scope=ServiceScope.PROTOTYPE)
public class MaintenanceView extends VerticalLayout implements View {


    private static final String SUMMARY_HELP_GUID = "GUID-13FB9C57-8B0E-44C5-991E-13DD46A5C437.html";
    private static final String NETWORK_HELP_GUID = "GUID-976702C1-382D-4DB7-8ECA-F1443594DF90.html";
    private static final String MAINTENANCE_HELP_GUID = "GUID-1A1B4F83-6C91-4268-9CA0-4E99A09AEE05.html";

    private static final String MAINTENANCE_EMAIL_GUID = "GUID-5D09C4DA-E3C3-4A51-9699-9CC4D8610955.html";
    private static final String MAINTENANCE_ARCHIVE_GUID = "GUID-CAD74210-DE2C-4240-9937-32AD3D88B6F8.html";

    private static final long serialVersionUID = 1L;
    // private static final Logger log =
    // LogComponent.getLogger(MaintenanceView.class);

    private static final Logger log = LoggerFactory.getLogger(MaintenanceView.class);

    TabSheet subMenu = null;
    TabSheet tabs = new TabSheet();

    @Reference
    ServerApi server;

    @Reference
    BackupServiceApi backupService;

    @Reference
    UpgradeServiceApi upgradeService;

    @Reference
    RestoreServiceApi restoreService;

    @Reference
    GetNetworkSettingsServiceApi getNetworkSettingsService;

    @Reference
    CheckNetworkSettingsServiceApi checkNetworkSettingsService;

    @Reference
    SetNetworkSettingsServiceApi setNetworkSettingsService;

    @Reference
    GetNATSettingsServiceApi getNATSettingsService;

    @Reference
    SetNATSettingsServiceApi setNATSettingsService;

    @Reference
    DeleteSslCertificateServiceApi deleteSslCertificateService;

    @Reference
    ListSslCertificatesServiceApi listSslCertificatesService;

    @Reference
    ArchiveServiceApi archiveService;

    @Reference
    GetJobsArchiveServiceApi getJobsArchiveService;

    @Reference
    UpdateJobsArchiveServiceApi updateJobsArchiveService;

    @Reference
    GetEmailSettingsServiceApi getEmailSettingsService;

    @Reference
    SetEmailSettingsServiceApi setEmailSettingsService;

    @Reference
    ValidationApi validator;

    @Reference
    X509TrustManagerApi trustManager;

    @Reference
    ArchiveApi archiver;

    private BundleContext ctx;

    @Activate
    void activate(BundleContext ctx) throws Exception {
        this.ctx = ctx;
        setSizeFull();
        addStyleName(StyleConstants.BASE_CONTAINER);

        // creating tabs sheet for this view
        this.tabs.addStyleName(ValoTheme.TABSHEET_FRAMED);
        this.tabs.setSizeFull();

        // adding Summary tab to tabSheet
        this.tabs.addTab(createTab(VmidcMessages.getString(VmidcMessages_.MAINTENANCE_SUMMARY_TITLE),
                VmidcMessages.getString(VmidcMessages_.MAINTENANCE_SUMMARY_TITLE), buildSummary(), SUMMARY_HELP_GUID));
        // adding network tab to tabSheet
        this.tabs
                .addTab(createTab(VmidcMessages.getString(VmidcMessages_.MAINTENANCE_NETWORK_TITLE),
                        VmidcMessages.getString(VmidcMessages_.MAINTENANCE_NETWORK_NAME), buildNetworkForm(),
                        NETWORK_HELP_GUID));
        // adding ssl configuration tab to tabSheet
        this.tabs
                .addTab(createTab(VmidcMessages.getString(VmidcMessages_.MAINTENANCE_SSLCONFIGURATION_TITLE),
                        VmidcMessages.getString(VmidcMessages_.MAINTENANCE_SSLCONFIGURATION_NAME), buildSslConfigurationForm(), null));
        // adding email tab to tabSheet
        this.tabs.addTab(createTab(VmidcMessages.getString(VmidcMessages_.MAINTENANCE_EMAIL_TITLE),
                VmidcMessages.getString(VmidcMessages_.MAINTENANCE_EMAIL_NAME), buildEmailForm(),
                MAINTENANCE_EMAIL_GUID));
        // adding Upgrade tab to tabSheet
        this.tabs.addTab(createTab(VmidcMessages.getString(VmidcMessages_.MAINTENANCE_MAINTENANCE_TITLE),
                VmidcMessages.getString(VmidcMessages_.MAINTENANCE_MAINTENANCE_TITLE), buildUpgradeForm(),
                MAINTENANCE_HELP_GUID));
        // adding Archive tab to tabSheet
        this.tabs.addTab(createTab(VmidcMessages.getString(VmidcMessages_.MAINTENANCE_JOBSARCHIVE_TITLE),
                VmidcMessages.getString(VmidcMessages_.MAINTENANCE_JOBSARCHIVE_NAME), new ArchiveLayout(this.archiveService,
                        this.getJobsArchiveService, this.updateJobsArchiveService),
                MAINTENANCE_ARCHIVE_GUID));
        // adding Support tab to tabSheet
        this.tabs.addTab(createTab(VmidcMessages.getString(VmidcMessages_.MAINTENANCE_SUPPORT_TITLE),
                VmidcMessages.getString(VmidcMessages_.MAINTENANCE_SUPPORT_NAME), new SupportLayout(), null));

        // adding tab sheet to the view
        addComponent(this.tabs);
        setExpandRatio(this.tabs, 1L);
    }

    private VerticalLayout createTab(String caption, String title, FormLayout content, String guid) {
        VerticalLayout tabSheet = new VerticalLayout();
        tabSheet.setCaption(caption);
        tabSheet.setStyleName(StyleConstants.TAB_SHEET);
        Panel panel = new Panel();
        // creating subHeader inside panel
        panel.setContent(content);
        panel.setSizeFull();
        tabSheet.addComponent(ViewUtil.createSubHeader(title, guid));
        tabSheet.addComponent(panel);
        return tabSheet;
    }

    private FormLayout buildNetworkForm() {
        return new NetworkLayout(this.getNetworkSettingsService, this.checkNetworkSettingsService,
                this.setNetworkSettingsService, this.getNATSettingsService,
                this.setNATSettingsService, this.validator, this.server);
    }

    private FormLayout buildSslConfigurationForm() {
        return new SslConfigurationLayout(this.deleteSslCertificateService, this.listSslCertificatesService, this.trustManager, this.ctx);
    }

    private FormLayout buildEmailForm() {
        return new EmailLayout(this.getEmailSettingsService, this.setEmailSettingsService);
    }

    private FormLayout buildUpgradeForm() {
        return new ManageLayout(this.backupService, this.upgradeService, this.restoreService, this.server, this.validator);
    }

    private FormLayout buildSummary() {
        return new SummaryLayout(this.server, this.backupService, this.archiver);
    }

    @Override
    public void enter(ViewChangeEvent event) {
        String parameters = event.getParameters();
        if (!StringUtils.isEmpty(parameters)) {
            Map<String, String> paramMap = ViewUtil.stringToMap(parameters);
            try {
                if (paramMap.get(ViewUtil.NETWORK_PARAM_KEY) != null) {
                    this.tabs.setSelectedTab(this.tabs.getTab(1));
                    log.info("Entered Network tab");
                } else if (paramMap.get(ViewUtil.SSL_CONFIGURATION_PARAM_KEY) != null) {
                    this.tabs.setSelectedTab(this.tabs.getTab(2));
                    log.info("Entered SSL configuration tab");
                }else if (paramMap.get(ViewUtil.EMAIL_PARAM_KEY) != null) {
                    this.tabs.setSelectedTab(this.tabs.getTab(3));
                    log.info("Entered Email tab");
                } else if (paramMap.get(ViewUtil.ARCHIVE_PARAM_KEY) != null) {
                    this.tabs.setSelectedTab(this.tabs.getTab(4));
                    log.info("Entered Archive tab");
                }
            } catch (NumberFormatException ne) {
                log.warn("Invalid Parameters for Maintenance View. " + parameters);
            }
        }
    }
}
