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
    private String orderUUID;
    private double totalPrice;
    private String status;          // pending, validated, shipped, delivered, cancelled
    private LocalDateTime createdAt;
    private List<OrderItem> items;
    private Payment payment;

    public Order() {
        this.orderUUID = UUID.randomUUID().toString();
        this.status = "pending";
        this.createdAt = LocalDateTime.now();
        this.totalPrice = 0.0;
        this.items = new ArrayList<>();
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

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
}