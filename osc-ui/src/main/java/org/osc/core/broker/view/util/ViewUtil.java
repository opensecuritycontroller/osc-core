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

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.osc.core.broker.service.api.server.ServerApi;
import org.osc.core.broker.service.dto.BaseDto;
import org.osc.core.broker.service.dto.job.LockObjectDto;
import org.osc.core.broker.view.MainUI;
import org.osc.core.broker.view.common.StyleConstants;
import org.osc.core.broker.view.common.VmidcMessages;
import org.osc.core.broker.view.common.VmidcMessages_;
import org.osc.core.broker.window.LoadingIndicatorCRUDBaseWindow;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.tepi.filtertable.FilterGenerator;
import org.tepi.filtertable.FilterTable;
import org.tepi.filtertable.datefilter.DateInterval;

import com.vaadin.data.Container.Filter;
import com.vaadin.data.util.BeanContainer;
import com.vaadin.data.util.DefaultItemSorter;
import com.vaadin.data.util.DefaultItemSorter.DefaultPropertyValueComparator;
import com.vaadin.data.util.ItemSorter;
import com.vaadin.data.util.filter.Between;
import com.vaadin.data.util.filter.Compare;
import com.vaadin.server.ExternalResource;
import com.vaadin.server.Page;
import com.vaadin.server.ThemeResource;
import com.vaadin.server.VaadinServletService;
import com.vaadin.ui.AbstractField;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.Component;
import com.vaadin.ui.Field;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.JavaScript;
import com.vaadin.ui.Label;
import com.vaadin.ui.Link;
import com.vaadin.ui.Notification;
import com.vaadin.ui.UI;
import com.vaadin.ui.Window;
import com.vaadin.ui.themes.Reindeer;
import com.vaadin.ui.themes.ValoTheme;

public class ViewUtil {

    private static final String HELP_WINDOW_NAME_1 = "ISC_help_1";
    private static final String HELP_WINDOW_NAME_2 = "ISC_help_2";
    private static boolean useFirstName = true;

    private static final Logger log = LoggerFactory.getLogger(ViewUtil.class);

    private static final String PARAM_ITEM_VALUE_SEPARATOR = "=";
    private static final String PARAM_SEPARATOR = "/";

    public static final String EMAIL_PARAM_KEY = "emailTab";
    public static final String NETWORK_PARAM_KEY = "networkTab";
    public static final String SSL_CONFIGURATION_PARAM_KEY = "sslConfigurationTab";
    public static final String ARCHIVE_PARAM_KEY = "archiveTab";
    public static final String JOB_ID_PARAM_KEY = "jobId";
    public static final String ALERT_ID_PARAM_KEY = "alertId";
    public static final String VC_ID_PARAM_KEY = "vcId";
    public static final String MC_ID_PARAM_KEY = "mcId";
    public static final String DA_ID_PARAM_KEY = "daId";
    public static final String DAI_ID_PARAM_KEY = "daiId";
    public static final String VS_ID_PARAM_KEY = "vsId";
    public static final String DS_ID_PARAM_KEY = "dsId";
    public static final String SG_ID_PARAM_KEY = "sgId";
    public static final String SGI_ID_PARAM_KEY = "sgiId";

    /**
     * @param caption
     *            Caption Text Representing Header
     * @param guid
     *            Help GUID for caller view
     * @return
     *         Horizontal Layout containing Caption text and Help button
     */
    public static HorizontalLayout createSubHeader(String caption, String guid) {

        HorizontalLayout subHeader = new HorizontalLayout();
        subHeader.setWidth("100%");
        subHeader.setHeight("35px");
        subHeader.setSpacing(true);
        subHeader.addStyleName("toolbar");
        final Label title = new Label(caption);
        title.setSizeUndefined();
        subHeader.addComponent(title);
        subHeader.setComponentAlignment(title, Alignment.MIDDLE_LEFT);
        subHeader.setExpandRatio(title, 1);

        // create help button if we have some GUID else do not add this button
        if (guid != null) {

            Button helpButton = new Button();
            helpButton.setImmediate(true);
            helpButton.setStyleName(Reindeer.BUTTON_LINK);
            helpButton.setDescription("Help");
            helpButton.setIcon(new ThemeResource("img/Help.png"));
            subHeader.addComponent(helpButton);
            helpButton.addClickListener(new HelpButtonListener(guid));
        }

        return subHeader;
    }

