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

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.jclouds.openstack.keystone.v2_0.domain.Tenant;
import org.osc.core.broker.model.entities.virtualization.SecurityGroupMemberType;
import org.osc.core.broker.service.openstack.ListOpenstackMembersService;
import org.osc.core.broker.service.openstack.ListRegionByVcIdService;
import org.osc.core.broker.service.openstack.ListTenantByVcIdService;
import org.osc.core.broker.service.openstack.request.BaseOpenStackRequest;
import org.osc.core.broker.service.openstack.request.ListOpenstackMembersRequest;
import org.osc.core.broker.service.request.BaseIdRequest;
import org.osc.core.broker.service.response.ListResponse;
import org.osc.core.broker.service.response.SetResponse;
import org.osc.core.broker.service.securitygroup.ListSecurityGroupMembersBySgService;
import org.osc.core.broker.service.securitygroup.SecurityGroupDto;
import org.osc.core.broker.service.securitygroup.SecurityGroupMemberItemDto;
import org.osc.core.broker.view.common.StyleConstants;
import org.osc.core.broker.view.common.VmidcMessages;
import org.osc.core.broker.view.common.VmidcMessages_;
import org.osc.core.broker.view.util.ViewUtil;
import org.osc.core.broker.window.LoadingIndicatorCRUDBaseWindow;
import org.osc.core.broker.window.ProgressIndicatorWindow;
import org.tepi.filtertable.FilterDecorator;
import org.tepi.filtertable.FilterTable;
import org.tepi.filtertable.numberfilter.NumberFilterPopupConfig;

import com.google.common.collect.Sets;
import com.vaadin.data.Container.ItemSetChangeEvent;
import com.vaadin.data.Container.ItemSetChangeListener;
import com.vaadin.data.Property;
import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.data.util.BeanContainer;
import com.vaadin.data.util.BeanItemContainer;
import com.vaadin.event.dd.DragAndDropEvent;
import com.vaadin.event.dd.DropHandler;
import com.vaadin.event.dd.acceptcriteria.AcceptAll;
import com.vaadin.event.dd.acceptcriteria.AcceptCriterion;
import com.vaadin.server.Resource;
import com.vaadin.shared.ui.datefield.Resolution;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.CustomTable;
import com.vaadin.ui.CustomTable.ColumnGenerator;
import com.vaadin.ui.CustomTable.TableDragMode;
import com.vaadin.ui.CustomTable.TableTransferable;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Notification;
import com.vaadin.ui.OptionGroup;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.ValoTheme;

public abstract class BaseSecurityGroupWindow extends LoadingIndicatorCRUDBaseWindow {

    private static final int TEXT_CHANGE_FILTER_DELAY = 500;

    private static final int SELECTOR_TABLE_HEIGHT = 200;
    private static final int SELECTOR_TABLE_WIDTH = 400;
    private static final int ITEMS_NAME_COLUMN_WIDTH = SELECTOR_TABLE_WIDTH - 3;

    private static final int SELECTED_TABLE_WIDTH = SELECTOR_TABLE_WIDTH + 250;

    private static final int SELECTED_ITEMS_NAME_COLUMN_WIDTH = ITEMS_NAME_COLUMN_WIDTH - 130;

    private static final int SELECTED_ITEMS_REGION_COLUMN_WIDTH = SELECTED_TABLE_WIDTH - ITEMS_NAME_COLUMN_WIDTH - 150;

    private static final int SELECTED_ITEMS_TYPE_COLUMN_WIDTH = SELECTED_TABLE_WIDTH - SELECTED_ITEMS_NAME_COLUMN_WIDTH
            - SELECTED_ITEMS_REGION_COLUMN_WIDTH - 165;

    private static final String PROTECT_EXTERNAL = "protectExternal";

    private final class ImmediateTextFilterDecorator implements FilterDecorator {

        /**
         *
         */
        private static final long serialVersionUID = 1L;

        @Override
        public boolean usePopupForNumericProperty(Object propertyId) {
            return false;
        }

