package quicSupport;

import org.apache.commons.lang3.tuple.Pair;

import java.io.FileInputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;

public class LoadCertificate {

    public static Pair<Certificate,PrivateKey> getCertificate(String keystoreFilename, String keystorePassword, String alias) throws Exception{
        //keytool -genkeypair -alias mycert -keyalg RSA -keysize 2048 -validity 365 -keystore keystore.jks


        try{
            FileInputStream fis = new FileInputStream(keystoreFilename);
            KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType());
            char[] keystorePasswordChars = keystorePassword.toCharArray();
            keystore.load(fis, keystorePasswordChars);
            Certificate cert = keystore.getCertificate(alias);
            PrivateKey privateKey = (PrivateKey) keystore.getKey(alias, keystorePassword.toCharArray());
            if (cert != null) {
                System.out.println("Certificate loaded successfully!");
            } else {
                System.out.println("Certificate not found!");
            }
            return Pair.of(cert,privateKey);
        }catch (Exception e){
            throw e;
        }
    }
}
