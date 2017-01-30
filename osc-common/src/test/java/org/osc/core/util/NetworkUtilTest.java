package org.osc.core.util;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.osc.core.util.NetworkUtil;

public class NetworkUtilTest {

    @Test
    public void testGetPrefixLength() {
        List<Integer> prefixLength = Arrays.asList(32, 31, 30, 29, 28, 27, 26, 25, 24, 23, 22, 21, 20, 19, 18, 17, 16,
                15, 14, 13, 12, 11, 10, 9, 8, 7, 6, 5, 4, 3, 2, 1, 0);

        for (int i = 0; i < NetworkUtil.validNetMasks.size(); i++) {
            assertEquals(Integer.valueOf(NetworkUtil.getPrefixLength(NetworkUtil.validNetMasks.get(i))),
                    prefixLength.get(i));
        }

        List<String> invalidNetMasks = Arrays.asList("192.168.0.1", "foo", "127.0.0.1");

        for (String invalidNetMask : invalidNetMasks) {
            try {
                NetworkUtil.getPrefixLength(invalidNetMask);
                fail("Expected to throw IllegalArgument exception");
            } catch (IllegalArgumentException iae) {
                // Expected exception.
            }
        }

    }

}