        @Override
        public boolean isTextFilterImmediate(Object propertyId) {
            return true;
        }

        @Override
        public String getToCaption() {
            return null;
        }

        @Override
        public int getTextChangeTimeout(Object propertyId) {
            return TEXT_CHANGE_FILTER_DELAY;
        }

        @Override
        public String getSetCaption() {
            return null;
        }

        @Override
        public NumberFilterPopupConfig getNumberFilterPopupConfig() {
            return null;
        }

        @Override
        public Locale getLocale() {
            return null;
        }

        @Override
        public String getFromCaption() {
            return null;
        }

        @Override
        public Resource getEnumFilterIcon(Object propertyId, Object value) {
            return null;
        }

        @Override
        public String getEnumFilterDisplayName(Object propertyId, Object value) {
            return null;
        }

        @Override
        public String getDateFormatPattern(Object propertyId) {
            return null;
        }

        @Override
        public Resolution getDateFieldResolution(Object propertyId) {
            return null;
        }

        @Override
        public String getClearCaption() {
            return null;
        }

        @Override
        public Resource getBooleanFilterIcon(Object propertyId, boolean value) {
            return null;
        }

        @Override
        public String getBooleanFilterDisplayName(Object propertyId, boolean value) {
            return null;
        }

        @Override
        public String getAllItemsVisibleString() {
            return null;
        }
    }

    private static final long serialVersionUID = 1L;
    private static Logger log = Logger.getLogger(BaseSecurityGroupWindow.class);

    protected SecurityGroupDto currentSecurityGroup = null;

    private ValueChangeListener tenantChangedListener;
    private ValueChangeListener triggerPopulateFromListListener;
    /**
     * Indicates whether the from list is being loaded for the first time or not. After the first time
     * the from list is loaded, this is set to false.
     */
    private boolean isInitialFromListLoad = true;

    protected static final String TYPE_SELECTION = "By Type";
    protected static final String TYPE_ALL = "All Servers belonging to Tenant";

    // form fields
    protected TextField name;
    protected ComboBox tenant;
    protected ComboBox region;
    protected OptionGroup protectionTypeOption;
    protected ComboBox protectionEntityType;
    protected VerticalLayout selector;

    protected FilterTable itemsTable;
    protected FilterTable selectedItemsTable;

    protected BeanContainer<String, SecurityGroupMemberItemDto> itemsContainer;
    protected BeanContainer<String, SecurityGroupMemberItemDto> selectedItemsContainer;

    public BaseSecurityGroupWindow() {
        super();
        initListeners();
    }

    @SuppressWarnings("serial")
    private Component getType() {
        this.protectionTypeOption = new OptionGroup("Selection Type:");
        this.protectionTypeOption.addItem(TYPE_ALL);
        this.protectionTypeOption.addItem(TYPE_SELECTION);
        this.protectionTypeOption.select(TYPE_ALL);
        this.protectionTypeOption.addValueChangeListener(new ValueChangeListener() {
            @Override
            public void valueChange(ValueChangeEvent event) {
                if (BaseSecurityGroupWindow.this.protectionTypeOption.getValue() == TYPE_ALL) {
                    enableSelection(false);
                } else if (BaseSecurityGroupWindow.this.protectionTypeOption.getValue() == TYPE_SELECTION) {
                    enableSelection(true);
                }
                // Populate to list first and then the from list since we use the 'to' list items to exclude them from
                // the 'from' list
                populateToList();
                populateFromList();
            }
        });

        return this.protectionTypeOption;
    }

    private Component getProtectionEntityType() {
        this.protectionEntityType = new ComboBox();
        this.protectionEntityType.setTextInputAllowed(false);
        this.protectionEntityType.setNullSelectionAllowed(false);
        this.protectionEntityType.addItem(SecurityGroupMemberType.VM);
        this.protectionEntityType.addItem(SecurityGroupMemberType.NETWORK);
        this.protectionEntityType.addItem(SecurityGroupMemberType.SUBNET);
        this.protectionEntityType.select(SecurityGroupMemberType.VM);

        this.protectionEntityType.addValueChangeListener(this.triggerPopulateFromListListener);

        return this.protectionEntityType;
    }

