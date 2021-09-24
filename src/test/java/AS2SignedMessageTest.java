import com.sun.org.slf4j.internal.Logger;
import com.sun.org.slf4j.internal.LoggerFactory;
import org.apache.camel.component.as2.api.*;
import org.apache.commons.io.IOUtils;
import org.apache.http.*;
import org.apache.http.entity.ContentType;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpCoreContext;
import org.apache.http.protocol.HttpRequestHandler;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.Security;

public class AS2SignedMessageTest {

    @SuppressWarnings("unused")
    private static final Logger LOG = LoggerFactory.getLogger(AS2MessageTest.class);


    public static final String EDI_MSG = "Hello Camel AS2-API";
    private static final String TARGET_HOST = "localhost";
    private static final int TARGET_PORT = 8080;
    private static final String AS2_VERSION = "1.1";
    private static final String USER_AGENT = "Camel AS2 Endpoint";
    private static final String REQUEST_URI = "/Camel";
    private static final String AS2_TO = "Amazon";
    private static final String AS2_FROM = "MyCompany";
    private static final String SUBJECT = "Cert Test Case";
    private static final String FROM = "test@co.kr";
    private static final String CLIENT_FQDN = "client.example.org";
    private static final String SERVER_FQDN = "server.example.org";
    private static final String ORIGIN_SERVER = "MyServer-HTTP/1.1";


    private static AS2ServerConnection testServer;

    private static SelfCertReader selfCertReader = new SelfCertReader("self_signed_cert/test_cacert.crt",  "self_signed_cert/test_private_key.pem");

    @BeforeClass
    public static void setUpOnce() throws Exception {
        Security.addProvider(new BouncyCastleProvider());
        selfCertReader.setup();
        testServer = new AS2ServerConnection(
                AS2_VERSION,
                ORIGIN_SERVER,
                SERVER_FQDN,
                TARGET_PORT,
                AS2SignatureAlgorithm.SHA1WITHRSA,
                selfCertReader.getChain(),
                selfCertReader.getPrivateKey(),
                selfCertReader.getPrivateKey());

        testServer.listen("/Camel", new HttpRequestHandler() {
            @Override
            public void handle(HttpRequest request, HttpResponse response, HttpContext context)
                    throws HttpException, IOException {
                try {
                    HttpEntity entity = ((HttpEntityEnclosingRequest)request).getEntity();
                    String result = IOUtils.toString(entity.getContent(), StandardCharsets.UTF_8);
                    System.out.println("---------------------server---------------");
                    System.out.println(result);
                    System.out.println("---------------------server---------------");
                    context.setAttribute(SUBJECT, SUBJECT);
                    context.setAttribute(FROM, FROM);
                } catch (Exception e) {
                    throw new HttpException("Failed to parse AS2 Message Entity", e);
                }
            }
        });
    }

    @AfterClass
    public static void tearDownOnce() throws Exception {
        testServer.close();
    }

    @Test public void  SignedMessageTest() throws Exception{

        AS2ClientConnection clientConnection = new AS2ClientConnection(AS2_VERSION,USER_AGENT,CLIENT_FQDN,TARGET_HOST,TARGET_PORT);
        AS2ClientManager as2ClientManager = new AS2ClientManager(clientConnection);

        ContentType contentType = ContentType.create(AS2MediaType.APPLICATION_EDIFACT, AS2Charset.US_ASCII);

        HttpCoreContext httpCoreContext = as2ClientManager.send(
                EDI_MSG, REQUEST_URI, SUBJECT,
                FROM, AS2_TO, AS2_FROM,
                AS2MessageStructure.SIGNED_ENCRYPTED, contentType, null,
                AS2SignatureAlgorithm.SHA1WITHRSA, selfCertReader.getChain(), selfCertReader.getPrivateKey(),
                null,"mrAS@example.org", null,
                AS2EncryptionAlgorithm.DES_EDE3_CBC, selfCertReader.getChain());

    }

}
