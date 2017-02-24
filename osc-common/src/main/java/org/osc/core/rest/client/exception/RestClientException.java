/*******************************************************************************
 * Copyright (c) 2017 Intel Corporation
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
package org.osc.core.rest.client.exception;

import java.net.NoRouteToHostException;
import java.net.SocketTimeoutException;

import com.sun.jersey.api.client.ClientResponse;


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
                && (getResponseCode().equals(ClientResponse.Status.UNAUTHORIZED.getStatusCode()) || getResponseCode()
                        .equals(ClientResponse.Status.FORBIDDEN.getStatusCode()));
    }

    public static boolean isCredentialError(Throwable exception) {
        return exception instanceof RestClientException && ((RestClientException) exception).isCredentialError();
    }
}