    protected void enableSelection(boolean enable) {
        this.protectionEntityType.setEnabled(enable);
        this.selector.setEnabled(enable);
        this.selectedItemsTable.setEnabled(enable);
        this.itemsTable.setEnabled(enable);
    }

    @Override
    public void initForm() {
        try {
            this.form.addComponent(getName());
            this.form.addComponent(getTenant());
            this.form.addComponent(getRegion());
            this.form.addComponent(getType());
            this.form.addComponent(getProtectionEntityType());
            this.content.addComponent(getSelectionWidget());

        } catch (Exception e) {
            ViewUtil.iscNotification(e.getMessage(), Notification.Type.ERROR_MESSAGE);
        }
    }

    @Override
    public void makeServiceCalls(ProgressIndicatorWindow progressIndicatorWindow) {
        progressIndicatorWindow.updateStatus("Populating Tenant Information");
        // Dont auto select tenant in case of update, since update sets the tenant automatically once the load completes.
        populateTenants(!isUpdateWindow());
        progressIndicatorWindow.updateStatus("Populating Region Information");
        populateRegion();
    }

    @Override
    public boolean validateForm() {
        try {
            this.name.validate();
            this.tenant.validate();
            return true;
        } catch (Exception e) {
            ViewUtil.iscNotification(e.getMessage() + ".", Notification.Type.ERROR_MESSAGE);
        }
        return false;
    }

    protected TextField getName() {
        this.name = new TextField("Name");
        this.name.setImmediate(true);
        this.name.setRequired(true);
        this.name.setRequiredError("Name cannot be empty");
        return this.name;
    }

    protected ComboBox getTenant() {
        try {
            this.tenant = new ComboBox("Select Tenant");
            this.tenant.setTextInputAllowed(true);
            this.tenant.setNullSelectionAllowed(false);
            this.tenant.setImmediate(true);
            this.tenant.setRequired(true);
            this.tenant.setRequiredError("Tenant cannot be empty");

        } catch (Exception e) {
            ViewUtil.iscNotification(e.getMessage(), Notification.Type.ERROR_MESSAGE);
            log.error("Error populating Tenant List combobox", e);
        }

        return this.tenant;
    }

    protected ComboBox getRegion() {
        this.region = new ComboBox("Select Region");
        this.region.setTextInputAllowed(false);
        this.region.setNullSelectionAllowed(true);
        this.region.setImmediate(true);

        return this.region;
    }

