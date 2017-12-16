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
package org.osc.core.broker.rest.client.k8s;

import java.io.Closeable;
import java.io.IOException;

import org.osc.core.broker.model.entities.virtualization.VirtualizationConnector;

import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;

public class KubernetesClient implements Closeable {
    private DefaultKubernetesClient client;
    private VirtualizationConnector vc;

    public KubernetesClient(VirtualizationConnector vc) {
        this.vc = vc;

        final String URI = "http://" + this.vc.getProviderIpAddress()+ ":8080";

        Config config = new ConfigBuilder().withMasterUrl(URI).build();

        this.client = null;
        this.client = new DefaultKubernetesClient(config);
    }

    public VirtualizationConnector getVirtualizationConnector() {
        return this.vc;
    }

    @Override
    public void close() throws IOException {
        this.client.close();
    }

    DefaultKubernetesClient getClient() {
        return this.client;
    }
}