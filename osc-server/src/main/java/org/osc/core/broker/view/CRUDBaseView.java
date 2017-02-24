/*******************************************************************************
 * Copyright (c) 2017 Intel Corporation
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.osc.core.broker.service.GetDtoFromEntityService;
import org.osc.core.broker.service.dto.BaseDto;
import org.osc.core.broker.service.request.GetDtoFromEntityRequest;
import org.osc.core.broker.service.response.BaseDtoResponse;
import org.osc.core.broker.util.BroadcastMessage;
import org.osc.core.broker.view.common.StyleConstants;
import org.osc.core.broker.view.util.EventType;
import org.osc.core.broker.view.util.ToolbarButtons;
import org.osc.core.broker.view.util.ToolbarButtons.HorizontalAlignment;
import org.osc.core.broker.view.util.ViewUtil;
import org.tepi.filtertable.FilterTable;

import com.vaadin.data.Container.ItemSetChangeEvent;
import com.vaadin.data.Container.ItemSetChangeListener;
import com.vaadin.data.Item;
import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.data.util.BeanContainer;
import com.vaadin.data.util.BeanItem;
import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener;
import com.vaadin.server.ThemeResource;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.VerticalSplitPanel;
import com.vaadin.ui.themes.Reindeer;
import com.vaadin.ui.themes.ValoTheme;

/**
 * @param
 *            <P>
 *            Parent DTO class
 * @param <C>
 *            Child DTO class
 */
public abstract class CRUDBaseView<P extends BaseDto, C extends BaseDto> extends VerticalLayout implements View {

    //private static final Logger log = Logger.getLogger(CRUDBaseView.class);

    public static final long NULL_SELECTION_ITEM_ID = -1L;

    private static final long serialVersionUID = 1L;

    private long parentItemId;
    private long childItemId;

    protected FilterTable parentTable;
    protected FilterTable childTable;

    protected VerticalLayout parentContainerLayout;
    public VerticalLayout childContainerLayout;

    protected HorizontalLayout parentToolbar;
    protected HorizontalLayout childToolbar;

    protected BeanContainer<Long, P> parentContainer;
    protected BeanContainer<Long, C> childContainer;
    protected List<P> itemList;
    public VerticalSplitPanel viewSplitter;

    protected Map<String, CRUDBaseSubView<?, ?>> parentSubViewMap;
    public Map<String, CRUDBaseSubView<?, ?>> childSubViewMap;

    private PageInformationComponent infoText;

    @SuppressWarnings("serial")
    private final ClickListener buttonClickListener = new ClickListener() {

        @Override
        public void buttonClick(ClickEvent event) {
            try {
                buttonClicked(event);
            } catch (Exception e) {
                ViewUtil.showError("Error invoking action in view", e);
            }
        }
    };

    public BeanItem<P> getParentItem() {
        return this.parentContainer.getItem(getParentItemId());
    }

    public BeanItem<C> getChildItem() {
        return this.childContainer.getItem(getChildItemId());
    }

    public void createView(String parentTitle, List<ToolbarButtons> parentCRUDButtons) {
        createView(parentTitle, parentCRUDButtons, false, null, null);
    }

    public void createView(String parentTitle, List<ToolbarButtons> parentCRUDButtons, boolean multiSelect) {
        createView(parentTitle, parentCRUDButtons, multiSelect, null, null);
    }

    public void createView(String parentTitle, List<ToolbarButtons> parentCRUDButtons, String childTitle,
            List<ToolbarButtons> childCRUDButtons) {
        createView(parentTitle, parentCRUDButtons, false, childTitle, childCRUDButtons);

    }

    public void createView(String parentTitle, List<ToolbarButtons> parentCRUDButtons, boolean parentMultiSelect,
            String childTitle, List<ToolbarButtons> childCRUDButtons) {
        createView(parentTitle, parentCRUDButtons, parentMultiSelect, childTitle, childCRUDButtons, null, null);

    }

