package dao;

import database.DatabaseConnection;
import model.StockAlert;
import model.StockMovement;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class StockMovementDAO {

    private final Connection conn;

    public StockMovementDAO() {
        this.conn = DatabaseConnection.getConnection();
    }

    public void save(StockMovement m) {
        String sql = "INSERT INTO stock_movements (product_id, movement_type, quantity, previous_stock, new_stock, reason, admin_user_id, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, m.getProductId());
            ps.setString(2, m.getMovementType());
            ps.setInt(3, m.getQuantity());
            ps.setInt(4, m.getPreviousStock());
            ps.setInt(5, m.getNewStock());
            ps.setString(6, m.getReason());
            if (m.getAdminUserId() != null) ps.setInt(7, m.getAdminUserId());
            else ps.setNull(7, Types.INTEGER);
            ps.setTimestamp(8, Timestamp.valueOf(m.getCreatedAt() == null ? LocalDateTime.now() : m.getCreatedAt()));
            ps.executeUpdate();
            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) m.setId(rs.getInt(1));
        } catch (SQLException e) {
            System.err.println("Erreur StockMovementDAO.save : " + e.getMessage());
        }
    }

    public List<StockAlert> findLowStockAlerts() {
        List<StockAlert> alerts = new ArrayList<>();
        String sql = """
            SELECT p.id_product, p.name, p.stock, 5 AS threshold,
                   CASE WHEN p.stock = 0 THEN 'out' ELSE 'low' END AS level,
                   'active' AS status,
                   p.created_at
            FROM products p
            WHERE p.stock <= 5
            ORDER BY p.stock ASC
            """;
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                alerts.add(new StockAlert(
                        rs.getInt("id_product"),
                        rs.getString("name"),
                        rs.getInt("stock"),
                        rs.getInt("threshold"),
                        rs.getString("level"),
                        rs.getString("status"),
                        rs.getTimestamp("created_at").toLocalDateTime()
                ));
            }
        } catch (SQLException e) {
            System.err.println("Erreur StockMovementDAO.findLowStockAlerts : " + e.getMessage());
        }
        return alerts;
    }

    public List<StockMovement> findAllMovements() {
        List<StockMovement> movements = new ArrayList<>();
        String sql = """
            SELECT sm.*, p.name AS product_name
            FROM stock_movements sm
            LEFT JOIN products p ON sm.product_id = p.id_product
            ORDER BY sm.created_at DESC
            """;
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                movements.add(new StockMovement(
                        rs.getInt("id"),
                        rs.getInt("product_id"),
                        rs.getString("product_name"),
                        rs.getString("movement_type"),
                        rs.getInt("quantity"),
                        rs.getInt("previous_stock"),
                        rs.getInt("new_stock"),
                        rs.getString("reason"),
                        rs.getObject("admin_user_id") != null ? rs.getInt("admin_user_id") : null,
                        rs.getTimestamp("created_at").toLocalDateTime()
                ));
            }
        } catch (SQLException e) {
            System.err.println("Erreur StockMovementDAO.findAllMovements : " + e.getMessage());
        }
        return movements;
    }
}