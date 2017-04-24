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

import java.util.LinkedHashMap;
import java.util.Locale;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.osc.core.broker.service.LoginService;
import org.osc.core.broker.service.request.LoginRequest;
import org.osc.core.broker.service.response.LoginResponse;
import org.osc.core.broker.util.BroadcastMessage;
import org.osc.core.broker.view.alarm.AlarmView;
import org.osc.core.broker.view.util.BroadcasterUtil;
import org.osc.core.broker.view.util.BroadcasterUtil.BroadcastListener;
import org.osc.core.broker.view.util.ViewUtil;
import org.osc.core.broker.view.vc.VirtualizationConnectorView;
import org.osc.core.server.Server;
import org.osc.core.util.ServerUtil;
import org.osc.core.util.VersionUtil;

import com.vaadin.annotations.PreserveOnRefresh;
import com.vaadin.annotations.Push;
import com.vaadin.annotations.Theme;
import com.vaadin.data.Validator.EmptyValueException;
import com.vaadin.event.ShortcutAction.KeyCode;
import com.vaadin.event.ShortcutListener;
import com.vaadin.navigator.Navigator;
import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener;
import com.vaadin.server.CustomizedSystemMessages;
import com.vaadin.server.ErrorHandler;
import com.vaadin.server.Page;
import com.vaadin.server.SystemMessages;
import com.vaadin.server.SystemMessagesInfo;
import com.vaadin.server.SystemMessagesProvider;
import com.vaadin.server.ThemeResource;
import com.vaadin.server.UploadException;
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinService;
import com.vaadin.server.VaadinServletService;
import com.vaadin.shared.communication.PushMode;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.shared.ui.ui.Transport;
import com.vaadin.ui.Accordion;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.Component;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Image;
import com.vaadin.ui.Label;
import com.vaadin.ui.NativeButton;
import com.vaadin.ui.Notification;
import com.vaadin.ui.PasswordField;
import com.vaadin.ui.TextField;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.Reindeer;

@Theme("vmidc")
@SuppressWarnings("serial")
@PreserveOnRefresh
@Push(value = PushMode.AUTOMATIC, transport = Transport.WEBSOCKET)
public class MainUI extends UI implements BroadcastListener {

    private static final int SESSION_EXPIRE_TIME_OUT_IN_SECS = 1800;
    // Status View
    public static final String VIEW_FRAGMENT_APPLIANCE_INSTANCES = "Appliance Instances";
    public static final String VIEW_FRAGMENT_JOBS = "Jobs";
    public static final String VIEW_FRAGMENT_ALERTS = "Alerts";

    // Setup Views
    public static final String VIEW_FRAGMENT_VIRTUALIZATION_CONNECTORS = "Virtualization Connectors";
    public static final String VIEW_FRAGMENT_SECURITY_MANAGER_CONNECTORS = "Manager Connectors";
    public static final String VIEW_FRAGMENT_SECURITY_FUNCTION_CATALOG = "Service Function Catalog";
    public static final String VIEW_FRAGMENT_DISTRIBUTED_APPLIANCES = "Distributed Appliance";

    // Manage Views
    public static final String VIEW_FRAGMENT_SERVER = "Server";
    public static final String VIEW_FRAGMENT_PLUGIN = "Plugins";
    public static final String VIEW_FRAGMENT_USERS = "Users";
    public static final String VIEW_FRAGMENT_ALARMS = "Alarms";

    private static final long serialVersionUID = 1L;

    private static final Logger log = Logger.getLogger(MainUI.class);

    public CRUDBaseView<?, ?> currentView;

    // accordion used as side navigation
    private final Accordion accordion = new Accordion();

    // accordion tabs
    private final CssLayout status = new CssLayout();
    private final CssLayout setup = new CssLayout();
    private final CssLayout options = new CssLayout();

    VerticalLayout root = new VerticalLayout();
    VerticalLayout loginLayout;
    HorizontalLayout header = new HorizontalLayout();
    HorizontalLayout mainLayout = new HorizontalLayout();
    CssLayout menu = new CssLayout();
    CssLayout content = new CssLayout();

