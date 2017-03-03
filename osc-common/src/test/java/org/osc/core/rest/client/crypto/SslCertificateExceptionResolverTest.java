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

import org.junit.Assert;
import org.junit.Test;

import javax.net.ssl.SSLHandshakeException;
import java.rmi.RemoteException;

public class SslCertificateExceptionResolverTest {

    private SslCertificateExceptionResolver resolver = new SslCertificateExceptionResolver();

    @Test
    public void testCheckExceptionTypeForSSL_WithRemoteException_ReturnsFalse(){
        //Act.
        boolean result = this.resolver.checkExceptionTypeForSSL(new RemoteException());

        //Assert.
        Assert.assertFalse(result);
    }

    @Test
    public void testCheckExceptionTypeForSSL_WithSslHandshakeException_ReturnsTrue(){
        //Act.
        boolean result = this.resolver.checkExceptionTypeForSSL(new Exception("Test", new SSLHandshakeException("No trusted certificate")));
        //Assert.
        Assert.assertTrue(result);
    }

    @Test
    public void testCheckExceptionTypeForSSL_WithGenericException_WithMessageException_ReturnsTrue(){
        //Act.
        boolean result = this.resolver.checkExceptionTypeForSSL(new Exception("javax.net.ssl.SSLHandshakeException: sun.security.validator.ValidatorException: No trusted certificate found"));

        //Assert.
        Assert.assertTrue(result);
    }
}
