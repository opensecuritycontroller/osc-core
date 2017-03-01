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
package org.osc.core.broker.rest.client.openstack.vmidc.api;

import org.jclouds.date.internal.SimpleDateFormatDateService;
import org.junit.Test;

public class OsClientTest {

    /**
     * getting image details fails sometime because of time parsing issue
     * see https://issues.apache.org/jira/browse/JCLOUDS-333
     *
     * Having milli seconds in the time returned from openstack causes an parse and illegal argument exceptions. When
     * the issue is resolved this test will fail and we can remove the workaround in the code.
     *
     * see org.osc.core.broker.rest.client.openstack.jcloud.JCloudGlance.getImageById(String, String)
     *
     * @throws Exception
     */
    @Test(expected = IllegalArgumentException.class)
    public void testDateParsingIssue() throws Exception {

        SimpleDateFormatDateService service = new SimpleDateFormatDateService();
        service.iso8601SecondsDateParse("2015-06-19T15:06:58.000+0000");

    }
}
