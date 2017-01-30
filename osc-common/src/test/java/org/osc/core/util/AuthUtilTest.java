package org.osc.core.util;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.WebApplicationException;

public class AuthUtilTest {

	private HttpServletRequest mockHttpServletRequest;

	@Before
	public void setUp() {
		mockHttpServletRequest = Mockito.mock(HttpServletRequest.class);
	}

	//Invalid test cases
	@Test(expected = WebApplicationException.class)
	public void testAuthenticateMissingAuthorization() {
		Mockito.when(mockHttpServletRequest.getHeader("Authorization")).thenReturn(null);
		AuthUtil.authenticate(mockHttpServletRequest, "admin", "admin123");
	}

	@Test(expected = WebApplicationException.class)
	public void testAuthenticateInvalidTokensNull() {
		Mockito.when(mockHttpServletRequest.getHeader("Authorization")).thenReturn("");
		AuthUtil.authenticate(mockHttpServletRequest, "admin", "admin123");
	}

	@Test(expected = WebApplicationException.class)
	public void testAuthenticateInvalidTokensLength() {
		Mockito.when(mockHttpServletRequest.getHeader("Authorization")).thenReturn("abc\\s+xyz\\+s");
		AuthUtil.authenticate(mockHttpServletRequest, "admin", "admin123");
	}

	@Test(expected = WebApplicationException.class)
	public void testAuthenticateInvalidTokensBasic() {
		Mockito.when(mockHttpServletRequest.getHeader("Authorization")).thenReturn("BASIC1\\+s");
		AuthUtil.authenticate(mockHttpServletRequest, "admin", "admin123");
	}

	@Test(expected = WebApplicationException.class)
	public void testInvalidCredentials() {
		//Base64 encoded string "admin1:admin12345"
		Mockito.when(mockHttpServletRequest.getHeader("Authorization")).thenReturn("Basic YWRtaW4xOmFkbWluMTIzNDU=");
		AuthUtil.authenticate(mockHttpServletRequest, "admin", "admin123");
	}

	@Test(expected = WebApplicationException.class)
	public void testAuthenticateLocalRequestMissing() {
		Mockito.when(mockHttpServletRequest.getRemoteAddr()).thenReturn(null);
		AuthUtil.authenticateLocalRequest(mockHttpServletRequest);
	}

	@Test(expected = WebApplicationException.class)
	public void testAuthenticateLocalInvalidAddress() {
		Mockito.when(mockHttpServletRequest.getRemoteAddr()).thenReturn("127.0.0.2");
		AuthUtil.authenticateLocalRequest(mockHttpServletRequest);
	}


	//Valid test cases
	@Test
	public void validAuthenticate() {
		Mockito.when(mockHttpServletRequest.getHeader("Authorization")).thenReturn("Basic YWRtaW46YWRtaW4xMjM=");
		AuthUtil.authenticate(mockHttpServletRequest, "admin", "admin123");
	}

	@Test
	public void validAuthenticateLocalRequest() {
		Mockito.when(mockHttpServletRequest.getRemoteAddr()).thenReturn("127.0.0.1");
		AuthUtil.authenticateLocalRequest(mockHttpServletRequest);
	}

}
