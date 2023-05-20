package quicSupport.utils;

import org.apache.commons.lang3.tuple.Pair;

import java.io.FileInputStream;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;

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
            if (cert == null) {
                throw new Exception("ALIAS "+alias+" NOT FOUND");
            }
            return Pair.of(cert,privateKey);
        }catch (Exception e){
            throw e;
        }
    }
}
