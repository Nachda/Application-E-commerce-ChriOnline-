package security;

import java.io.FileInputStream;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.util.Arrays;

/**
 * Gestionnaire de keystores (format PKCS12).
 * Permet de charger des clés privées, certificats et clés publiques.
 */
public class KeystoreManager {

    private static final String KEYSTORE_TYPE = "PKCS12";

    /**
     * Charge une clé privée depuis un keystore.
     * @param keystorePath chemin du fichier .p12
     * @param keystorePassword mot de passe du keystore (sera effacé après usage)
     * @param alias alias de l'entrée
     * @param keyPassword mot de passe de la clé (souvent identique au keystore)
     * @return la clé privée, ou null si échec
     */
    public static PrivateKey loadPrivateKeyFromKeystore(
            String keystorePath, char[] keystorePassword, String alias, char[] keyPassword) {
        try (FileInputStream fis = new FileInputStream(keystorePath)) {
            KeyStore keyStore = KeyStore.getInstance(KEYSTORE_TYPE);
            keyStore.load(fis, keystorePassword);
            PrivateKey privateKey = (PrivateKey) keyStore.getKey(alias, keyPassword);
            if (privateKey == null) {
                System.err.println("❌ Clé privée non trouvée pour l'alias : " + alias);
            }
            return privateKey;
        } catch (Exception e) {
            System.err.println("❌ Erreur chargement clé privée : " + e.getMessage());
            return null;
        } finally {
            if (keystorePassword != null) Arrays.fill(keystorePassword, '\0');
            if (keyPassword != null) Arrays.fill(keyPassword, '\0');
        }
    }

    /**
     * Charge un certificat depuis un keystore.
     * @param keystorePath chemin du fichier .p12
     * @param keystorePassword mot de passe
     * @param alias alias de l'entrée
     * @return le certificat, ou null si échec
     */
    public static Certificate loadCertificate(
            String keystorePath, char[] keystorePassword, String alias) {
        try (FileInputStream fis = new FileInputStream(keystorePath)) {
            KeyStore keyStore = KeyStore.getInstance(KEYSTORE_TYPE);
            keyStore.load(fis, keystorePassword);
            Certificate cert = keyStore.getCertificate(alias);
            if (cert == null) {
                System.err.println("❌ Certificat non trouvé pour l'alias : " + alias);
            }
            return cert;
        } catch (Exception e) {
            System.err.println("❌ Erreur chargement certificat : " + e.getMessage());
            return null;
        } finally {
            if (keystorePassword != null) Arrays.fill(keystorePassword, '\0');
        }
    }

    /**
     * Extrait la clé publique d'un certificat.
     * @param certificate le certificat
     * @return la clé publique, ou null
     */
    public static PublicKey loadPublicKeyFromCertificate(Certificate certificate) {
        if (certificate == null) return null;
        return certificate.getPublicKey();
    }

    /**
     * Vérifie si un alias existe dans un truststore.
     * @param truststorePath chemin du truststore
     * @param truststorePassword mot de passe
     * @param alias alias à vérifier
     * @return true si l'alias est présent
     */
    public static boolean verifyCertificateInTruststore(
            String truststorePath, char[] truststorePassword, String alias) {
        try (FileInputStream fis = new FileInputStream(truststorePath)) {
            KeyStore truststore = KeyStore.getInstance(KEYSTORE_TYPE);
            truststore.load(fis, truststorePassword);
            return truststore.containsAlias(alias);
        } catch (Exception e) {
            System.err.println("❌ Erreur vérification truststore : " + e.getMessage());
            return false;
        } finally {
            if (truststorePassword != null) Arrays.fill(truststorePassword, '\0');
        }
    }

    /**
     * Charge un certificat depuis un truststore.
     */
    public static Certificate loadCertificateFromTruststore(
            String truststorePath, char[] truststorePassword, String alias) {
        try (FileInputStream fis = new FileInputStream(truststorePath)) {
            KeyStore truststore = KeyStore.getInstance(KEYSTORE_TYPE);
            truststore.load(fis, truststorePassword);
            return truststore.getCertificate(alias);
        } catch (Exception e) {
            System.err.println("❌ Erreur chargement certificat depuis truststore : " + e.getMessage());
            return null;
        } finally {
            if (truststorePassword != null) Arrays.fill(truststorePassword, '\0');
        }
    }
}