package org.osc.core.broker.util;

import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.List;

import org.junit.Test;
import org.osc.core.broker.service.exceptions.VmidcBrokerInvalidEntryException;

import com.google.common.collect.Lists;

public class ValidateUtilTest {

    private static final List<String> validDaNames = Lists.newArrayList("arv-1","arv-56789-123");
    private static final List<String> invalidDaNames = Lists.newArrayList("arv_#", "arv_longer_than_13_chars");

    /**
     * Unsupported emails which are valid
     * "email@[123.123.123.123]"
     * "\"email\"@example.com"
     *
     */
    private static final List<String> validEmails = Lists.newArrayList("email@example.com",
            "firstname.lastname@example.com",
            "email@subdomain.example.com",
            "firstname+lastname@example.com",
            "email@123.123.123.123",
            "1234567890@example.com",
            "email@example-one.com",
            "_______@example.com",
            "email@example.name",
            "email@example.museum",
            "email@example.co.jp",
            "firstname-lastname@example.com");
    /**
     * Invalid emails which are supported
     * ".email@example.com"
     * "email.@example.com"
     * "email..email@example.com"
     * "email@-example.com"
     * "email@example.web"
     * "email@111.222.333.44444"
     * "Abc..123@example.com"
     */
    private static final List<String> invalidEmails = Lists.newArrayList("plainaddress",
            "#@%^%#$@#$@#.com",
            "@example.com",
            "Joe Smith <email@example.com>",
            "email.example.com",
            "email@example@example.com",
            "あいうえお@example.com",
            "email@example.com (Joe Smith)",
            "email@example",
            "email@example..com");


    private static final List<String> validIps = Lists.newArrayList("1.1.1.1",
            "255.255.255.255", "192.168.1.1", "10.10.1.1", "132.254.111.10", "26.10.2.10", "127.0.0.1",
            "2001:cdba:0000:0000:0000:0000:3257:9652", "2001:cdba:0:0:0:0:3257:9652", "2001:cdba::3257:9652",
            "2000::", "2002:c0a8:101::42", "2003:dead:beef:4dad:23:46:bb:101", "::192:168:0:1");

    private static final List<String> invalidIps = Lists.newArrayList("10.10.10",
            "10.10",
            "10",
            "a.a.a.a",
            "10.0.0.a",
            "10.10.10.256",
            "222.222.2.999",
            "999.10.10.20",
            "2222.22.22.22",
            "22.2222.22.2",
            "[2001:db8:0:1]:80",
            "1200:0000:AB00:1234:O000:2552:7777:1313",
            "2001::0234:C1ab::A0:aabc:003F");

    @Test
    public void testCheckForNullFields() {
        HashMap<String, Object> map = new HashMap<>();
        map.put("foo", new Object());
        map.put("foo1", new Object());
        map.put("foo2", new Object());
        map.put("foo3", new Object());

        try {
            ValidateUtil.checkForNullFields(map);
        } catch (VmidcBrokerInvalidEntryException e) {
            fail("Error in checkForNullFields. No fields are null and exception happens");
        }

        map.clear();

        map.put("foo", new Object());
        map.put("foo1", null);
        map.put("foo2", new Object());
        map.put("foo3", new Object());

        try {
            ValidateUtil.checkForNullFields(map);
            fail("Error in checkForNullFields, fields are null and exception does not happen");
        } catch (VmidcBrokerInvalidEntryException e) {
            // Nothing to do
        }

    }

    @Test
    public void testValidateDaName() {
        for(String item : validDaNames) {
            assertTrue(ValidateUtil.validateDaName(item));
        }

        for(String item : invalidDaNames) {
            assertFalse(ValidateUtil.validateDaName(item));
        }
    }

    @Test
    public void testValidateFieldLength() {
        HashMap<String, String> map = new HashMap<>();
        map.put("foo1", "foo_value1");
        map.put("foo2", "foo_value2_longer");
        map.put("foo3", "foo_");
        map.put("foo4", "foo_value4_veryLong_value");

        try {
            ValidateUtil.validateFieldLength(map, 30);
        } catch (VmidcBrokerInvalidEntryException e) {
            fail("Error in validateFieldLength");
        }

        map.clear();

        map.put("foo1", "foo_value1");
        map.put("foo2", "foo_value2_longer");
        map.put("foo3", "foo_");
        map.put("foo4", "foo_value4_veryLong_value");

        try {
            ValidateUtil.validateFieldLength(map, 11);
            fail("Error in validateFieldLength, We should not get here");
        } catch (VmidcBrokerInvalidEntryException e) {
            // Nothing to do
        }
    }

    @Test
    public void testCheckForValidEmailAddress() {

        for (String item : validEmails) {
            try {
                ValidateUtil.checkForValidEmailAddress(item);
            } catch (VmidcBrokerInvalidEntryException e) {
                fail("Error in checkForValidEmailAddress. " + item + " Is a valid email address.");
            }
        }

        for (String item : invalidEmails) {
            try {
                ValidateUtil.checkForValidEmailAddress(item);
                fail("Error in checkForValidEmailAddress. " + item + " Is a not a valid email address.");
            } catch (VmidcBrokerInvalidEntryException e) {
                // Nothing to do,expected
            }
        }
    }

    @Test
    public void testCheckForValidIpAddressFormat() {
        for (String item : validIps) {
            try {
                ValidateUtil.checkForValidIpAddressFormat(item);
            } catch (VmidcBrokerInvalidEntryException e) {
                fail("Error in checkForValidIpAddressFormat. " + item + " Is an invalid ip address.");
            }
        }

        for (String item : invalidIps) {
            try {
                ValidateUtil.checkForValidIpAddressFormat(item);
                fail("Error in checkForValidIpAddressFormat. " + item + " Is a not an invalid ip address.");
            } catch (VmidcBrokerInvalidEntryException e) {
                // Nothing to do,expected
            }
        }
    }
}
