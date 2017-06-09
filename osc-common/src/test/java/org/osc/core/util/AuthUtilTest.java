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

import java.net.URI;
import java.net.URISyntaxException;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.UriInfo;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.osc.core.broker.service.api.server.EncryptionApi;
import org.osc.core.broker.service.api.server.LoggingApi;

@RunWith(MockitoJUnitRunner.class)
public class AuthUtilTest {

    private static final String USER = "admin";
    private static final String PASSWORD = "admin123";
    private static final String ENCRYPTED_PASSWORD = "encrypted";

    @Mock
	private ContainerRequestContext mockRequest;

    @Mock
    private EncryptionApi encryption;

    @Mock
    private LoggingApi logging;

    @Mock
    private UriInfo uriInfo;

    @InjectMocks
    private AuthUtil authUtil;

	@Before
	public void setUp() throws Exception {
        Mockito.when(this.encryption.validateAESCTR(PASSWORD, ENCRYPTED_PASSWORD)).thenReturn(true);
	}

	//Invalid test cases
	@Test(expected = WebApplicationException.class)
	public void testAuthenticateMissingAuthorization() {
		Mockito.when(this.mockRequest.getHeaderString("Authorization")).thenReturn(null);
		this.authUtil.authenticate(this.mockRequest, USER, ENCRYPTED_PASSWORD);
	}

	@Test(expected = WebApplicationException.class)
	public void testAuthenticateInvalidTokensNull() {
		Mockito.when(this.mockRequest.getHeaderString("Authorization")).thenReturn("");
		this.authUtil.authenticate(this.mockRequest, USER, ENCRYPTED_PASSWORD);
	}

	@Test(expected = WebApplicationException.class)
	public void testAuthenticateInvalidTokensLength() {
		Mockito.when(this.mockRequest.getHeaderString("Authorization")).thenReturn("abc\\s+xyz\\+s");
		this.authUtil.authenticate(this.mockRequest, USER, ENCRYPTED_PASSWORD);
	}

	@Test(expected = WebApplicationException.class)
	public void testAuthenticateInvalidTokensBasic() {
		Mockito.when(this.mockRequest.getHeaderString("Authorization")).thenReturn("BASIC1\\+s");
		this.authUtil.authenticate(this.mockRequest, USER, ENCRYPTED_PASSWORD);
	}

	@Test(expected = WebApplicationException.class)
	public void testInvalidCredentials() {
		//Base64 encoded string "admin1:admin12345"
		Mockito.when(this.mockRequest.getHeaderString("Authorization")).thenReturn("Basic YWRtaW4xOmFkbWluMTIzNDU=");
		this.authUtil.authenticate(this.mockRequest, USER, ENCRYPTED_PASSWORD);
	}

	@Test(expected = WebApplicationException.class)
	public void testAuthenticateLocalRequestMissing() {
		this.authUtil.authenticateLocalRequest(this.mockRequest);
	}

	@Test(expected = WebApplicationException.class)
	public void testAuthenticateLocalInvalidAddress() throws URISyntaxException {
		Mockito.when(this.uriInfo.getRequestUri()).thenReturn(new URI("http://127.0.0.2"));
		Mockito.when(this.mockRequest.getUriInfo()).thenReturn(this.uriInfo);
		this.authUtil.authenticateLocalRequest(this.mockRequest);
	}

	//Valid test cases
	@Test
	public void validAuthenticate() {
		Mockito.when(this.mockRequest.getHeaderString("Authorization")).thenReturn("Basic YWRtaW46YWRtaW4xMjM=");
		this.authUtil.authenticate(this.mockRequest, USER, ENCRYPTED_PASSWORD);
	}

	@Test
	public void validAuthenticateLocalRequestByIp() throws URISyntaxException {
		Mockito.when(this.uriInfo.getRequestUri()).thenReturn(new URI("http://127.0.0.1"));
		Mockito.when(this.mockRequest.getUriInfo()).thenReturn(this.uriInfo);
		this.authUtil.authenticateLocalRequest(this.mockRequest);
	}

	@Test
	public void validAuthenticateLocalRequestByDomain() throws URISyntaxException {
		Mockito.when(this.uriInfo.getRequestUri()).thenReturn(new URI("http://localhost"));
		Mockito.when(this.mockRequest.getUriInfo()).thenReturn(this.uriInfo);
		this.authUtil.authenticateLocalRequest(this.mockRequest);
	}

}
