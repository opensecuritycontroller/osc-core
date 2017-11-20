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
package org.osc.core.broker.window.delete;

import java.util.List;

import org.osc.core.broker.service.api.DeleteAlarmServiceApi;
import org.osc.core.broker.service.api.DeleteAlertServiceApi;
import org.osc.core.broker.service.api.DeleteDeploymentSpecServiceApi;
import org.osc.core.broker.service.api.DeleteDistributedApplianceServiceApi;
import org.osc.core.broker.service.api.DeleteSecurityGroupInterfaceServiceApi;
import org.osc.core.broker.service.api.DeleteSecurityGroupServiceApi;
import org.osc.core.broker.service.api.ForceDeleteVirtualSystemServiceApi;
import org.osc.core.broker.service.api.server.ServerApi;
import org.osc.core.broker.service.api.vc.DeleteVirtualizationConnectorServiceApi;
import org.osc.core.broker.service.dto.AlarmDto;
import org.osc.core.broker.service.dto.AlertDto;
import org.osc.core.broker.service.dto.DistributedApplianceDto;
import org.osc.core.broker.service.dto.SecurityGroupDto;
import org.osc.core.broker.service.dto.SecurityGroupInterfaceDto;
import org.osc.core.broker.service.dto.VirtualSystemDto;
import org.osc.core.broker.service.dto.VirtualizationConnectorDto;
import org.osc.core.broker.service.dto.openstack.DeploymentSpecDto;
import org.osc.core.broker.service.request.AlertRequest;
import org.osc.core.broker.service.request.BaseDeleteRequest;
import org.osc.core.broker.service.request.BaseIdRequest;
import org.osc.core.broker.service.response.BaseJobResponse;
import org.osc.core.broker.view.util.ViewUtil;
import org.osc.core.broker.window.VmidcWindow;
import org.osc.core.broker.window.WindowUtil;
import org.osc.core.broker.window.button.OkCancelButtonModel;
import org.osc.core.broker.window.button.OkCancelForceDeleteModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.Notification;

public class DeleteWindowUtil {

    private static final Logger log = LoggerFactory.getLogger(DeleteWindowUtil.class);

    public static void deleteDeploymentSpec(final DeleteDeploymentSpecServiceApi deleteDeploymentSpecService,
            final DeploymentSpecDto dto, ServerApi server) {
        final VmidcWindow<? extends OkCancelButtonModel> deleteWindow;
        if (dto.isMarkForDeletion()) {
            deleteWindow = WindowUtil.createForceDeleteWindow("Delete Deployment Specification ",
                    "Delete Deployment Specification - " + dto.getName());
        } else {
            deleteWindow = WindowUtil.createAlertWindow("Delete Deployment Specification ",
                    "Delete Deployment Specification - " + dto.getName());
        }
        deleteWindow.getComponentModel().setOkClickedListener(new ClickListener() {

            /**
             *
             */
            private static final long serialVersionUID = 1L;

            @Override
            public void buttonClick(ClickEvent event) {
                BaseDeleteRequest delRequest = new BaseDeleteRequest();
                try {

                    if (deleteWindow.getComponentModel() instanceof OkCancelForceDeleteModel) {
                        // if check box is visible then get its value chosen by user
                        delRequest.setForceDelete(((OkCancelForceDeleteModel) deleteWindow.getComponentModel())
                                .getForceDeleteCheckBoxValue());
                    }

                    delRequest.setParentId(dto.getParentId());
                    delRequest.setId(dto.getId());
                    log.info("deleting Deployment Spec - " + dto.getName());

                    BaseJobResponse response = deleteDeploymentSpecService.dispatch(delRequest);
                    deleteWindow.close();

                    if (response.getJobId() != null) {
                        ViewUtil.showJobNotification(response.getJobId(), server);
                    }

                } catch (Exception e) {
                    ViewUtil.iscNotification(e.getMessage(), Notification.Type.ERROR_MESSAGE);
                }
            }
        });
        ViewUtil.addWindow(deleteWindow);

    }

