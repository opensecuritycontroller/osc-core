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
package org.osc.core.broker.rest.server;

import java.io.IOException;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.ext.Provider;

import org.osc.core.broker.service.api.PasswordUtilApi;
import org.osc.core.broker.service.api.RestConstants;
import org.osc.core.broker.rest.server.annotations.NsxAuth;
import org.osc.core.util.AuthUtil;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(service = NsxAuthFilter.class)
@Provider
@NsxAuth
public class NsxAuthFilter implements ContainerRequestFilter {
    @Reference
    PasswordUtilApi passwordUtil;

    @Override
    public void filter(ContainerRequestContext containerRequestContext) throws IOException {
        AuthUtil.authenticate(containerRequestContext, RestConstants.VMIDC_NSX_LOGIN, this.passwordUtil.getVmidcNsxPass());
    }

}