    @SuppressWarnings("serial")
    protected Component getSelectionWidget() {
        this.selector = new VerticalLayout();
        this.selector.addStyleName(StyleConstants.VMIDC_WINDOW_CONTENT_WRAPPER);
        this.selector.setSizeFull();

        this.itemsTable = createSelectorTable();
        this.itemsTable.setCaption(VmidcMessages.getString(VmidcMessages_.SELECTOR_TITLE));
        this.itemsTable.setColumnWidth("name", ITEMS_NAME_COLUMN_WIDTH);

        this.itemsContainer = createItemContainer();
        this.itemsTable.setContainerDataSource(this.itemsContainer);
        this.itemsTable.setVisibleColumns("name");

        this.selectedItemsTable = createSelectorTable();
        this.selectedItemsTable.setCaption(VmidcMessages.getString(VmidcMessages_.SELECTED_TITLE));
        this.selectedItemsTable.setWidth(SELECTED_TABLE_WIDTH + "px");
        this.selectedItemsTable.setColumnWidth("name", SELECTED_ITEMS_NAME_COLUMN_WIDTH);
        this.selectedItemsTable.setColumnWidth("region", SELECTED_ITEMS_REGION_COLUMN_WIDTH);
        this.selectedItemsTable.setColumnWidth("type", SELECTED_ITEMS_TYPE_COLUMN_WIDTH);

        this.selectedItemsContainer = createItemContainer();
        this.selectedItemsTable.setContainerDataSource(this.selectedItemsContainer);
        this.selectedItemsTable.setVisibleColumns("name", "region", "type", PROTECT_EXTERNAL);
        this.selectedItemsTable.setColumnHeader(PROTECT_EXTERNAL, "Protect External");

        VerticalLayout selectorButtonLayout = new VerticalLayout();
        selectorButtonLayout.addStyleName(StyleConstants.SELECTOR_BUTTON_LAYOUT);

        Button addButton = new Button(VmidcMessages.getString(VmidcMessages_.SELECTOR_TO_BUTTON));
        addButton.addStyleName(StyleConstants.SELECTOR_BUTTON);
        addButton.addClickListener(new ClickListener() {

            @Override
            public void buttonClick(ClickEvent event) {
                moveItems(BaseSecurityGroupWindow.this.itemsTable, BaseSecurityGroupWindow.this.selectedItemsTable);
            }

        });
        Button removeButton = new Button(VmidcMessages.getString(VmidcMessages_.SELECTOR_FROM_BUTTON));
        removeButton.addStyleName(StyleConstants.SELECTOR_BUTTON);
        removeButton.addClickListener(new ClickListener() {

            @Override
            public void buttonClick(ClickEvent event) {
                moveItems(BaseSecurityGroupWindow.this.selectedItemsTable, BaseSecurityGroupWindow.this.itemsTable);
            }
        });

        selectorButtonLayout.addComponent(addButton);
        selectorButtonLayout.addComponent(removeButton);

        HorizontalLayout selectorLayout = new HorizontalLayout();

        selectorLayout.addComponent(createSelectorTableLayout(this.itemsTable, this.itemsContainer));
        selectorLayout.addComponent(selectorButtonLayout);
        selectorLayout.addComponent(createSelectorTableLayout(this.selectedItemsTable, this.selectedItemsContainer));

        this.selector.addComponent(selectorLayout);

        return this.selector;

    }

    protected void updateCountFooter(CustomTable table, int count) {
        table.setColumnFooter("name", VmidcMessages.getString(VmidcMessages_.SELECTOR_COUNT, count));
    }

    protected void populateToList() {
        this.selectedItemsContainer.removeAllItems();
        this.selectedItemsTable.removeAllItems();
        if (this.protectionTypeOption.getValue() != TYPE_ALL && this.currentSecurityGroup.getId() != null) {
            try {
                ListSecurityGroupMembersBySgService memberListService = new ListSecurityGroupMembersBySgService();

                SetResponse<SecurityGroupMemberItemDto> response = memberListService.dispatch(new BaseIdRequest(
                        this.currentSecurityGroup.getId()));
                CustomTable toTable = this.selectedItemsTable;
                for (SecurityGroupMemberItemDto memberItem : response.getSet()) {
                    this.selectedItemsContainer.addBean(memberItem);
                    handleProtectExternal(toTable, memberItem.getOpenstackId(), memberItem);
                }
                updateCountFooter(this.selectedItemsTable, this.selectedItemsTable.getItemIds().size());
            } catch (Exception e) {
                ViewUtil.iscNotification(e.getMessage(), Notification.Type.ERROR_MESSAGE);
                log.error("Error getting to Servers List", e);
            }
        }

    }

    private void populateTenants(boolean autoSelect) {
        try {
            Long vcId = this.currentSecurityGroup.getParentId();

            if (vcId != null) {
                // Calling List Service
                BaseIdRequest req = new BaseIdRequest();
                req.setId(vcId);
                ListTenantByVcIdService service = new ListTenantByVcIdService();

                List<Tenant> tenantList = service.dispatch(req).getList();

                this.tenant.removeValueChangeListener(this.tenantChangedListener);
                this.tenant.removeAllItems();

                BeanItemContainer<Tenant> tenantListContainer = new BeanItemContainer<Tenant>(Tenant.class, tenantList);
                this.tenant.setContainerDataSource(tenantListContainer);
                this.tenant.setItemCaptionPropertyId("name");

                this.tenant.addValueChangeListener(this.tenantChangedListener);
                if (autoSelect && tenantList.get(0) != null) {
                    this.tenant.select(tenantList.get(0));
                }
            } else {
                this.tenant.removeAllItems();
            }
        } catch (Exception e) {
            ViewUtil.iscNotification(e.getMessage(), Notification.Type.ERROR_MESSAGE);
            log.error("Error getting tenant List", e);
        }
    }

