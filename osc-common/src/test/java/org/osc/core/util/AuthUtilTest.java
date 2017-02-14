package org.osc.core.util;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.osc.core.util.encryption.AESCTREncryption;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.WebApplicationException;
import java.io.IOException;
import java.security.KeyStore;
import java.util.Properties;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AuthUtilTest {

	private HttpServletRequest mockHttpServletRequest;
	// password encrypted with AES CTR
	private String encryptedPassword;
	private String user = "admin";

	KeyStore testKeyStore;
	KeyStoreProvider.KeyStoreFactory testKeyStoreFactory;

	@Before
	public void setUp() throws Exception {
		mockHttpServletRequest = Mockito.mock(HttpServletRequest.class);

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
		Mockito.when(mockHttpServletRequest.getHeader("Authorization")).thenReturn(null);
		AuthUtil.authenticate(mockHttpServletRequest, user, encryptedPassword);
	}

	@Test(expected = WebApplicationException.class)
	public void testAuthenticateInvalidTokensNull() {
		Mockito.when(mockHttpServletRequest.getHeader("Authorization")).thenReturn("");
		AuthUtil.authenticate(mockHttpServletRequest, user, encryptedPassword);
	}

	@Test(expected = WebApplicationException.class)
	public void testAuthenticateInvalidTokensLength() {
		Mockito.when(mockHttpServletRequest.getHeader("Authorization")).thenReturn("abc\\s+xyz\\+s");
		AuthUtil.authenticate(mockHttpServletRequest, user, encryptedPassword);
	}

	@Test(expected = WebApplicationException.class)
	public void testAuthenticateInvalidTokensBasic() {
		Mockito.when(mockHttpServletRequest.getHeader("Authorization")).thenReturn("BASIC1\\+s");
		AuthUtil.authenticate(mockHttpServletRequest, user, encryptedPassword);
	}

	@Test(expected = WebApplicationException.class)
	public void testInvalidCredentials() {
		//Base64 encoded string "admin1:admin12345"
		Mockito.when(mockHttpServletRequest.getHeader("Authorization")).thenReturn("Basic YWRtaW4xOmFkbWluMTIzNDU=");
		AuthUtil.authenticate(mockHttpServletRequest, user, encryptedPassword);
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
		AuthUtil.authenticate(mockHttpServletRequest, user, encryptedPassword);
	}

	@Test
	public void validAuthenticateLocalRequest() {
		Mockito.when(mockHttpServletRequest.getRemoteAddr()).thenReturn("127.0.0.1");
		AuthUtil.authenticateLocalRequest(mockHttpServletRequest);
	}

}
