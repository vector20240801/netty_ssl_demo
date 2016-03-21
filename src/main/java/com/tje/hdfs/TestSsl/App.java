package com.tje.hdfs.TestSsl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLEngine;

/**
 * Hello world!
 *
 */
public class App 
{
	private static final String kmfPath = "resource\\hadoop.p12";
    public static void main( String[] args ) throws KeyStoreException, NoSuchAlgorithmException
    {
        KeyStore ks = KeyStore.getInstance("JKS");
        KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
        InputStream in = null;
        try {
			in = new FileInputStream(new File(kmfPath));
			ks.load(in, "Ada123456".toCharArray());
			kmf.init(ks, "Ada123456".toCharArray());
			SSLEngine sslEngine = SslContextFactory.getServerContext().createSSLEngine();
			sslEngine.setNeedClientAuth(false);
		} catch (FileNotFoundException e) {
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (CertificateException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (UnrecoverableKeyException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException e) {}
			}
		}
    }
}
