package org.osc.core.util;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.net.URISyntaxException;

public class AuthUtilTest {

	private ContainerRequestContext mockRequest;

	private UriInfo uriInfo;

	@Before
	public void setUp() {
		mockRequest = Mockito.mock(ContainerRequestContext.class);
		uriInfo = Mockito.mock(UriInfo.class);
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
		AuthUtil.authenticate(mockRequest, "admin", "admin123");
	}

	@Test
	public void validAuthenticateLocalRequest() throws URISyntaxException {
		Mockito.when(uriInfo.getRequestUri()).thenReturn(new URI("http://127.0.0.1"));
		Mockito.when(mockRequest.getUriInfo()).thenReturn(uriInfo);
		AuthUtil.authenticateLocalRequest(mockRequest);
	}

}
