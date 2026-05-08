package database;

import config.DatabaseConfig;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Gestionnaire de connexion JDBC (singleton).
 * Utilise les paramètres définis dans DatabaseConfig.
 */
public class DatabaseConnection {

    private static Connection connection;

    // Constructeur privé pour empêcher l'instanciation
    private DatabaseConnection() {}

    /**
     * Retourne une connexion active, ouverte si nécessaire.
     * @return Connection JDBC ou null en cas d'échec
     */
    public static Connection getConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                // Chargement explicite du driver (optionnel mais recommandé)
                Class.forName("com.mysql.cj.jdbc.Driver");
                connection = DriverManager.getConnection(
                        DatabaseConfig.URL,
                        DatabaseConfig.USER,
                        DatabaseConfig.PASSWORD
                );
                System.out.println("✅ Connexion à MySQL réussie !");
            }
        } catch (ClassNotFoundException e) {
            System.err.println("❌ Driver MySQL introuvable : " + e.getMessage());
        } catch (SQLException e) {
            System.err.println("❌ Erreur de connexion : " + e.getMessage());
        }
        return connection;
    }

    /**
     * Ferme la connexion proprement.
     */
    public static void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                System.out.println("🔒 Connexion fermée.");
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur lors de la fermeture : " + e.getMessage());
        }
    }
}