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