package service;

import dao.ProductDAO;
import model.Product;
import dao.StockMovementDAO;
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
        var productOpt = productDAO.findById(productId);
        if (productOpt == null) return false;

        int previousStock = productOpt.getStock();
        int newStock;
        switch (movementType) {
            case "add"    -> newStock = previousStock + quantity;
            case "remove" -> newStock = Math.max(0, previousStock - quantity);
            case "adjust" -> newStock = quantity; // quantité = nouveau stock absolu
            default -> throw new IllegalArgumentException("Type de mouvement invalide : " + movementType);
        }

        boolean updated = productDAO.updateStock(productId, newStock);
        if (!updated) return false;

        StockMovement movement = new StockMovement(0, productId, productOpt.getName(),
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