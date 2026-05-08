package security;

import java.security.KeyPair;
import java.security.KeyPairGenerator;

/**
 * Génère une paire de clés RSA 2048 bits.
 * Utilisé pour la création initiale des clés admin.
 */
public class RSAKeyGenerator {

    /**
     * Génère une nouvelle paire de clés RSA.
     * @return KeyPair (clé publique + clé privée)
     * @throws Exception si l'algorithme n'est pas disponible
     */
    public static KeyPair generateKeyPair() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        return generator.generateKeyPair();
    }
}