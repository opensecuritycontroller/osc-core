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
package org.osc.core.broker.rest.server.api.test;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.glassfish.jersey.test.TestProperties;
import org.osc.core.broker.util.api.ApiUtil;

import java.util.Arrays;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.when;

public class BaseJerseyTest extends JerseyTest {

    protected final String authorizationHeader = "Authorization";

    protected final String authorizationCreds = "Basic YWRtaW46YWRtaW4xMjM=";

    public BaseJerseyTest() {
    }

    protected void baseTestConfiguration(){
        enable(TestProperties.LOG_TRAFFIC);
        enable(TestProperties.DUMP_ENTITY);
    }

    protected ResourceConfig getBaseResourceConfiguration() {
        return new ResourceConfig()
                .register(com.fasterxml.jackson.jaxrs.json.JacksonJaxbJsonProvider.class)
                .register(ApiUtil.class)
                .packages("org.osc.core.broker.rest.server.exception");
    }

    protected String getString155(){
        StringBuilder sb = new StringBuilder();
        Arrays.stream(new int[155]).forEach(s -> sb.append("1"));
        return sb.toString();
    }

    protected void callRealMethods(ApiUtil apiUtil) throws Exception {
        when(apiUtil.submitBaseRequestToService(any(), any())).thenCallRealMethod();
        when(apiUtil.submitRequestToService(any(), any())).thenCallRealMethod();
        when(apiUtil.getListResponse(any(), any())).thenCallRealMethod();
        when(apiUtil.createIdMismatchException(any(), any())).thenCallRealMethod();
        when(apiUtil.getResponseForBaseRequest(any(), any())).thenCallRealMethod();
        when(apiUtil.getResponse(any(), any())).thenCallRealMethod();

        doCallRealMethod().when(apiUtil).setIdOrThrow(any(),any(),any());
    }

}

