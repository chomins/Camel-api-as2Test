import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

public class SelfCertReader {
    private String selfSignedCert ;
    private String selfSignedPrivateKey ;

    private List<Certificate> chainAsList = new ArrayList<>();

    private PrivateKey privateKey;

    public SelfCertReader(String selfSignedCert, String selfSignedPrivateKey){
        this.selfSignedCert = selfSignedCert ;
        this.selfSignedPrivateKey = selfSignedPrivateKey;
    }


    public void setup() {

        InputStream selfSignedCertAsStream = getClass().getClassLoader().getResourceAsStream(selfSignedCert);
        if (selfSignedCertAsStream == null) {
            //LOG.error("Couldn't read out client certificate as stream.");
            throw new IllegalStateException("Couldn't read out certificate as stream.");
        }

        InputStream selfSignedPrivateKeyAsStream = getClass().getClassLoader().getResourceAsStream(selfSignedPrivateKey);
        if (selfSignedPrivateKeyAsStream == null) {
            //LOG.error("Couldn't read out private key as stream.");
            throw new IllegalStateException("Couldn't read out private key as stream.");
        }

        try {
            //stub cert
            Certificate mendelsonCert = getCertificateFromStream(selfSignedCertAsStream);
            chainAsList.add(mendelsonCert);

            //private key
            privateKey = getPrivateKeyFromStream(selfSignedPrivateKeyAsStream);

            System.out.println();
        } catch (IOException e) {
            Throwable rootCause = ExceptionUtils.getRootCause(e);
            String errMsg = "Error while trying to load certificate to the keyload. IO error when reading a byte array.  " + e;
            //LOG.error(errMsg, rootCause);
            System.out.println(errMsg);
        } catch (NoSuchAlgorithmException e) {
            Throwable rootCause = ExceptionUtils.getRootCause(e);
            String errMsg = "Error while trying to load certificate to the keyload. Requested algorithm isn't found.  " + e;
            //LOG.error(errMsg, rootCause);
            System.out.println(errMsg);
        } catch (CertificateException e) {
            Throwable rootCause = ExceptionUtils.getRootCause(e);
            String errMsg = "Error while trying to load certificate to the keyload. There is a certificate problem.  " + e;
            //LOG.error(errMsg, rootCause);
            System.out.println(errMsg);
        } catch (InvalidKeySpecException e) {
            Throwable rootCause = ExceptionUtils.getRootCause(e);
            String errMsg = "Can not init private key store  " + e;
            //LOG.error(errMsg, rootCause);
            System.out.println(errMsg);
        }
    }

    public Certificate[] getChain() {
        if (chainAsList.size() > 0) {
            Certificate[] arrayCert = new Certificate[chainAsList.size()];

            for (int i = 0; i < chainAsList.size(); i++) {
                arrayCert[i] = chainAsList.get(i);
            }
            return arrayCert;
        } else {
            return null;
        }
    }

    public PrivateKey getPrivateKey() {
        return privateKey;
    }

    private List<Certificate> getCertificatesFromStream(InputStream inputStream) throws IOException, CertificateException {
        CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
        return (List<Certificate>) certificateFactory.generateCertificates(inputStream);
    }

    private Certificate getCertificateFromStream(InputStream inputStream) throws IOException, CertificateException {
        CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
        return certificateFactory.generateCertificate(inputStream);
    }

    //https://stackoverflow.com/questions/18644286/creating-privatekey-object-from-pkcs12
    private PrivateKey getPrivateKeyFromStream(InputStream inputStream) throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
        byte[] privateKeyDER = getBytesFromPem(inputStream);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return keyFactory.generatePrivate(new PKCS8EncodedKeySpec(privateKeyDER));
    }

    private byte[] getBytesFromPem(InputStream inputStream) throws IOException {
        String privateKeyPEM = IOUtils.toString(inputStream, StandardCharsets.UTF_8).replaceAll("-{5}.+-{5}", "").replaceAll("\\s", "");
        return Base64.getDecoder().decode(privateKeyPEM);
    }

    private byte[] getBytesFromPKCS12(InputStream inputStream) throws IOException {
        String privateKeyPKCS12 = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
        return privateKeyPKCS12.getBytes(StandardCharsets.UTF_8);
    }
}
