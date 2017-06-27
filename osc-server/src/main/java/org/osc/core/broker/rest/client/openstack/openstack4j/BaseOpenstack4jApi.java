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
package org.osc.core.broker.rest.client.openstack.openstack4j;

import org.openstack4j.api.OSClient;

import java.io.Closeable;

/**
 * Designed to be a base class for all openstack4j API wrappers in the code.
 */
public abstract class BaseOpenstack4jApi implements Closeable {

    protected Endpoint endPoint;
    private KeystoneProvider keystoneProvider;

    BaseOpenstack4jApi(Endpoint endPoint) {
        this.endPoint = endPoint;
        this.keystoneProvider = KeystoneProvider.getInstance(endPoint);
    }

    public OSClient.OSClientV3 getOs() {
        return this.keystoneProvider.getAvailableSession();
    }
}