    LinkedHashMap<String, Class<? extends View>> statusViews = new LinkedHashMap<String, Class<? extends View>>() {
        {
            put(VIEW_FRAGMENT_ALERTS, AlertView.class);
            put(VIEW_FRAGMENT_APPLIANCE_INSTANCES, ApplianceInstanceView.class);
            put(VIEW_FRAGMENT_JOBS, JobView.class);
        }
    };

    LinkedHashMap<String, Class<? extends View>> setupViews = new LinkedHashMap<String, Class<? extends View>>() {
        {
            put(VIEW_FRAGMENT_VIRTUALIZATION_CONNECTORS, VirtualizationConnectorView.class);
            put(VIEW_FRAGMENT_SECURITY_MANAGER_CONNECTORS, ManagerConnectorView.class);
            put(VIEW_FRAGMENT_SECURITY_FUNCTION_CATALOG, ApplianceView.class);
            put(VIEW_FRAGMENT_DISTRIBUTED_APPLIANCES, DistributedApplianceView.class);
        }
    };

    LinkedHashMap<String, Class<? extends View>> manageViews = new LinkedHashMap<String, Class<? extends View>>() {
        {
            put(VIEW_FRAGMENT_USERS, UserView.class);
            put(VIEW_FRAGMENT_ALARMS, AlarmView.class);
            put(VIEW_FRAGMENT_PLUGIN, PluginView.class);
            put(VIEW_FRAGMENT_SERVER, MaintenanceView.class);
        }
    };

    private Navigator nav;

    @Override
    protected void init(VaadinRequest request) {

        Page.getCurrent().setTitle(Server.PRODUCT_NAME);
        setLocale(Locale.US);
        setContent(this.root);
        this.root.addStyleName("root");
        this.root.setSizeFull();

        this.nav = new Navigator(this, this.content);
        this.nav.addView("", AlertView.class);
        this.nav.setErrorView(AlertView.class);

        for (String page : this.statusViews.keySet()) {
            this.nav.addView(page, this.statusViews.get(page));
        }
        for (String page : this.setupViews.keySet()) {
            this.nav.addView(page, this.setupViews.get(page));
        }
        for (String page : this.manageViews.keySet()) {
            this.nav.addView(page, this.manageViews.get(page));
        }

        // setting idle timeout to 30 minutes here instead of web.xml
        request.getWrappedSession().setMaxInactiveInterval(SESSION_EXPIRE_TIME_OUT_IN_SECS);

        if (getSession().getAttribute("user") != null) {
            // session exists go to Main View
            buildMainView();
        } else {
            // no session exists go to login page
            buildLoginForm();
        }

        // override vaadin default error messages
        VaadinService.getCurrent().setSystemMessagesProvider(new SystemMessagesProvider() {
            @Override
            public SystemMessages getSystemMessages(SystemMessagesInfo systemMessagesInfo) {
                CustomizedSystemMessages messages = new CustomizedSystemMessages();
                // disable communication errorS
                messages.setCommunicationErrorCaption(null);
                messages.setCommunicationErrorMessage(null);
                messages.setCommunicationErrorNotificationEnabled(false);
                messages.setSessionExpiredCaption(null);
                messages.setSessionExpiredMessage(Server.PRODUCT_NAME
                        + " session timeout. Please <u>click here</u> to Login again.");
                messages.setSessionExpiredNotificationEnabled(true);
                messages.setSessionExpiredURL(getLogoutUrl());
                return messages;
            }
        });

        // generic error handler for any UI exceptions
        UI.getCurrent().setErrorHandler(new ErrorHandler() {
            @Override
            public void error(com.vaadin.server.ErrorEvent event) {
                if (!(event.getThrowable() instanceof UploadException)) {

                    // show error as an error notification to the user
                    ViewUtil.iscNotification(event.getThrowable().getMessage(), Notification.Type.ERROR_MESSAGE);
                    log.error("Unhandled error", event.getThrowable());
                }
            }
        });
    }

