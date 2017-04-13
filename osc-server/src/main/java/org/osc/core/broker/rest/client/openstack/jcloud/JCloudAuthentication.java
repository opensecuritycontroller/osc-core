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
package org.osc.core.broker.rest.client.openstack.jcloud;

import java.io.Closeable;
import java.util.Arrays;
import java.util.List;

import org.jclouds.openstack.keystone.v2_0.AuthenticationApi;
import org.jclouds.openstack.keystone.v2_0.domain.Access;
import org.jclouds.openstack.keystone.v2_0.domain.PasswordCredentials;

public class JCloudAuthentication extends BaseJCloudApi {

    private AuthenticationApi authentication;

    public JCloudAuthentication(Endpoint endPoint) {
        super(endPoint);
        this.authentication = JCloudUtil.buildApi(AuthenticationApi.class, JCloudKeyStone.OPENSTACK_SERVICE_KEYSTONE, endPoint);
    }

    public Access getTenantAccess() {
        return this.authentication.authenticateWithTenantNameAndCredentials(this.endPoint.getTenant(),
                PasswordCredentials.createWithUsernameAndPassword(this.endPoint.getUser(),this.endPoint.getPassword()));
    }

    @Override
    protected List<? extends Closeable> getApis() {
        return Arrays.asList(this.authentication);
    }
}
