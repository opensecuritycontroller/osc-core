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
import java.io.InputStream;
import java.nio.charset.Charset;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.osc.core.broker.util.ServerUtil;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
//TODO balmukund: The class under test now powermock is being used to mock static dependencies. 
//This will be removed when refactoring happens along with other classes.
@RunWith(PowerMockRunner.class)
@PrepareForTest({ ServerUtil.class, Runtime.class })
public class NetworkSettingsApiTest {

    private NetworkSettingsApi networkSettingsApi;
    private Runtime runtimeMock;
    private Process processMock;


    @Before
    public void init() throws Exception {
        MockitoAnnotations.initMocks(this);
        networkSettingsApi = new NetworkSettingsApi();
        PowerMockito.mockStatic(Runtime.class);
        runtimeMock = PowerMockito.mock(Runtime.class);
        processMock = PowerMockito.mock(Process.class);
        PowerMockito.when(processMock.waitFor()).thenReturn(0);
        Mockito.when(Runtime.getRuntime()).thenReturn(runtimeMock);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testGetIPv4LocalNetMask_With_IP_Command_ReturnNetMask() throws IOException
    {
        InputStream stubInputStream = IOUtils.toInputStream("255.255.0.0", Charset.defaultCharset());
        PowerMockito.mockStatic(ServerUtil.class);
        PowerMockito.when(ServerUtil.execWithLines(Matchers.anyString(), Matchers.anyList())).thenReturn(0);
        PowerMockito.when(processMock.getInputStream()).thenReturn(stubInputStream);
        PowerMockito.when(runtimeMock.exec(Matchers.anyString())).thenReturn(processMock);
        Mockito.when(Runtime.getRuntime()).thenReturn(runtimeMock);
        String netmask = this.networkSettingsApi.getIPv4LocalNetMask();
        assertNotNull(netmask);
    }
	
    @SuppressWarnings("unchecked")
    @Test
    public void testGetDefaultGateway_With_IP_Command_ReturnDefaultGateway() throws IOException
    {
        InputStream stubInputStream = IOUtils.toInputStream("172.17.0.2/16", Charset.defaultCharset());
        PowerMockito.mockStatic(ServerUtil.class);
        PowerMockito.when(ServerUtil.execWithLines(Matchers.anyString(), Matchers.anyList())).thenReturn(0);
        PowerMockito.when(processMock.getInputStream()).thenReturn(stubInputStream);
        PowerMockito.when(runtimeMock.exec(Matchers.anyString())).thenReturn(processMock);
        Mockito.when(Runtime.getRuntime()).thenReturn(runtimeMock);
        String defaultGateway = this.networkSettingsApi.getDefaultGateway();
        assertNotNull(defaultGateway);
    }
}
