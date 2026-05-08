package model;

import java.util.ArrayList;
import java.util.List;

/**
 * Représente un panier d'achat.
 * Mappe la table 'carts' et contient une liste de CartItem.
 */
public class Cart {
    private int id;
    private int clientId;
    private List<CartItem> items;
    private String createdAt;

    public Cart() {
        this.items = new ArrayList<>();
    }

    public Cart(int id, int clientId, String createdAt) {
        this.id = id;
        this.clientId = clientId;
        this.createdAt = createdAt;
        this.items = new ArrayList<>();
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getClientId() { return clientId; }
    public void setClientId(int clientId) { this.clientId = clientId; }

    public List<CartItem> getItems() { return items; }
    public void setItems(List<CartItem> items) { this.items = items; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    /**
     * Ajoute un produit au panier ou augmente la quantité s'il existe déjà.
     */
    public void addProduct(Product product, int quantity) {
        if (product == null) throw new IllegalArgumentException("Produit null");
        for (CartItem item : items) {
            if (item.getProduct() != null && item.getProduct().getIdProduct() == product.getIdProduct()) {
                item.setQuantity(item.getQuantity() + quantity);
                return;
            }
        }
        items.add(new CartItem(0, product, quantity));
    }

    /**
     * Supprime un produit du panier par son id.
     * @return true si supprimé
     */
    public boolean removeProduct(int productId) {
        return items.removeIf(item -> item.getProduct() != null && item.getProduct().getIdProduct() == productId);
    }

    public double calculateTotal() {
        double total = 0.0;
        for (CartItem item : items) {
            total += item.calculateSubtotal();
        }
        return total;
    }

    public void clearCart() {
        items.clear();
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }
}