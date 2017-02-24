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
