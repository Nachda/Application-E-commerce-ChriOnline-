package security;

import javax.crypto.Cipher;
import javax.crypto.KeyAgreement;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.net.Socket;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.Base64;

/**
 * Canal de communication sécurisé (TLS-like simplifié).
 *
 * Protocole :
 * 1. Le serveur envoie son certificat X.509 (Base64).
 * 2. Le client vérifie le certificat avec son truststore.
 * 3. Échange de clés ECDH éphémère (courbes elliptiques, 256 bits).
 * 4. Dérivation d'une clé AES 256 bits à partir du secret partagé (SHA-256).
 * 5. Tous les messages suivants sont chiffrés avec AES-GCM (confidentialité + intégrité).
 *
 * Chaque message chiffré est précédé de :
 * - IV (12 octets)
 * - Longueur des données chiffrées (int, 4 octets)
 * - Données chiffrées (incluant le tag GCM)
 */
public class SecureChannel {

    private final Socket socket;
    private final DataInputStream dataIn;
    private final DataOutputStream dataOut;

    private SecretKey sessionKey;      // Clé AES de session
    private Cipher encryptCipher;      // Chiffreur (initialisé à chaque envoi avec nouvel IV)
    private Cipher decryptCipher;      // Déchiffreur (initialisé à chaque réception)

    private static final String AES_GCM = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;    // 96 bits recommandé
    private static final int GCM_TAG_LENGTH = 128;  // bits

    // ──────────────────────── Constructeur côté serveur ────────────────────────
    /**
     * Côté serveur : envoie le certificat puis effectue le handshake.
     * @param socket la socket déjà connectée
     * @param serverPrivateKey la clé privée du serveur (chargée depuis son keystore)
     * @param serverCertificate le certificat X.509 du serveur (pour l'envoyer au client)
     */
    public SecureChannel(Socket socket, PrivateKey serverPrivateKey, X509Certificate serverCertificate) throws Exception {
        this.socket = socket;
        this.dataIn = new DataInputStream(socket.getInputStream());
        this.dataOut = new DataOutputStream(socket.getOutputStream());

        // Étape 1 : envoi du certificat
        byte[] certBytes = serverCertificate.getEncoded();
        String certBase64 = Base64.getEncoder().encodeToString(certBytes);
        sendLine(certBase64);

        // Étape 2 : réception de la clé publique ECDH du client
        String clientPubKeyBase64 = readLine();
        byte[] clientPubKeyBytes = Base64.getDecoder().decode(clientPubKeyBase64);
        KeyFactory kf = KeyFactory.getInstance("EC");
        PublicKey clientPubKey = kf.generatePublic(new X509EncodedKeySpec(clientPubKeyBytes));

        // Étape 3 : génération de la paire ECDH du serveur
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("EC");
        kpg.initialize(256);
        KeyPair serverDHKeyPair = kpg.generateKeyPair();

        // Envoi de la clé publique du serveur
        byte[] serverPubKeyBytes = serverDHKeyPair.getPublic().getEncoded();
        String serverPubKeyBase64 = Base64.getEncoder().encodeToString(serverPubKeyBytes);
        sendLine(serverPubKeyBase64);

        // Étape 4 : calcul du secret partagé
        KeyAgreement ka = KeyAgreement.getInstance("ECDH");
        ka.init(serverDHKeyPair.getPrivate());
        ka.doPhase(clientPubKey, true);
        byte[] sharedSecret = ka.generateSecret();

        // Étape 5 : dérivation de la clé AES
        sessionKey = deriveKey(sharedSecret);

        // Nettoyage mémoire
        Arrays.fill(sharedSecret, (byte) 0);

        System.out.println("✅ Canal sécurisé établi (serveur)");
    }