    /**
     * returns an empty View layout containing CRUD ToolBar, CSS layout, Table
     * with columnList and Title of the view
     *
     * @param parentTitle
     *            Title of the parent panel
     * @param parentCRUDButtons
     *            Array Toolbar buttons to diaplay parent panel
     * @param parentMultiSelect
     *            multi select parent table
     * @param childTitle
     *            Title of child panel
     * @param childCRUDButtons
     *            Array of toolbar buttons to display in child panel
     * @param parentSubViewList
     *            Map of CRUDBaseSubViews with their respective Button captions for Parent Table
     * @param childSubViewList
     *            Map of CRUDBaseSubViews with their respective Button captions for Child Table
     */
    public void createView(String parentTitle, List<ToolbarButtons> parentCRUDButtons, boolean parentMultiSelect,
            String childTitle, List<ToolbarButtons> childCRUDButtons,
            Map<String, CRUDBaseSubView<?, ?>> parentSubViewList, Map<String, CRUDBaseSubView<?, ?>> childSubViewMap) {

        boolean addChildTable = childTitle != null;
        setSizeFull();
        this.parentSubViewMap = parentSubViewList;
        this.childSubViewMap = childSubViewMap;
        // parent container with header
        this.parentContainerLayout = new VerticalLayout();
        this.parentContainerLayout.addStyleName(StyleConstants.BASE_CONTAINER);
        this.parentContainerLayout.setSizeFull();

        // parent panel with table and CRUD buttons
        final VerticalLayout parentPanel = new VerticalLayout();
        parentPanel.addStyleName("panel");
        parentPanel.setSizeFull();

        this.parentContainerLayout.addComponent(createHeader(parentTitle, false));
        this.parentContainerLayout.addComponent(parentPanel);

        this.infoText = new PageInformationComponent();
        this.infoText.addStyleName(StyleConstants.PAGE_INFO_COMPONENT);

        parentPanel.addComponent(this.infoText);

        if (parentCRUDButtons != null) {
            parentPanel.addComponent(createParentToolbar(parentCRUDButtons));
        }

        parentPanel.addComponent(createParentTable(parentMultiSelect));

        // expand parentTable with parentPanel
        parentPanel.setExpandRatio(this.parentTable, 1L);

        // expand parentPanel with parentContainer
        this.parentContainerLayout.setExpandRatio(parentPanel, 1L);

        if (addChildTable) {
            // child container with header
            this.childContainerLayout = new VerticalLayout();
            this.childContainerLayout.addStyleName(StyleConstants.BASE_CONTAINER);
            this.childContainerLayout.setSizeFull();

            // child panel with table and CRUD buttons
            final VerticalLayout childPanel = new VerticalLayout();
            childPanel.addStyleName("panel");
            childPanel.setSizeFull();

            this.childContainerLayout.addComponent(createHeader(childTitle, true));
            this.childContainerLayout.addComponent(childPanel);

            if (childCRUDButtons != null) {
                childPanel.addComponent(createChildToolBar(childCRUDButtons));
            }

            // adding table to panel layout
            childPanel.addComponent(createChildTable());

            // expand childTable with childPanel
            childPanel.setExpandRatio(this.childTable, 1L);

            // expand childPanel with childContainer
            this.childContainerLayout.setExpandRatio(childPanel, 1L);

            this.viewSplitter = new VerticalSplitPanel();
            this.viewSplitter.addStyleName(ValoTheme.SPLITPANEL_LARGE);
            this.viewSplitter.addComponent(this.parentContainerLayout);
            this.viewSplitter.addComponent(this.childContainerLayout);
            this.viewSplitter.setImmediate(true);
            this.viewSplitter.setMaxSplitPosition(75, Unit.PERCENTAGE);
            this.viewSplitter.setMinSplitPosition(25, Unit.PERCENTAGE);

            // adding split panel to the main view
            addComponent(this.viewSplitter);
        } else {
            addComponent(this.parentContainerLayout);
        }

    }

    /**
     * @see PageInformationComponent#setInfoText(String)
     */
    public void setInfoText(String title, String content) {
        this.infoText.setInfoText(title, content);
    }

