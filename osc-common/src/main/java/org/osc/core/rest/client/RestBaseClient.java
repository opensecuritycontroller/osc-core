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
package org.osc.core.rest.client;

import com.google.common.base.Throwables;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.ClientResponse.Status;
import com.sun.jersey.api.client.GenericType;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.WebResource.Builder;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.api.client.filter.LoggingFilter;
import com.sun.jersey.client.urlconnection.HTTPSProperties;
import org.apache.log4j.Logger;
import org.osc.core.rest.client.crypto.SslContextProvider;
import org.osc.core.rest.client.exception.ClientResponseNotOkException;
import org.osc.core.rest.client.exception.RestClientException;
import org.osc.core.rest.client.messages.VmidcCommonMessages;
import org.osc.core.rest.client.messages.VmidcCommonMessages_;
import org.osc.core.rest.client.util.LoggingUtil;
import org.osc.core.util.NetworkUtil;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.MultivaluedMap;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import java.io.StringWriter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.ConnectException;
import java.net.NoRouteToHostException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public abstract class RestBaseClient {

    // REST client timeout default is set to infinity. This is not acceptable
    // for all cases.
    // Setting REST client timeouts to 1 minute to ensure infinite hang.
    public static final int DEFAULT_READ_TIMEOUT = 60 * 1000;
    private static final int REQUEST_THREAD_TIMEOUT = DEFAULT_READ_TIMEOUT + 5000;
    public static final int DEFAULT_CONNECTION_TIMEOUT = 5 * 1000;

    public static boolean enableDebugLogging = false;

    private static Logger log = Logger.getLogger(RestBaseClient.class);

    private final LoggingFilter debugFilter = new LoggingFilter(System.out);
    private Client client;
    private WebResource webResource;
    private ClientConfig clientCfg;
    private String host;
    protected String headerKey;
    protected String headerKeyValue;
    protected String contentType;
    protected Cookie cookie;
    protected MultivaluedMap<String, String> lastRequestHeaders;

    private String urlBase;
    private final String mediaType;

    public RestBaseClient(String urlBase, String mediaType) {
        this.urlBase = urlBase;
        this.mediaType = mediaType;
        this.contentType = mediaType;
    }

    public void setUrlBase(String urlBase) {
        this.urlBase = urlBase;
    }

    public void enableDebug() {
        this.client.addFilter(this.debugFilter);
    }

    public void disableDebug() {
        this.client.removeFilter(this.debugFilter);
    }

    public static String getLocalHostIp() {
        try {
            return NetworkUtil.getHostIpAddress();
        } catch (Exception e) {
            log.error("Error while Host IP address ", e);
        }
        return "";
    }

    /**
     * Override default connect timeout (1 minute)
     *
     * @param connectTimeout
     *            The timeout to use wait for REST calls reply
     */
    public void setConnectTimeout(Integer connectTimeout) {
        this.client.setConnectTimeout(connectTimeout);
    }

    /**
     * Override default read timeout (1 minute)
     *
     * @param readTimeout
     *            The timeout to use wait for REST calls reply
     */
    public void setReadTimeout(Integer readTimeout) {
        this.client.setReadTimeout(readTimeout);
    }

    protected void initRestBaseClient(String host, String userName, String password, boolean isHttps) {
        initRestBaseClient(host, 0, userName, password, isHttps);
    }

    /**
     * Initialize Rest base client. If username and password is null, then we dont add basic auth filter.
     *
     *
     * @param host the host ip/name
     * @param port
     *            if port is greater than 0 then uses the port else does not specify port and uses default http and
     *            https ports based
     *            on the input
     * @param userName username to use. null if we dont want to use basic auth
     * @param password password to use. null if we dont want to use basic auth
     * @param isHttps whether http or https
     */
    protected void initRestBaseClient(String host, int port, String userName, String password, boolean isHttps) {

        this.host = host;
        String urlPrefix = (isHttps ? "https" : "http") + "://" + host + (port > 0 ? ":" + port : "");
        String restBaseUrl = urlPrefix + this.urlBase;

        if (isHttps) {
            this.clientCfg = configureHttpsClient();
        } else {
            this.clientCfg = new DefaultClientConfig();
        }

        this.client = Client.create(this.clientCfg);
        this.client.setConnectTimeout(DEFAULT_CONNECTION_TIMEOUT);
        this.client.setReadTimeout(DEFAULT_READ_TIMEOUT);
        if (enableDebugLogging) {
            this.client.addFilter(this.debugFilter);
        }
        if (userName != null && password != null) {
            this.client.addFilter(new com.sun.jersey.api.client.filter.HTTPBasicAuthFilter(userName, password));
        }
        this.webResource = this.client.resource(restBaseUrl);
    }

    private ClientConfig configureHttpsClient() {

        SSLContext ctx = new SslContextProvider().getSSLContext();
        HttpsURLConnection.setDefaultSSLSocketFactory(ctx.getSocketFactory());

        ClientConfig config = new DefaultClientConfig();

        config.getProperties().put(HTTPSProperties.PROPERTY_HTTPS_PROPERTIES,
                new HTTPSProperties((hostname, session) -> true, ctx));

        return config;
    }

    public <T> List<T> getResources(String resourcePath, final Class<T> clazz) throws Exception {

        ParameterizedType genericType = new ParameterizedType() {
            @Override
            public Type[] getActualTypeArguments() {
                return new Type[] { clazz };
            }

            @Override
            public Type getRawType() {
                return List.class;
            }

            @Override
            public Type getOwnerType() {
                return List.class;
            }
        };

        GenericType<List<T>> type = new GenericType<List<T>>(genericType) {
        };

        ClientResponse res = null;
        try {
            final Builder requestBuilder = getRequestBuilder(resourcePath);
            Callable<ClientResponse> task = new Callable<ClientResponse>() {
                @Override
                public ClientResponse call() throws Exception {
                    return requestBuilder.get(ClientResponse.class);
                }
            };

            res = execRequestTimeout(task);
            Status status = getClientResponseStatus(res);

            if (status == ClientResponse.Status.OK) {
                if (res.hasEntity()) {
                    return res.getEntity(type);
                } else {
                    return null;
                }
            } else {
                throw new ClientResponseNotOkException();
            }

        } catch (Exception ex) {

            RestClientException restClientException = createRestClientException("GET", this.webResource.getURI() + "/"
                    + resourcePath, null, res, ex);
            log.debug("Error invoking getResources", restClientException);

            throw restClientException;
        }
    }

    private ClientResponse execRequestTimeout(Callable<ClientResponse> task) throws InterruptedException,
            TimeoutException {
        ExecutorService executor = Executors.newCachedThreadPool();
        Future<ClientResponse> future = executor.submit(task);
        try {
            return future.get(REQUEST_THREAD_TIMEOUT, TimeUnit.MILLISECONDS);
        } catch (ExecutionException e) {
            throw Throwables.propagate(e.getCause());
        } finally {
            executor.shutdownNow();
            try {
                executor.awaitTermination(1, TimeUnit.MINUTES);
            } catch (InterruptedException e) {
                log.warn("Couldn't wait any longer for REST call completion. Shutting down request thread.", e);
            }
        }
    }

    public <T> T getResource(String resourcePath, final Class<T> clazz) throws Exception {
        return getResource(resourcePath, clazz, null);
    }

    public <T> T getResource(String resourcePath, final Class<T> clazz, MultivaluedMap<String, String> params)
            throws Exception {
        return getResource(resourcePath, clazz, params, null);
    }

    public <T> T getResource(String resourcePath, final Class<T> clazz, MultivaluedMap<String, String> params,
            MultivaluedMap<String, String> headers) throws Exception {

        ClientResponse res = null;
        try {
            final Builder requestBuilder = getRequestBuilder(resourcePath, params);
            Callable<ClientResponse> task = new Callable<ClientResponse>() {
                @Override
                public ClientResponse call() throws Exception {
                    return requestBuilder.get(ClientResponse.class);
                }
            };

            res = execRequestTimeout(task);
            Status status = getClientResponseStatus(res);

            if (status == ClientResponse.Status.OK) {

                if (res.hasEntity()) {
                    return res.getEntity(clazz);
                } else {
                    return null;
                }

            } else {
                throw new ClientResponseNotOkException();
            }

        } catch (Exception ex) {

            RestClientException restClientException = createRestClientException("GET", this.webResource.getURI() + "/"
                    + resourcePath, null, res, ex);
            log.debug("Error invoking getResources", restClientException);

            throw restClientException;

        }
    }

    public <I, O> O postResource(String resourcePath, Class<O> resClazz, I input) throws Exception {
        return postResource(resourcePath, resClazz, input, null);
    }

    public <I, O> O postResource(String resourcePath, Class<O> resClazz, I input, MultivaluedMap<String, String> params)
            throws Exception {
        return postResource(resourcePath, resClazz, input, params, null);
    }

    @SuppressWarnings("unchecked")
    public <I, O> O postResource(String resourcePath, Class<O> resClazz, final I input,
            MultivaluedMap<String, String> params, MultivaluedMap<String, String> headers) throws Exception {

        ClientResponse res = null;

        try {

            final Builder requestBuilder = getRequestBuilder(resourcePath, params, headers);
            Callable<ClientResponse> task = new Callable<ClientResponse>() {
                @Override
                public ClientResponse call() throws Exception {
                    return requestBuilder.post(ClientResponse.class, input);
                }
            };

            res = execRequestTimeout(task);
            Status status = getClientResponseStatus(res);

            if (status == ClientResponse.Status.OK || status == ClientResponse.Status.CREATED) {
                this.lastRequestHeaders = res.getHeaders();

                if (res.hasEntity()) {
                    return res.getEntity(resClazz);
                }
                return (O) res;
            } else if (status == ClientResponse.Status.NO_CONTENT) {
                return null;
            } else {
                throw new ClientResponseNotOkException();
            }

        } catch (Exception ex) {

            RestClientException restClientException = createRestClientException("POST", this.webResource.getURI() + "/"
                    + resourcePath, input, res, ex);
            log.debug("Error invoking postResource", restClientException);

            throw restClientException;

        }
    }

    public <I> void putResource(String resourcePath, I input) throws Exception {
        putResource(resourcePath, input, null);
    }

    public <I> void putResource(String resourcePath, final I input, Cookie cookie) throws Exception {

        ClientResponse res = null;
        try {

            final Builder requestBuilder = getRequestBuilder(resourcePath);
            Callable<ClientResponse> task = new Callable<ClientResponse>() {
                @Override
                public ClientResponse call() throws Exception {
                    return requestBuilder.put(ClientResponse.class, input);
                }
            };

            res = execRequestTimeout(task);
            Status status = getClientResponseStatus(res);

            if (status != ClientResponse.Status.OK && status != ClientResponse.Status.NO_CONTENT) {
                throw new ClientResponseNotOkException();
            } else {
                this.lastRequestHeaders = res.getHeaders();
            }

        } catch (Exception ex) {

            RestClientException restClientException = createRestClientException("PUT", this.webResource.getURI() + "/"
                    + resourcePath, input, res, ex);
            log.debug("Error invoking putResource", restClientException);

            throw restClientException;

        }
    }

    public String deleteResource(String resourcePath) throws Exception {

        final Builder requestBuilder = getRequestBuilder(resourcePath);
        return deleteResource(resourcePath, requestBuilder);
    }

    public String deleteResource(URI resourcePath) throws Exception {

        final Builder requestBuilder = getRequestBuilder(resourcePath);
        return deleteResource(resourcePath.getPath(), requestBuilder);
    }

    private String deleteResource(String resourcePath, final Builder requestBuilder) throws RestClientException {
        ClientResponse res = null;
        try {
            Callable<ClientResponse> task = new Callable<ClientResponse>() {
                @Override
                public ClientResponse call() throws Exception {
                    return requestBuilder.delete(ClientResponse.class);
                }
            };

            res = execRequestTimeout(task);
            Status status = getClientResponseStatus(res);

            if (status != ClientResponse.Status.OK && status != ClientResponse.Status.ACCEPTED
                    && status != ClientResponse.Status.NO_CONTENT) {
                throw new ClientResponseNotOkException();
            } else {
                if (status != ClientResponse.Status.NO_CONTENT) {
                    return res.getEntity(String.class);
                }
                return null;
            }
        } catch (Exception ex) {

            RestClientException restClientException = createRestClientException("DELETE", this.webResource.getURI()
                    + "/" + resourcePath, null, res, ex);
            log.debug("Error invoking deleteResource", restClientException);

            throw restClientException;
        }

    }

    public WebResource getWebResource() {
        return this.webResource;
    }

    public static <T> String pojoToXml(T pojo) throws Exception {
        StringWriter writer = new StringWriter();
        JAXBContext context = JAXBContext.newInstance(pojo.getClass());
        Marshaller ms = context.createMarshaller();
        ms.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        ms.setProperty(Marshaller.JAXB_FRAGMENT, Boolean.TRUE);
        ms.marshal(pojo, writer);

        return writer.toString();
    }

    /**
     * Sets up the request builder and the webresource with the given values.
     * <p>
     * Cookie and params can be null.
     * </p>
     * Does the following<br/>
     * 1. Setup the resource path<br/>
     * 2. Set the query params<br/>
     * 3. Set the cookie<br/>
     * 4. Set the accept header media type<br/>
     * 5. Set the header key and value if they are not null and set the content-type to application/json<br/>
     * else<br/>
     * set the content type to application/xml<br/>
     *
     * @return the request builder
     */
    private Builder getRequestBuilder(String resourcePath, MultivaluedMap<String, String> params) {
        WebResource localWebResource = this.webResource.path(resourcePath);
        return getRequestBuilder(localWebResource, params, null);
    }

    private Builder getRequestBuilder(String resourcePath, MultivaluedMap<String, String> params,
            MultivaluedMap<String, String> headers) {
        WebResource localWebResource = this.webResource.path(resourcePath);
        return getRequestBuilder(localWebResource, params, headers);
    }

    private Builder getRequestBuilder(String resourcePath) {
        WebResource localWebResource = this.webResource.path(resourcePath);
        return getRequestBuilder(localWebResource, null, null);
    }

    private Builder getRequestBuilder(URI resourcePath) {
        WebResource localWebResource = this.webResource.uri(resourcePath);
        return getRequestBuilder(localWebResource, null, null);
    }

    private Builder getRequestBuilder(WebResource localWebResource, MultivaluedMap<String, String> params,
            MultivaluedMap<String, String> headers) {
        if (params != null) {
            localWebResource = localWebResource.queryParams(params);
        }

        Builder requestBuilder = localWebResource.getRequestBuilder();
        requestBuilder.accept(this.mediaType);

        if (this.headerKey != null && this.headerKeyValue != null) {
            requestBuilder.header(this.headerKey, this.headerKeyValue).header("content-type", this.contentType);
        } else {
            requestBuilder.header("content-type", this.contentType);
        }

        if (headers != null) {
            for (Entry<String, List<String>> header : headers.entrySet()) {
                requestBuilder.header(header.getKey(), header.getValue());
                requestBuilder.accept(header.getValue().get(0));
            }
        }

        if (this.cookie != null) {
            requestBuilder.cookie(this.cookie);
        }
        return requestBuilder;
    }

    /**
     * Create a RestClient exception wrapping the resource path, response, status and exception information
     *
     * @param resourcePath
     *            the resource path
     * @param clientResponse
     *            the response
     * @param ex
     *            the exception
     *
     * @return the RestClientException
     */
    private RestClientException createRestClientException(String method, String resourcePath, Object input,
            ClientResponse clientResponse, Exception ex) {

        if (input != null) {
            if (this.mediaType.contains("xml")) {
                log.error("REST Exception Encountered. Input is:\n" + LoggingUtil.pojoToXmlString(input));
            } else {
                log.error("REST Exception Encountered. Input is:\n" + LoggingUtil.pojoToJsonString(input));
            }
        }

        String response = null;
        if (clientResponse != null) {
            try {
                response = clientResponse.getEntity(String.class);
                log.error("REST Exception Encountered. Response is:\n" + response);
            } catch (Exception e) {
                log.error("REST Exception Encountered. Response cannot be parsed");
            }
        }
        String exceptionMessage;
        Integer status = null;
        if (clientResponse != null) {
            status = clientResponse.getStatus();
        }
        if (status != null
                && (status == ClientResponse.Status.UNAUTHORIZED.getStatusCode() || status == ClientResponse.Status.FORBIDDEN
                        .getStatusCode())) {
            exceptionMessage = VmidcCommonMessages.getString(VmidcCommonMessages_.EXCEPTION_AUTH);
        } else if (ex.getCause() instanceof SocketTimeoutException || ex.getCause() instanceof ConnectException
                || ex.getCause() instanceof NoRouteToHostException) {
            exceptionMessage = VmidcCommonMessages.getString(VmidcCommonMessages_.EXCEPTION_NETWORK_CONNECTIVITY,
                    this.host, ex.getCause().getMessage());
        } else {
            String msg;
            if (ex instanceof ClientHandlerException && ex.getCause() != null) {
                msg = ex.getCause().getMessage();
            } else {
                msg = ex.getMessage();
            }
            exceptionMessage = VmidcCommonMessages.getString(VmidcCommonMessages_.EXCEPTION_RESOURCE_FAILURE, method,
                    resourcePath, msg, status, response);
        }
        // If exception is an client handler exception, get the actual cause of the exception and pass that in.
        RestClientException restClientException = new RestClientException(exceptionMessage,
                ex instanceof ClientHandlerException ? ex.getCause() : ex);
        restClientException.setHost(this.host);
        restClientException.setResourcePath(resourcePath);
        restClientException.setResponse(response);
        restClientException.setResponseCode(status);

        return restClientException;
    }

    private Status getClientResponseStatus(ClientResponse res) {
        return res.getClientResponseStatus();
    }

    public Cookie getCookie() {
        return this.cookie;
    }
}
