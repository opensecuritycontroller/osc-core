package org.osc.core.rest.client.crypto;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * SSL socket factory that ensures that disabled SSL certificates will not be used in SSL socket
 */
public class SSLSocketFactoryWithValidCipherSuites extends SSLSocketFactory {
    private SSLSocketFactory wrapped;

    private static final String[] DISABLED_CIPHER_SUITES = new String[] {
        "TLS_KRB5_WITH_DES_CBC_SHA",
        "TLS_KRB5_EXPORT_WITH_DES_CBC_40_SHA",
        "TLS_RSA_WITH_NULL_SHA256",
        "TLS_RSA_WITH_AES_128_CBC_SHA"
    };

    public SSLSocketFactoryWithValidCipherSuites(SSLSocketFactory decorated) {
        this.wrapped = decorated;
    }

    @Override
    public String[] getDefaultCipherSuites() {
        return this.wrapped.getDefaultCipherSuites();
    }

    @Override
    public String[] getSupportedCipherSuites() {
        List<String> supportedCipherSuites = Arrays.asList(this.wrapped.getDefaultCipherSuites());
        Arrays.stream(DISABLED_CIPHER_SUITES).forEach( (disabledCipher) -> {
            supportedCipherSuites.remove(disabledCipher);
        } );

        return supportedCipherSuites.stream().toArray(String[]::new);
    }

    @Override
    public Socket createSocket(Socket socket, String s, int i, boolean b) throws IOException {
        return enableSecureCipherSuites(this.wrapped.createSocket(socket, s, i, b));
    }

    @Override
    public Socket createSocket(String socket, int i) throws IOException, UnknownHostException {
        return enableSecureCipherSuites(this.wrapped.createSocket(socket, i));
    }

    @Override
    public Socket createSocket(String socket, int i, InetAddress inetAddress, int i1) throws IOException, UnknownHostException {
        return enableSecureCipherSuites(this.wrapped.createSocket(socket, i, inetAddress, i1));
    }

    @Override
    public Socket createSocket(InetAddress inetAddress, int i) throws IOException {
        return enableSecureCipherSuites(this.wrapped.createSocket(inetAddress, i));
    }

    @Override
    public Socket createSocket(InetAddress inetAddress, int i, InetAddress inetAddress1, int i1) throws IOException {
        return enableSecureCipherSuites(this.wrapped.createSocket(inetAddress, i, inetAddress1, i1));
    }

    private Socket enableSecureCipherSuites(Socket socket) {
        if(socket != null && socket instanceof SSLSocket) {
            ((SSLSocket)socket).setEnabledCipherSuites(getSupportedCipherSuites());
        }

        return socket;
    }
}