    @SuppressWarnings("serial")
    public static class HelpButtonListener implements ClickListener {

        private final String guid;

        public HelpButtonListener(String guid) {
            this.guid = guid;
        }

        @Override
        public void buttonClick(ClickEvent event) {

            ViewUtil.showHelpBrowserWindow(this.guid);
        }
    }

    public static void setButtonsEnabled(boolean enabled, HorizontalLayout layout) {
        setButtonsEnabled(enabled, layout, java.util.Collections.<String>emptyList());
    }

    /**
     * @param enabled
     *            either enable or disable all the buttons in the gived Layout
     * @param layout
     *            Layout these buttons belongs to
     * @param ignoreList
     *            Buttons who does not need this state change i.e. Add button
     */
    public static void setButtonsEnabled(boolean enabled, HorizontalLayout layout, List<String> ignoreList) {
        if (layout != null) {
            Iterator<Component> iterate = layout.iterator();
            while (iterate.hasNext()) {
                Component c = iterate.next();
                if (c instanceof Button && !ignoreList.contains(c.getId())) {
                    c.setEnabled(enabled);
                }
            }
        }
    }

    /**
     *
     * @param enabled
     *            either enable or disable given set of buttons
     * @param layout
     *            Layout these buttons belongs to
     * @param itemsToEnable
     *            List of Buttons which needs to be enabled/disabled
     */
    public static void enableToolBarButtons(boolean enabled, HorizontalLayout layout, List<String> itemsToEnable) {
        if (layout != null) {
            Iterator<Component> iterate = layout.iterator();
            while (iterate.hasNext()) {
                Component c = iterate.next();
                if (c instanceof Button && itemsToEnable.contains(c.getId())) {
                    c.setEnabled(enabled);
                }
            }
        }
    }

    /**
     *
     *
     *
     * @param layout
     *            Parent layout of the button
     * @param id
     *            String id of the button
     * @return
     *         Returns a Button Object from the ID provided
     */
    public static Button getButtonById(HorizontalLayout layout, String id) {
        if (layout != null) {
            Iterator<Component> iterate = layout.iterator();
            while (iterate.hasNext()) {
                Component c = iterate.next();
                if (c instanceof Button && c.getId().equals(id)) {
                    return (Button) c;
                }
            }
        }
        return null;
    }

    /**
     * @param container
     *            BeanContainer of the table which needs to be updated
     * @param dto
     *            BeanItem(DTO)
     * @param table
     *            Table this dto belongs to
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static void updateTableContainer(BeanContainer container, BaseDto dto, FilterTable table) {
        int indexOfItem = container.indexOfId(dto.getId());
        boolean wasSelected = table.isSelected(dto.getId());
        container.removeItem(dto.getId());
        container.addItemAt(indexOfItem, dto.getId(), dto);
        if (wasSelected) {
            table.select(dto.getId());
        }
    }

    public static void showHelpBrowserWindow(String guid) {
        JavaScript javaScript = MainUI.getCurrent().getPage().getJavaScript();
        if (!useFirstName) {
            javaScript.execute(closeNativeWindow(HELP_WINDOW_NAME_1));
        } else {
            javaScript.execute(closeNativeWindow(HELP_WINDOW_NAME_2));
        }

        MainUI.getCurrent().getPage().open("/WebHelp/" + guid,
                useFirstName ? HELP_WINDOW_NAME_1 : HELP_WINDOW_NAME_2);
        useFirstName = !useFirstName;
    }

    /**
     * Generates a link to the Jobs page if both the last Job status and the last Job id are not null. If Id is null,
     * just
     * returns a label with the job status. If status is null and id is not null, returns the job id as a link to the
     * jobs page
     *
     * @param lastJobStatus
     *            the last job status
     * @param lastJobId
     *            the last job id
     *
     * @return link to job page if both last Job status and the last Job id are not null
     */
    public static Object generateJobLink(String lastJobStatus, String lastJobState, Long lastJobId,
            ServerApi server) {
        if (lastJobStatus != null && lastJobId != null) {
            return createJobLink(VmidcMessages.getString(VmidcMessages_.JOB_LINK_CAPTION_ID,
                    resolveJobStateAndStatus(lastJobStatus, lastJobState), lastJobId), lastJobId, server);
        } else if (lastJobId == null && lastJobStatus != null) {
            return new Label(resolveJobStateAndStatus(lastJobStatus, lastJobState));
        } else if (lastJobId != null && lastJobStatus == null) {
            return createJobLink(lastJobId.toString(), lastJobId, server);
        }
        return null;
    }

