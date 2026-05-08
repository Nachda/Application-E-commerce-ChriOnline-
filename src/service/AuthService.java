package service;

import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

import dao.UserDAO;
import model.Client;
import model.User;
import security.PasswordSecurityManager;

/**
 * Gère l'authentification et l'inscription des utilisateurs.
 * Utilise le double hachage : SHA-256 côté client + PBKDF2 côté serveur.
 */
public class AuthService {

    private final UserDAO userDAO;

    public AuthService() {
        this.userDAO = new UserDAO();
    }

    /**
     * Génère un salt aléatoire pour le hachage serveur.
     */
    private String generateSalt() {
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[16];
        random.nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }

    /**
     * Hache un mot de passe déjà haché côté client avec un salt.
     * @param clientHashedPassword le hash SHA-256 reçu du client
     * @param salt le salt (Base64)
     * @return le nouveau hash PBKDF2 (salt:hash)
     */
    public String hashPassword(String clientHashedPassword, String salt) {
        // On applique PBKDF2 sur le hash client. Le résultat est combiné avec le salt.
        // On utilise PasswordSecurityManager, mais attention : il attend un char[].
        // On va plutôt utiliser une version simplifiée car le client envoie déjà un hash.
        // Méthode directe :
        try {
            // On convertit le hash client en char[] pour PasswordSecurityManager
            char[] hashChars = clientHashedPassword.toCharArray();
            String finalHash = PasswordSecurityManager.hashPassword(hashChars);
            // hashPassword retourne "salt+hash" encodé en Base64.
            // Mais nous avons déjà un salt séparé ? On va plutôt refaire le processus complet.
            // Pour rester cohérent avec l'ancien code, on va stocker "salt:hashPBKDF2" séparément.
            // Donc on va appeler une méthode interne qui fait PBKDF2 sur le hash client + le salt fourni.
            return hashWithSalt(clientHashedPassword, salt);
        } catch (Exception e) {
            System.err.println("Erreur hashPassword : " + e.getMessage());
            return null;
        }
    }

    // Méthode privée pour faire PBKDF2 sur (hashClient) avec un salt donné
    private String hashWithSalt(String data, String saltBase64) {
        try {
            byte[] salt = Base64.getDecoder().decode(saltBase64);
            PBEKeySpec spec = new PBEKeySpec(data.toCharArray(), salt, 65536, 256);
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            byte[] hash = factory.generateSecret(spec).getEncoded();
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Vérifie si le mot de passe client correspond au hash stocké (format "salt:hash").
     */
    public boolean checkPassword(String clientHashedPassword, String storedHash) {
        if (storedHash == null || storedHash.isBlank()) return false;
        String[] parts = storedHash.split(":");
        if (parts.length != 2) return false;
        String salt = parts[0];
        String expectedHash = parts[1];
        String computedHash = hashWithSalt(clientHashedPassword, salt);
        return expectedHash.equals(computedHash);
    }

    /**
     * Authentifie un utilisateur.
     * @param email l'email
     * @param clientHashedPassword le hash SHA-256 du mot de passe (venant du client)
     * @return l'utilisateur si succès, null sinon
     */
    public User login(String email, String clientHashedPassword) {
        if (email == null || email.isEmpty() || clientHashedPassword == null || clientHashedPassword.isEmpty())
            return null;

        User user = userDAO.findByEmail(email);
        if (user == null) return null;
        if (!"active".equalsIgnoreCase(user.getStatus())) return null;

        if (checkPassword(clientHashedPassword, user.getPassword())) {
            System.out.println("✅ Connexion réussie : " + email);
            return user;
        }
        return null;
    }

    /**
     * Enregistre un client avec statut "pending" (en attente OTP).
     * @return true si succès
     */
    public boolean registerPending(String nom, String prenom, String email,
                                   String clientHashedPassword, String address,
                                   String phone, String ville) {
        if (nom == null || nom.isBlank() || prenom == null || prenom.isBlank() ||
                email == null || email.isBlank() || clientHashedPassword == null || clientHashedPassword.isBlank())
            return false;

        if (userDAO.emailExists(email)) return false;

        // Générer un nouveau salt et calculer le hash PBKDF2
        String salt = generateSalt();
        String serverHash = hashWithSalt(clientHashedPassword, salt);
        if (serverHash == null) return false;

        // Format stocké : "salt:hash"
        String finalStoredHash = salt + ":" + serverHash;

        Client client = new Client(nom, prenom, email, finalStoredHash, address, phone, ville);
        return userDAO.savePendingClient(client);
    }

    public boolean emailExists(String email) {
        return userDAO.emailExists(email);
    }

    public boolean isAccountActive(String email) {
        return userDAO.isAccountActive(email);
    }
}