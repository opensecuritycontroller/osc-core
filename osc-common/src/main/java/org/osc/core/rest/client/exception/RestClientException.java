package org.osc.core.rest.client.exception;

import javax.ws.rs.core.Response;
import java.net.NoRouteToHostException;
import java.net.SocketTimeoutException;

/**
 * A general rest exception. Any of the members can be null.
 */
public class RestClientException extends Exception {
    private static final long serialVersionUID = 1L;

    private Integer responseCode;
    private String resourcePath;
    private String response;
    private String host;

    public RestClientException(String message, Throwable cause) {
        super(message, cause);
    }

    public Integer getResponseCode() {
        return this.responseCode;
    }

    public void setResponseCode(Integer responseCode) {
        this.responseCode = responseCode;
    }

    public String getResourcePath() {
        return this.resourcePath;
    }

    public void setResourcePath(String resourcePath) {
        this.resourcePath = resourcePath;
    }

    public String getResponse() {
        return this.response;
    }

    public void setResponse(String response) {
        this.response = response;
    }

    public String getHost() {
        return this.host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public boolean isConnectException() {
        return isConnectException(this);
    }

    public static boolean isConnectException(final Throwable exception) {
        return (exception instanceof SocketTimeoutException || exception.getCause() instanceof SocketTimeoutException)
                || (exception instanceof NoRouteToHostException || exception.getCause() instanceof NoRouteToHostException);
    }

    public boolean isCredentialError() {
        return getResponseCode() != null
                && (getResponseCode().equals(Response.Status.UNAUTHORIZED.getStatusCode()) || getResponseCode()
                .equals(Response.Status.FORBIDDEN.getStatusCode()));
    }

    public static boolean isCredentialError(Throwable exception) {
        return exception instanceof RestClientException && ((RestClientException) exception).isCredentialError();
    }
}
