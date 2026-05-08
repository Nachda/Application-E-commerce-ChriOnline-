import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.security.KeyStore;
import java.util.Scanner;

public class SetupAESKey {
    public static void main(String[] args) throws Exception {
        Scanner scanner = new Scanner(System.in);
        System.out.print("Mot de passe du keystore serveur (server.p12) : ");
        String keystorePassword = scanner.nextLine();
        scanner.close();

        // 1. Générer une clé AES 256 bits
        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        keyGen.init(256);
        SecretKey aesKey = keyGen.generateKey();

        // 2. Charger le keystore existant
        KeyStore ks = KeyStore.getInstance("PKCS12");
        try (FileInputStream fis = new FileInputStream("server.p12")) {
            ks.load(fis, keystorePassword.toCharArray());
        }

        // 3. Ajouter la clé AES (protégée par le même mot de passe)
        KeyStore.SecretKeyEntry skEntry = new KeyStore.SecretKeyEntry(aesKey);
        ks.setEntry("aes-key", skEntry, new KeyStore.PasswordProtection(keystorePassword.toCharArray()));

        // 4. Sauvegarder
        try (FileOutputStream fos = new FileOutputStream("server.p12")) {
            ks.store(fos, keystorePassword.toCharArray());
        }

        System.out.println("✅ Clé AES ajoutée dans server.p12 sous l'alias 'aes-key'.");
    }
}