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
package org.osc.core.broker.util.network;

import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.osc.core.broker.service.dto.NetworkSettingsDto;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

//TODO balmukund: The class test will refactor during refactor of  dependency on a complex DB query to use an in mem db.
//Until then powermock is being used to mock static dependencies. This should be removed when this refactoring happens.
@RunWith(PowerMockRunner.class)
@PrepareForTest({ InetAddress.class, NetworkInterface.class, InterfaceAddress.class, Process.class,
		ProcessBuilder.class })
public class NetworkSettingsApiTest {

    NetworkSettingsDto networkSettingsDto;
    NetworkSettingsApi networkSettingsApi;
    @Mock
    InetAddress inetAddress;
    @Mock
    NetworkInterface networkInterface;
    @Mock
    InterfaceAddress interfaceAddress;
    @Mock
    Process process;
    @Mock
    ProcessBuilder processBuilder;


    @Before
    public void init() throws IOException {
        networkSettingsDto = new NetworkSettingsDto();
        networkSettingsApi = new NetworkSettingsApi();
    }

    @Test
    public void testGetIPv4LocalNetMask_ForLocalhost_ReturnIPv4LocalNetMask() throws UnknownHostException, SocketException {
        short prefixLen=16;
        List<InterfaceAddress> list=new ArrayList<>();
        list.add(interfaceAddress);
        PowerMockito.mockStatic(InetAddress.class);
        PowerMockito.mockStatic(NetworkInterface.class);
        PowerMockito.when(InetAddress.getLocalHost()).thenReturn(inetAddress);
        PowerMockito.when(NetworkInterface.getByInetAddress(inetAddress)).thenReturn(networkInterface);
        PowerMockito.when(networkInterface.getInterfaceAddresses()).thenReturn(list);
        PowerMockito.when(networkInterface.getInterfaceAddresses().get(0).getNetworkPrefixLength()).thenReturn(prefixLen);
        String netMask = this.networkSettingsApi.getIPv4LocalNetMask();
        assertNotNull(netMask);
    }
}
