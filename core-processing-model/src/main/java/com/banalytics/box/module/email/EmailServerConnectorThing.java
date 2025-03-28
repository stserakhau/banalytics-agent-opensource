package com.banalytics.box.module.email;

import com.banalytics.box.module.AbstractThing;
import com.banalytics.box.module.BoxEngine;
import com.banalytics.box.module.State;
import com.banalytics.box.module.Thing;
import com.cronutils.utils.StringUtils;
import jakarta.activation.DataHandler;
import jakarta.activation.DataSource;
import jakarta.activation.FileDataSource;
import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;

import java.io.File;
import java.io.StringReader;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.apache.commons.lang3.StringUtils.isNotEmpty;

/**
 * https://commons.apache.org/proper/commons-vfs/filesystems.html
 */
@Slf4j
@Order(Thing.StarUpOrder.DATA_EXCHANGE)
public class EmailServerConnectorThing extends AbstractThing<EMailServerConnectorConfig> {
    private final ExecutorService sendMailPool = Executors.newFixedThreadPool(2);
    private Session mailSession;

    @Override
    public String getTitle() {
        return configuration.title;
    }

    public EmailServerConnectorThing(BoxEngine metricDeliveryService) {
        super(metricDeliveryService);
    }

    @Override
    public Object uniqueness() {
        return configuration.title;
    }

    @Override
    protected void doInit() throws Exception {
    }

    @Override
    protected void doStart() throws Exception {
        Properties prop = new Properties();
        prop.load(new StringReader(configuration.properties));
        prop.put("mail.smtp.socketFactory.port", "465");
        prop.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");

//        prop.put("mail.smtp.host", "smtp.gmail.com");
//        prop.put("mail.smtp.port", "465");
//        prop.put("mail.smtp.auth", "true");
//        prop.put("mail.smtp.socketFactory.port", "465");
//        prop.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");

//            String host_name = "smtp.gmail.com";
//    props.put("mail.smtp.starttls.enable", "true");
//    props.put("mail.smtp.host", host_name);
//    props.put("mail.smtp.user", Email_Id);
//    props.put("mail.smtp.password", password);
//    props.put("mail.smtp.port", "587");
//    props.put("mail.smtp.auth", "true");

        this.mailSession = Session.getInstance(prop, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(configuration.username, configuration.password);
            }
        });
    }

    /**
     * @param fromEmail "abc@email.com"
     * @param toEmails  "to_username_a@gmail.com, to_username_b@yahoo.com"
     */
    public void sendMessage(
            String fromEmail,
            String toEmails,
            String ccEmails,
            String bccEmails,
            String subject,
            String htmlContent, File... attachments) {
        if (this.getState() != State.RUN) {
            return;
        }
        sendMailPool.submit(() -> {
            try {
                Message message = new MimeMessage(mailSession);
                if (StringUtils.isEmpty(fromEmail)) {
                    message.setFrom(new InternetAddress(configuration.username));
                } else {
                    message.setFrom(new InternetAddress(fromEmail));
                }
                message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmails));
                if (isNotEmpty(ccEmails)) {
                    message.setRecipients(Message.RecipientType.CC, InternetAddress.parse(ccEmails));
                }
                if (isNotEmpty(bccEmails)) {
                    message.setRecipients(Message.RecipientType.BCC, InternetAddress.parse(bccEmails));
                }
                message.setSubject(subject);

                {
                    Multipart multipart = new MimeMultipart();
                    {// Create the message part
                        MimeBodyPart messageBodyPart = new MimeBodyPart();
                        messageBodyPart.setContent(htmlContent, "text/html");
                        multipart.addBodyPart(messageBodyPart);
                    }
                    if (attachments != null) {
                        for (File file : attachments) {
                            BodyPart attachmentPart = new MimeBodyPart();
                            DataSource source = new FileDataSource(file);
                            attachmentPart.setDataHandler(new DataHandler(source));
                            attachmentPart.setFileName(file.getName());
                            multipart.addBodyPart(attachmentPart);
                        }
                    }
                    message.setContent(multipart);
                }
                Transport.send(message);
            } catch (MessagingException e) {
                onProcessingException(e);
            }
        });
    }

    @Override
    protected void doStop() throws Exception {
        this.mailSession = null;
    }

    public static void main(String[] args) throws MessagingException {
        Properties prop = new Properties();
        prop.put("mail.smtp.host", "smtp.gmail.com");
        prop.put("mail.smtp.port", "465");
        prop.put("mail.smtp.auth", "true");
        prop.put("mail.smtp.socketFactory.port", "465");
        prop.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
        prop.put("mail.debug", "true");

        Session mailSession = Session.getInstance(prop, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication("siarhei.tserakhau@gmail.com", "scay zdir awyk rmyg");
            }
        });

        Message message = new MimeMessage(mailSession);
        message.setFrom(new InternetAddress("siarhei.tserakhau@gmail.com"));
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse("siarhei.tserakhau@gmail.com"));
        message.setSubject("Test Mail Subject");

        // Создаем тело письма
        MimeBodyPart messageBodyPart = new MimeBodyPart();
        messageBodyPart.setText("Mail Body");

        // Добавляем его в Multipart
        Multipart multipart = new MimeMultipart();
        multipart.addBodyPart(messageBodyPart);

        // Устанавливаем Multipart как контент письма
        message.setContent(multipart);

        Transport.send(message);
    }
}