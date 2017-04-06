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

import org.osc.core.broker.model.entities.User;
import org.osc.core.broker.rest.server.AgentAuthFilter;
import org.osc.core.broker.rest.server.NsxAuthFilter;
import org.osc.core.broker.rest.server.OscAuthFilter;
import org.osc.core.broker.service.exceptions.VmidcException;
import org.osc.core.broker.service.persistence.OSCEntityManager;
import org.osc.core.broker.util.db.HibernateUtil;
import org.osgi.service.transaction.control.ScopedWorkException;

public class PasswordUtil {

    public static void initPasswordFromDb(String loginName) throws InterruptedException, VmidcException {

        EntityManager em = HibernateUtil.getTransactionalEntityManager();
        User user;

        try {
            user = HibernateUtil.getTransactionControl().required(() -> {
                OSCEntityManager<User> emgr = new OSCEntityManager<User>(User.class, em);
                return  emgr.findByFieldName("loginName", loginName);
            });
            if (user.getLoginName().equals(AgentAuthFilter.VMIDC_AGENT_LOGIN)) {
                AgentAuthFilter.VMIDC_AGENT_PASS = user.getPassword();
            } else if (user.getLoginName().equals(NsxAuthFilter.VMIDC_NSX_LOGIN)) {
                NsxAuthFilter.VMIDC_NSX_PASS = user.getPassword();
            } else if (user.getLoginName().equals(OscAuthFilter.OSC_DEFAULT_LOGIN)) {
                OscAuthFilter.OSC_DEFAULT_PASS = user.getPassword();
            }

        } catch (ScopedWorkException swe) {
            throw swe.asRuntimeException();
        }
    }

}
