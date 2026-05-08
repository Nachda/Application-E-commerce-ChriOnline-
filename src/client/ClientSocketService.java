package client;

import security.SecureChannel;
import javax.swing.*;
import java.net.Socket;
import java.util.Arrays;
import java.util.UUID;

/**
 * Service de communication client/serveur avec SecureChannel.
 * Ajoute un nonce et un token de session à chaque requête.
 */
public class ClientSocketService {

    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 5001;
    private static final String TRUSTSTORE_PATH = "truststore.p12";

    private Socket socket;
    private SecureChannel secureChannel;
    private boolean connected = false;
    private String sessionToken;   // <-- stocke le token après authentification

    /**
     * Définit le token de session (appelé après login ou challenge admin).
     */
    public void setSessionToken(String token) {
        this.sessionToken = token;
    }

    public String getSessionToken() {
        return sessionToken;
    }

    public boolean connect() {
        try {
            if (connected && socket != null && !socket.isClosed()) {
                return true;
            }

            JPasswordField pf = new JPasswordField();
            int ok = JOptionPane.showConfirmDialog(null, pf,
                    "Mot de passe du Truststore", JOptionPane.OK_CANCEL_OPTION);
            if (ok != JOptionPane.OK_OPTION) {
                return false;
            }
            char[] trustPass = pf.getPassword();
            if (trustPass.length == 0) {
                JOptionPane.showMessageDialog(null,
                        "Mot de passe vide !", "Erreur", JOptionPane.ERROR_MESSAGE);
                return false;
            }

            socket = new Socket(SERVER_HOST, SERVER_PORT);
            secureChannel = new SecureChannel(socket, TRUSTSTORE_PATH, trustPass.clone());
            Arrays.fill(trustPass, '\0');

            String msg = secureChannel.receiveEncryptedMessage();
            connected = "CONNECTED_TO_SERVER".equals(msg);
            return connected;

        } catch (Exception e) {
            connected = false;
            JOptionPane.showMessageDialog(null,
                    "Impossible de se connecter au serveur.\n" + e.getMessage(),
                    "Erreur de connexion", JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }

    /**
     * Envoie une requête en incluant automatiquement le NONCE et le TOKEN.
     */
    public String sendRequest(String request) {
        try {
            if (!connected || socket == null || socket.isClosed()) {
                if (!connect()) return "ERROR:SERVER_UNREACHABLE";
            }
            String nonce = UUID.randomUUID().toString() + "_" + System.currentTimeMillis();
            String token = (sessionToken != null) ? sessionToken : "none";
            String securedRequest = "NONCE:" + nonce + ":" + "TOKEN:" + token + ":" + request;
            secureChannel.sendEncryptedMessage(securedRequest);
            return secureChannel.receiveEncryptedMessage();
        } catch (Exception e) {
            connected = false;
            return "ERROR:COMMUNICATION";
        }
    }

    // ────────── Méthodes métier ──────────

    public String login(String email, String password) {
        String hashed = ClientHashUtil.hashPasswordClient(password);
        if (hashed == null) return "ERROR:HASH_FAILED";
        return sendRequest("LOGIN:" + safe(email) + ":" + hashed);
    }

    public String register(String nom, String prenom, String email, String password,
                           String address, String phone, String ville) {
        String hashed = ClientHashUtil.hashPasswordClient(password);
        if (hashed == null) return "ERROR:HASH_FAILED";
        return sendRequest("REGISTER:" + safe(nom) + ":" + safe(prenom) + ":" + safe(email)
                + ":" + hashed + ":" + safe(address) + ":" + safe(phone) + ":" + safe(ville));
    }

    public String sendOtp(String email) { return sendRequest("SEND_OTP:" + safe(email)); }
    public String verifyOtp(String email, String code) { return sendRequest("VERIFY_OTP:" + safe(email) + ":" + safe(code)); }
    public String getProducts() { return sendRequest("GET_PRODUCTS"); }
    public String getProduct(int id) { return sendRequest("GET_PRODUCT:" + id); }
    public String getCategories() { return sendRequest("GET_CATEGORIES"); }

    public String addToCart(int clientId, int productId, int quantity) {
        return sendRequest("CART_ADD:" + clientId + ":" + productId + ":" + quantity);
    }
    public String getCart(int clientId) { return sendRequest("CART_GET:" + clientId); }
    public String removeFromCart(int clientId, int productId) {
        return sendRequest("CART_REMOVE:" + clientId + ":" + productId);
    }
    public String clearCart(int clientId) { return sendRequest("CART_CLEAR:" + clientId); }
    public String checkout(int clientId) { return sendRequest("CHECKOUT:" + clientId); }
    public String pay(String uuid, String method) { return sendRequest("PAYMENT:" + safe(uuid) + ":" + safe(method)); }

    // Admin
    public String adminAddProduct(String name, String desc, double price, int stock, String image, int catId) {
        return sendRequest("ADMIN_ADD_PRODUCT:" + safe(name) + ":" + safe(desc) + ":" + price + ":" + stock + ":" + safe(image) + ":" + catId);
    }
    public String adminUpdateProduct(int id, String name, String desc, double price, int stock, String image, int catId) {
        return sendRequest("ADMIN_UPDATE_PRODUCT:" + id + ":" + safe(name) + ":" + safe(desc) + ":" + price + ":" + stock + ":" + safe(image) + ":" + catId);
    }
    public String adminDeleteProduct(int id) { return sendRequest("ADMIN_DELETE_PRODUCT:" + id); }
    public String adminGetCategories() { return sendRequest("ADMIN_GET_CATEGORIES"); }
    public String adminAddCategory(String name, String desc) { return sendRequest("ADMIN_ADD_CATEGORY:" + safe(name) + ":" + safe(desc)); }
    public String adminUpdateCategory(int id, String name, String desc) { return sendRequest("ADMIN_UPDATE_CATEGORY:" + id + ":" + safe(name) + ":" + safe(desc)); }
    public String adminDeleteCategory(int id) { return sendRequest("ADMIN_DELETE_CATEGORY:" + id); }
    public String adminGetUsers() { return sendRequest("ADMIN_GET_USERS"); }
    public String adminGetOrders() { return sendRequest("ADMIN_GET_ORDERS"); }
    public String adminUpdateOrderStatus(int orderId, String status) { return sendRequest("ADMIN_UPDATE_ORDER_STATUS:" + orderId + ":" + safe(status)); }

    // Admin dashboard (nouveau)
    public String adminGetDashboardSummary() { return sendRequest("ADMIN_GET_DASHBOARD_SUMMARY"); }
    public String adminGetNotifications() { return sendRequest("ADMIN_GET_NOTIFICATIONS"); }
    public String adminMarkNotificationRead(int id) { return sendRequest("ADMIN_MARK_NOTIFICATION_READ:" + id); }
    public String adminGetStockAlerts() { return sendRequest("ADMIN_GET_STOCK_ALERTS"); }
    public String adminGetStockHistory() { return sendRequest("ADMIN_GET_STOCK_HISTORY"); }
    public String adminAdjustStock(int productId, int quantity, String type, String reason, int adminId) {
        return sendRequest("ADMIN_ADJUST_STOCK:" + productId + ":" + quantity + ":" + safe(type) + ":" + safe(reason) + ":" + adminId);
    }
    
    // Profil
    public String getProfile(int userId) { return sendRequest("GET_PROFILE:" + userId); }
    public String updateProfile(int userId, String fullName, String email, String phone, String address, String city) {
        return sendRequest("UPDATE_PROFILE:" + userId + ":" + safe(fullName) + ":" + safe(email) + ":" + safe(phone) + ":" + safe(address) + ":" + safe(city));
    }

    public void close() {
        try {
            if (secureChannel != null) secureChannel.close();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            connected = false;
        }
    }

    private String safe(String s) {
        return s == null ? "" : s.replace(":", "-").replace(";", ",").replace("|", "/");
    }
}