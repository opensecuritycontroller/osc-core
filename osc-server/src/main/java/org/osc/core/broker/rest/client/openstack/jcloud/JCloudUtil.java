package org.osc.core.broker.rest.client.openstack.jcloud;

import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableSet;
import com.google.inject.AbstractModule;
import com.google.inject.Module;
import com.google.inject.TypeLiteral;
import org.apache.log4j.Logger;
import org.jclouds.Constants;
import org.jclouds.ContextBuilder;
import org.jclouds.http.HttpResponseException;
import org.jclouds.openstack.nova.v2_0.NovaApi;
import org.jclouds.openstack.nova.v2_0.domain.FloatingIP;
import org.jclouds.openstack.nova.v2_0.extensions.FloatingIPApi;
import org.jclouds.rest.ResourceNotFoundException;
import org.osc.core.broker.service.exceptions.VmidcBrokerInvalidRequestException;
import org.osc.core.rest.client.RestBaseClient;

import javax.net.ssl.SSLContext;
import java.io.Closeable;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Properties;

public class JCloudUtil {

    private static final Logger log = Logger.getLogger(JCloudUtil.class);

    // Enable JCloud logging
    //static Iterable<Module> modules = ImmutableSet.<Module> of(new SLF4JLoggingModule());

    // TODO: Future. Openstack. Externalize the timeout values
    private static final Properties OVERRIDES = new Properties();

    static {
        OVERRIDES.setProperty(Constants.PROPERTY_REQUEST_TIMEOUT, String.valueOf(RestBaseClient.DEFAULT_READ_TIMEOUT));
        OVERRIDES.setProperty(Constants.PROPERTY_CONNECTION_TIMEOUT, String.valueOf(RestBaseClient.DEFAULT_CONNECTION_TIMEOUT));
        OVERRIDES.setProperty(Constants.PROPERTY_LOGGER_WIRE_LOG_SENSITIVE_INFO, String.valueOf(false));
        OVERRIDES.setProperty(Constants.PROPERTY_RELAX_HOSTNAME, String.valueOf(true));
        OVERRIDES.setProperty(Constants.PROPERTY_USER_THREADS, String.valueOf(10));
    }

    static <A extends Closeable> A buildApi(Class<A> api, String serviceName, Endpoint endPoint) {

        String endpointURL;
        try {
            endpointURL = prepareEndpointURL(endPoint);
        } catch (URISyntaxException | MalformedURLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }

        ContextBuilder contextBuilder = ContextBuilder.newBuilder(serviceName)
                .endpoint(endpointURL)
                .credentials(endPoint.getTenant() + ":" + endPoint.getUser(), endPoint.getPassword())
                .overrides(OVERRIDES);

        if (endPoint.isHttps()) {
            contextBuilder = configureSSLContext(contextBuilder, endPoint.getSslContext());
        }

        return contextBuilder.buildApi(api);
    }

    private static String prepareEndpointURL(Endpoint endPoint) throws URISyntaxException, MalformedURLException {
        String schema = endPoint.isHttps() ? "https" : "http";
        URI uri = new URI(schema, null, endPoint.getEndPointIP(), 5000, "/v2.0", null, null);
        return uri.toURL().toString();
    }

    private static ContextBuilder configureSSLContext(ContextBuilder contextBuilder, final SSLContext sslContext) {
        Module customSSLModule = new AbstractModule() {
            @Override
            protected void configure() {
                bind(new TypeLiteral<Supplier<SSLContext>>() {
                }).toInstance(() -> sslContext);
            }
        };
        contextBuilder.modules(ImmutableSet.of(customSSLModule));
        return contextBuilder;
    }

    /**
     * A synchronous way to allocate floating ip(within ourselfs). Since this is a static method, we would lock on
     * the JCloudUtil class objects which prevents multiple threads from making the floating ip call at the same time.
     *null
     * @throws VmidcBrokerInvalidRequestException in case we get an exception while allocating the floating ip
     */
    public static synchronized FloatingIP allocateFloatingIp(JCloudNova jCloudNova, String zone, String poolName,
                                                             String serverId) throws VmidcBrokerInvalidRequestException {
        boolean newIPAllocated = false;
        NovaApi novaApi = jCloudNova.getNovaApi();
        FloatingIPApi floatingIpApi = getFloatingIpApi(zone, novaApi);
        FloatingIP ip = null;
        // Find first ip not allocated, add that to this server
        for (FloatingIP floatingIp : floatingIpApi.list().toList()) {
            if (floatingIp.getFixedIp() == null && poolName.equals(floatingIp.getPool())) {
                ip = floatingIp;
                break;
            }
        }

        // If ip is still null, allocate new ip from the pool
        if (ip == null) {
            FloatingIP floatingIp = floatingIpApi.allocateFromPool(poolName);
            if (floatingIp != null) {
                ip = floatingIp;
                newIPAllocated = true;
            } else {
                throw new IllegalStateException("Ip pool exhausted");
            }
        }

        try {
            floatingIpApi.addToServer(ip.getIp(), serverId);
        } catch (HttpResponseException response) {
            if (newIPAllocated) {
                log.info("Deleting Floating IP as we could not able to assosiate it with our SVA " + ip);
                deleteFloatingIp(jCloudNova, zone, ip.getIp(), ip.getId());
            }
            throw new VmidcBrokerInvalidRequestException(response.getContent());
        }
        log.info("Allocated Floating ip: " + ip + " To server with Id: " + serverId);

        return ip;
    }

    public static synchronized void deleteFloatingIp(JCloudNova jCloudNova, String zone, String ip, String floatingIpId) {
        NovaApi novaApi = jCloudNova.getNovaApi();
        FloatingIPApi floatingIpApi = getFloatingIpApi(zone, novaApi);

        log.info("Deleting Floating ip: " + ip + " with Id: " + floatingIpId);
        try {
            floatingIpApi.delete(floatingIpId);
        } catch (ResourceNotFoundException ex) {
            log.warn("Floating IP : " + ip + " with Id: " + floatingIpId + " not found.");
        }
    }

    private static FloatingIPApi getFloatingIpApi(String zone, NovaApi novaApi) {
        return BaseJCloudApi.getOptionalOrThrow(novaApi.getFloatingIPApi(zone), "Floating Ip Api");
    }
}