    // returns Table containing columns from the columnList and entries provided
    // by populateTable method.
    @SuppressWarnings({ "serial", "unchecked" })
    private FilterTable createParentTable(final boolean multiSelect) {

        this.parentTable = new FilterTable();
        this.parentTable.setStyleName(ValoTheme.TABLE_COMPACT);
        this.parentTable.setSizeFull();
        this.parentTable.setSelectable(true);
        this.parentTable.setColumnCollapsingAllowed(true);
        this.parentTable.setColumnReorderingAllowed(true);
        this.parentTable.setImmediate(true);
        this.parentTable.setNullSelectionAllowed(false);
        if (!multiSelect) {
            this.parentTable.setNullSelectionItemId(NULL_SELECTION_ITEM_ID);
        }
        this.parentTable.setFilterBarVisible(true);
        this.parentTable.setMultiSelect(multiSelect);

        this.parentTable.setFilterGenerator(ViewUtil.getFilterGenerator());
        initParentTable();
        this.parentContainer.setItemSorter(ViewUtil.getCaseInsensitiveItemSorter());
        // populate table with view specific values
        populateParentTable();

        // Setting parentItemID to the first value row in the parent table
        // by default (if exist).
        selectFirstParentItemIfExists(multiSelect);
        // Handle Selection when filtering changes the items
        this.parentTable.addItemSetChangeListener(new ItemSetChangeListener() {

            @Override
            public void containerItemSetChange(ItemSetChangeEvent event) {
                // If the table is empty, reset the selection so the child table updates itself
                if (CRUDBaseView.this.parentTable.getItemIds().size() == 0) {
                    CRUDBaseView.this.parentTable.setValue(null);
                } else if (CRUDBaseView.this.parentTable.getValue() == null
                        || CRUDBaseView.this.parentTable.getValue() instanceof Set
                        && ((Set<?>) CRUDBaseView.this.parentTable.getValue()).isEmpty()) {
                    // If the table does not have a selection but has items, select the first item
                    selectFirstParentItemIfExists(multiSelect);
                }
            }
        });
        // Handle selection changes
        this.parentTable.addValueChangeListener(new ValueChangeListener() {
            @Override
            public void valueChange(ValueChangeEvent event) {
                Object value = event.getProperty().getValue();
                if (value != null) {
                    if (multiSelect) {
                        parentTableClicked((Set<Long>) value);
                    } else {
                        parentTableClicked((Long) value);
                    }
                } else {
                    parentTableClicked(NULL_SELECTION_ITEM_ID);
                }
                if (CRUDBaseView.this.childContainerLayout != null && !CRUDBaseView.this.viewSplitter
                        .getSecondComponent().equals(CRUDBaseView.this.childContainerLayout)) {
                    CRUDBaseView.this.viewSplitter.removeComponent(CRUDBaseView.this.viewSplitter.getSecondComponent());
                    CRUDBaseView.this.viewSplitter.addComponent(CRUDBaseView.this.childContainerLayout);
                }

            }
        });

        return this.parentTable;
    }

    // returns Table containing columns from the columnList and entries provided
    // by populateTable method.
    @SuppressWarnings("serial")
    private FilterTable createChildTable() {
        this.childTable = new FilterTable();
        this.childTable.setStyleName(ValoTheme.TABLE_COMPACT);
        this.childTable.setSizeFull();
        this.childTable.setSelectable(true);
        this.childTable.setColumnCollapsingAllowed(true);
        this.childTable.setColumnReorderingAllowed(true);
        this.childTable.setFilterBarVisible(true);
        this.childTable.setImmediate(true);
        this.childTable.setNullSelectionAllowed(false);
        this.childTable.setNullSelectionItemId(NULL_SELECTION_ITEM_ID);

        this.childTable.setFilterGenerator(ViewUtil.getFilterGenerator());
        initChildTable();
        this.childContainer.setItemSorter(ViewUtil.getCaseInsensitiveItemSorter());
        if (getParentItem() != null) {
            populateChildTable(getParentItem());
        }

        // Setting childItemID to the first value row in the child table
        // by default (if exist).
        if (this.childTable.firstItemId() != null) {
            childTableClicked((Long) this.childTable.firstItemId());
            this.childTable.setValue(this.childTable.firstItemId());
        }

        // Handle Selection when filtering changes the items
        this.childTable.addItemSetChangeListener(new ItemSetChangeListener() {

            @Override
            public void containerItemSetChange(ItemSetChangeEvent event) {
                // If the table is empty, reset the selection so the table updates itself
                if (CRUDBaseView.this.childTable.getItemIds().size() == 0) {
                    CRUDBaseView.this.childTable.setValue(null);
                } else if (CRUDBaseView.this.childTable.getValue() == null) {
                    // If the table does not have a selection but has items, select the first item
                    childTableClicked((Long) CRUDBaseView.this.childTable.firstItemId());
                }
            }
        });

        // Handle selection changes
        this.childTable.addValueChangeListener(new ValueChangeListener() {

            @Override
            public void valueChange(ValueChangeEvent event) {
                Object value = event.getProperty().getValue();
                if (value != null) {
                    childTableClicked((Long) value);
                } else {
                    childTableClicked(NULL_SELECTION_ITEM_ID);
                }
            }
        });

        return this.childTable;
    }

