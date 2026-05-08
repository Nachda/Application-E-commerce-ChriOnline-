package model;

import java.time.LocalDateTime;

/**
 * Représente un paiement.
 * Mappe la table 'payments'.
 */
public class Payment {
    private int id;
    private int orderId;
    private String method;   // "card" ou "especes"
    private double amount;
    private String status;   // pending, success, failed
    private LocalDateTime paidAt;

    public Payment() {}

    public Payment(int id, int orderId, String method, double amount, String status, LocalDateTime paidAt) {
        this.id = id;
        this.orderId = orderId;
        setMethod(method);
        setAmount(amount);
        setStatus(status);
        this.paidAt = paidAt;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getOrderId() { return orderId; }
    public void setOrderId(int orderId) { this.orderId = orderId; }

    public String getMethod() { return method; }
    public void setMethod(String method) {
        if (method == null || (!method.equalsIgnoreCase("card") && !method.equalsIgnoreCase("especes"))) {
            throw new IllegalArgumentException("Méthode invalide : " + method);
        }
        this.method = method.toLowerCase();
    }

    public double getAmount() { return amount; }
    public void setAmount(double amount) {
        if (amount < 0) throw new IllegalArgumentException("Montant négatif");
        this.amount = amount;
    }

    public String getStatus() { return status; }
    public void setStatus(String status) {
        if (status == null || (!status.equalsIgnoreCase("pending") &&
                !status.equalsIgnoreCase("success") && !status.equalsIgnoreCase("failed"))) {
            throw new IllegalArgumentException("Statut invalide : " + status);
        }
        this.status = status.toLowerCase();
    }

    public LocalDateTime getPaidAt() { return paidAt; }
    public void setPaidAt(LocalDateTime paidAt) { this.paidAt = paidAt; }

    /**
     * Simule le traitement du paiement (succès automatique si montant > 0).
     * @return true si le paiement a réussi
     */
    public boolean processPayment() {
        if (amount <= 0) {
            this.status = "failed";
            return false;
        }
        this.status = "success";
        this.paidAt = LocalDateTime.now();
        return true;
    }
}