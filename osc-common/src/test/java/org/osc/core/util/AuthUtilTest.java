package org.osc.core.util;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.osc.core.util.encryption.AESCTREncryption;

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
		AESCTREncryption.setKeyProvider(() -> { return "A6EBBF1CDCC166710670DE15015EA0AF"; });
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
	public void validAuthenticateLocalRequest() throws URISyntaxException {
		Mockito.when(uriInfo.getRequestUri()).thenReturn(new URI("http://127.0.0.1"));
		Mockito.when(mockRequest.getUriInfo()).thenReturn(uriInfo);
		AuthUtil.authenticateLocalRequest(mockRequest);
	}

}