    public static Object generateMgrLink(String prefix, String ipAddress, String port, String resource) {
        return generateMgrLink(ipAddress, prefix + ipAddress + port + resource);
    }

    public static Object generateMgrLink(String caption, String url) {
        Link mgrLink = new Link();
        mgrLink.setCaption(caption);
        mgrLink.setResource(new ExternalResource(url));
        mgrLink.setDescription("Click to go to application");
        mgrLink.setTargetName("_blank");
        return mgrLink;
    }

    public static Object generateObjectLink(LockObjectDto or, ServerApi server) {
        String viewFragment;
        String paramObjectId;
        switch (or.getType().getName()) {
        case "VIRTUALIZATION_CONNECTOR":
            viewFragment = MainUI.VIEW_FRAGMENT_VIRTUALIZATION_CONNECTORS;
            paramObjectId = VC_ID_PARAM_KEY;
            break;
        case "APPLIANCE_MANAGER_CONNECTOR":
            viewFragment = MainUI.VIEW_FRAGMENT_SECURITY_MANAGER_CONNECTORS;
            paramObjectId = MC_ID_PARAM_KEY;
            break;
        case "DISTRIBUTED_APPLIANCE":
            viewFragment = MainUI.VIEW_FRAGMENT_DISTRIBUTED_APPLIANCES;
            paramObjectId = DA_ID_PARAM_KEY;
            break;
        case "VIRTUAL_SYSTEM":
            viewFragment = MainUI.VIEW_FRAGMENT_DISTRIBUTED_APPLIANCES;
            paramObjectId = VS_ID_PARAM_KEY;
            break;
        case "DEPLOYMENT_SPEC":
            viewFragment = MainUI.VIEW_FRAGMENT_DISTRIBUTED_APPLIANCES;
            paramObjectId = DS_ID_PARAM_KEY;
            break;
        case "DISTRIBUTED_APPLIANCE_INSTANCE":
            viewFragment = MainUI.VIEW_FRAGMENT_APPLIANCE_INSTANCES;
            paramObjectId = DAI_ID_PARAM_KEY;
            break;
        case "SECURITY_GROUP":
            viewFragment = MainUI.VIEW_FRAGMENT_VIRTUALIZATION_CONNECTORS;
            paramObjectId = SG_ID_PARAM_KEY;
            break;
        case "SECURITY_GROUP_INTERFACE":
            viewFragment = MainUI.VIEW_FRAGMENT_DISTRIBUTED_APPLIANCES;
            paramObjectId = SGI_ID_PARAM_KEY;
            break;
        case "JOB":
            viewFragment = MainUI.VIEW_FRAGMENT_JOBS;
            paramObjectId = JOB_ID_PARAM_KEY;
            break;
        case "EMAIL":
            viewFragment = MainUI.VIEW_FRAGMENT_SERVER;
            paramObjectId = EMAIL_PARAM_KEY;
            break;
        case "SSL_CONFIGURATION":
            viewFragment = MainUI.VIEW_FRAGMENT_SERVER;
            paramObjectId = SSL_CONFIGURATION_PARAM_KEY;
            break;
        case "NETWORK":
            viewFragment = MainUI.VIEW_FRAGMENT_SERVER;
            paramObjectId = NETWORK_PARAM_KEY;
            break;
        case "ARCHIVE":
            viewFragment = MainUI.VIEW_FRAGMENT_SERVER;
            paramObjectId = ARCHIVE_PARAM_KEY;
            break;
        case "ALERT":
            viewFragment = MainUI.VIEW_FRAGMENT_ALERTS;
            paramObjectId = ALERT_ID_PARAM_KEY;
            break;
        default:
            return null;
        }

        HashMap<String, Object> paramMap = new HashMap<>();
        paramMap.put(paramObjectId, or.getId());

        Link jobLink = createInternalLink(viewFragment, paramMap, or.getName(),
                VmidcMessages.getString(VmidcMessages_.LINK_DESCRIPTION, or.getType(), or.getName()), server);

        if (jobLink == null) {
            return new Label(or.getName());
        }
        return jobLink;
    }

