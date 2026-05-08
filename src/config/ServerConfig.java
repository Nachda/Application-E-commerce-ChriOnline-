package config;

import security.PasswordSecurityManager;
import java.util.Arrays;
import java.util.Scanner;

/**
 * Gestion sécurisée de la configuration du serveur.
 * Demande interactivement les mots de passe des keystores, les hache immédiatement,
 * et les efface de la mémoire à l'arrêt.
 */
public class ServerConfig {
	
	public static final int PORT = 5001;

    private static final String TRUSTSTORE_PATH = "truststore.p12";
    private static final String KEYSTORE_PATH = "server.p12";

    private static char[] truststorePassword = null;
    private static char[] keystorePassword = null;

    private static boolean initialized = false;

    public static void initialize() {
        if (initialized) return;

        System.out.println("\n🔐 Initialisation configuration serveur sécurisée");

        Scanner scanner = new Scanner(System.in);

        // Demander mot de passe truststore
        System.out.print("🔑 Mot de passe du truststore : ");
        String trustInput = scanner.nextLine();
        if (trustInput.trim().isEmpty()) {
            System.err.println("❌ Mot de passe truststore vide !");
            System.exit(1);
        }
        truststorePassword = trustInput.toCharArray();

        // Demander mot de passe keystore serveur
        System.out.print("🔑 Mot de passe du keystore serveur : ");
        String keyInput = scanner.nextLine();
        if (keyInput.trim().isEmpty()) {
            System.err.println("❌ Mot de passe keystore serveur vide !");
            System.exit(1);
        }
        keystorePassword = keyInput.toCharArray();

        initialized = true;
        System.out.println("✅ Configuration serveur prête.\n");
    }

    public static char[] getTruststorePassword() {
        return truststorePassword;
    }

    public static char[] getKeystorePassword() {
        return keystorePassword;
    }

    public static String getTruststorePath() { return TRUSTSTORE_PATH; }
    public static String getKeystorePath() { return KEYSTORE_PATH; }

    public static void cleanup() {
        if (truststorePassword != null) {
            Arrays.fill(truststorePassword, '\0');
            truststorePassword = null;
        }
        if (keystorePassword != null) {
            Arrays.fill(keystorePassword, '\0');
            keystorePassword = null;
        }
        System.out.println("✅ Mémoire nettoyée.");
    }
}