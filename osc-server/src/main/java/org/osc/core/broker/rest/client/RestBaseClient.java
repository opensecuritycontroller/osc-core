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
package org.osc.core.broker.rest.client;

import java.io.StringWriter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.ConnectException;
import java.net.NoRouteToHostException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.InvocationCallback;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;

import org.apache.log4j.Logger;
import org.glassfish.jersey.client.ClientProperties;
import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;
import org.glassfish.jersey.logging.LoggingFeature;
import org.osc.core.broker.service.common.VmidcMessages;
import org.osc.core.broker.service.common.VmidcMessages_;
import org.osc.core.broker.service.exceptions.RestClientException;
import org.osc.core.broker.util.NetworkUtil;
import org.osc.core.broker.util.crypto.SslContextProvider;
import org.osc.core.broker.util.log.LoggingUtil;

public abstract class RestBaseClient {

    // REST client timeout default is set to infinity. This is not acceptable
    // for all cases.
    // Setting REST client timeouts to 1 minute to ensure infinite hang.
    public static final int DEFAULT_READ_TIMEOUT = 60 * 1000;
    private static final int REQUEST_THREAD_TIMEOUT = DEFAULT_READ_TIMEOUT + 5000;
    public static final int DEFAULT_CONNECTION_TIMEOUT = 5 * 1000;

    public static boolean enableDebugLogging = false;

    private static Logger log = Logger.getLogger(RestBaseClient.class);

    private Client client;
    private WebTarget webTarget;
    private String host;
    protected String headerKey;
    protected String headerKeyValue;
    protected String contentType;
    protected Cookie cookie;
    protected MultivaluedMap<String, Object> lastRequestHeaders;

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

    public static void setDebugLogging(boolean on) {
        enableDebugLogging = on;
    }

    public void enableDebug() {
        this.client.property(LoggingFeature.LOGGING_FEATURE_LOGGER_LEVEL_CLIENT, Level.FINE);
    }

    public void disableDebug() {
        this.client.property(LoggingFeature.LOGGING_FEATURE_LOGGER_LEVEL_CLIENT, Level.OFF);
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
        this.client.property(ClientProperties.CONNECT_TIMEOUT, connectTimeout);
    }

    /**
     * Override default read timeout (1 minute)
     *
     * @param readTimeout
     *            The timeout to use wait for REST calls reply
     */
    public void setReadTimeout(Integer readTimeout) {
        this.client.property(ClientProperties.READ_TIMEOUT, readTimeout);
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
            this.client = configureHttpsClient();
        } else {
            this.client = configureDefaultClient();
        }

        this.client.property(ClientProperties.CONNECT_TIMEOUT, DEFAULT_CONNECTION_TIMEOUT);
        this.client.property(ClientProperties.READ_TIMEOUT, DEFAULT_READ_TIMEOUT);
        this.client.register(com.fasterxml.jackson.jaxrs.json.JacksonJaxbJsonProvider.class);

        this.webTarget = this.client.target(restBaseUrl);

