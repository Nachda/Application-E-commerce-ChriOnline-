package service;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.Properties;

/**
 * Service d'envoi d'emails (pour OTP).
 * Utilise le compte Gmail configuré.
 * ⚠️ En production, les identifiants doivent être externalisés.
 */
public class EmailService {

    private static final String FROM_EMAIL = "nourouddine.nachda@gmail.com";
    private static final String APP_PASSWORD = "sehn ovlu pvtp epxj"; // mot de passe d'application

    public boolean sendOtpEmail(String toEmail, String otpCode) {
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");
        props.put("mail.smtp.ssl.trust", "smtp.gmail.com");

        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(FROM_EMAIL, APP_PASSWORD);
            }
        });

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(FROM_EMAIL));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
            message.setSubject("ChriOnline - Code de vérification");

            String body = "Bonjour,\n\n"
                    + "Votre code de vérification ChriOnline est : " + otpCode + "\n\n"
                    + "Ce code expire dans 10 minutes.\n\n"
                    + "L'équipe ChriOnline";
            message.setText(body);
            Transport.send(message);
            System.out.println("Email envoyé à " + toEmail);
            return true;
        } catch (MessagingException e) {
            System.err.println("Erreur envoi email : " + e.getMessage());
            return false;
        }
    }
}