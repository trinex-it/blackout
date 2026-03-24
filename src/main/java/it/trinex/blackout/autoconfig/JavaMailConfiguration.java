package it.trinex.blackout.autoconfig;

import it.trinex.blackout.properties.MailProperties;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Properties;

@AutoConfiguration
@EnableConfigurationProperties(MailProperties.class)
public class JavaMailConfiguration {

    private final MailProperties mailProperties;

    public JavaMailConfiguration(MailProperties mailProperties) {
        this.mailProperties = mailProperties;
    }

    @Bean
    @ConditionalOnProperty(prefix = "blackout.mail", name = "enabled", havingValue = "true")
    public JavaMailSender javaMailSender() {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();

        // Impostazioni base SMTP da MailProperties
        mailSender.setHost(mailProperties.getHost());
        mailSender.setPort(mailProperties.getPort());
        mailSender.setUsername(mailProperties.getUsername());
        mailSender.setPassword(mailProperties.getPassword());

        // Proprietà aggiuntive Jakarta Mail
        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", mailProperties.getProtocol());
        props.put("mail.smtp.auth", mailProperties.getAuth().toString());
        props.put("mail.smtp.starttls.enable", mailProperties.getStarttls().toString());
        props.put("mail.debug", mailProperties.getDebug().toString());

        return mailSender;
    }
}
