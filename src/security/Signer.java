package security;

import java.security.PrivateKey;
import java.security.Signature;

/**
 * Signe un challenge avec une clé privée RSA.
 * Utilisé par le client admin pour prouver la possession de la clé privée.
 */
public class Signer {

    /**
     * Signe le challenge avec la clé privée donnée.
     * @param challenge Le challenge à signer
     * @param privateKey La clé privée RSA
     * @return La signature au format byte[]
     * @throws Exception si erreur de signature
     */
    public static byte[] sign(String challenge, PrivateKey privateKey) throws Exception {
        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initSign(privateKey);
        signature.update(challenge.getBytes());
        return signature.sign();
    }
}