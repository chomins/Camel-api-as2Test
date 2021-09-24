import com.sun.org.slf4j.internal.Logger;
import com.sun.org.slf4j.internal.LoggerFactory;
import org.apache.camel.component.as2.api.*;
import org.apache.camel.component.as2.api.entity.ApplicationEDIFACTEntity;
import org.apache.commons.io.IOUtils;
import org.apache.http.*;
import org.apache.http.entity.ContentType;
import org.apache.http.message.BasicHttpEntityEnclosingRequest;
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


import static org.junit.Assert.*;

public class AS2MessageTest {
    public static final String EDI_MSG = "Hello Camel AS2-API";

    @SuppressWarnings("unused")
    private static final Logger LOG = LoggerFactory.getLogger(AS2MessageTest.class);

    private static final String METHOD = "POST";
    private static final String TARGET_HOST = "localhost";
    private static final int TARGET_PORT = 8080;
    private static final String AS2_VERSION = "1.1";
    private static final String USER_AGENT = "Camel AS2 Endpoint";
    private static final String REQUEST_URI = "/";
    private static final String AS2_NAME = "878051556";
    private static final String SUBJECT = "Test Case";
    private static final String FROM = "mrAS@example.org";
    private static final String CLIENT_FQDN = "client.example.org";
    private static final String SERVER_FQDN = "server.example.org";

    private static AS2ServerConnection testServer;



    @BeforeClass
    public static void setUpOnce() throws Exception {

        testServer = new AS2ServerConnection(AS2_VERSION,
                "MyServer-HTTP/1.1",
                SERVER_FQDN,
                8080,
                null,
                null,
                null,
                null);

        testServer.listen("*", new HttpRequestHandler() {
            @Override
            public void handle(HttpRequest request, HttpResponse response, HttpContext context)
                    throws HttpException, IOException {
                try {
                    org.apache.camel.component.as2.api.entity.EntityParser.parseAS2MessageEntity(request);
                    HttpEntity entity = ((HttpEntityEnclosingRequest)request).getEntity();
                    String result = IOUtils.toString(entity.getContent(), StandardCharsets.UTF_8);
                    System.out.println("---------------------server---------------");
                    System.out.println(result);
                    System.out.println("---------------------server---------------");
                    context.setAttribute(SUBJECT, SUBJECT);
                    context.setAttribute(FROM, AS2_NAME);
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



    @Test
    public void plainEDIMessageTest() throws Exception {
        AS2ClientConnection clientConnection = new AS2ClientConnection(AS2_VERSION, USER_AGENT, CLIENT_FQDN, TARGET_HOST, TARGET_PORT);
        AS2ClientManager clientManager = new AS2ClientManager(clientConnection);

        ContentType contentType = ContentType.create(AS2MediaType.APPLICATION_EDIFACT, AS2Charset.US_ASCII);
        HttpCoreContext httpContext = clientManager.send(EDI_MSG,
                REQUEST_URI,
                SUBJECT,
                FROM,
                AS2_NAME,
                AS2_NAME,
                AS2MessageStructure.PLAIN,
                contentType,
                null,
                null,
                null,
                null,
                null,"mrAS@example.org", null,null,null);
    }

    @Test
    public void mdnMessageTest() throws Exception {
        AS2ClientConnection clientConnection = new AS2ClientConnection(AS2_VERSION, USER_AGENT, CLIENT_FQDN, TARGET_HOST, TARGET_PORT);
        AS2ClientManager clientManager = new AS2ClientManager(clientConnection);

        HttpCoreContext httpContext = clientManager.send(EDI_MSG, REQUEST_URI, SUBJECT, FROM, AS2_NAME, AS2_NAME,
                AS2MessageStructure.PLAIN, ContentType.create(AS2MediaType.APPLICATION_EDIFACT, AS2Charset.US_ASCII),
                null, null, null,null, null,"mrAS@example.org",null,null,null);

        @SuppressWarnings("unused")
        HttpResponse response = httpContext.getResponse();
    }
}
