package client;

/**
 * Mémorise les informations de session du client connecté.
 */
public class AppSession {

    private int clientId;
    private String role;
    private String orderUUID;
    private double lastOrderTotal;
    private String fullName;
    private String sessionToken; 
    
 // getters & setters ...
    public String getSessionToken() { return sessionToken; }
    public void setSessionToken(String sessionToken) { this.sessionToken = sessionToken; }

    public int getClientId() { return clientId; }
    public void setClientId(int clientId) { this.clientId = clientId; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getOrderUUID() { return orderUUID; }
    public void setOrderUUID(String orderUUID) { this.orderUUID = orderUUID; }

    public double getLastOrderTotal() { return lastOrderTotal; }
    public void setLastOrderTotal(double lastOrderTotal) { this.lastOrderTotal = lastOrderTotal; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public void clearOrderData() {
        this.orderUUID = null;
        this.lastOrderTotal = 0.0;
    }

    public boolean isAdmin() {
        return "admin".equalsIgnoreCase(role);
    }
}