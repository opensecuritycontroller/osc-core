package org.osc.core.broker.view;

import org.osc.core.broker.view.common.StyleConstants;
import org.osc.core.broker.view.common.VmidcMessages;
import org.osc.core.broker.view.common.VmidcMessages_;
import org.osc.core.broker.view.maintenance.ManagerPluginsLayout;
import org.osc.core.broker.view.maintenance.SdnControllerPluginsLayout;
import org.osc.core.broker.view.util.ViewUtil;

import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.Panel;
import com.vaadin.ui.TabSheet;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.ValoTheme;

@SuppressWarnings("serial")
public class PluginView extends VerticalLayout implements View {

    private static final String MAINTENANCE_MANAGER_PLUGIN_GUID = "GUID-07FFF1BC-EA9E-426B-A247-4AF6BD12B350.html";
    private static final String MAINTENANCE_CONTROLLER_PLUGIN_GUID = "GUID-DD7DFB29-CB4F-4DD1-BFF5-21694F740D0E.html";

    TabSheet subMenu = null;
    TabSheet tabs = new TabSheet();

    public PluginView() throws Exception {
        setSizeFull();
        addStyleName(StyleConstants.BASE_CONTAINER);

        // creating tabs sheet for this view
        this.tabs.addStyleName(ValoTheme.TABSHEET_FRAMED);
        this.tabs.setSizeFull();

        // adding SDN Controller Plugin
        this.tabs.addTab(createTab("SDN Controller Plugins", "SDN Controller Plugins",
                new SdnControllerPluginsLayout(), MAINTENANCE_CONTROLLER_PLUGIN_GUID));

        // Adding Manager Plugin tab
        this.tabs.addTab(createTab(VmidcMessages.getString(VmidcMessages_.MAINTENANCE_MANAGERPLUGIN_TITLE),
                VmidcMessages.getString(VmidcMessages_.MAINTENANCE_MANAGERPLUGIN_NAME), new ManagerPluginsLayout(),
                MAINTENANCE_MANAGER_PLUGIN_GUID));

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

    @Override
    public void enter(ViewChangeEvent event) {
    }

}