    private void populateRegion() {
        try {
            Tenant tenantDto = (Tenant) this.tenant.getValue();

            if (tenantDto != null) {
                this.region.removeValueChangeListener(this.triggerPopulateFromListListener);
                this.region.removeAllItems();

                BaseOpenStackRequest req = new BaseOpenStackRequest();
                req.setTenantName(tenantDto.getName());
                req.setTenantId(tenantDto.getId());
                req.setId(this.currentSecurityGroup.getParentId());

                ListRegionByVcIdService service = new ListRegionByVcIdService();
                ListResponse<String> response = service.dispatch(req);

                this.region.addItems(response.getList());

                this.region.addValueChangeListener(this.triggerPopulateFromListListener);
                if (response.getList().get(0) != null) {
                    this.region.select(response.getList().get(0));
                }
            } else {
                this.region.removeAllItems();
            }
        } catch (Exception e) {
            ViewUtil.iscNotification(e.getMessage(), Notification.Type.ERROR_MESSAGE);
            log.error("Error getting Region List", e);
        }
    }

    private void populateFromList() {
        try {

            Tenant tenantDto = (Tenant) this.tenant.getValue();
            String region = (String) this.region.getValue();
            SecurityGroupMemberType memberType = (SecurityGroupMemberType) this.protectionEntityType.getValue();
            boolean isProtectAll = this.protectionTypeOption.getValue() == TYPE_ALL;

            this.itemsContainer.removeAllItems();
            this.itemsTable.removeAllItems();

            if (tenantDto != null && StringUtils.isNotEmpty(region) && !isProtectAll) {

                ListOpenstackMembersRequest req = new ListOpenstackMembersRequest();
                if (this.currentSecurityGroup.getId() != null) {
                    req.setId(this.currentSecurityGroup.getId());
                }
                req.setParentId(this.currentSecurityGroup.getParentId());
                req.setTenantName(tenantDto.getName());
                req.setTenantId(tenantDto.getId());
                req.setRegion(region);
                req.setType(memberType);
                if (this.isInitialFromListLoad) {
                    req.setCurrentSelectedMembers(null);
                } else {
                    req.setCurrentSelectedMembers(getSelectedMembers());
                }
                ListOpenstackMembersService service = new ListOpenstackMembersService();
                ListResponse<SecurityGroupMemberItemDto> response = service.dispatch(req);
                this.isInitialFromListLoad = false;

                this.itemsContainer.addAll(response.getList());
                updateCountFooter(this.itemsTable, this.itemsContainer.size());

                this.itemsContainer.sort(new Object[] { "name" }, new boolean[] { true });
            }
        } catch (Exception e) {
            ViewUtil.iscNotification(e.getMessage(), Notification.Type.ERROR_MESSAGE);
            log.error("Error getting Servers List", e);
        }
    }

    @SuppressWarnings("serial")
    private void initListeners() {
        this.tenantChangedListener = new ValueChangeListener() {

            @Override
            public void valueChange(ValueChangeEvent event) {
                if (BaseSecurityGroupWindow.this.selectedItemsContainer != null) {
                    BaseSecurityGroupWindow.this.selectedItemsContainer.removeAllItems();
                }
                if (BaseSecurityGroupWindow.this.itemsContainer != null) {
                    BaseSecurityGroupWindow.this.itemsContainer.removeAllItems();
                }
                if (BaseSecurityGroupWindow.this.region != null) {
                    populateRegion();
                }
            }
        };
        this.triggerPopulateFromListListener = new ValueChangeListener() {

            @Override
            public void valueChange(ValueChangeEvent event) {
                if (BaseSecurityGroupWindow.this.itemsTable != null) {
                    populateFromList();
                }
            }
        };
        // We don't want enter to close the dialog
        getComponentModel().getOkButton().removeClickShortcut();
    }

