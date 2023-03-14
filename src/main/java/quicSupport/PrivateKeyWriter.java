package quicSupport;

import java.io.File;
import java.io.FileOutputStream;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;

public class PrivateKeyWriter {

    public static File writeToFile(PrivateKey key, String filePath) throws Exception {
        // Open a file output stream for the output file
        FileOutputStream fos = new FileOutputStream(filePath);

        try {
            // Write the private key data to the output file in PEM format
            byte[] keyData = key.getEncoded();
            String keyDataString = "-----BEGIN PRIVATE KEY-----\n" + Base64.getEncoder().encodeToString(keyData) + "\n-----END PRIVATE KEY-----\n";
            fos.write(keyDataString.getBytes());
        } finally {
            // Close the file output stream
            fos.close();
        }
        File outputFile = new File(filePath);
        return outputFile;
    }
}