    private void buildLoginForm() {
        addStyleName("login");

        this.loginLayout = new VerticalLayout();
        this.loginLayout.setSizeFull();
        this.loginLayout.addStyleName("login-layout");
        this.root.addComponent(this.loginLayout);

        final CssLayout loginPanel = new CssLayout();
        loginPanel.addStyleName("login-panel");

        HorizontalLayout labels = new HorizontalLayout();
        labels.setWidth("100%");
        labels.setMargin(true);
        labels.addStyleName("labels");
        loginPanel.addComponent(labels);

        Label product = new Label(Server.PRODUCT_NAME);
        product.addStyleName("product-label-login");
        labels.addComponent(new Image(null, new ThemeResource("img/logo.png")));

        labels.addComponent(product);
        labels.setComponentAlignment(product, Alignment.MIDDLE_LEFT);
        labels.setExpandRatio(product, 1);

        HorizontalLayout fields = new HorizontalLayout();
        fields.setSpacing(true);
        fields.setMargin(true);
        fields.addStyleName("fields");

        final TextField username = new TextField("Login ID");
        username.focus();
        username.setImmediate(true);
        username.setRequired(true);
        username.setRequiredError("Login ID or Password cannot be empty");
        fields.addComponent(username);

        final PasswordField password = new PasswordField("Password");
        password.setRequired(true);
        password.setImmediate(true);
        password.setRequiredError("Login ID or Password cannot be empty");
        fields.addComponent(password);

        final Button login = new Button("Log In");
        login.addStyleName("default");
        fields.addComponent(login);
        fields.setComponentAlignment(login, Alignment.BOTTOM_LEFT);

        final ShortcutListener enter = new ShortcutListener("Login", KeyCode.ENTER, null) {

            @Override
            public void handleAction(Object sender, Object target) {
                login.click();
            }
        };

        login.addClickListener(new ClickListener() {

            @Override
            public void buttonClick(ClickEvent event) {
                try {
                    username.validate();
                    password.validate();
                    LoginRequest request = new LoginRequest();
                    request.setLoginName(username.getValue().trim());
                    request.setPassword(password.getValue());
                    LoginService loginService = new LoginService();
                    LoginResponse response = loginService.dispatch(request);
                    if (response != null) {
                        login.removeShortcutListener(enter);
                        if (getSession() != null) {
                            getSession().setAttribute("user", username.getValue());
                        }
                        MainUI.this.root.removeComponent(MainUI.this.loginLayout);
                        buildMainView();
                    }
                } catch (EmptyValueException e) {
                    ViewUtil.iscNotification(e.getMessage() + ".", Notification.Type.ERROR_MESSAGE);
                    username.focus();
                } catch (Exception e) {
                    username.focus();
                    ViewUtil.iscNotification(e.getMessage(), Notification.Type.ERROR_MESSAGE);
                }
            }
        });
        login.addShortcutListener(enter);
        loginPanel.addComponent(fields);
        this.loginLayout.addComponent(loginPanel);
        this.loginLayout.setComponentAlignment(loginPanel, Alignment.MIDDLE_CENTER);
    }

    private void buildMainView() {
        buildHeader();
        buildMainLayout();
        this.root.setExpandRatio(this.mainLayout, 1);
        BroadcasterUtil.register(this);
        // adding view change listener to navigator
        addViewChangeListener();
        String uriFragment = Page.getCurrent().getUriFragment();
        if (StringUtils.isBlank(uriFragment)) {
            uriFragment = VIEW_FRAGMENT_ALERTS;
        }
        String sanitizedFragment = StringUtils.remove(uriFragment, '!').replace('+', ' ');
        this.nav.navigateTo(sanitizedFragment);

    }

    private void buildMainLayout() {
        this.mainLayout.setWidth("100%");
        this.mainLayout.setHeight("100%");
        this.mainLayout.addStyleName("view-content");
        VerticalLayout sidebar = buildSidebar();
        this.mainLayout.addComponent(sidebar);
        // Content
        this.mainLayout.addComponent(this.content);
        this.content.setSizeFull();
        this.mainLayout.setExpandRatio(this.content, 1);
        this.root.addComponent(this.mainLayout);
    }

