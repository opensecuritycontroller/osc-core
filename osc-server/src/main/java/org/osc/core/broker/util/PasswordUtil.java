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
package org.osc.core.broker.util;

import javax.persistence.EntityManager;
import javax.ws.rs.container.ContainerRequestContext;

import org.osc.core.broker.model.entities.User;
import org.osc.core.broker.service.api.PasswordUtilApi;
import org.osc.core.broker.service.api.RestConstants;
import org.osc.core.broker.service.exceptions.VmidcException;
import org.osc.core.broker.service.persistence.OSCEntityManager;
import org.osc.core.broker.util.db.DBConnectionManager;
import org.osc.core.util.AuthUtil;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.transaction.control.ScopedWorkException;

@Component(service = {PasswordUtil.class, PasswordUtilApi.class})
public class PasswordUtil implements PasswordUtilApi {

    @Reference
    AuthUtil authenticator;

    @Reference
    DBConnectionManager dbConnectionManager;

    @Reference
    TransactionalBroadcastUtil txBroadcastUtil;

    private String oscDefaultPass = "";

    public void setOscDefaultPass(String oscDefaultPass) {
        this.oscDefaultPass = oscDefaultPass;
    }

    @Override
    public String getOscDefaultPass() {
        return this.oscDefaultPass;
    }

    public void initPasswordFromDb(String loginName) throws InterruptedException, VmidcException {

        EntityManager em = this.dbConnectionManager.getTransactionalEntityManager();
        User user;

        try {
            user = this.dbConnectionManager.getTransactionControl().required(() -> {
                OSCEntityManager<User> emgr = new OSCEntityManager<User>(User.class, em, this.txBroadcastUtil);
                return  emgr.findByFieldName("loginName", loginName);
            });
            if (user.getLoginName().equals(RestConstants.OSC_DEFAULT_LOGIN)) {
                setOscDefaultPass(user.getPassword());
            }

        } catch (ScopedWorkException swe) {
            throw swe.asRuntimeException();
        }
    }

    @Override
    public void authenticateLocalRequest(ContainerRequestContext request) {
        this.authenticator.authenticateLocalRequest(request);
    }

    @Override
    public void authenticateOscRequest(ContainerRequestContext request) {
        this.authenticator.authenticate(request, RestConstants.OSC_DEFAULT_LOGIN, getOscDefaultPass());
    }

}
