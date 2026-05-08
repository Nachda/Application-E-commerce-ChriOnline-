package security;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import java.security.SecureRandom;
import java.security.KeyStore;
import java.io.FileInputStream;
import java.util.Base64;

/**
 * Chiffrement / déchiffrement AES-256-GCM des données personnelles en base.
 * La clé est lue depuis le keystore serveur (server.p12, alias 'aes-key').
 */
public class DataEncryptor {

    private static final String AES_GCM = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;   // 96 bits
    private static final int GCM_TAG_LENGTH = 128; // bits

    private SecretKey aesKey;

    /**
     * Charge la clé AES depuis le keystore serveur.
     * @param keystorePath chemin du keystore (server.p12)
     * @param keystorePassword mot de passe du keystore
     */
    public DataEncryptor(String keystorePath, char[] keystorePassword) throws Exception {
        KeyStore ks = KeyStore.getInstance("PKCS12");
        try (FileInputStream fis = new FileInputStream(keystorePath)) {
            ks.load(fis, keystorePassword);
        }
        KeyStore.ProtectionParameter protParam = new KeyStore.PasswordProtection(keystorePassword);
        KeyStore.SecretKeyEntry skEntry = (KeyStore.SecretKeyEntry) ks.getEntry("aes-key", protParam);
        if (skEntry == null) {
            throw new IllegalStateException("Clé AES introuvable dans le keystore (alias 'aes-key')");
        }
        this.aesKey = skEntry.getSecretKey();
    }

    /**
     * Chiffre une donnée.
     * @param plainText la donnée en clair
     * @return une chaîne au format "Base64(IV) : Base64(ciphertext)"
     */
    public String encrypt(String plainText) throws Exception {
        Cipher cipher = Cipher.getInstance(AES_GCM);
        byte[] iv = new byte[GCM_IV_LENGTH];
        new SecureRandom().nextBytes(iv);
        GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.ENCRYPT_MODE, aesKey, spec);
        byte[] cipherText = cipher.doFinal(plainText.getBytes("UTF-8"));
        return Base64.getEncoder().encodeToString(iv) + ":" + Base64.getEncoder().encodeToString(cipherText);
    }

    /**
     * Déchiffre une donnée.
     * @param encryptedData la chaîne produite par encrypt()
     * @return la donnée en clair, ou une chaîne vide en cas d'erreur
     */
    public String decrypt(String encryptedData) {
        try {
            String[] parts = encryptedData.split(":");
            if (parts.length != 2) return "";
            byte[] iv = Base64.getDecoder().decode(parts[0]);
            byte[] cipherText = Base64.getDecoder().decode(parts[1]);
            Cipher cipher = Cipher.getInstance(AES_GCM);
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, aesKey, spec);
            byte[] plainBytes = cipher.doFinal(cipherText);
            return new String(plainBytes, "UTF-8");
        } catch (Exception e) {
            System.err.println("Erreur déchiffrement : " + e.getMessage());
            return "";
        }
    }
}