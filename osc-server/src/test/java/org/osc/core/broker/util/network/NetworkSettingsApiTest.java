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

import java.io.IOException;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.osc.core.broker.service.dto.NetworkSettingsDto;
import org.osc.core.broker.util.NetworkUtil;
import org.osc.core.broker.util.ServerUtil;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

//TODO balmukund: The class test will refactor during refactor of  dependency on a complex DB query to use an in mem db.
//Until then powermock is being used to mock static dependencies. This should be removed when this refactoring happens.
@RunWith(PowerMockRunner.class)
@PrepareForTest({ NetworkUtil.class, ServerUtil.class, NetworkSettingsApi.class })
public class NetworkSettingsApiTest {

    NetworkSettingsDto networkSettingsDto;

    @Before
    public void init() throws IOException {
        networkSettingsDto = new NetworkSettingsDto();
    }

	@Test
	public void testGetNetworkSettings_WithSpecificIPAddr_ReturnNetMask() throws Exception {
        String ip = "172.17.0.2";
        String hostDefaultGateway = "172.17.0.1";
        String[] hostDNSSvr = { "10.248.2.1", "10.3.86.116" };
        NetworkSettingsApi networkSettingsApiSpy = PowerMockito.spy(new NetworkSettingsApi());
        PowerMockito.doReturn(hostDNSSvr).when(networkSettingsApiSpy, "getDNSSettings");
        PowerMockito.doReturn(hostDefaultGateway).when(networkSettingsApiSpy, "getDefaultGateway");
        PowerMockito.mockStatic(NetworkUtil.class);
        PowerMockito.mockStatic(ServerUtil.class);
        PowerMockito.when(ServerUtil.isWindows()).thenReturn(false);
        PowerMockito.when(NetworkUtil.getHostIpAddress()).thenReturn(ip);
        networkSettingsDto = networkSettingsApiSpy.getNetworkSettings();
        Assert.assertEquals("255.255.0.0", networkSettingsDto.getHostSubnetMask());
    }

}
