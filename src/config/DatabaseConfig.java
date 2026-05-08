package config;

/**
 * Paramètres de connexion à la base de données.
 * Centralisés pour faciliter la maintenance et la sécurité.
 */
public class DatabaseConfig {
    // URL JDBC (MySQL)
	public static final String URL = "jdbc:mysql://localhost:3306/chrionline?useSSL=false&serverTimezone=UTC&useLegacyDatetimeCode=false";
    // Identifiants (à externaliser en production)
    public static final String USER = "root";
    public static final String PASSWORD = ""; // ← mets ton mot de passe si nécessaire
}