    public static void deleteDistributedAppliance(final DistributedApplianceDto dto, DeleteDistributedApplianceServiceApi deleteDistributedApplianceService,
            ServerApi server) {
        final VmidcWindow<? extends OkCancelButtonModel> deleteWindow;
        if (dto.isMarkForDeletion()) {
            deleteWindow = WindowUtil.createForceDeleteWindow("Delete Distributed Appliance ",
                    "Delete Distributed Appliance - " + dto.getName());
        } else {
            deleteWindow = WindowUtil.createAlertWindow("Delete Distributed Appliance ",
                    "Delete Distributed Appliance - " + dto.getName());
        }

        deleteWindow.getComponentModel().setOkClickedListener(new ClickListener() {

            /**
             *
             */
            private static final long serialVersionUID = 1L;

            @Override
            public void buttonClick(ClickEvent event) {
                BaseDeleteRequest delRequest = new BaseDeleteRequest();
                try {

                    if (deleteWindow.getComponentModel() instanceof OkCancelForceDeleteModel) {
                        // if check box is visible then get its value chosen by user
                        delRequest.setForceDelete(((OkCancelForceDeleteModel) deleteWindow.getComponentModel())
                                .getForceDeleteCheckBoxValue());
                    }

                    delRequest.setId(dto.getId());
                    log.info("deleting Distributed Appliance - " + dto.getName());
                    BaseJobResponse response = deleteDistributedApplianceService.dispatch(delRequest);
                    deleteWindow.close();

                    if (response.getJobId() != null) {
                        // don't show this for force deletion
                        ViewUtil.showJobNotification(response.getJobId(), server);
                    }

                } catch (Exception e) {
                    ViewUtil.iscNotification(e.getMessage(), Notification.Type.ERROR_MESSAGE);
                }
            }
        });
        ViewUtil.addWindow(deleteWindow);
    }

    @SuppressWarnings("serial")
    public static void deleteVirtualSystem(final ForceDeleteVirtualSystemServiceApi forceDeleteVsService,
            final VirtualSystemDto dto, ServerApi server) {

        final VmidcWindow<OkCancelButtonModel> deleteWindow = WindowUtil
                .createAlertWindow("Force Delete Virtual System", "Force Delete Virtual System - " + dto.getName());

        deleteWindow.getComponentModel().setOkClickedListener(new ClickListener() {

            @Override
            public void buttonClick(ClickEvent event) {
                try {
                    BaseJobResponse response = forceDeleteVsService.dispatch(new BaseDeleteRequest(dto.getId(), true));
                    deleteWindow.close();
                    if (response.getJobId() != null) {
                        ViewUtil.showJobNotification(response.getJobId(), server);
                    }
                } catch (Exception e) {
                    ViewUtil.iscNotification(e.getMessage(), Notification.Type.ERROR_MESSAGE);
                }
            }
        });

        ViewUtil.addWindow(deleteWindow);
    }

    public static void deleteSecurityGroupInterface(final SecurityGroupInterfaceDto dto,
            DeleteSecurityGroupInterfaceServiceApi deleteSecurityGroupInterfaceService) {

        final VmidcWindow<OkCancelButtonModel> deleteWindow = WindowUtil.createAlertWindow(
                "Delete Security Group interface", "Delete Security Group interface  - " + dto.getName());

        deleteWindow.getComponentModel().setOkClickedListener(new ClickListener() {

            /**
             *
             */
            private static final long serialVersionUID = 1L;

            @Override
            public void buttonClick(ClickEvent event) {
                BaseIdRequest delRequest = new BaseIdRequest();
                // Delete SecurityGroupInterface service has no response so not needed.
                try {
                    delRequest.setParentId(dto.getParentId());
                    delRequest.setId(dto.getId());
                    log.info("deleting Security Group interface - " + dto.getName());

                    deleteSecurityGroupInterfaceService.dispatch(delRequest);
                    deleteWindow.close();
                } catch (Exception e) {
                    ViewUtil.iscNotification(e.getMessage(), Notification.Type.ERROR_MESSAGE);
                }
            }
        });

        ViewUtil.addWindow(deleteWindow);

    }

