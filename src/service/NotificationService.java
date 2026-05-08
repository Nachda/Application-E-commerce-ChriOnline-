package service;

import dao.NotificationDAO;
import model.Notification;
import model.Product;

import java.util.List;

/**
 * Service de gestion des notifications.
 */
public class NotificationService {

    private final NotificationDAO notificationDAO;

    public NotificationService() {
        this.notificationDAO = new NotificationDAO();
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
     * Vérifie tous les produits à faible stock et crée des notifications si
     * nécessaire. Appelée régulièrement par l'admin.
     */
    public void syncLowStockNotifications() {
        // On pourrait parcourir tous les produits et appeler syncProductStockNotification.
        // Pour l'instant, on se contente de renvoyer la liste existante.
        // L'implémentation complète peut être ajoutée plus tard.
    }

    public List<Notification> getUnreadNotifications() {
        return notificationDAO.findUnread();
    }

    public boolean markAsRead(int notificationId) {
        return notificationDAO.markAsRead(notificationId);
    }
}