    public static Object generateObjectLink(Set<LockObjectDto> ors, ServerApi server) {
        if (ors == null || ors.isEmpty()) {
            return null;
        }

        LockObjectDto or = (LockObjectDto) ors.toArray()[0];
        return generateObjectLink(or, server);
    }

    /**
     * Serializes the map into a String. Use {@link #stringToMap(String)} to convert back from the string
     * to a map. Supports Basic data type by using toString on them.
     *
     * @param mapToSerialize
     *            with key being the param name and value being the value of the param
     *
     * @return String representation of the map
     */
    public static String mapToString(Map<String, Object> mapToSerialize) {
        StringBuffer serializedString = new StringBuffer();
        for (Entry<String, Object> entry : mapToSerialize.entrySet()) {
            serializedString.append(PARAM_SEPARATOR);
            serializedString.append(entry.getKey());
            serializedString.append(PARAM_ITEM_VALUE_SEPARATOR);
            serializedString.append(entry.getValue().toString());
        }
        return serializedString.toString();
    }

    /**
     * Deserializes the String serialized by {@link #mapToString(Map)} into a map.
     * Supports Basic data type by using toString on them.
     *
     * @param serializedMap
     *            serialized Map
     *
     * @return Map the map of param:value
     */
    public static Map<String, String> stringToMap(String serializedMap) {
        HashMap<String, String> paramMap = new HashMap<>();
        for (String param : serializedMap.split(PARAM_SEPARATOR)) {
            if (param.contains(PARAM_ITEM_VALUE_SEPARATOR)) {
                String[] paramArray = param.split(PARAM_ITEM_VALUE_SEPARATOR);
                paramMap.put(paramArray[0], paramArray[1]);
            }
        }
        return paramMap;
    }

    private static String resolveJobStateAndStatus(String lastJobStatus, String lastJobState) {
        if (isJobSuccessful(lastJobStatus) && !isJobComplete(lastJobState)) {
            return lastJobState.toString();
        }
        return lastJobStatus.toString();
    }

    private static final String JOB_COMPLETE = "COMPLETED";
    private static final String JOB_SUCCESSFUL = "PASSED";

    public static boolean isJobComplete(String jobState) {
        return JOB_COMPLETE.equals(jobState);
    }

    public static boolean isJobSuccessful(String jobStatus) {
        return JOB_SUCCESSFUL.equals(jobStatus);
    }

    private static final String TASK_FINISHED = "COMPLETED";
    private static final String TASK_QUEUED = "QUEUED";
    private static final String TASK_PENDING = "PENDING";
    private static final String TASK_NOT_RUNNING = "NOT_RUNNING";

    public static boolean isTaskFinished(String taskState) {
        return TASK_FINISHED.equals(taskState);
    }

    public static boolean isTaskQueued(String taskState) {
        return TASK_QUEUED.equals(taskState);
    }

    public static boolean isTaskPending(String taskState) {
        return TASK_PENDING.equals(taskState);
    }

    public static boolean isTaskNotRunning(String taskState) {
        return TASK_NOT_RUNNING.equals(taskState);
    }


    private static final String TASK_SUCCESSFUL = "PASSED";
    private static final String TASK_FAILED = "FAILED";
    private static final String TASK_SKIPPED = "SKIPPED";


    public static boolean isTaskSuccessful(String taskStatus) {
        return TASK_SUCCESSFUL.equals(taskStatus);
    }

    public static boolean isTaskFailed(String taskStatus) {
        return TASK_FAILED.equals(taskStatus);
    }

