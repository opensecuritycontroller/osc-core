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

import java.util.Arrays;
import java.util.List;

import org.osc.core.broker.service.api.AddUserServiceApi;
import org.osc.core.broker.service.api.DeleteUserServiceApi;
import org.osc.core.broker.service.api.ListUserServiceApi;
import org.osc.core.broker.service.api.UpdateUserServiceApi;
import org.osc.core.broker.service.api.server.ServerApi;
import org.osc.core.broker.service.dto.BaseDto;
import org.osc.core.broker.service.dto.UserDto;
import org.osc.core.broker.service.request.ListUserRequest;
import org.osc.core.broker.service.response.ListResponse;
import org.osc.core.broker.view.util.ToolbarButtons;
import org.osc.core.broker.view.util.ViewUtil;
import org.osc.core.broker.window.add.AddUserWindow;
import org.osc.core.broker.window.delete.DeleteUserWindow;
import org.osc.core.broker.window.update.UpdateUserWindow;
import org.slf4j.LoggerFactory;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ServiceScope;
import org.slf4j.Logger;

import com.vaadin.data.util.BeanContainer;
import com.vaadin.data.util.BeanItem;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Notification;

@Component(service={UserView.class}, scope=ServiceScope.PROTOTYPE)
public class UserView extends CRUDBaseView<UserDto, BaseDto> {

    private static final String USER_HELP_GUID = "GUID-0F15567D-E6F9-470C-97BE-4F0224048233.html";

    /**
	 *
	 */
    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(UserView.class);

    @Reference
    private AddUserServiceApi addUserService;

    @Reference
    private ListUserServiceApi listUserService;

    @Reference
    private DeleteUserServiceApi deleteUserService;

    @Reference
    private UpdateUserServiceApi updateUserService;

    @Reference
    private ServerApi server;

    @Activate
    private void activate() {
        createView("Users", Arrays.asList(ToolbarButtons.ADD, ToolbarButtons.EDIT, ToolbarButtons.DELETE));
    }

    @Override
    public void buttonClicked(ClickEvent event) throws Exception {
        if (event.getButton().getId().equals(ToolbarButtons.ADD.getId())) {
            log.debug("Redirecting to Add User Window");
            ViewUtil.addWindow(new AddUserWindow(this, this.addUserService));
        }
        if (event.getButton().getId().equals(ToolbarButtons.EDIT.getId())) {
            log.debug("Redirecting to Update User Window");
            ViewUtil.addWindow(new UpdateUserWindow(this, this.updateUserService, this.server));
        }
        if (event.getButton().getId().equals(ToolbarButtons.DELETE.getId())) {
            log.debug("Redirecting to Delete User Window");
            ViewUtil.addWindow(new DeleteUserWindow(this, this.deleteUserService));
        }
    }

    @Override
    public void initParentTable() {
        this.parentContainer = new BeanContainer<Long, UserDto>(UserDto.class);
        this.parentTable.setContainerDataSource(this.parentContainer);
        this.parentTable.setVisibleColumns("loginName", "firstName", "lastName", "role", "email");

        // Customizing header names
        this.parentTable.setColumnHeader("loginName", "User Name");
        this.parentTable.setColumnHeader("firstName", "First");
        this.parentTable.setColumnHeader("lastName", "Last");
        this.parentTable.setColumnHeader("role", "Role");
        this.parentTable.setColumnHeader("email", "Email");
    }

    @Override
    public void populateParentTable() {

        ListUserRequest listRequest = new ListUserRequest();

        try {
            ListResponse<UserDto> res = this.listUserService.dispatch(listRequest);
            List<UserDto> listResponse = res.getList();

            // Creating table with list of vendors
            for (UserDto user : listResponse) {
                this.parentContainer.addItem(user.getId(), user);
            }

        } catch (Exception e) {
            log.error("Fail to populate User table", e);
            ViewUtil.iscNotification("Fail to populate User table (" + e.getMessage() + ")",
                    Notification.Type.ERROR_MESSAGE);
        }
    }

    @Override
    public void initChildTable() {
        // not needed in this View

    }

    @Override
    public void populateChildTable(BeanItem<UserDto> parentItem) {
        // not needed in this view

    }

    @Override
    protected String getParentHelpGuid() {
        return USER_HELP_GUID;
    }
}
