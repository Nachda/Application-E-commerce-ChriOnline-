package security;

import java.security.SecureRandom;
import java.util.Base64;

/**
 * Génère un challenge aléatoire pour l'authentification admin par challenge-réponse.
 * Utilise SecureRandom (cryptographiquement sûr) et encode en Base64.
 */
public class ChallengeGenerator {

    /**
     * Génère un challenge aléatoire de 32 octets encodé en Base64.
     * @return le challenge (44 caractères)
     */
    public static String generateChallenge() {
        byte[] random = new byte[32];
        new SecureRandom().nextBytes(random);
        return Base64.getEncoder().encodeToString(random);
    }
}