    // ──────────────────────── Constructeur côté client ────────────────────────
    /**
     * Côté client : reçoit le certificat, le vérifie, puis effectue le handshake.
     * @param socket la socket déjà connectée
     * @param truststorePath chemin du truststore PKCS12
     * @param truststorePassword mot de passe du truststore (sera effacé après usage)
     */
    public SecureChannel(Socket socket, String truststorePath, char[] truststorePassword) throws Exception {
        this.socket = socket;
        this.dataIn = new DataInputStream(socket.getInputStream());
        this.dataOut = new DataOutputStream(socket.getOutputStream());

        // Étape 1 : réception du certificat serveur
        String certBase64 = readLine();
        byte[] certBytes = Base64.getDecoder().decode(certBase64);
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        X509Certificate serverCert = (X509Certificate) cf.generateCertificate(new ByteArrayInputStream(certBytes));

        // Étape 2 : vérification avec le truststore
        KeyStore truststore = KeyStore.getInstance("PKCS12");
        try (FileInputStream fis = new FileInputStream(truststorePath)) {
            truststore.load(fis, truststorePassword);
        } finally {
            Arrays.fill(truststorePassword, '\0'); // nettoyage immédiat
        }

        if (!truststore.containsAlias("server")) {
            throw new SecurityException("Certificat serveur non trouvé dans le truststore (alias 'server')");
        }

        Certificate trustedCert = truststore.getCertificate("server");
        // Vérifier que le certificat reçu est le même que celui du truststore
        if (!serverCert.equals(trustedCert)) {
            throw new SecurityException("Le certificat du serveur ne correspond pas à celui du truststore");
        }

        // Étape 3 : génération de la paire ECDH du client
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("EC");
        kpg.initialize(256);
        KeyPair clientDHKeyPair = kpg.generateKeyPair();

        // Envoi de la clé publique du client
        byte[] clientPubKeyBytes = clientDHKeyPair.getPublic().getEncoded();
        String clientPubKeyBase64 = Base64.getEncoder().encodeToString(clientPubKeyBytes);
        sendLine(clientPubKeyBase64);

        // Étape 4 : réception de la clé publique ECDH du serveur
        String serverPubKeyBase64 = readLine();
        byte[] serverPubKeyBytes = Base64.getDecoder().decode(serverPubKeyBase64);
        KeyFactory kf = KeyFactory.getInstance("EC");
        PublicKey serverPubKey = kf.generatePublic(new X509EncodedKeySpec(serverPubKeyBytes));

        // Étape 5 : calcul du secret partagé
        KeyAgreement ka = KeyAgreement.getInstance("ECDH");
        ka.init(clientDHKeyPair.getPrivate());
        ka.doPhase(serverPubKey, true);
        byte[] sharedSecret = ka.generateSecret();

        // Étape 6 : dérivation de la clé AES
        sessionKey = deriveKey(sharedSecret);
        Arrays.fill(sharedSecret, (byte) 0);

        System.out.println("✅ Canal sécurisé établi (client)");
    }

    // ──────────────────────── Méthodes de chiffrement ────────────────────────
    /**
     * Envoie un message chiffré (AES-GCM).
     */
    public synchronized void sendEncryptedMessage(String message) throws Exception {
        Cipher cipher = Cipher.getInstance(AES_GCM);
        byte[] iv = new byte[GCM_IV_LENGTH];
        new SecureRandom().nextBytes(iv);
        GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.ENCRYPT_MODE, sessionKey, spec);
        byte[] encrypted = cipher.doFinal(message.getBytes("UTF-8"));

        dataOut.write(iv);
        dataOut.writeInt(encrypted.length);
        dataOut.write(encrypted);
        dataOut.flush();
    }

    /**
     * Reçoit un message chiffré (AES-GCM).
     */
    public synchronized String receiveEncryptedMessage() throws Exception {
        byte[] iv = new byte[GCM_IV_LENGTH];
        dataIn.readFully(iv);
        int length = dataIn.readInt();
        byte[] encrypted = new byte[length];
        dataIn.readFully(encrypted);

        Cipher cipher = Cipher.getInstance(AES_GCM);
        GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.DECRYPT_MODE, sessionKey, spec);
        byte[] decrypted = cipher.doFinal(encrypted);
        return new String(decrypted, "UTF-8");
    }

    // ──────────────────────── Méthodes internes ────────────────────────
    /**
     * Envoie une ligne en clair (pendant le handshake).
     * Format : longueur (2 octets) + données UTF-8.
     */
    private void sendLine(String line) throws IOException {
        byte[] data = line.getBytes("UTF-8");
        dataOut.writeShort(data.length);
        dataOut.write(data);
        dataOut.flush();
    }

    /**
     * Reçoit une ligne en clair (pendant le handshake).
     */
    private String readLine() throws IOException {
        int length = dataIn.readUnsignedShort();
        byte[] data = new byte[length];
        dataIn.readFully(data);
        return new String(data, "UTF-8");
    }

    /**
     * Dérive une clé AES 256 bits à partir du secret partagé ECDH.
     */
    private SecretKey deriveKey(byte[] sharedSecret) throws Exception {
        MessageDigest sha = MessageDigest.getInstance("SHA-256");
        byte[] keyBytes = sha.digest(sharedSecret);
        return new SecretKeySpec(keyBytes, "AES");
    }

    /**
     * Libère la socket.
     */
    public void close() throws IOException {
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
    }
}