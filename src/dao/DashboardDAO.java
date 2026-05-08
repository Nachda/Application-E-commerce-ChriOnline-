package dao;

import database.DatabaseConnection;
import model.DashboardSummary;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Requêtes agrégées pour le tableau de bord.
 */
public class DashboardDAO {

    private final Connection conn;

    public DashboardDAO() {
        this.conn = DatabaseConnection.getConnection();
    }

    public DashboardSummary fetchSummary() {
        DashboardSummary summary = new DashboardSummary();
        String sql = """
            SELECT
                (SELECT COUNT(*) FROM products) AS total_products,
                (SELECT COUNT(*) FROM products WHERE stock > 0 AND stock <= 5) AS low_stock,
                (SELECT COUNT(*) FROM products WHERE stock = 0) AS out_of_stock,
                (SELECT COUNT(*) FROM users WHERE role = 'client') AS total_users,
                (SELECT COUNT(*) FROM orders) AS total_orders,
                (SELECT COUNT(*) FROM orders WHERE status = 'pending') AS pending_orders,
                (SELECT COUNT(*) FROM orders WHERE status = 'paid' OR status = 'validated') AS paid_orders,
                (SELECT COALESCE(SUM(total_price), 0) FROM orders WHERE DATE(created_at) = CURDATE()) AS today_revenue,
                (SELECT COALESCE(SUM(total_price), 0) FROM orders WHERE YEAR(created_at) = YEAR(CURDATE()) AND MONTH(created_at) = MONTH(CURDATE())) AS month_revenue,
                (SELECT COUNT(*) FROM notifications WHERE is_read = 0) AS unread
            FROM dual
            """;
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                summary.setTotalProducts(rs.getInt("total_products"));
                summary.setLowStockProducts(rs.getInt("low_stock"));
                summary.setOutOfStockProducts(rs.getInt("out_of_stock"));
                summary.setTotalUsers(rs.getInt("total_users"));
                summary.setTotalOrders(rs.getInt("total_orders"));
                summary.setPendingOrders(rs.getInt("pending_orders"));
                summary.setPaidOrders(rs.getInt("paid_orders"));
                summary.setTodayRevenue(rs.getDouble("today_revenue"));
                summary.setMonthRevenue(rs.getDouble("month_revenue"));
                summary.setUnreadNotifications(rs.getInt("unread"));
            }
        } catch (SQLException e) {
            System.err.println("Erreur DashboardDAO : " + e.getMessage());
        }
        return summary;
    }
}