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

import java.lang.reflect.ParameterizedType;
import java.util.Arrays;
import java.util.Set;

import org.osc.core.broker.service.api.GetDtoFromEntityServiceApi;
import org.osc.core.broker.service.api.GetDtoFromEntityServiceFactoryApi;
import org.osc.core.broker.service.broadcast.BroadcastMessage;
import org.osc.core.broker.service.broadcast.EventType;
import org.osc.core.broker.service.dto.BaseDto;
import org.osc.core.broker.service.request.GetDtoFromEntityRequest;
import org.osc.core.broker.service.response.BaseDtoResponse;
import org.osc.core.broker.view.common.StyleConstants;
import org.osc.core.broker.view.util.ToolbarButtons;
import org.osc.core.broker.view.util.ViewUtil;
import org.tepi.filtertable.FilterTable;

import com.vaadin.data.Container.ItemSetChangeEvent;
import com.vaadin.data.Container.ItemSetChangeListener;
import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.data.util.BeanContainer;
import com.vaadin.data.util.BeanItem;
import com.vaadin.server.ThemeResource;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.Reindeer;
import com.vaadin.ui.themes.ValoTheme;

/**
 *
 * @param <P>
 *            Parent DTO for this subView
 * @param <C>
 *            children of the given parent object
 */
public abstract class CRUDBaseSubView<P extends BaseDto, C extends BaseDto> extends CssLayout {

    /**
     *
     */

    private static final long serialVersionUID = 1L;
    protected static final long NULL_SELECTION_ITEM_ID = -1L;
    protected String title;
    protected CRUDBaseView<?, ?> currentView;
    protected BeanItem<C> selectedItem;
    protected long selectedItemId;
    public FilterTable table;
    protected BeanContainer<Long, C> tableContainer;
    protected HorizontalLayout toolbar;
    protected P parent;

    protected ToolbarButtons[] buttons;

    @SuppressWarnings("serial")
    private final ClickListener buttonClickListener = new ClickListener() {

        @Override
        public void buttonClick(ClickEvent event) {
            try {
                buttonClicked(event);
            } catch (Exception e) {
                ViewUtil.showError("Error invoking action in subview", e);
            }
        }
    };

    public CRUDBaseSubView(CRUDBaseView<?, ?> currentView, String title, ToolbarButtons[] buttons, P parent) {
        super();
        this.title = title;
        this.currentView = currentView;
        this.buttons = buttons;
        this.parent = parent;
    }

    @Override
    public void attach() {
        // Note this must not be run in the constructor as the concrete
        // sub-type will not be fully initialized (its constructor will not
        // yet have run!).
        createSubView(this.title, this.buttons);
    }

    private void createSubView(String title, ToolbarButtons[] buttons) {
        setSizeFull();
        final VerticalLayout layout = new VerticalLayout();
        layout.addStyleName(StyleConstants.BASE_CONTAINER);
        layout.setSizeFull();

        final VerticalLayout panel = new VerticalLayout();
        panel.addStyleName("panel");
        panel.setSizeFull();

        layout.addComponent(createHeader(title));
        layout.addComponent(panel);

        if (buttons != null) {
            panel.addComponent(createToolbar(buttons));
        }

        panel.addComponent(createTable());
        panel.setExpandRatio(this.table, 1L);
        layout.setExpandRatio(panel, 1L);
        addComponent(layout);
    }

    @SuppressWarnings("serial")
    private FilterTable createTable() {
        this.table = new FilterTable();
        this.table.setStyleName(ValoTheme.TABLE_COMPACT);
        this.table.setSizeFull();
        this.table.setSelectable(true);
        this.table.setColumnCollapsingAllowed(true);
        this.table.setColumnReorderingAllowed(true);
        this.table.setImmediate(true);
        this.table.setNullSelectionAllowed(false);
        this.table.setFilterBarVisible(true);
        this.table.setFilterGenerator(ViewUtil.getFilterGenerator());
        initTable();
        populateTable();

        if (this.table.firstItemId() != null) {
            tableClicked((Long) this.table.firstItemId());
            this.table.setValue(this.table.firstItemId());
        }

        // Handle Selection when filtering changes the items
        this.table.addItemSetChangeListener(new ItemSetChangeListener() {
            @Override
            public void containerItemSetChange(ItemSetChangeEvent event) {
                // If the table is empty, reset the selection so the child table updates itself
                if (CRUDBaseSubView.this.table.getItemIds().size() == 0) {
                    CRUDBaseSubView.this.table.setValue(null);
                } else if (CRUDBaseSubView.this.table.getValue() == null
                        || CRUDBaseSubView.this.table.getValue() instanceof Set
                        && ((Set<?>) CRUDBaseSubView.this.table.getValue()).isEmpty()) {
                    // If the table does not have a selection but has items, select the first item
                    tableClicked((Long) CRUDBaseSubView.this.table.firstItemId());
                    CRUDBaseSubView.this.table.setValue(CRUDBaseSubView.this.table.firstItemId());
                }
            }
        });

        // Handle selection changes
        this.table.addValueChangeListener(new ValueChangeListener() {
            @Override
            public void valueChange(ValueChangeEvent event) {
                Object value = event.getProperty().getValue();
                if (value != null) {
                    tableClicked((Long) value);
                } else {
                    tableClicked(NULL_SELECTION_ITEM_ID);
                }
            }
        });

        return this.table;
    }

