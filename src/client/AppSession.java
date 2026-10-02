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
    
    public AppSession() {
        this.clientId = 0;
        this.role = "";
        this.fullName = "";
        this.orderUUID = null;
        this.lastOrderTotal = 0.0;
    }

    // Identité
    public int getClientId() { return clientId; }
    public void setClientId(int clientId) { this.clientId = clientId; }
    
    public int getUserId() { return clientId; }   // alias pour compatibilité admin

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role != null ? role : ""; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName != null ? fullName : ""; }

    public boolean isAdmin() { return "admin".equalsIgnoreCase(role); }
    public boolean isClient() { return "client".equalsIgnoreCase(role); }
    public boolean isLoggedIn() { return clientId > 0 && role != null && !role.isBlank(); }

    // Session token
    public String getSessionToken() { return sessionToken; }
    public void setSessionToken(String sessionToken) { this.sessionToken = sessionToken; }

    // Commande en cours
    public String getOrderUUID() { return orderUUID; }
    public void setOrderUUID(String orderUUID) { this.orderUUID = orderUUID; }

    public double getLastOrderTotal() { return lastOrderTotal; }
    public void setLastOrderTotal(double lastOrderTotal) { this.lastOrderTotal = lastOrderTotal; }

    public void clearOrderData() {
        this.orderUUID = null;
        this.lastOrderTotal = 0.0;
    }

    public void clearSession() {
        this.clientId = 0;
        this.role = "";
        this.fullName = "";
        clearOrderData();
    }
}