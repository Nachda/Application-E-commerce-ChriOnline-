package service;

import dao.NotificationDAO;
import dao.ProductDAO;
import model.Notification;
import model.Product;

import java.util.List;

/**
 * Service de gestion des notifications.
 */
public class NotificationService {

    private final NotificationDAO notificationDAO;
    private final ProductDAO productDAO;

    public NotificationService() {
        this.notificationDAO = new NotificationDAO();
        this.productDAO = new ProductDAO();
    }

    /**
     * Synchronise les notifications de stock faible : crée une notification
     * si le stock d'un produit passe sous le seuil donné.
     */
    public void syncProductStockNotification(Product product, int threshold) {
        if (product == null) return;
        boolean alreadyExists = notificationDAO.existsStockAlertForProduct(product.getIdProduct());
        if (product.getStock() <= threshold && !alreadyExists) {
            String title = "Stock faible : " + product.getName();
            String message = "Le produit " + product.getName() +
                    " a un stock de " + product.getStock() +
                    " (seuil : " + threshold + ").";
            Notification n = new Notification(0, title, message, "stock_alert",
                    product.getStock() == 0 ? "critical" : "warning", false,
                    "product", product.getIdProduct(), java.time.LocalDateTime.now());
            notificationDAO.save(n);
        }
    }

    /**
     * Vérifie tous les produits à faible stock et crée des notifications si nécessaire.
     */
    public void syncLowStockNotifications() {
        List<Product> products = productDAO.findAll();
        for (Product p : products) {
            syncProductStockNotification(p, 5);
        }
    }

    public List<Notification> getUnreadNotifications() {
        return notificationDAO.findUnread();
    }

    public boolean markAsRead(int notificationId) {
        return notificationDAO.markAsRead(notificationId);
    }
}