    private Button getTableItemsSelectionButton(String text) {
        Button itemsSelectionButton = new Button(text);
        itemsSelectionButton.addStyleName(ValoTheme.BUTTON_LINK);
        itemsSelectionButton.addStyleName(StyleConstants.LINK_BUTTON);
        return itemsSelectionButton;
    }

    @SuppressWarnings("serial")
    private FilterTable createSelectorTable() {
        final FilterTable table = new FilterTable();
        table.setStyleName(ValoTheme.TABLE_COMPACT);
        table.setSizeFull();
        table.setSelectable(true);
        table.setColumnCollapsingAllowed(false);
        table.setColumnReorderingAllowed(false);
        table.setImmediate(true);
        table.setNullSelectionAllowed(false);
        table.setFilterBarVisible(true);
        table.setPageLength(3);
        updateCountFooter(table, 0);
        table.setMultiSelect(true);

        table.setHeight(SELECTOR_TABLE_HEIGHT + "px");
        table.setWidth(SELECTOR_TABLE_WIDTH + "px");
        table.setFooterVisible(true);
        table.setColumnHeader("name", VmidcMessages.getString(VmidcMessages_.NAME));
        table.setColumnHeader("region", VmidcMessages.getString(VmidcMessages_.OS_REGION));
        table.setColumnHeader("type", VmidcMessages.getString(VmidcMessages_.OS_MEMBER_TYPE));
        table.addGeneratedColumn(PROTECT_EXTERNAL, new CheckBoxGenerator());

        table.sort(new Object[] { "name" }, new boolean[] { true });
        table.setFilterDecorator(new ImmediateTextFilterDecorator());
        // Enable Drag drop
        table.setDragMode(TableDragMode.MULTIROW);
        table.setDropHandler(new DropHandler() {

            @Override
            public AcceptCriterion getAcceptCriterion() {
                return AcceptAll.get();
            }

            @SuppressWarnings("unchecked")
            @Override
            public void drop(DragAndDropEvent event) {
                CustomTable fromTable = ((TableTransferable) event.getTransferable()).getSourceComponent();
                CustomTable toTable = (CustomTable) event.getTargetDetails().getTarget();
                Set<String> itemIdsSelected = (Set<String>) fromTable.getValue();
                // If drag started on an unselected row, select it in the from table before we move
                if (itemIdsSelected == null || itemIdsSelected.size() == 0) {
                    fromTable.select(((TableTransferable) event.getTransferable()).getItemId());
                }
                moveItems(fromTable, toTable);
            }
        });

        table.addItemSetChangeListener(new ItemSetChangeListener() {

            @Override
            public void containerItemSetChange(ItemSetChangeEvent event) {
                updateCountFooter(table, table.getItemIds().size());
            }
        });

        return table;
    }

    @SuppressWarnings("unchecked")
    protected Set<SecurityGroupMemberItemDto> getSelectedMembers() {
        this.selectedItemsTable.getFilterable().removeAllContainerFilters();
        Collection<String> itemIdSelected = (Collection<String>) this.selectedItemsTable.getItemIds();
        Set<SecurityGroupMemberItemDto> selectedMembers = new HashSet<>();
        for (String itemId : itemIdSelected) {
            selectedMembers.add(this.selectedItemsContainer.getItem(itemId).getBean());
        }

        return selectedMembers;
    }