    // returns Header layout with view Title
    @SuppressWarnings("serial")
    private HorizontalLayout createHeader(String title, final boolean isChildTable) {

        HorizontalLayout header = null;
        if (isChildTable) {
            header = ViewUtil.createSubHeader(title, getChildHelpGuid());
        } else {
            header = ViewUtil.createSubHeader(title, getParentHelpGuid());
        }

        Button refresh = new Button();
        refresh.setStyleName(Reindeer.BUTTON_LINK);
        refresh.setDescription("Refresh");
        refresh.setIcon(new ThemeResource("img/Refresh.png"));
        refresh.addClickListener(new ClickListener() {
            @Override
            public void buttonClick(ClickEvent event) {
                if (isChildTable) {
                    populateChildTable(getParentItem());
                } else {
                    populateParentTable();
                }
            }
        });
        header.addComponent(refresh);
        return header;
    }

    // Returns ToolBar UI
    private HorizontalLayout createParentToolbar(List<ToolbarButtons> parentCRUDButtons) {

        this.parentToolbar = new HorizontalLayout();
        this.parentToolbar.addStyleName("buttonToolbar");
        this.parentToolbar.setWidth("100%");
        Label filler = new Label();

        for (ToolbarButtons button : parentCRUDButtons) {
            if (button == null) {
                continue;
            }
            if (button.getAlignment() == HorizontalAlignment.RIGHT
                    && this.parentToolbar.getComponentIndex(filler) == -1) {
                this.parentToolbar.addComponent(filler);
                this.parentToolbar.setExpandRatio(filler, 1.0f);
            }
            Button buttonComponent = ViewUtil.buildToolbarButton(this.parentToolbar, button, this.buttonClickListener);
            if (button == ToolbarButtons.ADD || button == ToolbarButtons.SHOW_PENDING_ACKNOWLEDGE_ALERTS
                    || button == ToolbarButtons.SHOW_ALL_ALERTS) {
                buttonComponent.setEnabled(true);
            }
            // TODO: Future. Later use the following code to support Parent Table drill down
        }
        if (this.parentToolbar.getComponentIndex(filler) == -1) {
            this.parentToolbar.addComponent(filler);
            this.parentToolbar.setExpandRatio(filler, 1.0f);
        }

        return this.parentToolbar;
    }

    private HorizontalLayout createChildToolBar(List<ToolbarButtons> buttonList) {
        this.childToolbar = new HorizontalLayout();
        this.childToolbar.addStyleName("buttonToolbar");

        for (ToolbarButtons button : buttonList) {
            if (button == null) {
                continue;
            }
            ViewUtil.buildToolbarButton(this.childToolbar, button, this.buttonClickListener);
        }

        return this.childToolbar;
    }

    @Override
    public void enter(ViewChangeListener.ViewChangeEvent event) {
        MainUI main = (MainUI) UI.getCurrent();
        main.currentView = this;
    }

    private void setParentItem(BeanItem<P> parentItem) {
    }

    private void setChildItem(Item childItem) {
    }

    public long getParentItemId() {
        return this.parentItemId;
    }

    public long getChildItemId() {
        return this.childItemId;
    }

    public BeanContainer<Long, P> getParentContainer() {
        return this.parentContainer;
    }

    public BeanContainer<Long, C> getChildContainer() {
        return this.childContainer;
    }

    @SuppressWarnings("unchecked")
    /*
     * If you override this method make sure you check for NULL_SELECTION_ITEM_ID in the child.
     */
    public void parentTableClicked(long parentItemId) {
        this.parentItemId = parentItemId;
        if (parentItemId != NULL_SELECTION_ITEM_ID) {
            ViewUtil.setButtonsEnabled(true, this.parentToolbar);
            setParentItem((BeanItem<P>) this.parentTable.getItem(parentItemId));
            this.parentTable.setValue(parentItemId);
            ViewUtil.enableToolBarButtons(true, this.childToolbar, Arrays.asList(ToolbarButtons.ADD_CHILD.getId()));
        } else {
            ViewUtil.setButtonsEnabled(false, this.parentToolbar);
            setParentItem(null);
        }
        if (this.childTable != null) {
            // populating child table according to the item selected in the parent table
            populateChildTable(getParentItem());

            if (this.childTable.firstItemId() != null) {
                childTableClicked((Long) this.childTable.firstItemId());
                this.childTable.setValue(this.childTable.firstItemId());
            } else {
                ViewUtil.setButtonsEnabled(false, this.childToolbar, Arrays.asList(ToolbarButtons.ADD_CHILD.getId()));
            }
        }

    }

