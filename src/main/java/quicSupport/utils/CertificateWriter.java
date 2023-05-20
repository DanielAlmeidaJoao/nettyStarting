package quicSupport.utils;

import java.io.File;
import java.io.FileOutputStream;
import java.security.cert.X509Certificate;
import java.util.Base64;

public class CertificateWriter {

    public static File writeToFile(X509Certificate cert, String filePath) throws Exception {
        // Open a file output stream for the output file
        FileOutputStream fos = new FileOutputStream(filePath);

        try {
            // Write the certificate data to the output file in PEM format
            byte[] certData = cert.getEncoded();
            String certDataString = "-----BEGIN CERTIFICATE-----\n" + Base64.getEncoder().encodeToString(certData) + "\n-----END CERTIFICATE-----\n";
            fos.write(certDataString.getBytes());
        } finally {
            // Close the file output stream
            fos.close();
        }
        File outputFile = new File(filePath);
        return outputFile;
    }
}
