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



public class IpAddress implements Comparable<IpAddress> {

    private final int value;

    public IpAddress(int value) {
        this.value = value;
    }

    public IpAddress(String ipAddressStr) {
        String[] parts = ipAddressStr.split("\\.");
        if (parts.length != 4) {
            throw new IllegalArgumentException();
        }
        value = Integer.parseInt(parts[0], 10) << 8 * 3 & 0xFF000000 | Integer.parseInt(parts[1], 10) << 8 * 2
                & 0x00FF0000 | Integer.parseInt(parts[2], 10) << 8 * 1 & 0x0000FF00
                | Integer.parseInt(parts[3], 10) << 8 * 0 & 0x000000FF;
    }

    public int getOctet(int ipAddressNum) {

        if (ipAddressNum < 0 || ipAddressNum >= 4) {
            throw new IndexOutOfBoundsException();
        }

        return value >> ipAddressNum * 8 & 0x000000FF;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        for (int i = 3; i >= 0; --i) {
            sb.append(getOctet(i));
            if (i != 0) {
                sb.append(".");
            }
        }

        return sb.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof IpAddress) {
            return value == ((IpAddress) obj).value;
        }
        return false;
    }

    @Override
    public int compareTo(IpAddress other) {

        if (getValue() == other.getValue()) {
            return 0;
        } else if (getValue() > other.getValue()) {
            return 1;
        } else {
            return -1;
        }
    }


    @Override
    public int hashCode() {
        return value;
    }

    public int getValue() {
        return value;
    }

    public IpAddress next() {
        return new IpAddress(value + 1);
    }

}
