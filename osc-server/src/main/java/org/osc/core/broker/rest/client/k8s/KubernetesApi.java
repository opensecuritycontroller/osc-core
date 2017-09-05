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

import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;

public class KubernetesApi {
    private DefaultKubernetesClient client;

    KubernetesApi(DefaultKubernetesClient client) {
        this.client = client;
    }

    KubernetesClient getKubernetesClient() {
        return this.client;
    }

    public void setKubernetesClient(org.osc.core.broker.rest.client.k8s.KubernetesClient client) {
        if (client == null || client.getClient() == null) {
            throw new IllegalArgumentException("The provided client cannot be null.");
        }

        this.client = client.getClient();
    }
}

