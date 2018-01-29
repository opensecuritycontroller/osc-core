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
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.MockitoAnnotations;
import org.osc.core.broker.util.ServerUtil;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
//TODO balmukund: The class under test now PowerMockito is being used to mock static dependencies.
//This will be removed when refactoring happens along with other classes.
//TODO balmukund: Improve assert to actually validate that the netmask is within the the output list of execWithLog.
//TDO balmukund: Will cover abnormal case as well.
@RunWith(PowerMockRunner.class)
@PrepareForTest({ ServerUtil.class })
public class NetworkSettingsApiTest {

    private NetworkSettingsApi networkSettingsApi;

    @Before
    public void init() throws Exception {
        MockitoAnnotations.initMocks(this);
        networkSettingsApi = new NetworkSettingsApi();
    }

    @Test
    public void testGetIPv4LocalNetMask_With_IPCommand_ReturnNetMask() throws IOException
    {
        PowerMockito.mockStatic(ServerUtil.class);
        PowerMockito.when(ServerUtil.execWithLog(Matchers.anyString(), Matchers.anyList())).thenReturn(0);
        String netmask = this.networkSettingsApi.getIPv4LocalNetMask();
        assertNotNull(netmask);
    }

    @Test
    public void testGetDefaultGateway_With_IPCommand_Exec_ReturnDefaultGateway() throws IOException
    {
        PowerMockito.mockStatic(ServerUtil.class);
        PowerMockito.when(ServerUtil.execWithLog(Matchers.anyString(), Matchers.anyList())).thenReturn(0);
        String defaultGateway = this.networkSettingsApi.getDefaultGateway();
        assertNotNull(defaultGateway);
    }

}

