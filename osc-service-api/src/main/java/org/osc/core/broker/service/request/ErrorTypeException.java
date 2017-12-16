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
package org.osc.core.broker.service.request;

import org.osc.core.broker.service.exceptions.VmidcBrokerValidationException;

/**
 * Wraps an exception and provides more context on the type of exception
 */
@SuppressWarnings("serial")
public class ErrorTypeException extends VmidcBrokerValidationException {

	public enum ErrorType {
		CONTROLLER_EXCEPTION,
		PROVIDER_EXCEPTION,
		PROVIDER_CONNECT_EXCEPTION,
		PROVIDER_AUTH_EXCEPTION,
		IP_CHANGED_EXCEPTION,
		MANAGER_CONNECTOR_EXCEPTION,
		RABBITMQ_EXCEPTION;
	}

    private ErrorType type;

    public ErrorTypeException(Throwable e, ErrorType type) {
        super(e);
        this.type = type;
    }

    public ErrorTypeException(String s, ErrorType type) {
        super(s);
        this.type = type;
    }

    public ErrorType getType() {
        return this.type;
    }

}
