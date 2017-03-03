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
package org.osc.core.rest.client.crypto;

import javax.net.ssl.SSLException;

/**
 * Class responsible for checking if exception is caused by SSL issue
 */
public class SslCertificateExceptionResolver {

    /**
     * Checks if given throwable is instance of SSLException
     *
     * @param originalCause - error cause
     * @return bool - verified cause status
     */
    public boolean checkExceptionTypeForSSL(Throwable originalCause) {
        Throwable cause = originalCause;
        while (null != cause.getCause()) {
            cause = cause.getCause();
            if (cause instanceof SSLException) {
                return true;
            }
        }

        String detailedMessage = originalCause.getMessage();
        return detailedMessage != null && detailedMessage.contains("javax.net.ssl.SSL");
    }

}