    private void parentTableClicked(Set<Long> selectedItems) {
        ViewUtil.setButtonsEnabled(true, this.parentToolbar);
        ViewUtil.enableToolBarButtons(true, this.childToolbar, Arrays.asList(ToolbarButtons.ADD_CHILD.getId()));

        this.itemList = new ArrayList<P>();
        Object[] itemIds = selectedItems.toArray();
        for (Object itemId : itemIds) {
            this.itemList.add(this.parentContainer.getItem(itemId).getBean());

        }
        if (this.itemList.size() == 1) {
            this.parentItemId = (long) itemIds[0];
            setParentItem(this.parentContainer.getItem(this.parentItemId));
            this.parentTable.setValue(this.parentItemId);
        }
    }

    protected void childTableClicked(long childItemId) {
        if (childItemId != NULL_SELECTION_ITEM_ID) {
            ViewUtil.setButtonsEnabled(true, this.childToolbar);
            this.childItemId = childItemId;
            setChildItem(this.childTable.getItem(childItemId));
            this.childTable.setValue(childItemId);
        } else {
            ViewUtil.setButtonsEnabled(false, this.childToolbar, Arrays.asList(ToolbarButtons.ADD_CHILD.getId()));
        }
    }

    public void syncTables(BroadcastMessage msg, boolean child) throws Exception {
        if (child) {
            syncChildTable(msg);
        } else {
            syncParentTable(msg);
        }
    }

