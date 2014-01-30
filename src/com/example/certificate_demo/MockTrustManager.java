
package com.example.certificate_demo;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.X509TrustManager;

import android.util.Log;

/**
 * mock trust manager to trust all certs
 * 
 * @author omercan
 */
public class MockTrustManager implements X509TrustManager {

    private static final String TAG = "MockTrustManager";

    @Override
    public void checkClientTrusted(X509Certificate[] chain, String authType)
            throws CertificateException {
        Log.d(TAG, "checkClientTrusted:" + getCertInfo(chain));
    }

    @Override
    public void checkServerTrusted(X509Certificate[] chain, String authType)
            throws CertificateException {
        Log.d(TAG, "checkClientTrusted:" + getCertInfo(chain));

    }

    @Override
    public X509Certificate[] getAcceptedIssuers() {
        // TODO Auto-generated method stub
        return null;
    }

    private String getCertInfo(X509Certificate[] certs) {
        final StringBuffer sb = new StringBuffer();
        for (X509Certificate cert : certs) {
            sb.append(cert.getIssuerDN());
            sb.append("\n");
        }

        return sb.toString();
    }
}