    @SuppressWarnings("unchecked")
    private void moveItems(CustomTable fromTable, CustomTable toTable) {
        if (fromTable.equals(toTable)) {
            return;
        }
        BeanContainer<String, SecurityGroupMemberItemDto> fromContainer = (BeanContainer<String, SecurityGroupMemberItemDto>) fromTable
                .getContainerDataSource();
        BeanContainer<String, SecurityGroupMemberItemDto> toContainer = (BeanContainer<String, SecurityGroupMemberItemDto>) toTable
                .getContainerDataSource();

        boolean isMovingToSelectedItemList = toTable == this.selectedItemsTable;

        SecurityGroupMemberType memberType = (SecurityGroupMemberType) this.protectionEntityType.getValue();
        Set<String> itemIdsSelected = (Set<String>) fromTable.getValue();
        for (String itemId : itemIdsSelected) {
            if (fromContainer.getItem(itemId) != null) {
                SecurityGroupMemberItemDto memberItem = fromContainer.getItem(itemId).getBean();
                // Add the item to the 'to' container, if the 'to' container is the selected items table
                if (isMovingToSelectedItemList) {
                    toContainer.addBean(memberItem);
                    handleProtectExternal(toTable, itemId, memberItem);
                } else if (memberItem.getType() == memberType) {
                    // If the 'to' container is not the selected list, we need to check the current selected type
                    // from the UI and add the member only if it matches the selected type
                    toContainer.addBean(memberItem);
                }
            }
            fromContainer.removeItem(itemId);
            fromTable.removeItem(itemId);
        }

        toTable.setValue(itemIdsSelected);
        updateCountFooter(fromTable, fromTable.getItemIds().size());
        updateCountFooter(toTable, toTable.getItemIds().size());

        toTable.sort(new Object[] { toTable.getSortContainerPropertyId() }, new boolean[] { toTable.isSortAscending() });
    }

    private void handleProtectExternal(CustomTable toTable, String itemId, SecurityGroupMemberItemDto memberItem) {
        boolean enableProtectExternalFlag = !SecurityGroupMemberType.SUBNET.equals(memberItem.getType());
        toTable.getContainerProperty(itemId, PROTECT_EXTERNAL).setReadOnly(enableProtectExternalFlag);
    }

    private BeanContainer<String, SecurityGroupMemberItemDto> createItemContainer() {
        BeanContainer<String, SecurityGroupMemberItemDto> container = new BeanContainer<String, SecurityGroupMemberItemDto>(
                SecurityGroupMemberItemDto.class);
        container.setBeanIdProperty("openstackId");
        container.setItemSorter(ViewUtil.getCaseInsensitiveItemSorter());
        return container;
    }

    @SuppressWarnings("serial")
    private Component createSelectorTableLayout(final FilterTable table,
            final BeanContainer<String, SecurityGroupMemberItemDto> tableContainer) {
        VerticalLayout layout = new VerticalLayout();

        Button allButton = getTableItemsSelectionButton(VmidcMessages.getString(VmidcMessages_.SELECTOR_ALL));
        allButton.addClickListener(new ClickListener() {

            @Override
            public void buttonClick(ClickEvent event) {
                table.setValue(tableContainer.getItemIds());
            }
        });

        Button noneButton = getTableItemsSelectionButton(VmidcMessages.getString(VmidcMessages_.SELECTOR_NONE));
        noneButton.addClickListener(new ClickListener() {

            @Override
            public void buttonClick(ClickEvent event) {
                table.setValue(null);
            }
        });

        Button invertButton = getTableItemsSelectionButton(VmidcMessages.getString(VmidcMessages_.SELECTOR_INVERT));

        invertButton.addClickListener(new ClickListener() {

            @SuppressWarnings("unchecked")
            @Override
            public void buttonClick(ClickEvent event) {
                Set<String> itemIdsSelected = (Set<String>) table.getValue();
                Collection<String> allItems = (Collection<String>) table.getItemIds();
                table.setValue(Sets.difference(new HashSet<>(allItems), itemIdsSelected));
            }
        });
        HorizontalLayout buttonLayout = new HorizontalLayout(allButton, noneButton, invertButton);

        layout.addComponent(table);
        layout.addComponent(buttonLayout);

        return layout;
    }

    private boolean isUpdateWindow() {
        return this.currentSecurityGroup.getId() != null;
    }

    @SuppressWarnings("serial")
    private class CheckBoxGenerator implements ColumnGenerator {
        @Override
        public Object generateCell(CustomTable source, Object itemId, Object columnId) {
            Property<?> prop = source.getItem(itemId).getItemProperty(columnId);
            return new CheckBox(null, prop);
        }
    }
}