    public static void deleteVirtualizationConnector(DeleteVirtualizationConnectorServiceApi deleteVcService,
            final VirtualizationConnectorDto dto, ServerApi server) {

        final VmidcWindow<OkCancelButtonModel> deleteWindow = WindowUtil.createAlertWindow(
                "Delete Virtualization Connector", "Delete Virtualization Connector  - " + dto.getName());

        deleteWindow.getComponentModel().setOkClickedListener(new ClickListener() {

            /**
             *
             */
            private static final long serialVersionUID = 1L;

            @Override
            public void buttonClick(ClickEvent event) {
                BaseIdRequest delRequest = new BaseIdRequest();

                try {
                    delRequest.setId(dto.getId());
                    log.info("deleting Virtualization Connector - " + dto.getName());
                    deleteVcService.dispatch(delRequest);
                    deleteWindow.close();
                } catch (Exception e) {
                    ViewUtil.iscNotification(e.getMessage(), Notification.Type.ERROR_MESSAGE);
                }
            }
        });

        ViewUtil.addWindow(deleteWindow);
    }

    public static void deleteSecurityGroup(DeleteSecurityGroupServiceApi deleteService, final SecurityGroupDto dto,
            ServerApi server) {

        final VmidcWindow<? extends OkCancelButtonModel> deleteWindow;
        if (dto.isMarkForDeletion()) {
            deleteWindow = WindowUtil.createForceDeleteWindow("Delete Security Group",
                    "Delete Security Group - " + dto.getName());
        } else {
            deleteWindow = WindowUtil.createAlertWindow("Delete Security Group",
                    "Delete Security Group - " + dto.getName());
        }
        deleteWindow.getComponentModel().setOkClickedListener(new ClickListener() {

            /**
             *
             */
            private static final long serialVersionUID = 1L;

            @Override
            public void buttonClick(ClickEvent event) {
                BaseDeleteRequest delRequest = new BaseDeleteRequest();

                try {

                    if (deleteWindow.getComponentModel() instanceof OkCancelForceDeleteModel) {
                        // if check box is visible then get its value chosen by user
                        delRequest.setForceDelete(((OkCancelForceDeleteModel) deleteWindow.getComponentModel())
                                .getForceDeleteCheckBoxValue());
                    }

                    delRequest.setId(dto.getId());
                    delRequest.setParentId(dto.getParentId());
                    log.info("deleting Security Group - " + dto.getName());
                    BaseJobResponse response = deleteService.dispatch(delRequest);
                    deleteWindow.close();

                    if (response.getJobId() != null) {
                        // don't show this for force deletion
                        ViewUtil.showJobNotification(response.getJobId(), server);
                    }

                } catch (Exception e) {
                    ViewUtil.iscNotification(e.getMessage(), Notification.Type.ERROR_MESSAGE);
                }
            }
        });

        ViewUtil.addWindow(deleteWindow);

    }

    public static void deleteAlarm(final DeleteAlarmServiceApi deleteAlarmService, final AlarmDto dto) {

        final VmidcWindow<OkCancelButtonModel> deleteWindow = WindowUtil.createAlertWindow("Delete Alarm",
                "Delete Alarm  - " + dto.getName());

        deleteWindow.getComponentModel().setOkClickedListener(new ClickListener() {

            /**
             *
             */
            private static final long serialVersionUID = 1L;

            @Override
            public void buttonClick(ClickEvent event) {
                BaseIdRequest delRequest = new BaseIdRequest();

                try {
                    delRequest.setId(dto.getId());
                    log.info("deleting alarm - " + dto.getName());
                    deleteAlarmService.dispatch(delRequest);
                    deleteWindow.close();

                } catch (Exception e) {
                    ViewUtil.iscNotification(e.getMessage(), Notification.Type.ERROR_MESSAGE);
                }
            }
        });

        ViewUtil.addWindow(deleteWindow);

    }

    public static void deleteAlert(final DeleteAlertServiceApi deleteAlertService, final List<AlertDto> dtoList) {
        final VmidcWindow<OkCancelButtonModel> deleteWindow = WindowUtil.createAlertWindow("Delete Alert",
                "Delete selected alert(s)? ");

        deleteWindow.getComponentModel().setOkClickedListener(new ClickListener() {

            private static final long serialVersionUID = 1L;

            @Override
            public void buttonClick(ClickEvent event) {

                try {
                    AlertRequest req = new AlertRequest();
                    req.setDtoList(dtoList);
                    deleteAlertService.dispatch(req);
                    deleteWindow.close();
                    log.info("Delete Alert(s) Successful!");
                } catch (Exception e) {
                    log.error("Failed to delete Alert(s)", e);
                    ViewUtil.iscNotification("Failed to delete Alert(s).", Notification.Type.WARNING_MESSAGE);
                }
            }
        });

        ViewUtil.addWindow(deleteWindow);
    }

}
