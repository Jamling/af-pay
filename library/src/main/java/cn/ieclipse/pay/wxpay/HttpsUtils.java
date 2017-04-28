/*
 * Copyright (C) 2015-2017 QuickAF
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package cn.ieclipse.pay.wxpay;

import android.text.TextUtils;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

/**
 * Description
 *
 * @author Jamling
 */

public abstract class HttpsUtils {
    public static byte[] post(String url, String body) {
        if (TextUtils.isEmpty(url)) {
            throw new NullPointerException("url can't be empty");
        }
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
            if (connection instanceof HttpsURLConnection) {
                initHttps((HttpsURLConnection) connection, null);
            }
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            DataOutputStream out = new DataOutputStream(connection.getOutputStream());
            out.write(body.getBytes());
            out.close();
            int code = connection.getResponseCode();
            
            if (code == HttpURLConnection.HTTP_OK) {
                DataInputStream in = new DataInputStream(connection.getInputStream());
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                copyStream(in, bos);
                in.close();
                byte[] data = bos.toByteArray();
                bos.close();
                return data;
            }
            connection.disconnect();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
    
    public static int copyStream(InputStream is, OutputStream os) throws IOException {
        int total = 0;
        int read = 0;
        byte[] buf = new byte[8192];
        while ((read = is.read(buf)) > 0) {
            os.write(buf, 0, read);
            total += read;
        }
        return total;
    }
    
    public static void initHttps(HttpsURLConnection connection, String protocol) {
        try {
            SSLContext context = SSLContext.getInstance(TextUtils.isEmpty(protocol) ? "TLS" : protocol);
            context.init(null, new TrustManager[]{new EmptyTrustManager()}, null);
            connection.setSSLSocketFactory(context.getSocketFactory());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public static class EmptyTrustManager implements X509TrustManager {
        
        @Override
        public void checkClientTrusted(X509Certificate[] chain,
                                       String authType) throws java.security.cert.CertificateException {
            
        }
        
        @Override
        public void checkServerTrusted(X509Certificate[] chain,
                                       String authType) throws java.security.cert.CertificateException {
            if (chain == null || chain.length <= 0) {
                throw new IllegalArgumentException("Server X509Certificate is empty");
            }
            for (X509Certificate cert : chain) {
                System.out.println(cert.getPublicKey());
                cert.checkValidity();
            }
        }
        
        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[0];
        }
    }
    
    public static class EmptyHostVerifier implements HostnameVerifier {
        @Override
        public boolean verify(String hostname, SSLSession session) {
            System.out.println("verify " + hostname);
            HostnameVerifier hv = HttpsURLConnection.getDefaultHostnameVerifier();
            return hv.verify(hostname, session);
        }
    }
}