    private VerticalLayout buildSidebar() {
        VerticalLayout sideBar = new VerticalLayout();
        sideBar.addStyleName("sidebar");
        sideBar.addComponent(buildMainMenu());
        sideBar.setExpandRatio(this.menu, 1);
        sideBar.setWidth(null);
        sideBar.setHeight("100%");
        return sideBar;
    }

    private void buildHeader() {
        this.header.addStyleName("branding");
        this.header.addStyleName("header");

        // product name and information
        Label product = new Label(Server.PRODUCT_NAME + "<br> <span class='product-version'> Version: "
                + VersionUtil.getVersion().getVersionStr() + "</span>", ContentMode.HTML);
        product.addStyleName("product-label");
        product.setSizeUndefined();

        HorizontalLayout brandingLayout = new HorizontalLayout();
        brandingLayout.addStyleName("header-content");
        brandingLayout.addComponent(new Image(null, new ThemeResource("img/logo.png")));
        brandingLayout.addComponent(product);

        // creating home help button
        Button mainHelpButton = new Button();
        mainHelpButton.setImmediate(true);
        mainHelpButton.setStyleName(Reindeer.BUTTON_LINK);
        mainHelpButton.setDescription("Help");
        mainHelpButton.setIcon(new ThemeResource("img/headerHelp.png"));
        mainHelpButton.addClickListener(new ClickListener() {

            private String guid = "";

            @Override
            public void buttonClick(ClickEvent event) {
                ViewUtil.showHelpBrowserWindow(this.guid);
            }

        });

        HorizontalLayout helpLayout = new HorizontalLayout();
        helpLayout.addComponent(mainHelpButton);
        helpLayout.addStyleName("homeHelpButton");

        // Adding current user to header
        Label user = new Label("User: " + getCurrent().getSession().getAttribute("user").toString());
        // header banner
        HorizontalLayout userlayout = new HorizontalLayout();
        userlayout.addStyleName("user");
        userlayout.addComponent(user);
        // create Logout button next to user
        userlayout.addComponent(buildLogout());
        // Adding help button to the user layout next to logout button
        userlayout.addComponent(helpLayout);

        this.header.setWidth("100%");
        this.header.setHeight("65px");
        this.header.addComponent(brandingLayout);
        this.header.addComponent(userlayout);
        this.header.setExpandRatio(brandingLayout, 1);
        this.root.addComponent(this.header);
    }

    private CssLayout buildMainMenu() {
        buildSubmenu(this.status, this.statusViews);
        buildSubmenu(this.setup, this.setupViews);
        buildSubmenu(this.options, this.manageViews);

        this.accordion.addTab(this.status, "Status", new ThemeResource("img/status_header.png"));
        this.accordion.addTab(this.setup, "Setup", new ThemeResource("img/setup_header.png"));
        this.accordion.addTab(this.options, "Manage", new ThemeResource("img/manage_header.png"));

        this.menu.addComponent(this.accordion);
        this.menu.addStyleName("menu");
        this.menu.setHeight("100%");

        return this.menu;
    }

    private void buildSubmenu(CssLayout submenu, LinkedHashMap<String, Class<? extends View>> views) {
        for (final String view : views.keySet()) {
            NativeButton b = new NativeButton(view);
            // selecting default menu button
            if (view.equals(VIEW_FRAGMENT_ALERTS)) {
                b.addStyleName("selected");
            }
            b.addClickListener(new ClickListener() {
                @Override
                public void buttonClick(ClickEvent event) {
                    clearMenuSelection();
                    event.getButton().addStyleName("selected");
                    if (!MainUI.this.nav.getState().equals(view)) {
                        MainUI.this.nav.navigateTo(view);
                    }
                }
            });
            submenu.setSizeFull();
            submenu.addComponent(b);
        }
    }

    private Button buildLogout() {
        Button exit = new Button("Logout");
        exit.setDescription("Logout");
        exit.setWidth("100%");
        exit.addClickListener(new ClickListener() {
            @Override
            public void buttonClick(ClickEvent event) {
                getSession().setAttribute("user", null);
                for (UI ui : getSession().getUIs()) {
                    ui.close();
                }
            }
        });
        return exit;
    }