    public static boolean isTaskSkipped(String taskStatus) {
        return TASK_SKIPPED.equals(taskStatus);
    }

    private static Link createJobLink(String caption, Long lastJobId, ServerApi server) {
        HashMap<String, Object> paramMap = new HashMap<>();
        paramMap.put(JOB_ID_PARAM_KEY, lastJobId);

        return createInternalLink(MainUI.VIEW_FRAGMENT_JOBS, paramMap, caption,
                VmidcMessages.getString(VmidcMessages_.JOB_LINK_TOOLTIP), server);
    }

    private static Link createInternalLink(String fragment, HashMap<String, Object> paramMap, String linkCaption,
            String linkDescription, ServerApi server) {

        String jobLinkUrl = createInternalUrl(fragment, paramMap, server);
        if (jobLinkUrl == null) {
            return null;
        }
        Link jobLink = new Link();
        jobLink.setCaption(linkCaption);
        jobLink.setDescription(linkDescription);
        jobLink.setResource(new ExternalResource(jobLinkUrl));

        return jobLink;
    }

    private static String getCurrentPageUrl(ServerApi server) {
        String url;
        if (Page.getCurrent() != null && Page.getCurrent().getLocation() != null) {
            url = Page.getCurrent().getLocation().toString();
        } else if (VaadinServletService.getCurrentRequest() != null) {
            url = VaadinServletService.getCurrentRequest().getContextPath();
        } else {
            url = "https://" + server.getServerIpAddress() + "/";
        }
        // Workaround bug in URL generation
        url = url.replace("#!", "/#!");
        url = url.replace("//#!", "/#!");
        return url;
    }

    public static String createInternalUrl(String fragment, HashMap<String, Object> paramMap,
            ServerApi server) {
        String url = getCurrentPageUrl(server);
        try {
            URL currentUrl = new URL(url);

            if (paramMap == null) {
                String linkUrl = currentUrl.toString().replace(currentUrl.getRef(),
                        "!" + URLEncoder.encode(fragment, "UTF-8"));
                return linkUrl;
            }

            if (currentUrl.getRef() != null) {
                String linkUrl = currentUrl.toString().replace(currentUrl.getRef(),
                        "!" + URLEncoder.encode(fragment, "UTF-8") + mapToString(paramMap));
                return linkUrl;
            } else {
                return currentUrl.toString().replace(currentUrl.getPath(),
                        "/#!" + URLEncoder.encode(fragment, "UTF-8") + mapToString(paramMap));
            }
        } catch (MalformedURLException | UnsupportedEncodingException e) {
            log.error("Unable to get location for currentPage: " + Page.getCurrent().getLocation().toString(), e);
            return null;
        }
    }

    /**
     * @param toolbar
     *            HorizontalLayout which contains all the action Buttons
     * @param toolbarButton
     *            Which Tool bar button to create (Provided using ENUM constant)
     * @param listner
     *            Click listener called when this button is clicked
     * @return
     */

    public static Button buildToolbarButton(HorizontalLayout toolbar, ToolbarButtons toolbarButton,
            ClickListener listner) {
        Button button = new Button(toolbarButton.getText());
        button.addStyleName(StyleConstants.BUTTON_TOOLBAR);
        button.setDescription(toolbarButton.getTooltip());
        button.setStyleName(ValoTheme.BUTTON_LINK);
        if (StringUtils.isNotEmpty(toolbarButton.getImageLocation())) {
            button.setIcon(new ThemeResource(toolbarButton.getImageLocation()), toolbarButton.toString());
        }
        button.setEnabled(false);
        button.setId(toolbarButton.getId());
        button.addClickListener(listner);
        toolbar.addComponent(button);
        return button;
    }

    private static String closeNativeWindow(String windowName) {
        StringBuilder stringBuilder = new StringBuilder();

        // Get reference to window
        stringBuilder.append("var window1 = window.open('', '");
        stringBuilder.append(windowName);
        stringBuilder.append("', '', true);");
        // Close the window if exists
        stringBuilder.append("if(window1){window1.close();}");

        return stringBuilder.toString();
    }

