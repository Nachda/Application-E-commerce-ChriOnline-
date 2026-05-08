package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import config.ServerConfig;
import database.DatabaseConnection;
import model.Admin;
import model.Client;
import model.User;
import security.DataEncryptor;

public class UserDAO {

    private final Connection connection;
    private final DataEncryptor encryptor;

    public UserDAO() {
        this.connection = DatabaseConnection.getConnection();
        try {
            this.encryptor = new DataEncryptor(
                ServerConfig.getKeystorePath(),
                ServerConfig.getKeystorePassword()
            );
        } catch (Exception e) {
            throw new RuntimeException("Impossible d'initialiser le chiffrement des données", e);
        }
    }

    // ──────────────────────────────────────────────
    // Recherche par email
    // ──────────────────────────────────────────────
    public User findByEmail(String email) {
        String sql = "SELECT u.*, c.address, c.phone, c.ville " +
                     "FROM users u LEFT JOIN clients c ON u.id = c.id " +
                     "WHERE u.email = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, email);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return mapUser(rs);
            }
        } catch (SQLException e) {
            System.err.println("Erreur findByEmail : " + e.getMessage());
        }
        return null;
    }

    // ──────────────────────────────────────────────
    // Enregistrement
    // ──────────────────────────────────────────────
    public boolean save(Client client) {
        return saveClientWithStatus(client, "active");
    }

    public boolean savePendingClient(Client client) {
        return saveClientWithStatus(client, "pending");
    }

    private boolean saveClientWithStatus(Client client, String status) {
        String sqlUser = "INSERT INTO users (nom, prenom, email, password, role, status) VALUES (?, ?, ?, ?, 'client', ?)";
        String sqlClient = "INSERT INTO clients (id, address, phone, ville) VALUES (?, ?, ?, ?)";

        try {
            connection.setAutoCommit(false);

            try (PreparedStatement psUser = connection.prepareStatement(sqlUser, Statement.RETURN_GENERATED_KEYS)) {
                // Chiffrer les champs sensibles avant insertion
                psUser.setString(1, encryptor.encrypt(client.getNom()));
                psUser.setString(2, encryptor.encrypt(client.getPrenom()));
                psUser.setString(3, client.getEmail());
                psUser.setString(4, client.getPassword());
                psUser.setString(5, status);
                psUser.executeUpdate();

                ResultSet keys = psUser.getGeneratedKeys();
                if (!keys.next()) {
                    connection.rollback();
                    return false;
                }
                int userId = keys.getInt(1);
                client.setId(userId);

                try (PreparedStatement psClient = connection.prepareStatement(sqlClient)) {
                    psClient.setInt(1, userId);
                    psClient.setString(2, encryptor.encrypt(client.getAddress()));
                    psClient.setString(3, encryptor.encrypt(client.getPhone()));
                    psClient.setString(4, encryptor.encrypt(client.getVille()));
                    psClient.executeUpdate();
                }
            }

            connection.commit();
            return true;

        } catch (Exception e) {
            try { connection.rollback(); } catch (SQLException ignored) {}
            System.err.println("Erreur save client : " + e.getMessage());
            return false;
        } finally {
            try { connection.setAutoCommit(true); } catch (SQLException ignored) {}
        }
    }

    // ──────────────────────────────────────────────
    // Vérifications
    // ──────────────────────────────────────────────
    public boolean emailExists(String email) {
        String sql = "SELECT COUNT(*) FROM users WHERE email = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, email);
            ResultSet rs = ps.executeQuery();
            return rs.next() && rs.getInt(1) > 0;
        } catch (SQLException e) {
            System.err.println("Erreur emailExists : " + e.getMessage());
            return false;
        }
    }

    public boolean isAccountActive(String email) {
        String sql = "SELECT status FROM users WHERE email = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, email);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return "active".equalsIgnoreCase(rs.getString("status"));
            }
        } catch (SQLException e) {
            System.err.println("Erreur isAccountActive : " + e.getMessage());
        }
        return false;
    }

    // ──────────────────────────────────────────────
    // Recherche par ID
    // ──────────────────────────────────────────────
    public User findById(int userId) {
        String sql = "SELECT u.*, c.address, c.phone, c.ville " +
                     "FROM users u LEFT JOIN clients c ON u.id = c.id " +
                     "WHERE u.id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return mapUser(rs);
            }
        } catch (SQLException e) {
            System.err.println("Erreur findById : " + e.getMessage());
        }
        return null;
    }

    // ──────────────────────────────────────────────
    // Liste de tous les utilisateurs
    // ──────────────────────────────────────────────
    public List<User> findAll() {
        List<User> users = new ArrayList<>();
        String sql = "SELECT u.*, c.address, c.phone, c.ville " +
                     "FROM users u LEFT JOIN clients c ON u.id = c.id " +
                     "ORDER BY u.id DESC";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                users.add(mapUser(rs));
            }
        } catch (SQLException e) {
            System.err.println("Erreur findAll : " + e.getMessage());
        }
        return users;
    }

    // ──────────────────────────────────────────────
    // Mise à jour du profil
    // ──────────────────────────────────────────────
    public boolean updateProfile(int userId, String fullName, String email,
                                 String phone, String address, String ville) {
        String prenom = "";
        String nom = fullName;
        if (fullName != null && fullName.trim().contains(" ")) {
            int idx = fullName.trim().indexOf(" ");
            prenom = fullName.trim().substring(0, idx).trim();
            nom = fullName.trim().substring(idx + 1).trim();
        }

        try {
            connection.setAutoCommit(false);

            String sqlUser = "UPDATE users SET nom = ?, prenom = ?, email = ? WHERE id = ?";
            try (PreparedStatement ps = connection.prepareStatement(sqlUser)) {
                ps.setString(1, encryptor.encrypt(nom));
                ps.setString(2, encryptor.encrypt(prenom));
                ps.setString(3, email);
                ps.setInt(4, userId);
                ps.executeUpdate();
            }

            String roleSql = "SELECT role FROM users WHERE id = ?";
            String role = null;
            try (PreparedStatement ps = connection.prepareStatement(roleSql)) {
                ps.setInt(1, userId);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) role = rs.getString("role");
            }

            if ("client".equalsIgnoreCase(role)) {
                String checkSql = "SELECT id FROM clients WHERE id = ?";
                boolean exists = false;
                try (PreparedStatement ps = connection.prepareStatement(checkSql)) {
                    ps.setInt(1, userId);
                    ResultSet rs = ps.executeQuery();
                    exists = rs.next();
                }

                if (exists) {
                    String updateSql = "UPDATE clients SET address = ?, phone = ?, ville = ? WHERE id = ?";
                    try (PreparedStatement ps = connection.prepareStatement(updateSql)) {
                        ps.setString(1, encryptor.encrypt(address));
                        ps.setString(2, encryptor.encrypt(phone));
                        ps.setString(3, encryptor.encrypt(ville));
                        ps.setInt(4, userId);
                        ps.executeUpdate();
                    }
                } else {
                    String insertSql = "INSERT INTO clients (id, address, phone, ville) VALUES (?, ?, ?, ?)";
                    try (PreparedStatement ps = connection.prepareStatement(insertSql)) {
                        ps.setInt(1, userId);
                        ps.setString(2, encryptor.encrypt(address));
                        ps.setString(3, encryptor.encrypt(phone));
                        ps.setString(4, encryptor.encrypt(ville));
                        ps.executeUpdate();
                    }
                }
            }

            connection.commit();
            return true;
        } catch (Exception e) {
            try { connection.rollback(); } catch (SQLException ignored) {}
            System.err.println("Erreur updateProfile : " + e.getMessage());
            return false;
        } finally {
            try { connection.setAutoCommit(true); } catch (SQLException ignored) {}
        }
    }

    // ──────────────────────────────────────────────
    // Méthode privée de mappage
    // ──────────────────────────────────────────────
    private User mapUser(ResultSet rs) throws SQLException {
        String role = rs.getString("role");
        String status = rs.getString("status");
        if ("client".equalsIgnoreCase(role)) {
            Client client = new Client(
                    encryptor.decrypt(rs.getString("nom")),
                    encryptor.decrypt(rs.getString("prenom")),
                    rs.getString("email"),
                    rs.getString("password"),
                    encryptor.decrypt(rs.getString("address")),
                    encryptor.decrypt(rs.getString("phone")),
                    encryptor.decrypt(rs.getString("ville"))
            );
            client.setId(rs.getInt("id"));
            client.setRole(role);
            client.setStatus(status);
            return client;
        } else {
            Admin admin = new Admin(
                    encryptor.decrypt(rs.getString("nom")),
                    encryptor.decrypt(rs.getString("prenom")),
                    rs.getString("email"),
                    rs.getString("password")
            );
            admin.setId(rs.getInt("id"));
            admin.setRole(role);
            admin.setStatus(status);
            return admin;
        }
    }
}