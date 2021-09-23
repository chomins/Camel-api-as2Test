package com.example.camelas2apitest.route;

import com.example.camelas2apitest.cert.SelfSignedCertLoader;
import com.sun.istack.ByteArrayDataSource;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.as2.api.AS2MessageStructure;
import org.apache.camel.component.as2.api.entity.DispositionNotificationMultipartReportEntity;
import org.apache.camel.http.common.HttpMessage;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import javax.mail.BodyPart;
import javax.mail.Header;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMultipart;
import javax.servlet.http.HttpServletRequest;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;

public class LocalClientRouteBuilder extends RouteBuilder {

    @Autowired
    CamelContext camelContext;

    @Autowired
    private SelfSignedCertLoader selfSignedCertLoader;

    @Value("${as2.version}")
    private String as2Version;

    @Value("${camel.server.uri}")
    private String as2RequestUri;

    @Value("${camel.server.port}")
    private Integer as2ServerPortNumber;

    private static org.apache.http.entity.ContentType contentType =
            org.apache.http.entity.ContentType.create("application/edifact", (Charset) null);

    @Override
    public void configure() throws Exception{
        from("jetty:http://localhost:3500/link")
                .routeId("as2ClientLocalhost")
                .process(new Processor() {
                    @Override
                    public void process(Exchange exchange) throws Exception {
                        String messageIn = exchange.getIn().getBody(String.class);
                        System.out.println("Request Body Msg: "+ messageIn);
                        if (exchange.getIn() instanceof HttpMessage) {
                            HttpMessage httpMessage =
                                    (HttpMessage) exchange.getIn();
                            HttpServletRequest request = httpMessage.getRequest();
                            String httpMethod = request.getMethod();

                            System.out.println("HTTP method: " + httpMethod);

                            if ("POST".equals(httpMethod)) {
                                System.out.println("POST request");
                            } else {
                                System.out.println("not POST request");
                            }
                        }
                        exchange.getIn().reset();
                        exchange.getIn().setBody("Sample_EDI");

                        exchange.getIn().setHeader("CamelAS2.as2To", "DKtestAS2");
                        exchange.getIn().setHeader("CamelAS2.as2From", "DKcompanyAS2");

                        exchange.getIn().setHeader("CamelAS2.as2Version", as2Version);
                        exchange.getIn().setHeader("CamelAS2.ediMessageContentType", contentType);
                        exchange.getIn().setHeader("CamelAS2.server", "DK AS2Client Localhost");
                        exchange.getIn().setHeader("CamelAS2.subject", "testDK");
                        exchange.getIn().setHeader("CamelAS2.from", "DKEdi");
                        exchange.getIn().setHeader("CamelAS2.dispositionNotificationTo", "dk2k@mail.ru");
                        exchange.getIn().setHeader("CamelAS2.requestUri", as2RequestUri);

                        exchange.getIn().setHeader("CamelAS2.as2MessageStructure", AS2MessageStructure.PLAIN);
                    }
                })
                .to("as2://client/send?targetHostName=localhost" +
                        "&targetPortNumber={{camel.server.port}}" + // http
                        "&inBody=ediMessage" +
                        "&requestUri={{camel.server.uri}}"
                )
                .id("DKAS2senderToLocalhost")
                .process(new Processor() {
                    @Override
                    public void process(Exchange exchange) throws Exception {
                        if (exchange.getIn() != null) {
                            try {
                                String messageIn = exchange.getIn().getBody(String.class);
                                System.out.println("+++++++++++++++++++++++++++++++++++++++++++++++++++");
                                System.out.println("Received MDN Message: " + messageIn);
                                processMultipartMessage(exchange.getIn());
                                System.out.println("+++++++++++++++++++++++++++++++++++++++++++++++++++");
                            } catch (NullPointerException npe) {
                                log.error("NPE caught");
                            }
                        } else {
                            log.warn("process(): null for MDN");
                        }
                    }
                });
    }
    private void processMultipartMessage(Message message) {
        //org.apache.camel.component.as2.api.entity.DispositionNotificationMultipartReportEntity
        if (message.getBody() instanceof DispositionNotificationMultipartReportEntity) {
            DispositionNotificationMultipartReportEntity dispositionNotificationMultipartReportEntity =
                    (DispositionNotificationMultipartReportEntity) message.getBody();
            try {
                InputStream inputStream = dispositionNotificationMultipartReportEntity.getContent();

                ByteArrayDataSource datasource = new ByteArrayDataSource(IOUtils.toByteArray(inputStream), "multipart/report");
                MimeMultipart multipart = new MimeMultipart(datasource);

                int count = multipart.getCount();
                log.debug("count " + count);
                for (int i = 0; i < count; i++) {
                    BodyPart bodyPart = multipart.getBodyPart(i);
                    if (bodyPart.isMimeType("text/plain")) {
                        log.info("text/plain");
                        System.out.println(bodyPart.getContent().getClass());
                        Enumeration<Header> headerEnumeration = bodyPart.getAllHeaders();
                        while (headerEnumeration.hasMoreElements()) {
                            Header header = headerEnumeration.nextElement();
                            System.out.println(header.getName() + ": " + header.getValue());
                        }
                        System.out.println("----");
                        System.out.println(bodyPart.getContent());
                        System.out.println("----");
                        //processTextData(bodyPart.getContent());
                    } else if (bodyPart.isMimeType("application/octet-stream")) {
                        log.info("application/octet-stream");
                        System.out.println(bodyPart.getContent().getClass());
                        //processBinaryData(bodyPart.getInputStream());
                    } else if (bodyPart.isMimeType("message/disposition-notification")) {
                        // MDN!
                        log.info("message/disposition-notification");
                        Enumeration<Header> headerEnumeration = bodyPart.getAllHeaders();
                        while (headerEnumeration.hasMoreElements()) {
                            Header header = headerEnumeration.nextElement();
                            System.out.println(header.getName() + ": " + header.getValue());
                        }
                        //System.out.println(bodyPart.getContent());
                        if (bodyPart.getContent() instanceof ByteArrayInputStream) {
                            ByteArrayInputStream byteArrayInputStream = (ByteArrayInputStream) bodyPart.getContent();
                            int n = byteArrayInputStream.available();
                            byte[] bytes = new byte[n];
                            byteArrayInputStream.read(bytes, 0, n);
                            String s = new String(bytes, StandardCharsets.UTF_8);
                            System.out.println("----");
                            System.out.println(new String(bytes));
                            System.out.println("----");
                        }
                    } else {
                        System.out.println(bodyPart.getContent().getClass());
                        log.warn("default " + bodyPart.getContentType());
                    }
                }
            } catch (IOException | MessagingException e) {
                e.printStackTrace();
            }
        }
        //System.out.println();
    }
}
