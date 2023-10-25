package org.hortonmachine.gears.io.wcs.readers;

import java.security.cert.X509Certificate;
import javax.net.ssl.X509TrustManager;
import java.security.cert.CertificateException;

public class InsecureTrustManager implements X509TrustManager {
    @Override
    public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        // Don't perform client certificate validation
    }

    @Override
    public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        // Don't perform server certificate validation
    }

    @Override
    public X509Certificate[] getAcceptedIssuers() {
        return null;
    }
}