    // returns Header layout with view Title
    @SuppressWarnings("serial")
    private HorizontalLayout createHeader(String title) {
        HorizontalLayout header = ViewUtil.createSubHeader(title, getSubViewHelpGuid());
        Button refresh = new Button();
        refresh.setStyleName(Reindeer.BUTTON_LINK);
        refresh.setDescription("Refresh");
        refresh.setIcon(new ThemeResource("img/Refresh.png"));
        refresh.addClickListener(new ClickListener() {
            @Override
            public void buttonClick(ClickEvent event) {
                populateTable();
            }
        });
        header.addComponent(refresh);
        return header;
    }

    public HorizontalLayout createToolbar(ToolbarButtons[] crudButtons) {
        this.toolbar = new HorizontalLayout();
        this.toolbar.addStyleName("buttonToolbar");
        for (ToolbarButtons button : crudButtons) {
            if (button == null) {
                continue;
            }
            Button buttonComponent = ViewUtil.buildToolbarButton(this.toolbar, button, this.buttonClickListener);
            if (button == ToolbarButtons.ADD || button == ToolbarButtons.BACK) {
                buttonComponent.setEnabled(true);
            }
        }

        return this.toolbar;
    }

    protected String getSubViewHelpGuid() {
        return null;
    }

    protected void tableClicked(long selectedItemId) {
        if (selectedItemId != NULL_SELECTION_ITEM_ID) {
            ViewUtil.setButtonsEnabled(true, this.toolbar);
            setSelectedItemId(selectedItemId);
            setSelectedItem(this.tableContainer.getItem(this.selectedItemId));
            this.table.setValue(selectedItemId);
        } else {
            ViewUtil.setButtonsEnabled(false, this.toolbar,
                    Arrays.asList(ToolbarButtons.ADD.getId(), ToolbarButtons.BACK.getId()));
        }
    }

    public BeanContainer<Long, C> getTableContainer() {
        return this.tableContainer;
    }

    public void setTableContainer(BeanContainer<Long, C> tableContainer) {
        this.tableContainer = tableContainer;
    }

    public long getSelectedItemId() {
        return this.selectedItemId;
    }

    public void setSelectedItemId(long selectedItemId) {
        this.selectedItemId = selectedItemId;
    }

    public BeanItem<C> getSelectedItem() {
        return this.selectedItem;
    }

    public void setSelectedItem(BeanItem<C> selectedItem) {
        this.selectedItem = selectedItem;
    }

    @SuppressWarnings("unchecked")
    public void syncTable(BroadcastMessage msg, GetDtoFromEntityServiceFactoryApi getDtoFromEntityServiceFactory) throws Exception {

        if (msg.getEventType().equals(EventType.DELETED)) {
            // delete item from child container
            getTableContainer().removeItem(msg.getEntityId());

            if (this.table.firstItemId() != null) {
                // select first element from the table
                tableClicked((Long) this.table.firstItemId());
            } else {
                ViewUtil.setButtonsEnabled(false, this.toolbar,
                        Arrays.asList(ToolbarButtons.ADD.getId(), ToolbarButtons.BACK.getId()));
            }
        } else {
            C dto = null;
            if (msg.getDto() == null) {
                GetDtoFromEntityRequest req = new GetDtoFromEntityRequest();
                req.setEntityId(msg.getEntityId());
                req.setEntityName(msg.getReceiver());

                ParameterizedType parameterizedType = (ParameterizedType) getClass().getGenericSuperclass();
                Class<C> childClass = (Class<C>) parameterizedType.getActualTypeArguments()[1];
                GetDtoFromEntityServiceApi<C> getDtoService = getDtoFromEntityServiceFactory.getService(childClass);
                BaseDtoResponse<C> res = getDtoService.dispatch(req);
                dto = res.getDto();
            } else {
                dto = (C) msg.getDto();
            }

            if (msg.getEventType().equals(EventType.UPDATED) && getTableContainer().getItem(msg.getEntityId()) != null) {
                ViewUtil.updateTableContainer(this.tableContainer, dto, this.table);
            } else {
                // check if the item belongs to the same parent entity which user is looking at
                if (dto.getParentId().equals(this.currentView.getChildItemId())) {
                    // add item to sun view table
                    getTableContainer().addItemAt(0, msg.getEntityId(), dto);
                }

            }
        }
    }

    @SuppressWarnings("unchecked")
    protected P getDtoInContext() {
        return (P) this.currentView.getChildContainer().getItem(this.currentView.getChildItemId()).getBean();
    }

    public abstract void initTable();

    public abstract void populateTable();

    public abstract void buttonClicked(ClickEvent event) throws Exception;
}