    private void clearMenuSelection() {
        for (Component next : this.menu) {
            if (next instanceof Accordion) {
                clearAccordionSubMenu((Accordion) next);
            }
        }
    }

    private void clearAccordionSubMenu(Accordion subMenu) {
        for (Component next : subMenu) {
            if (next instanceof CssLayout) {
                clearSubmenuSelection((CssLayout) next);
            }
        }
    }

    private void clearSubmenuSelection(CssLayout subMenu) {
        for (Component next : subMenu) {
            if (next instanceof NativeButton) {
                next.removeStyleName("selected");
            }
        }
    }

    @Override
    public void receiveBroadcast(final BroadcastMessage msg) {
        access(new Runnable() {
            @Override
            public void run() {
                try {
                    if (MainUI.this.currentView == null) {
                        return;
                    }
                    if (msg.getEntityId() == Long.MIN_VALUE) {
                        return;
                    }
                    String dto = msg.getReceiver() + "Dto";
                    if (MainUI.this.currentView.isDtoChangeRelevantToParentView(dto)) {
                        MainUI.this.currentView.syncTables(msg, false);
                    } else if (MainUI.this.currentView.isDtoChangeRelevantToChildView(dto)) {
                        MainUI.this.currentView.syncTables(msg, true);
                    } else if (MainUI.this.currentView.isDtoRelevantToParentSubView(dto)) {
                        MainUI.this.currentView.delegateBroadcastMessagetoSubView(msg, false);
                    } else if (MainUI.this.currentView.isDtoRelevantToChildSubView(dto)) {
                        MainUI.this.currentView.delegateBroadcastMessagetoSubView(msg, true);
                    }
                } catch (Exception e) {
                    log.error("Fail to receive DTO broadcast", e);
                }
            }
        });
    }

    @Override
    public void detach() {
        try {
            // unregister before closing
            BroadcasterUtil.unregister(this);
            log.info("MainUI.detach() called");
            super.detach();
        } catch (Exception e) {
            log.error("Detach Exception " + e.getMessage());
        }
    }

    private String getLogoutUrl() {
        if (VaadinServletService.getCurrentRequest() != null) {
            return VaadinServletService.getCurrentRequest().getContextPath();
        } else {
            return "https://" + ServerUtil.getServerIP() + "/";
        }
    }

    @Override
    public void close() {
        try {
            log.info("MainUI.close() called");
            if (UI.getCurrent() != null && UI.getCurrent().getPage() != null) {
                UI.getCurrent().getPage().setLocation(getLogoutUrl());
            }
            super.close();
        } catch (Exception e) {
            log.error("Destroy Exception " + e.getMessage());
        }
    }

    private void addViewChangeListener() {
        this.nav.addViewChangeListener(new ViewChangeListener() {
            @Override
            public boolean beforeViewChange(ViewChangeEvent event) {
                return true;
            }

            @Override
            public void afterViewChange(ViewChangeEvent event) {
                if (updateAccordion(event, MainUI.this.statusViews, MainUI.this.status)) {
                    return;
                } else if (updateAccordion(event, MainUI.this.setupViews, MainUI.this.setup)) {
                    return;
                } else {
                    updateAccordion(event, MainUI.this.manageViews, MainUI.this.options);
                }
            }

            private boolean updateAccordion(ViewChangeEvent event,
                    LinkedHashMap<String, Class<? extends View>> viewMap, CssLayout subMenu) {
                int i = 0;
                for (Class<? extends View> view : viewMap.values()) {
                    if (event.getNewView().getClass().equals(view)) {
                        clearMenuSelection();
                        MainUI.this.accordion.setSelectedTab(subMenu);
                        if (subMenu.getComponent(i) != null) {
                            subMenu.getComponent(i).addStyleName("selected");
                        }
                        return true;

                    }
                    i++;
                }
                return false;
            }

        });
    }
}
