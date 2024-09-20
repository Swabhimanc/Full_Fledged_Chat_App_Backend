package com.connecto.utilities;

import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.HtmlEmail;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Random;

public class EmailService {

    private static String OTP = "";

    public static String sendOTP(String name, String userEmail) throws EmailException, IOException {
        Random random = new Random();
        int randomNumber = random.nextInt(999999); // Generates a number between 0 and 999999
        String OTP = String.format("%06d", randomNumber);

        // Load the HTML template using the class loader
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        InputStream inputStream = classLoader.getResourceAsStream("templates/OtpTemplate.html");
        if (inputStream == null) {
            throw new FileNotFoundException("OtpTemplate.html not found in the resources/templates folder.");
        }
        String htmlTemplate = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        htmlTemplate = htmlTemplate.replace("{{name}}", name).replace("{{OTP}}", OTP);

        String userName = System.getenv("SENDER_EMAIL_ID");
        String password = System.getenv("SENDER_PASSWORD");

        if (userName == null || password == null) {
            throw new IllegalArgumentException("SENDER_EMAIL_ID or SENDER_PASSWORD environment variable is not set.");
        }
        HtmlEmail email = new HtmlEmail();
        email.setHostName("smtp.gmail.com");
        email.setSSLOnConnect(true);
        email.setAuthentication(userName, password);
        email.setFrom(userName, "ConnectO");
        email.setSubject("ConnectO New User Registration");
        email.setHtmlMsg(htmlTemplate);
        email.addTo(userEmail);

        // Send the email
        email.send();

        // Log the OTP
//        System.out.println("OTP: " + OTP);

        return OTP;
    }

    public static void sendResetLink(String name, String userEmail, String resetLink) throws EmailException, IOException {

        String userName = System.getenv("SENDER_EMAIL_ID");
        String password = System.getenv("SENDER_PASSWORD");

        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        InputStream inputStream = classLoader.getResourceAsStream("templates/ResetTemplate.html");
        if (inputStream == null) {
            throw new FileNotFoundException("OtpTemplate.html not found in the resources/templates folder.");
        }
        String htmlTemplate = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        htmlTemplate = htmlTemplate.replace("{{name}}", name).replace("{{link}}", resetLink);

        // create the email message
        HtmlEmail email = new HtmlEmail();
        email.setHostName("smtp.gmail.com");
        email.setSSLOnConnect(Boolean.TRUE);
        email.setAuthentication(userName, password);
        email.setFrom(userName, "ConnectO");
        email.setSubject("ConnectO Password Reset");
        email.setHtmlMsg(htmlTemplate);
        email.addTo(userEmail);
        // send the email
        email.send();
    }

    public static void sendResetConfirmation(String userEmail) throws EmailException {

        String userName = System.getenv("SENDER_EMAIL_ID");
        String password = System.getenv("SENDER_PASSWORD");

        HtmlEmail email = new HtmlEmail();
        email.setHostName("smtp.gmail.com");
        email.setSSLOnConnect(Boolean.TRUE);
        email.setAuthentication(userName, password);
        email.setFrom(userName, "ConnectO");
        email.setSubject("ConnectO New User Registration");
        email.setMsg("Your password has been changed recently.\n" +
                "If not done by you, reset your password immediately.");
        email.addTo(userEmail);
        email.send();
    }
}
