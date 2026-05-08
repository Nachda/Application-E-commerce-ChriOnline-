package dao;

import database.DatabaseConnection;
import model.Order;
import model.OrderItem;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class OrderDAO {

    private final Connection conn;

    public OrderDAO() {
        this.conn = DatabaseConnection.getConnection();
    }

    /**
     * Sauvegarde une commande et ses items (transaction).
     */
    public void save(Order order, int clientId) throws SQLException {
        String sql = "INSERT INTO orders (order_uuid, client_id, total_price, status, created_at) VALUES (?, ?, ?, ?, ?)";
        try {
            conn.setAutoCommit(false);
            try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, order.getOrderUUID());
                ps.setInt(2, clientId);
                ps.setDouble(3, order.getTotalPrice());
                ps.setString(4, order.getStatus());
                ps.setTimestamp(5, Timestamp.valueOf(order.getCreatedAt()));
                ps.executeUpdate();
                ResultSet rs = ps.getGeneratedKeys();
                if (rs.next()) order.setId(rs.getInt(1));
            }

            OrderItemDAO itemDAO = new OrderItemDAO();
            for (OrderItem item : order.getItems()) {
                itemDAO.save(item, order.getId());
            }
            conn.commit();
        } catch (SQLException e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(true);
        }
    }

    public List<Order> findByClient(int clientId) throws SQLException {
        List<Order> orders = new ArrayList<>();
        String sql = "SELECT * FROM orders WHERE client_id = ? ORDER BY id DESC";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, clientId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                orders.add(mapOrder(rs));
            }
        }
        return orders;
    }

    public Order findByUUID(String uuid) throws SQLException {
        String sql = "SELECT * FROM orders WHERE order_uuid = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, uuid);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapOrder(rs);
        }
        return null;
    }

    public List<Order> findAll() throws SQLException {
        List<Order> orders = new ArrayList<>();
        String sql = "SELECT * FROM orders ORDER BY id DESC";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) orders.add(mapOrder(rs));
        }
        return orders;
    }

    public void updateStatus(int orderId, String newStatus) throws SQLException {
        String sql = "UPDATE orders SET status = ? WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newStatus);
            ps.setInt(2, orderId);
            ps.executeUpdate();
        }
    }

    private Order mapOrder(ResultSet rs) throws SQLException {
        Order order = new Order();
        order.setId(rs.getInt("id"));
        order.setOrderUUID(rs.getString("order_uuid"));
        order.setTotalPrice(rs.getDouble("total_price"));
        order.setStatus(rs.getString("status"));
        order.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        // Charger les items
        OrderItemDAO itemDAO = new OrderItemDAO();
        List<OrderItem> items = itemDAO.findByOrder(order.getId());
        order.setItems(items);
        return order;
    }
}