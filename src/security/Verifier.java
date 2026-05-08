package security;

import java.security.PublicKey;
import java.security.Signature;

/**
 * Vérifie une signature RSA sur un challenge.
 * Utilisé par le serveur pour authentifier l'admin.
 */
public class Verifier {

    /**
     * Vérifie que la signature correspond bien au challenge avec la clé publique donnée.
     * @param challenge Le challenge original
     * @param signatureBytes La signature reçue
     * @param publicKey La clé publique de l'admin
     * @return true si la signature est valide
     * @throws Exception si erreur de vérification
     */
    public static boolean verify(String challenge, byte[] signatureBytes, PublicKey publicKey) throws Exception {
        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initVerify(publicKey);
        signature.update(challenge.getBytes());
        return signature.verify(signatureBytes);
    }
}