        if (enableDebugLogging) {
            this.client.property(LoggingFeature.LOGGING_FEATURE_VERBOSITY_CLIENT, LoggingFeature.Verbosity.PAYLOAD_ANY);
        }
        if (userName != null && password != null) {
            this.client.register(HttpAuthenticationFeature.basic(userName, password));
        }
    }

    private Client configureHttpsClient() {

        SSLContext ctx = SslContextProvider.getInstance().getSSLContext();
        HttpsURLConnection.setDefaultSSLSocketFactory(ctx.getSocketFactory());

        return ClientBuilder.newBuilder()
                .sslContext(ctx)
                .hostnameVerifier((s, sslSession) -> true)
                .build();
    }

    private Client configureDefaultClient() {
        return ClientBuilder.newBuilder()
                .hostnameVerifier((s, sslSession) -> true)
                .build();
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
        GenericType<List<T>> listOfClazzType = new GenericType<List<T>>(genericType) {};

        Response response = null;
        try {
            final Invocation.Builder requestBuilder = getRequestBuilder(resourcePath);
            response = requestBuilder.async().get(new InvocationLogCallback())
                    .get(REQUEST_THREAD_TIMEOUT, TimeUnit.MILLISECONDS);
            Response.Status status = Response.Status.fromStatusCode(response.getStatus());

            if (status == Response.Status.OK) {
                if (response.hasEntity()) {
                    return response.readEntity(listOfClazzType);
                }
                return null;
            } else {
                throw new ClientResponseNotOkException();
            }
        } catch (Exception ex) {
            RestClientException restClientException = createRestClientException("GET", this.webTarget.getUri() + "/"
                    + resourcePath, null, response, ex);
            log.debug("Error invoking getResources", restClientException);

            throw restClientException;
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
        Response response = null;
        try {
            final Invocation.Builder requestBuilder = getRequestBuilder(resourcePath, params, headers);
            response = requestBuilder.async().get(new InvocationLogCallback())
                    .get(REQUEST_THREAD_TIMEOUT, TimeUnit.MILLISECONDS);
            Response.Status status = Response.Status.fromStatusCode(response.getStatus());

            if (status == Response.Status.OK) {
                this.lastRequestHeaders = response.getHeaders();

                if (response.hasEntity()) {
                    return response.readEntity(clazz);
                }
                return null;
            } else {
                throw new ClientResponseNotOkException();
            }
        } catch (Exception ex) {
            RestClientException restClientException = createRestClientException("GET", this.webTarget.getUri() + "/"
                    + resourcePath, null, response, ex);
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
        Response response = null;
        try {
            final Invocation.Builder requestBuilder = getRequestBuilder(resourcePath, params, headers);
            response = requestBuilder.async().post(Entity.entity(input, this.mediaType),new InvocationLogCallback())
                    .get(REQUEST_THREAD_TIMEOUT, TimeUnit.MILLISECONDS);
            Response.Status status = Response.Status.fromStatusCode(response.getStatus());

            if (status == Response.Status.OK || status == Response.Status.CREATED) {
                this.lastRequestHeaders = response.getHeaders();

                if (response.hasEntity()) {
                    return response.readEntity(resClazz);
                }
                return (O) response;
            } else if (status == Response.Status.NO_CONTENT) {
                return null;
            } else {
                throw new ClientResponseNotOkException();
            }
        } catch (Exception ex) {
            RestClientException restClientException = createRestClientException("POST", this.webTarget.getUri() + "/"
                    + resourcePath, input, response, ex);
            log.debug("Error invoking postResource", restClientException);

            throw restClientException;
        }
    }

    public <I> void putResource(String resourcePath, I input) throws Exception {
        putResource(resourcePath, input, null);
    }

    public <I> void putResource(String resourcePath, final I input, Cookie cookie) throws Exception {
        Response response = null;
        try {
            final Invocation.Builder requestBuilder = getRequestBuilder(resourcePath);
            response = requestBuilder.async().put(Entity.entity(input, this.mediaType), new InvocationLogCallback())
                    .get(REQUEST_THREAD_TIMEOUT, TimeUnit.MILLISECONDS);
            Response.Status status = Response.Status.fromStatusCode(response.getStatus());

            if (status == Response.Status.OK || status == Response.Status.NO_CONTENT) {
                this.lastRequestHeaders = response.getHeaders();
            } else {
                throw new ClientResponseNotOkException();
            }
        } catch (Exception ex) {
            RestClientException restClientException = createRestClientException("PUT", this.webTarget.getUri() + "/"
                    + resourcePath, input, response, ex);
            log.debug("Error invoking putResource", restClientException);

            throw restClientException;
        }
    }

    public String deleteResource(String resourcePath) throws Exception {
        final Invocation.Builder requestBuilder = getRequestBuilder(resourcePath);
        return deleteResource(resourcePath, requestBuilder);
    }

    public String deleteResource(URI resourcePath) throws Exception {
        final Invocation.Builder requestBuilder = getRequestBuilder(resourcePath);
        return deleteResource(resourcePath.getPath(), requestBuilder);
    }

    private String deleteResource(String resourcePath, final Invocation.Builder requestBuilder) throws RestClientException {
        Response response = null;
        try {
            response = requestBuilder.async().delete(new InvocationLogCallback())
                    .get(REQUEST_THREAD_TIMEOUT, TimeUnit.MILLISECONDS);
            Response.Status status = Response.Status.fromStatusCode(response.getStatus());

            // if response is positive
            List<Response.Status> positives = Arrays.asList(
                    Response.Status.OK,
                    Response.Status.ACCEPTED,
                    Response.Status.NO_CONTENT);

            if (positives.contains(status)) {
                // if response contains content
                if (status == Response.Status.NO_CONTENT) {
                    return null;
                } else {
                    return response.readEntity(String.class);
                }
            } else { // response is not ok
                throw new ClientResponseNotOkException();
            }
        } catch (Exception ex) {
            RestClientException restClientException = createRestClientException("DELETE", this.webTarget.getUri()
                    + "/" + resourcePath, null, response, ex);
            log.debug("Error invoking deleteResource", restClientException);

            throw restClientException;
        }
    }

    public WebTarget getWebTarget() {
        return this.webTarget;
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

    private Invocation.Builder getRequestBuilder(String resourcePath, MultivaluedMap<String, String> params,
            MultivaluedMap<String, String> headers) {
        WebTarget localWebTarget = this.webTarget.path(resourcePath);
        return getRequestBuilder(localWebTarget, params, headers);
    }

    private Invocation.Builder getRequestBuilder(String resourcePath) {
        WebTarget localWebTarget = this.webTarget.path(resourcePath);
        return getRequestBuilder(localWebTarget, null, null);
    }

    private Invocation.Builder getRequestBuilder(URI resourcePath) {
        WebTarget localWebTarget = this.webTarget.path(resourcePath.toString());
        return getRequestBuilder(localWebTarget, null, null);
    }

    private Invocation.Builder getRequestBuilder(WebTarget localWebTarget, MultivaluedMap<String, String> params,
            MultivaluedMap<String, String> headers) {
        if (params != null) {
            for(String key: params.keySet()) {
                List<String> values = params.get(key);
                localWebTarget = localWebTarget.queryParam(key, values);
            }
        }

        Invocation.Builder requestBuilder = localWebTarget.request();
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
            Response clientResponse, Exception ex) {

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
                response = clientResponse.readEntity(String.class);
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
        if (status != null && (status == Response.Status.UNAUTHORIZED.getStatusCode() ||
                status == Response.Status.FORBIDDEN.getStatusCode())) {
            exceptionMessage = VmidcMessages.getString(VmidcMessages_.EXCEPTION_AUTH);
        } else if (ex.getCause() instanceof SocketTimeoutException || ex.getCause() instanceof ConnectException
                || ex.getCause() instanceof NoRouteToHostException) {
            exceptionMessage = VmidcMessages.getString(VmidcMessages_.EXCEPTION_NETWORK_CONNECTIVITY,
                    this.host, ex.getCause().getMessage());
        } else {
            String msg = ex.getMessage();
            // TODO: bkasperowicz Investigate on ClientHandlerException equivalent for Jersey 2.x
            //            if (ex instanceof ClientHandlerException && ex.getCause() != null) {
            //                msg = ex.getCause().getMessage();
            //            } else {
            //                msg = ex.getMessage();
            //            }
            exceptionMessage = VmidcMessages.getString(VmidcMessages_.EXCEPTION_RESOURCE_FAILURE, method,
                    resourcePath, msg, status, response);
        }
        // If exception is an client handler exception, get the actual cause of the exception and pass that in.
        // TODO: bkasperowicz Investigate on ClientHandlerException equivalent for Jersey 2.x
        RestClientException restClientException = new RestClientException(exceptionMessage,
                /*ex instanceof ClientHandlerException ? ex.getCause() :*/ ex);
        restClientException.setHost(this.host);
        restClientException.setResourcePath(resourcePath);
        restClientException.setResponse(response);
        restClientException.setResponseCode(status);

        return restClientException;
    }

    public Cookie getCookie() {
        return this.cookie;
    }

    public class InvocationLogCallback implements InvocationCallback<Response> {

        @Override
        public void completed(Response t) {
            log.trace("Request invocation completed. " + LoggingUtil.pojoToJsonString(t));
        }

        @Override
        public void failed(Throwable throwable) {
            if(throwable instanceof ProcessingException){
                log.error("Request invocation failed. Problem with parsing response to json");
            } else {
                log.error("Request invocation failed.", throwable);
            }
        }
    }
}
