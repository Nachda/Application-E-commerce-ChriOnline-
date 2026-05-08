package service;

import dao.OtpDAO;

import java.security.SecureRandom;

/**
 * Gère la génération et la vérification des codes OTP.
 */
public class OtpService {

    private final OtpDAO otpDAO;
    private final EmailService emailService;

    public OtpService() {
        this.otpDAO = new OtpDAO();
        this.emailService = new EmailService();
    }

    private String generateCode() {
        SecureRandom random = new SecureRandom();
        int code = 100000 + random.nextInt(900000);
        return String.valueOf(code);
    }

    /**
     * Envoie un code OTP par email.
     * @param email l'adresse email de l'utilisateur
     * @return true si envoyé avec succès
     */
    public boolean sendOtp(String email) {
        if (email == null || email.isBlank()) return false;
        if (!otpDAO.userExists(email)) return false;

        String code = generateCode();
        if (!otpDAO.saveOtp(email, code)) return false;
        if (!emailService.sendOtpEmail(email, code)) return false;

        System.out.println("OTP envoyé à " + email);
        return true;
    }

    /**
     * Vérifie un code OTP et active le compte si correct.
     * @param email l'email
     * @param code le code saisi
     * @return true si vérifié
     */
    public boolean verifyOtp(String email, String code) {
        if (email == null || code == null) return false;
        boolean valid = otpDAO.verifyOtp(email, code);
        if (valid) {
            otpDAO.activateAccount(email);
        }
        return valid;
    }
}