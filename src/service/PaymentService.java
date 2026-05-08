package service;

import dao.PaymentDAO;
import model.Payment;

public class PaymentService {

    private final PaymentDAO paymentDAO;

    public PaymentService() {
        this.paymentDAO = new PaymentDAO();
    }

    /**
     * Traite un paiement (simulation) et l'enregistre.
     * @param payment l'objet Payment avec orderId, method, amount
     * @return true si succès
     */
    public boolean processPayment(Payment payment) {
        if (payment == null) return false;
        if (!payment.processPayment()) return false; // simulation (set status success)
        return paymentDAO.save(payment);
    }
}