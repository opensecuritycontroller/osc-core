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
package org.osc.core.broker.rest.client.openstack.jcloud;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;

import org.osc.core.broker.service.common.VmidcMessages;
import org.osc.core.broker.service.common.VmidcMessages_;
import org.osc.core.broker.service.exceptions.ExtensionNotPresentException;

import com.google.common.base.Optional;
import com.google.common.io.Closeables;

/**
 * Desgined to be a base class for all jcloud API wrappers in the code.
 */
public abstract class BaseJCloudApi implements Closeable, AutoCloseable {

    protected Endpoint endPoint;

    public BaseJCloudApi(Endpoint endPoint) {
        this.endPoint = endPoint;
    }

    @Override
    public void close() throws IOException {
        for (Closeable api : getApis()) {
            Closeables.close(api, true);
        }
    }

    /**
     * Returns the API from the optional parameter passed in. Throws {@link ExtensionNotPresentException} in case
     * the api is not present
     */
    public static <T> T getOptionalOrThrow(Optional<? extends T> optional, String extensionName) {
        if (optional.isPresent()) {
            return optional.get();
        }

        throw new ExtensionNotPresentException(VmidcMessages.getString(VmidcMessages_.OS_EXTENSION_NOT_PRESENT, extensionName));
    }

    /**
     * List of API's the subclass uses.
     *
     * @return the API's used by the subclass.
     */
    protected abstract List<? extends Closeable> getApis();

}
