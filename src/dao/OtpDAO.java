package dao;

import database.DatabaseConnection;
import java.sql.*;

public class OtpDAO {

    private final Connection conn;

    public OtpDAO() {
        this.conn = DatabaseConnection.getConnection();
    }

    /**
     * Enregistre un code OTP avec expiration UTC +10 minutes, entièrement calculé par MySQL.
     */
    public boolean saveOtp(String email, String code) {
        String sql = "INSERT INTO otp_codes (email, code, expires_at) VALUES (?, ?, UTC_TIMESTAMP() + INTERVAL 10 MINUTE)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            ps.setString(2, code);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Erreur saveOtp : " + e.getMessage());
            return false;
        }
    }

    /**
     * Vérifie un code OTP (comparaison UTC). Supprime le code s'il est valide.
     */
    public boolean verifyOtp(String email, String code) {
        String selectSql = "SELECT * FROM otp_codes WHERE email = ? AND code = ? AND expires_at > UTC_TIMESTAMP()";
        String deleteSql = "DELETE FROM otp_codes WHERE email = ? AND code = ?";
        try {
            conn.setAutoCommit(false);
            try (PreparedStatement ps = conn.prepareStatement(selectSql)) {
                ps.setString(1, email);
                ps.setString(2, code);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    try (PreparedStatement psDel = conn.prepareStatement(deleteSql)) {
                        psDel.setString(1, email);
                        psDel.setString(2, code);
                        psDel.executeUpdate();
                    }
                    conn.commit();
                    return true;
                }
            }
            conn.rollback();
            return false;
        } catch (SQLException e) {
            try { conn.rollback(); } catch (SQLException ignored) {}
            System.err.println("Erreur verifyOtp : " + e.getMessage());
            return false;
        } finally {
            try { conn.setAutoCommit(true); } catch (SQLException ignored) {}
        }
    }

    public boolean userExists(String email) {
        String sql = "SELECT COUNT(*) FROM users WHERE email = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            ResultSet rs = ps.executeQuery();
            return rs.next() && rs.getInt(1) > 0;
        } catch (SQLException e) {
            return false;
        }
    }

    public void activateAccount(String email) {
        String sql = "UPDATE users SET status = 'active' WHERE email = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Erreur activation compte : " + e.getMessage());
        }
    }
}