    @SuppressWarnings("unchecked")
    protected void syncParentTable(BroadcastMessage msg) throws Exception {
        if (msg.getEventType().equals(EventType.DELETED)) {
            // delete item from parent container
            getParentContainer().removeItem(msg.getEntityId());
            // if no parent element exists we should remove all the child
            // elements
            if (this.parentContainer.getItemIds().size() == 0) {
                ViewUtil.setButtonsEnabled(false, this.parentToolbar,
                        Arrays.asList(ToolbarButtons.ADD.getId(), ToolbarButtons.SHOW_ALL_ALERTS.getId()));
                ViewUtil.enableToolBarButtons(true, this.parentToolbar,
                        Arrays.asList(ToolbarButtons.ADD.getId(), ToolbarButtons.SHOW_ALL_ALERTS.getId()));

                if (this.childContainer != null) {
                    this.childContainer.removeAllItems();
                    ViewUtil.setButtonsEnabled(false, this.childToolbar);
                }
            } else {
                selectFirstParentItemIfExists(this.parentTable.isMultiSelect());
            }
        } else {
            P dto = null;
            if (msg.getDto() == null) {
                GetDtoFromEntityRequest req = new GetDtoFromEntityRequest();
                req.setEntityId(msg.getEntityId());
                req.setEntityName(msg.getReceiver());
                GetDtoFromEntityService<P> getDtoService = new GetDtoFromEntityService<P>();
                BaseDtoResponse<P> res = getDtoService.dispatch(req);
                dto = res.getDto();
            } else {
                dto = (P) msg.getDto();
            }

            if (msg.getEventType().equals(EventType.UPDATED)
                    && getParentContainer().getItem(msg.getEntityId()) != null) {
                updateParentContainer(dto);
            } else {
                // add new item to the container
                getParentContainer().addItemAt(0, msg.getEntityId(), dto);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void syncChildTable(BroadcastMessage msg) throws Exception {
        if (msg.getEventType().equals(EventType.DELETED)) {
            // delete item from child container
            getChildContainer().removeItem(msg.getEntityId());

            if (this.childTable.firstItemId() != null) {
                // select first element from the table
                childTableClicked((Long) this.childTable.firstItemId());
            } else {
                ViewUtil.setButtonsEnabled(false, this.childToolbar, Arrays.asList(ToolbarButtons.ADD_CHILD.getId()));
            }
        } else {
            C dto = null;
            if (msg.getDto() == null) {
                GetDtoFromEntityRequest req = new GetDtoFromEntityRequest();
                req.setEntityId(msg.getEntityId());
                req.setEntityName(msg.getReceiver());
                GetDtoFromEntityService<C> getDtoService = new GetDtoFromEntityService<C>();
                BaseDtoResponse<C> res = getDtoService.dispatch(req);
                dto = res.getDto();
            } else {
                dto = (C) msg.getDto();
            }

            if (dto != null && dto.getParentId() != null && dto.getParentId().equals(getParentItemId())) {
                if (msg.getEventType().equals(EventType.UPDATED)
                        && getChildContainer().getItem(msg.getEntityId()) != null) {
                    updateChildContainer(dto);
                } else {
                    getChildContainer().addItem(msg.getEntityId(), dto);
                }
            }
        }
    }

    public void delegateBroadcastMessagetoSubView(BroadcastMessage msg, boolean child) throws Exception {
        if (!child) {
            for (Entry<String, CRUDBaseSubView<?, ?>> parentSubView : this.parentSubViewMap.entrySet()) {
                if (parentSubView.getValue().getTableContainer().getBeanType().getSimpleName()
                        .equals(msg.getReceiver() + "Dto")) {
                    parentSubView.getValue().syncTable(msg);
                }
            }
        } else {
            for (Entry<String, CRUDBaseSubView<?, ?>> childSubView : this.childSubViewMap.entrySet()) {
                if (childSubView.getValue() != null && childSubView.getValue().getTableContainer().getBeanType()
                        .getSimpleName().equals(msg.getReceiver() + "Dto")) {
                    childSubView.getValue().syncTable(msg);
                }
            }
        }
    }

    protected void updateParentContainer(P dto) {
        ViewUtil.updateTableContainer(getParentContainer(), dto, this.parentTable);
    }

    private void updateChildContainer(C dto) {
        ViewUtil.updateTableContainer(getChildContainer(), dto, this.childTable);
    }

    private void selectFirstParentItemIfExists(boolean multiSelect) {
        if (this.parentTable.firstItemId() != null) {
            if (multiSelect) {
                HashSet<Long> temp = new HashSet<Long>();
                temp.add((Long) this.parentTable.firstItemId());
                parentTableClicked(temp);
                this.parentTable.setValue(temp);
            } else {
                parentTableClicked((Long) this.parentTable.firstItemId());
                this.parentTable.setValue(this.parentTable.firstItemId());
            }
        }
    }

    /**
     * Gets the help id for the parent view
     *
     * @return
     */
    protected String getParentHelpGuid() {
        return null;
    }

    protected String getChildHelpGuid() {
        return null;
    }

    public boolean isDtoChangeRelevantToParentView(String dto) {
        return this.parentTable != null && getParentContainer() != null && getParentContainer().getBeanType() != null
                && getParentContainer().getBeanType().getSimpleName().equals(dto);
    }

    public boolean isDtoChangeRelevantToChildView(String dto) {
        return this.childTable != null && getChildContainer() != null && getChildContainer().getBeanType() != null
                && getChildContainer().getBeanType().getSimpleName().equals(dto);
    }

    public boolean isDtoRelevantToParentSubView(String dto) {
        if (this.parentSubViewMap != null) {
            for (Entry<String, CRUDBaseSubView<?, ?>> parentSubViewEntry : this.parentSubViewMap.entrySet()) {
                if (parentSubViewEntry.getValue() != null && parentSubViewEntry.getValue().table != null
                        && parentSubViewEntry.getValue().getTableContainer() != null
                        && parentSubViewEntry.getValue().getTableContainer().getBeanType() != null && parentSubViewEntry
                        .getValue().getTableContainer().getBeanType().getSimpleName().equals(dto)) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean isDtoRelevantToChildSubView(String dto) {
        if (this.childSubViewMap != null) {
            for (Entry<String, CRUDBaseSubView<?, ?>> childSubViewEntry : this.childSubViewMap.entrySet()) {
                if (childSubViewEntry.getValue() != null && childSubViewEntry.getValue().table != null
                        && childSubViewEntry.getValue().getTableContainer() != null
                        && childSubViewEntry.getValue().getTableContainer().getBeanType() != null
                        && childSubViewEntry.getValue().getTableContainer().getBeanType().getSimpleName().equals(dto)) {
                    return true;
                }
            }
        }
        return false;
    }

    public String getKeyforChildSubView(int index) {
        int i = 0;
        for (String key : this.childSubViewMap.keySet()) {
            if (i == index) {
                return key;
            }
        }
        return null;
    }

    public abstract void buttonClicked(ClickEvent event) throws Exception;

    public abstract void initParentTable();

    public abstract void populateParentTable();

    public abstract void initChildTable();

    public abstract void populateChildTable(BeanItem<P> parentItem);
}
