package dao;

import database.DatabaseConnection;
import model.Notification;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class NotificationDAO {

    private final Connection conn;

    public NotificationDAO() {
        this.conn = DatabaseConnection.getConnection();
    }

    public void save(Notification n) {
        String sql = "INSERT INTO notifications (title, message, type, level, is_read, entity_type, entity_id, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, n.getTitle());
            ps.setString(2, n.getMessage());
            ps.setString(3, n.getType());
            ps.setString(4, n.getLevel());
            ps.setBoolean(5, n.isRead());
            ps.setString(6, n.getEntityType());
            if (n.getEntityId() != null) ps.setInt(7, n.getEntityId());
            else ps.setNull(7, Types.INTEGER);
            ps.setTimestamp(8, Timestamp.valueOf(n.getCreatedAt() == null ? LocalDateTime.now() : n.getCreatedAt()));
            ps.executeUpdate();
            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) n.setId(rs.getInt(1));
        } catch (SQLException e) {
            System.err.println("Erreur NotificationDAO.save : " + e.getMessage());
        }
    }

    public List<Notification> findUnread() {
        List<Notification> list = new ArrayList<>();
        String sql = "SELECT * FROM notifications WHERE is_read = 0 ORDER BY created_at DESC";
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapNotification(rs));
            }
        } catch (SQLException e) {
            System.err.println("Erreur NotificationDAO.findUnread : " + e.getMessage());
        }
        return list;
    }

    public boolean markAsRead(int id) {
        String sql = "UPDATE notifications SET is_read = 1 WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Erreur NotificationDAO.markAsRead : " + e.getMessage());
            return false;
        }
    }

    public boolean existsStockAlertForProduct(int productId) {
        String sql = "SELECT COUNT(*) FROM notifications WHERE entity_type = 'product' AND entity_id = ? AND is_read = 0";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, productId);
            ResultSet rs = ps.executeQuery();
            return rs.next() && rs.getInt(1) > 0;
        } catch (SQLException e) {
            return false;
        }
    }

    private Notification mapNotification(ResultSet rs) throws SQLException {
        return new Notification(
                rs.getInt("id"),
                rs.getString("title"),
                rs.getString("message"),
                rs.getString("type"),
                rs.getString("level"),
                rs.getBoolean("is_read"),
                rs.getString("entity_type"),
                rs.getObject("entity_id") != null ? rs.getInt("entity_id") : null,
                rs.getTimestamp("created_at").toLocalDateTime()
        );
    }
}