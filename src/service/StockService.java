package service;

import dao.ProductDAO;
import dao.StockMovementDAO;
import model.Product;
import model.StockAlert;
import model.StockMovement;

import java.util.List;

/**
 * Service de gestion des stocks et des mouvements.
 */
public class StockService {

    private final StockMovementDAO stockMovementDAO;
    private final ProductDAO productDAO;

    public StockService() {
        this.stockMovementDAO = new StockMovementDAO();
        this.productDAO = new ProductDAO();
    }

    /**
     * Ajuste le stock d'un produit et enregistre le mouvement.
     * @return true si succès, false sinon
     */
    public boolean adjustStock(int productId, int quantity, String movementType,
                               String reason, Integer adminUserId) {
        Product product = productDAO.findById(productId);
        if (product == null) return false;

        int previousStock = product.getStock();
        int newStock;

        // Accepter les deux formats (français et anglais)
        switch (movementType.toLowerCase()) {
            case "add":
            case "entree":
                newStock = previousStock + quantity;
                break;
            case "remove":
            case "sortie":
                newStock = Math.max(0, previousStock - quantity);
                break;
            case "adjust":
            case "ajustement":
                newStock = quantity;
                break;
            default:
                throw new IllegalArgumentException("Type de mouvement invalide : " + movementType);
        }

        boolean updated = productDAO.updateStock(productId, newStock);
        if (!updated) return false;

        StockMovement movement = new StockMovement(0, productId, product.getName(),
                movementType, quantity, previousStock, newStock,
                reason, adminUserId, java.time.LocalDateTime.now());
        stockMovementDAO.save(movement);
        return true;
    }

    public List<StockAlert> getLowStockAlerts() {
        return stockMovementDAO.findLowStockAlerts();
    }

    public List<StockMovement> getStockHistory() {
        return stockMovementDAO.findAllMovements();
    }
}