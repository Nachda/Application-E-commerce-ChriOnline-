package service;

import dao.OrderDAO;
import model.CartItem;
import model.Order;
import model.OrderItem;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

public class OrderService {

    private final OrderDAO orderDAO;

    public OrderService() throws SQLException {
        this.orderDAO = new OrderDAO();
    }

    /**
     * Crée une commande à partir du contenu du panier.
     * @param clientId l'ID du client
     * @param cartItems les articles du panier
     * @return la commande créée
     */
    public Order createOrder(int clientId, List<CartItem> cartItems) throws SQLException {
        Order order = new Order();
        order.setOrderUUID(UUID.randomUUID().toString());
        order.setStatus("pending");

        for (CartItem c : cartItems) {
            OrderItem oi = new OrderItem();
            oi.setProduct(c.getProduct());
            oi.setQuantity(c.getQuantity());
            oi.setUnitPrice(c.getProduct().getPrice());
            order.ajouterItem(oi);
        }

        orderDAO.save(order, clientId);
        return order;
    }

    public Order getOrderByUUID(String uuid) throws SQLException {
        return orderDAO.findByUUID(uuid);
    }

    public void updateStatus(int orderId, String newStatus) throws SQLException {
        orderDAO.updateStatus(orderId, newStatus);
    }

    public List<Order> getOrdersByClient(int clientId) throws SQLException {
        return orderDAO.findByClient(clientId);
    }

    public List<Order> getAllOrders() throws SQLException {
        return orderDAO.findAll();
    }
}