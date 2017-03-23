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
package org.osc.core.util;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.osc.core.util.encryption.AESCTREncryption;
import org.osc.core.util.encryption.EncryptionException;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.net.URISyntaxException;
import java.io.IOException;
import java.security.KeyStore;
import java.util.Properties;

public class AuthUtilTest {

	private ContainerRequestContext mockRequest;

	private UriInfo uriInfo;
	// password encrypted with AES CTR
	private String encryptedPassword;
	private String user = "admin";

	KeyStore testKeyStore;
	KeyStoreProvider.KeyStoreFactory testKeyStoreFactory;

	@Before
	public void setUp() throws Exception {
        mockRequest = Mockito.mock(ContainerRequestContext.class);
        uriInfo = Mockito.mock(UriInfo.class);
		AESCTREncryption.setKeyProvider(new AESCTREncryption.KeyProvider() {
			@Override
			public String getKeyHex() throws EncryptionException {
				return "A6EBBF1CDCC166710670DE15015EA0AF";
			}

			@Override
			public void updateKey(String keyHex) throws EncryptionException {
				// dont do nothing
			}
		});

		encryptedPassword = EncryptionUtil.encryptAESCTR("admin123");
	}

	private String getAESCTRKeyPassword() throws IOException {
		Properties properties = new Properties();
		properties.load(getClass().getResourceAsStream(EncryptionUtil.SECURITY_PROPS_RESOURCE_PATH));
		return properties.getProperty(AESCTREncryption.PROPS_AESCTR_PASSWORD);
	}

	//Invalid test cases
	@Test(expected = WebApplicationException.class)
	public void testAuthenticateMissingAuthorization() {
		Mockito.when(mockRequest.getHeaderString("Authorization")).thenReturn(null);
		AuthUtil.authenticate(mockRequest, "admin", "admin123");
	}

	@Test(expected = WebApplicationException.class)
	public void testAuthenticateInvalidTokensNull() {
		Mockito.when(mockRequest.getHeaderString("Authorization")).thenReturn("");
		AuthUtil.authenticate(mockRequest, "admin", "admin123");
	}

	@Test(expected = WebApplicationException.class)
	public void testAuthenticateInvalidTokensLength() {
		Mockito.when(mockRequest.getHeaderString("Authorization")).thenReturn("abc\\s+xyz\\+s");
		AuthUtil.authenticate(mockRequest, "admin", "admin123");
	}

	@Test(expected = WebApplicationException.class)
	public void testAuthenticateInvalidTokensBasic() {
		Mockito.when(mockRequest.getHeaderString("Authorization")).thenReturn("BASIC1\\+s");
		AuthUtil.authenticate(mockRequest, "admin", "admin123");
	}

	@Test(expected = WebApplicationException.class)
	public void testInvalidCredentials() {
		//Base64 encoded string "admin1:admin12345"
		Mockito.when(mockRequest.getHeaderString("Authorization")).thenReturn("Basic YWRtaW4xOmFkbWluMTIzNDU=");
		AuthUtil.authenticate(mockRequest, "admin", "admin123");
	}

	@Test(expected = WebApplicationException.class)
	public void testAuthenticateLocalRequestMissing() {
		AuthUtil.authenticateLocalRequest(mockRequest);
	}

	@Test(expected = WebApplicationException.class)
	public void testAuthenticateLocalInvalidAddress() throws URISyntaxException {
		Mockito.when(uriInfo.getRequestUri()).thenReturn(new URI("http://127.0.0.2"));
		Mockito.when(mockRequest.getUriInfo()).thenReturn(uriInfo);
		AuthUtil.authenticateLocalRequest(mockRequest);
	}

	//Valid test cases
	@Test
	public void validAuthenticate() {
		Mockito.when(mockRequest.getHeaderString("Authorization")).thenReturn("Basic YWRtaW46YWRtaW4xMjM=");
		AuthUtil.authenticate(mockRequest, user, encryptedPassword);
	}

	@Test
	public void validAuthenticateLocalRequestByIp() throws URISyntaxException {
		Mockito.when(uriInfo.getRequestUri()).thenReturn(new URI("http://127.0.0.1"));
		Mockito.when(mockRequest.getUriInfo()).thenReturn(uriInfo);
		AuthUtil.authenticateLocalRequest(mockRequest);
	}

	@Test
	public void validAuthenticateLocalRequestByDomain() throws URISyntaxException {
		Mockito.when(uriInfo.getRequestUri()).thenReturn(new URI("http://localhost"));
		Mockito.when(mockRequest.getUriInfo()).thenReturn(uriInfo);
		AuthUtil.authenticateLocalRequest(mockRequest);
	}

}
