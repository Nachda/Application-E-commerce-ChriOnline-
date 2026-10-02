package model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Représente une commande.
 * Mappe la table 'orders'.
 */
public class Order {
    private int id;
    private int clientId;
    private String orderUUID;
    private double totalPrice;
    private String status;          // pending, validated, shipped, delivered, cancelled
    private LocalDateTime createdAt;
    private List<OrderItem> items;
    private Payment payment;

    // ⭐ NOUVEAU : Infos client pour l'affichage admin
    private String clientNom;
    private String clientPrenom;
    private String clientEmail;

    public Order() {
        this.orderUUID = UUID.randomUUID().toString();
        this.status = "pending";
        this.createdAt = LocalDateTime.now();
        this.totalPrice = 0.0;
        this.items = new ArrayList<>();
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getClientId() { return clientId; }
    public void setClientId(int clientId) { this.clientId = clientId; }

    public String getOrderUUID() { return orderUUID; }
    public void setOrderUUID(String orderUUID) { this.orderUUID = orderUUID; }

    public double getTotalPrice() { return totalPrice; }
    public void setTotalPrice(double totalPrice) { this.totalPrice = totalPrice; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public List<OrderItem> getItems() { return items; }
    public void setItems(List<OrderItem> items) {
        this.items = (items != null) ? items : new ArrayList<>();
        calculTotal();
    }

    public Payment getPayment() { return payment; }
    public void setPayment(Payment payment) { this.payment = payment; }

    public void ajouterItem(OrderItem item) {
        if (item != null) {
            items.add(item);
            calculTotal();
        }
    }

    public double calculTotal() {
        totalPrice = 0.0;
        for (OrderItem item : items) {
            totalPrice += item.calculSubtotal();
        }
        return totalPrice;
    }

    public void validerCommande() {
        this.status = "validated";
    }

    // ⭐ NOUVEAU : Infos client pour l'interface admin
    public String getClientNom() { return clientNom; }
    public void setClientNom(String clientNom) { this.clientNom = clientNom; }

    public String getClientPrenom() { return clientPrenom; }
    public void setClientPrenom(String clientPrenom) { this.clientPrenom = clientPrenom; }

    public String getClientEmail() { return clientEmail; }
    public void setClientEmail(String clientEmail) { this.clientEmail = clientEmail; }

    public String getClientFullName() {
        String nom = (clientNom != null) ? clientNom : "";
        String prenom = (clientPrenom != null) ? clientPrenom : "";
        String full = (prenom + " " + nom).trim();
        return full.isEmpty() ? "Client #" + clientId : full;
    }
}