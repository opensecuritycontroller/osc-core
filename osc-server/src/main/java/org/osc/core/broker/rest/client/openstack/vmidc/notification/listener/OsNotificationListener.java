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
package org.osc.core.broker.rest.client.openstack.vmidc.notification.listener;

import java.util.List;

import org.osc.core.broker.model.entities.BaseEntity;
import org.osc.core.broker.model.entities.virtualization.VirtualizationConnector;
import org.osc.core.broker.rest.client.openstack.vmidc.notification.OsNotificationObjectType;
import org.osc.core.broker.rest.client.openstack.vmidc.notification.OsRabbitMQClient;
import org.osc.core.broker.rest.client.openstack.vmidc.notification.runner.RabbitMQRunner;

/**
 * This abstract class implements a generic listener object which will listen to Notifications of the registered type
 * and act upon receiving them.
 * 
 * This class holds shared code across listeners
 * 
 */
public abstract class OsNotificationListener implements NotificationListener {

    protected VirtualizationConnector vc;
    protected OsNotificationObjectType objectType;
    protected List<String> objectIdList;
    protected BaseEntity entity;

    public List<String> getObjectIdList() {
        return this.objectIdList;
    }

    public void setObjectIdList(List<String> objectIdList) {
        this.objectIdList = objectIdList;
    }

    public OsNotificationListener(VirtualizationConnector vc, OsNotificationObjectType objectType,
            List<String> objectIdList, BaseEntity entity) {
        super();
        this.vc = vc;
        this.objectType = objectType;
        this.objectIdList = objectIdList;
        this.entity = entity;
    }

    @Override
    public void register(VirtualizationConnector vc, OsNotificationObjectType objectType) {
        OsRabbitMQClient client = RabbitMQRunner.getVcToRabbitMQClientMap().get(vc.getId());
        if (client != null) {
            client.registerListener(this, objectType);
        }

    }

    @Override
    public void unRegister(VirtualizationConnector vc, OsNotificationObjectType objectType) {
        OsRabbitMQClient client = RabbitMQRunner.getVcToRabbitMQClientMap().get(vc.getId());
        if (client != null) {
            client.removeListener(this, objectType);
        }
    }

    public OsNotificationObjectType getObjectType() {
        return this.objectType;
    }

    public void setObjectType(OsNotificationObjectType objectType) {
        this.objectType = objectType;
    }

    public BaseEntity getEntity() {
        return this.entity;
    }

    public void setEntity(BaseEntity entity) {
        this.entity = entity;
    }
}