    @SuppressWarnings("serial")
    public static FilterGenerator getFilterGenerator() {
        return new FilterGenerator() {

            @Override
            public AbstractField<?> getCustomFilterComponent(Object propertyId) {
                return null;
            }

            @Override
            public Filter generateFilter(Object propertyId, Object value) {
                // For Date filters we need to return timestamp filter instead of date
                if (value instanceof DateInterval) {
                    DateInterval interval = (DateInterval) value;
                    Comparable<?> actualFrom = interval.getFrom();
                    Comparable<?> actualTo = interval.getTo();
                    actualFrom = actualFrom == null ? null : new Timestamp(interval.getFrom().getTime());
                    actualTo = actualTo == null ? null : new Timestamp(interval.getTo().getTime());

                    if (actualFrom != null && actualTo != null) {
                        return new Between(propertyId, actualFrom, actualTo);
                    } else if (actualFrom != null) {
                        return new Compare.GreaterOrEqual(propertyId, actualFrom);
                    } else {
                        return new Compare.LessOrEqual(propertyId, actualTo);
                    }
                }
                return null;
            }

            @Override
            public void filterRemoved(Object propertyId) {
            }

            @Override
            public Filter filterGeneratorFailed(Exception reason, Object propertyId, Object value) {
                return null;
            }

            @Override
            public void filterAdded(Object propertyId, Class<? extends Filter> filterType, Object value) {
            }

            @Override
            public Filter generateFilter(Object propertyId, Field<?> originatingField) {
                return null;
            }
        };
    }

    @SuppressWarnings("serial")
    public static ItemSorter getCaseInsensitiveItemSorter() {
        return new DefaultItemSorter(new DefaultPropertyValueComparator() {

            @Override
            public int compare(Object o1, Object o2) {
                if ((o1 instanceof String) && (o2 instanceof String)) {
                    return ((String) o1).compareToIgnoreCase((String) o2);
                } else {
                    return super.compare(o1, o2);
                }
            }
        });
    }

    public static void addWindow(LoadingIndicatorCRUDBaseWindow window) {
        UI.getCurrent().addWindow(window);
    }

    public static void addWindow(Window window) {
        UI.getCurrent().addWindow(window);
        window.focus();
    }

    public static void showJobNotification(Long jobId, ServerApi server) {
        HashMap<String, Object> paramMap = null;
        if (jobId != null) {
            paramMap = new HashMap<>();
            paramMap.put(JOB_ID_PARAM_KEY, jobId);
        }

        String jobLinkUrl = createInternalUrl(MainUI.VIEW_FRAGMENT_JOBS, paramMap, server);
        if (jobLinkUrl != null) {
            if (jobId == null) {
                new Notification("Info", "<a href=\"" + jobLinkUrl + "\">" + " Go To Job View" + "</a>",
                        Notification.Type.TRAY_NOTIFICATION, true).show(Page.getCurrent());
            } else {
                new Notification("Info", "Job <a href=\"" + jobLinkUrl + "\">" + jobId + "</a> started.",
                        Notification.Type.TRAY_NOTIFICATION, true).show(Page.getCurrent());
            }
        } else {
            new Notification("Info", "Job started.", Notification.Type.TRAY_NOTIFICATION, true).show(Page.getCurrent());
        }
    }

    public static void showError(String error, Throwable e) {
        log.error(error, e);
        ViewUtil.iscNotification("Error!", error + " (" + e.getMessage() + ")", Notification.Type.ERROR_MESSAGE);

    }

    public static void iscNotification(String description, Notification.Type type) {
        ViewUtil.iscNotification(null, description, type);
    }

    public static void iscNotification(String caption, String description, Notification.Type type) {
        if (caption == null) {
            if (type.equals(Notification.Type.ERROR_MESSAGE)) {
                caption = "Error! ";
            } else if (type.equals(Notification.Type.WARNING_MESSAGE)) {
                caption = "Warning! ";
            }
        }
        Notification notif = new Notification(caption, description, type);
        if (type.equals(Notification.Type.ERROR_MESSAGE)) {
            notif.setDelayMsec(Notification.DELAY_FOREVER);
        } else {
            notif.setDelayMsec(10000); // 10 seconds delay
        }
        notif.show(UI.getCurrent().getPage());
    }

}
