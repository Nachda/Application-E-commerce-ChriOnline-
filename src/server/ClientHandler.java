package server;

import java.io.ByteArrayInputStream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.Socket;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import config.ServerConfig;
import dao.CategoryDAO;
import dao.OrderDAO;
import dao.UserDAO;
import model.Cart;
import model.CartItem;
import model.Category;
import model.Order;
import model.Payment;
import model.Product;
import model.User;
import model.Client;
import security.BruteForceProtection;
import security.ChallengeGenerator;
import security.InputValidator;
import security.KeystoreManager;
import security.ReplayProtection;
import security.SecureChannel;
import security.SessionManager;
import security.Verifier;
import service.AuthService;
import service.CartService;
import service.OrderService;
import service.OtpService;
import service.PaymentService;
import service.ProductService;

import service.DashboardService;
import service.NotificationService;
import service.StockService;
import model.DashboardSummary;
import model.Notification;
import model.StockAlert;
import model.StockMovement;

/**
 * Gestionnaire de connexion client.
 * Protections incluses : brute force, replay, SYN flood, session hijacking,
 * IP spoofing, injection de commandes.
 */
public class ClientHandler extends Thread {
	
	private static final Logger logger = LogManager.getLogger(ClientHandler.class);
    private static final Logger secLogger = LogManager.getLogger("security");

    private final Socket clientSocket;
    private final String clientIP;
    private SecureChannel secureChannel;


    private final AuthService authService;
    private final CartService cartService;
    private final ProductService productService;
    private OrderService orderService;
    private final PaymentService paymentService;
    private final OtpService otpService;
    private final UserDAO userDAO;

    private final BruteForceProtection bruteForceProtection;
    private final ReplayProtection replayProtection;
    private final SessionManager sessionManager;

    private static final Map<String, String> activeChallenges = new HashMap<>();

    public ClientHandler(Socket socket, String ip) {
        this.clientSocket = socket;
        this.clientIP = ip;
        
        logger.info("🆕 Nouveau ClientHandler créé pour l'IP : {}", clientIP);
        
        this.authService = new AuthService();
        this.cartService = new CartService();
        this.productService = new ProductService();
        this.paymentService = new PaymentService();
        this.otpService = new OtpService();
        this.userDAO = new UserDAO();
        this.bruteForceProtection = new BruteForceProtection();
        this.replayProtection = new ReplayProtection();
        this.sessionManager = new SessionManager();

        try {
            this.orderService = new OrderService();
        } catch (Exception e) {
        	 logger.error("Erreur initialisation OrderService :", e);
        }
    }

    @Override
    public void run() {
        try {
        	logger.info("🚀 Début du thread pour le client {}", clientIP);
            char[] keystorePassword = ServerConfig.getKeystorePassword();
            PrivateKey privateKey = KeystoreManager.loadPrivateKeyFromKeystore(
                    ServerConfig.getKeystorePath(),
                    keystorePassword.clone(),
                    "server",
                    keystorePassword.clone()
            );
            X509Certificate serverCert = (X509Certificate) KeystoreManager.loadCertificate(
                    ServerConfig.getKeystorePath(),
                    keystorePassword.clone(),
                    "server"
            );

            secureChannel = new SecureChannel(clientSocket, privateKey, serverCert);
            secureChannel.sendEncryptedMessage("CONNECTED_TO_SERVER");

            String request;
            while ((request = secureChannel.receiveEncryptedMessage()) != null) {
            	logger.info("📨 Requête reçue (chiffrée) : {}", request);
                String response = handleRequest(request);
                secureChannel.sendEncryptedMessage(response);
            }
        } catch (Exception e) {
        	logger.error("❌ Erreur critique pour le client {} : ", clientIP, e);
        } finally {
            closeResources();
        }
    }

    private String handleRequest(String rawRequest) {
        if (rawRequest == null || rawRequest.trim().isEmpty()) return "ERROR:EMPTY_REQUEST";
        if (rawRequest.equalsIgnoreCase("PING")) return "PONG";

        // ---------- Extraction du nonce et du token ----------
        String request = rawRequest;
        String nonce = null;
        String token = null;

        int firstColon = rawRequest.indexOf(':');
        if (firstColon > 0 && rawRequest.substring(0, firstColon).equals("NONCE")) {
            int secondColon = rawRequest.indexOf(':', firstColon + 1);
            if (secondColon > 0) {
                nonce = rawRequest.substring(firstColon + 1, secondColon);
                int fourthColon = rawRequest.indexOf(':', secondColon + 1);
                if (fourthColon > 0 && rawRequest.substring(secondColon + 1, fourthColon).equals("TOKEN")) {
                    int fifthColon = rawRequest.indexOf(':', fourthColon + 1);
                    if (fifthColon > 0) {
                        token = rawRequest.substring(fourthColon + 1, fifthColon);
                        request = rawRequest.substring(fifthColon + 1);
                    } else {
                        return "ERROR:MALFORMED_REQUEST";
                    }
                } else {
                    // Pas de token : on garde la requête à partir de la commande réelle
                    request = rawRequest.substring(secondColon + 1);
                }
            }
        } else {
            return "ERROR:MALFORMED_REQUEST";
        }

        if (!replayProtection.isNonceValid(nonce)) {
        	secLogger.debug("Replay détecté pour nonce {}", nonce);
            return "ERROR:REPLAY_DETECTED";
        }

        // ---------- Vérification de session pour les commandes protégées ----------
        if (!request.startsWith("LOGIN:") && !request.startsWith("REGISTER:") &&
            !request.startsWith("SEND_OTP:") && !request.startsWith("VERIFY_OTP:") &&
            !request.startsWith("ADMIN_LOGIN_REQUEST:") && !request.startsWith("ADMIN_LOGIN_VERIFY:")) {
        	
            if (token == null || token.equals("none")) {
            	secLogger.warn("⚠️ Token manquant pour IP {}", clientIP);
                return "ERROR:MISSING_TOKEN";
            }
            
            // Utiliser le token pour retrouver l'utilisateur, pas la commande
            int userId = sessionManager.getUserIdForToken(token);
            
            if (userId == -1 || !sessionManager.isValidSession(userId, token)) {
            	secLogger.warn("⚠️ Session invalide (token={}) depuis IP {}", token, clientIP);
                return "ERROR:INVALID_SESSION";
            }
            // Vérification supplémentaire pour les commandes qui contiennent un ID client explicite
            if (request.startsWith("CART_ADD:") || request.startsWith("CART_REMOVE:") ||
                request.startsWith("CART_GET:") || request.startsWith("CART_CLEAR:") ||
                request.startsWith("CHECKOUT:") || request.startsWith("GET_PROFILE:") ||
                request.startsWith("UPDATE_PROFILE:")) {
                int targetUserId = extractUserId(request);
                if (targetUserId != -1 && targetUserId != userId) {
                	secLogger.warn("🚨 Tentative d'accès non autorisé : user {} essaye d'accéder à user {} (IP {})",
                            userId, targetUserId, clientIP);
                    return "ERROR:INVALID_SESSION";
                }
            }
        }

        // ---------- Routage ----------
        if (request.startsWith("ADMIN_LOGIN_REQUEST:")) return handleAdminLoginRequest(request);
        if (request.startsWith("ADMIN_LOGIN_VERIFY:")) return handleAdminLoginVerify(request);

        try {
            if (request.startsWith("LOGIN:")) return handleLogin(request);
            if (request.startsWith("REGISTER:")) return handleRegister(request);
            if (request.startsWith("SEND_OTP:")) return handleSendOtp(request);
            if (request.startsWith("VERIFY_OTP:")) return handleVerifyOtp(request);

            if (request.startsWith("CART_ADD:")) return handleCartAdd(request);
            if (request.startsWith("CART_REMOVE:")) return handleCartRemove(request);
            if (request.startsWith("CART_GET:")) return handleCartGet(request);
            if (request.startsWith("CART_CLEAR:")) return handleCartClear(request);

            if (request.equalsIgnoreCase("GET_PRODUCTS")) return handleGetProducts();
            if (request.startsWith("GET_PRODUCT:")) return handleGetProduct(request);

            if (request.startsWith("CHECKOUT:")) return handleCheckout(request);
            if (request.startsWith("PAYMENT:")) return handlePayment(request);

            if (request.startsWith("ADMIN_ADD_PRODUCT:")) return handleAdminAddProduct(request);
            if (request.startsWith("ADMIN_UPDATE_PRODUCT:")) return handleAdminUpdateProduct(request);
            if (request.startsWith("ADMIN_DELETE_PRODUCT:")) return handleAdminDeleteProduct(request);

            if (request.equalsIgnoreCase("ADMIN_GET_CATEGORIES")) return handleAdminGetCategories();
            if (request.startsWith("ADMIN_ADD_CATEGORY:")) return handleAdminAddCategory(request);
            if (request.startsWith("ADMIN_UPDATE_CATEGORY:")) return handleAdminUpdateCategory(request);
            if (request.startsWith("ADMIN_DELETE_CATEGORY:")) return handleAdminDeleteCategory(request);

            if (request.equalsIgnoreCase("ADMIN_GET_USERS")) return handleAdminGetUsers();
            if (request.equalsIgnoreCase("ADMIN_GET_ORDERS")) return handleAdminGetOrders();
            if (request.startsWith("ADMIN_UPDATE_ORDER_STATUS:")) return handleAdminUpdateOrderStatus(request);
            
            // ────────── Admin Dashboard V2 ──────────
            if (request.equalsIgnoreCase("ADMIN_GET_DASHBOARD_SUMMARY")) return handleAdminGetDashboardSummary();
            if (request.equalsIgnoreCase("ADMIN_GET_NOTIFICATIONS")) return handleAdminGetNotifications();
            if (request.startsWith("ADMIN_MARK_NOTIFICATION_READ:")) return handleAdminMarkNotificationRead(request);
            if (request.equalsIgnoreCase("ADMIN_GET_STOCK_ALERTS")) return handleAdminGetStockAlerts();
            if (request.equalsIgnoreCase("ADMIN_GET_STOCK_HISTORY")) return handleAdminGetStockHistory();
            if (request.startsWith("ADMIN_ADJUST_STOCK:")) return handleAdminAdjustStock(request);

            if (request.startsWith("GET_PROFILE:")) return handleGetProfile(request);
            if (request.startsWith("UPDATE_PROFILE:")) return handleUpdateProfile(request);

            if (request.equalsIgnoreCase("GET_CATEGORIES")) return handleGetCategories();

            return "ERROR:UNKNOWN_COMMAND";
        } catch (Exception e) {
        	logger.error("Exception dans handleRequest :", e);
            return "ERROR:EXCEPTION_OCCURRED";
        }
    }

    /**
     * Extrait l'identifiant utilisateur d'une commande.
     * La plupart des commandes ont l'ID en 2ème champ.
     */
    private int extractUserId(String request) {
        String[] parts = request.split(":");
        if (parts.length > 1) {
            try {
                return Integer.parseInt(parts[1]);
            } catch (NumberFormatException ignored) {}
        }
        return -1;
    }

    // ────────── Authentification ──────────

    private String handleLogin(String request) {
        String[] parts = request.split(":");
        if (parts.length != 3) return "ERROR:LOGIN_FORMAT";
        String email = parts[1];
        String passwordHash = parts[2];

        if (!InputValidator.isValidEmail(email) || !InputValidator.isValidHash(passwordHash)) {
        	secLogger.warn("⚠️ Input invalide LOGIN pour {}", email);
            return "ERROR:INVALID_INPUT";
        }

        if (!bruteForceProtection.isAllowed(email, clientIP)) {
        	secLogger.debug("⛔ Accès bloqué (force brute) pour {}", email);
            return "ERROR:BRUTE_FORCE_BLOCKED";
        }

        User user = authService.login(email, passwordHash);
        if (user != null) {
            String token = sessionManager.createSession(user.getId(), clientIP);
            bruteForceProtection.reset(email, clientIP);
            logger.info("✅ Connexion réussie : {}", email);
            return "LOGIN_SUCCESS:" + user.getId() + ":" + user.getRole() + ":" + token;
        }

        bruteForceProtection.registerFailure(email, clientIP);
        secLogger.debug("❌ Login échoué pour {} depuis {}", email, clientIP);
        if (authService.emailExists(email) && !authService.isAccountActive(email)) {
            return "ERROR:ACCOUNT_NOT_ACTIVE";
        }
        return "ERROR:LOGIN_FAILED";
    }

    private String handleRegister(String request) {
        String[] parts = request.split(":");
        if (parts.length != 8) return "ERROR:REGISTER_FORMAT";
        if (!InputValidator.isValidName(parts[1]) || !InputValidator.isValidName(parts[2]) ||
            !InputValidator.isValidEmail(parts[3]) || !InputValidator.isValidHash(parts[4]) ||
            !InputValidator.isValidAddress(parts[5]) || !InputValidator.isValidPhone(parts[6]) ||
            !InputValidator.isValidName(parts[7])) {
        	secLogger.warn("⚠️ Tentative REGISTER avec input invalide depuis IP {}", clientIP);
            return "ERROR:INVALID_INPUT";
        }

        String nom = parts[1], prenom = parts[2], email = parts[3], password = parts[4];
        String address = parts[5], phone = parts[6], ville = parts[7];

        boolean success = authService.registerPending(nom, prenom, email, password, address, phone, ville);
        if (!success) return "ERROR:REGISTER_FAILED";
        boolean otpSent = otpService.sendOtp(email);
        return otpSent ? "REGISTER_SUCCESS_OTP_SENT" : "REGISTER_SUCCESS_BUT_OTP_FAILED";
    }

    private String handleSendOtp(String request) {
        String[] parts = request.split(":", 2);
        if (parts.length != 2) return "ERROR:SEND_OTP_FORMAT";
        return otpService.sendOtp(parts[1]) ? "OTP_SENT" : "ERROR:OTP_SEND_FAILED";
    }

    private String handleVerifyOtp(String request) {
        String[] parts = request.split(":");
        if (parts.length != 3) return "ERROR:VERIFY_OTP_FORMAT";
        return otpService.verifyOtp(parts[1], parts[2]) ? "OTP_VERIFIED" : "ERROR:OTP_INVALID";
    }

    // ────────── Panier ──────────

    private String handleCartAdd(String request) {
        String[] parts = request.split(":");
        if (parts.length != 4) return "ERROR:CART_ADD_FORMAT";
        int clientId = Integer.parseInt(parts[1]);
        int productId = Integer.parseInt(parts[2]);
        int qty = Integer.parseInt(parts[3]);
        Product p = productService.getProductById(productId);
        if (p == null) return "ERROR:PRODUCT_NOT_FOUND";
        if (p.getStock() < qty) return "ERROR:INSUFFICIENT_STOCK";
        CartItem item = new CartItem(0, p, qty);
        return cartService.addItemToCart(clientId, item) ? "CART_ADD_SUCCESS" : "ERROR:CART_ADD_FAILED";
    }

    private String handleCartRemove(String request) {
        String[] parts = request.split(":");
        if (parts.length != 3) return "ERROR:CART_REMOVE_FORMAT";
        int clientId = Integer.parseInt(parts[1]);
        int productId = Integer.parseInt(parts[2]);
        return cartService.removeItemFromCart(clientId, productId) ? "CART_REMOVE_SUCCESS" : "ERROR:CART_REMOVE_FAILED";
    }

    private String handleCartGet(String request) {
        String[] parts = request.split(":");
        if (parts.length != 2) return "ERROR:CART_GET_FORMAT";
        int clientId = Integer.parseInt(parts[1]);
        Cart cart = cartService.getCartByClient(clientId);
        if (cart == null || cart.isEmpty()) return "CART_EMPTY";

        StringBuilder sb = new StringBuilder("CART_DETAILS|Items=").append(cart.getItems().size())
                .append("|Total=").append(cart.calculateTotal());
        for (CartItem item : cart.getItems()) {
            if (item.getProduct() != null) {
                sb.append("|ProductId=").append(item.getProduct().getIdProduct())
                  .append(",Product=").append(safe(item.getProduct().getName()))
                  .append(",Qty=").append(item.getQuantity())
                  .append(",Subtotal=").append(item.calculateSubtotal());
            }
        }
        return sb.toString();
    }

    private String handleCartClear(String request) {
        String[] parts = request.split(":");
        if (parts.length != 2) return "ERROR:CART_CLEAR_FORMAT";
        return cartService.clearCart(Integer.parseInt(parts[1])) ? "CART_CLEAR_SUCCESS" : "ERROR:CART_CLEAR_FAILED";
    }

    // ────────── Produits ──────────

    private String handleGetProducts() {
        List<Product> products = productService.getAllProducts();
        if (products.isEmpty()) return "NO_PRODUCTS";
        StringBuilder sb = new StringBuilder();
        for (Product p : products) {
            sb.append(p.getIdProduct()).append(";").append(safe(p.getName())).append(";")
              .append(p.getPrice()).append(";").append(safe(p.getImage())).append(";")
              .append(safe(p.getCategory() != null ? p.getCategory().getName() : "Sans catégorie"))
              .append(";").append(p.getStock()).append("|");
        }
        return sb.substring(0, sb.length() - 1);
    }

    private String handleGetProduct(String request) {
        int id = Integer.parseInt(request.split(":")[1]);
        Product p = productService.getProductById(id);
        if (p == null) return "ERROR:PRODUCT_NOT_FOUND";
        return p.getIdProduct() + ";" + safe(p.getName()) + ";" + p.getPrice() + ";"
                + safe(p.getDescription()) + ";" + p.getStock() + ";" + safe(p.getImage()) + ";"
                + safe(p.getCategory() != null ? p.getCategory().getName() : "Sans catégorie");
    }

    // ────────── Commandes / Paiement ──────────

    private String handleCheckout(String request) {
        try {
            int clientId = Integer.parseInt(request.split(":")[1]);
            Cart cart = cartService.getCartByClient(clientId);
            if (cart == null || cart.isEmpty()) {
            	secLogger.warn("⚠️ Tentative checkout panier vide pour client {}", clientId);
            	return "ERROR:CART_EMPTY";
            }
            Order order = orderService.createOrder(clientId, cart.getItems());
            cartService.clearCart(clientId);
            return "ORDER_CREATED;" + order.getOrderUUID() + ";" + order.getTotalPrice();
        } catch (Exception e) {
            return "ERROR:CHECKOUT_EXCEPTION";
        }
    }

    private String handlePayment(String request) {
        try {
            String[] parts = request.split(":");
            if (parts.length != 3) return "ERROR:PAYMENT_FORMAT";
            if (!InputValidator.isValidPaymentMethod(parts[2])) return "ERROR:INVALID_INPUT";
            String uuid = parts[1];
            String method = parts[2];
            Order order = orderService.getOrderByUUID(uuid);
            if (order == null) return "ERROR:ORDER_NOT_FOUND";
            Payment payment = new Payment(0, order.getId(), method, order.getTotalPrice(), "pending", null);
            boolean success = paymentService.processPayment(payment);
            if (success) {
                orderService.updateStatus(order.getId(), "validated");
                return "PAYMENT_SUCCESS;" + uuid;
            } else {
                return "PAYMENT_FAILED;" + uuid;
            }
        } catch (Exception e) {
            return "ERROR:PAYMENT_EXCEPTION";
        }
    }

    // ────────── Admin ──────────

    private String handleAdminAddProduct(String request) {
        String[] parts = request.split(":", 7);
        if (parts.length != 7) return "ERROR:ADMIN_ADD_PRODUCT_FORMAT";
        Product p = new Product(0, parts[1], parts[2], parts[5], Double.parseDouble(parts[3]), Integer.parseInt(parts[4]));
        p.setCategory(new Category(Integer.parseInt(parts[6]), "", ""));
        return productService.addProduct(p) ? "ADMIN_ADD_PRODUCT_SUCCESS" : "ERROR:ADMIN_ADD_PRODUCT_FAILED";
    }

    private String handleAdminUpdateProduct(String request) {
        String[] parts = request.split(":", 8);
        if (parts.length != 8) return "ERROR:ADMIN_UPDATE_PRODUCT_FORMAT";
        Product p = new Product(Integer.parseInt(parts[1]), parts[2], parts[3], parts[6],
                Double.parseDouble(parts[4]), Integer.parseInt(parts[5]));
        p.setCategory(new Category(Integer.parseInt(parts[7]), "", ""));
        return productService.updateProduct(p) ? "ADMIN_UPDATE_PRODUCT_SUCCESS" : "ERROR:ADMIN_UPDATE_PRODUCT_FAILED";
    }

    private String handleAdminDeleteProduct(String request) {
        return productService.deleteProduct(Integer.parseInt(request.split(":")[1])) ?
                "ADMIN_DELETE_PRODUCT_SUCCESS" : "ERROR:ADMIN_DELETE_PRODUCT_FAILED";
    }

    private String handleAdminGetCategories() {
        try {
            CategoryDAO dao = new CategoryDAO();
            List<Category> cats = dao.findAll();
            if (cats.isEmpty()) return "NO_CATEGORIES";
            StringBuilder sb = new StringBuilder();
            for (Category c : cats) sb.append(c.getId()).append(";").append(safe(c.getName())).append(";").append(safe(c.getDescription())).append("|");
            return sb.substring(0, sb.length() - 1);
        } catch (Exception e) {
            return "ERROR:GET_CATEGORIES_EXCEPTION";
        }
    }

    private String handleAdminAddCategory(String request) {
        try {
            String[] parts = request.split(":", 3);
            new CategoryDAO().save(new Category(0, parts[1], parts[2]));
            return "ADMIN_ADD_CATEGORY_SUCCESS";
        } catch (Exception e) { return "ERROR:ADD_CATEGORY_EXCEPTION"; }
    }

    private String handleAdminUpdateCategory(String request) {
        try {
            String[] parts = request.split(":", 4);
            new CategoryDAO().update(new Category(Integer.parseInt(parts[1]), parts[2], parts[3]));
            return "ADMIN_UPDATE_CATEGORY_SUCCESS";
        } catch (Exception e) { return "ERROR:UPDATE_CATEGORY_EXCEPTION"; }
    }

    private String handleAdminDeleteCategory(String request) {
        try {
            new CategoryDAO().delete(Integer.parseInt(request.split(":")[1]));
            return "ADMIN_DELETE_CATEGORY_SUCCESS";
        } catch (Exception e) { return "ERROR:DELETE_CATEGORY_EXCEPTION"; }
    }

    private String handleAdminGetUsers() {
        List<User> users = userDAO.findAll();
        if (users.isEmpty()) return "NO_USERS";
        StringBuilder sb = new StringBuilder();
        for (User u : users) {
            sb.append(u.getId()).append(";").append(safe(u.getNom())).append(";")
              .append(safe(u.getPrenom())).append(";").append(safe(u.getEmail()))
              .append(";").append(safe(u.getRole())).append("|");
        }
        return sb.substring(0, sb.length() - 1);
    }

    private String handleAdminGetOrders() {
        try {
            List<Order> orders = new OrderDAO().findAll();
            if (orders.isEmpty()) return "NO_ORDERS";
            StringBuilder sb = new StringBuilder();
            for (Order o : orders) {
                sb.append(o.getId()).append(";").append(safe(o.getOrderUUID())).append(";")
                  .append(o.getTotalPrice()).append(";").append(safe(o.getStatus()))
                  .append(";").append(o.getCreatedAt()).append("|");
            }
            return sb.substring(0, sb.length() - 1);
        } catch (Exception e) { return "ERROR:ADMIN_GET_ORDERS_EXCEPTION"; }
    }

    private String handleAdminUpdateOrderStatus(String request) {
        try {
            String[] parts = request.split(":", 3);
            if (!InputValidator.isValidStatus(parts[2])) return "ERROR:INVALID_INPUT";
            new OrderDAO().updateStatus(Integer.parseInt(parts[1]), parts[2]);
            return "ADMIN_UPDATE_ORDER_STATUS_SUCCESS";
        } catch (Exception e) { return "ERROR:UPDATE_ORDER_STATUS_EXCEPTION"; }
    }
    
    // ────────── Admin Dashboard V2 ──────────

    private String handleAdminGetDashboardSummary() {
        try {
            DashboardSummary summary = new DashboardService().getDashboardSummary();
            return "DASHBOARD_SUMMARY:" +
                    summary.getTotalProducts() + ";" +
                    summary.getLowStockProducts() + ";" +
                    summary.getOutOfStockProducts() + ";" +
                    summary.getTotalUsers() + ";" +
                    summary.getTotalOrders() + ";" +
                    summary.getPendingOrders() + ";" +
                    summary.getPaidOrders() + ";" +
                    summary.getTodayRevenue() + ";" +
                    summary.getMonthRevenue() + ";" +
                    summary.getUnreadNotifications();
        } catch (Exception e) {
            return "ERROR:ADMIN_GET_DASHBOARD_SUMMARY_EXCEPTION";
        }
    }

    private String handleAdminGetNotifications() {
        try {
            NotificationService ns = new NotificationService();
            ns.syncLowStockNotifications();
            List<Notification> list = ns.getUnreadNotifications();
            if (list.isEmpty()) return "NO_NOTIFICATIONS";
            StringBuilder sb = new StringBuilder();
            for (Notification n : list) {
                sb.append(n.getId()).append(";")
                  .append(safe(n.getTitle())).append(";")
                  .append(safe(n.getMessage())).append(";")
                  .append(safe(n.getType())).append(";")
                  .append(safe(n.getLevel())).append(";")
                  .append(n.isRead()).append(";")
                  .append(safe(n.getEntityType() != null ? n.getEntityType() : "")).append(";")
                  .append(n.getEntityId() != null ? n.getEntityId().toString() : "").append(";")
                  .append(n.getCreatedAt()).append("|");
            }
            return sb.substring(0, sb.length() - 1);
        } catch (Exception e) {
            return "ERROR:ADMIN_GET_NOTIFICATIONS_EXCEPTION";
        }
    }

    private String handleAdminMarkNotificationRead(String request) {
        try {
            String[] parts = request.split(":");
            if (parts.length != 2) return "ERROR:ADMIN_MARK_NOTIFICATION_READ_FORMAT";
            int notificationId = Integer.parseInt(parts[1]);
            boolean ok = new NotificationService().markAsRead(notificationId);
            return ok ? "ADMIN_MARK_NOTIFICATION_READ_SUCCESS" : "ERROR:ADMIN_MARK_NOTIFICATION_READ_FAILED";
        } catch (Exception e) {
            return "ERROR:ADMIN_MARK_NOTIFICATION_READ_EXCEPTION";
        }
    }

    private String handleAdminGetStockAlerts() {
        try {
            List<StockAlert> alerts = new StockService().getLowStockAlerts();
            if (alerts.isEmpty()) return "NO_STOCK_ALERTS";
            StringBuilder sb = new StringBuilder();
            for (StockAlert a : alerts) {
                sb.append(a.getProductId()).append(";")
                  .append(safe(a.getProductName())).append(";")
                  .append(a.getCurrentStock()).append(";")
                  .append(a.getThreshold()).append(";")
                  .append(safe(a.getLevel())).append(";")
                  .append(safe(a.getStatus())).append(";")
                  .append(a.getCreatedAt()).append("|");
            }
            return sb.substring(0, sb.length() - 1);
        } catch (Exception e) {
            return "ERROR:ADMIN_GET_STOCK_ALERTS_EXCEPTION";
        }
    }

    private String handleAdminGetStockHistory() {
        try {
            List<StockMovement> history = new StockService().getStockHistory();
            if (history.isEmpty()) return "NO_STOCK_HISTORY";
            StringBuilder sb = new StringBuilder();
            for (StockMovement m : history) {
                sb.append(m.getId()).append(";")
                  .append(m.getProductId()).append(";")
                  .append(safe(m.getProductName())).append(";")
                  .append(safe(m.getMovementType())).append(";")
                  .append(m.getQuantity()).append(";")
                  .append(m.getPreviousStock()).append(";")
                  .append(m.getNewStock()).append(";")
                  .append(safe(m.getReason())).append(";")
                  .append(m.getAdminUserId() != null ? m.getAdminUserId().toString() : "").append(";")
                  .append(m.getCreatedAt()).append("|");
            }
            return sb.substring(0, sb.length() - 1);
        } catch (Exception e) {
            return "ERROR:ADMIN_GET_STOCK_HISTORY_EXCEPTION";
        }
    }

    private String handleAdminAdjustStock(String request) {
        try {
            String[] parts = request.split(":", 6);
            if (parts.length != 6) return "ERROR:ADMIN_ADJUST_STOCK_FORMAT";
            int productId = Integer.parseInt(parts[1]);
            int quantity = Integer.parseInt(parts[2]);
            String movementType = parts[3];
            String reason = parts[4];
            int adminUserId = Integer.parseInt(parts[5]);
            boolean ok = new StockService().adjustStock(productId, quantity, movementType, reason, adminUserId);
            return ok ? "ADMIN_ADJUST_STOCK_SUCCESS" : "ERROR:ADMIN_ADJUST_STOCK_FAILED";
        } catch (Exception e) {
            return "ERROR:ADMIN_ADJUST_STOCK_EXCEPTION";
        }
    }

    // ────────── Profil / Catégories publiques ──────────

    private String handleGetProfile(String request) {
        int userId = Integer.parseInt(request.split(":")[1]);
        User u = userDAO.findById(userId);
        if (u == null) return "ERROR:PROFILE_NOT_FOUND";
        String fullName = (u.getPrenom() != null ? u.getPrenom() : "") + " " + (u.getNom() != null ? u.getNom() : "");
        String phone = "", address = "", city = "";
        if (u instanceof Client c) {
            phone = c.getPhone(); address = c.getAddress(); city = c.getVille();
        }
        return "PROFILE_DATA:" + safe(fullName.trim()) + ";" + safe(u.getEmail()) + ";" + safe(phone)
                + ";" + safe(address) + ";" + safe(city) + ";" + safe(u.getRole());
    }

    private String handleUpdateProfile(String request) {
        String[] parts = request.split(":", 7);
        if (parts.length != 7) return "ERROR:UPDATE_PROFILE_FORMAT";
        if (!InputValidator.isValidName(parts[2]) || !InputValidator.isValidEmail(parts[3]) ||
            !InputValidator.isValidPhone(parts[4]) || !InputValidator.isValidAddress(parts[5]) ||
            !InputValidator.isValidName(parts[6])) {
            return "ERROR:INVALID_INPUT";
        }
        boolean ok = userDAO.updateProfile(Integer.parseInt(parts[1]), parts[2], parts[3], parts[4], parts[5], parts[6]);
        return ok ? "UPDATE_PROFILE_SUCCESS" : "ERROR:UPDATE_PROFILE_FAILED";
    }

    private String handleGetCategories() {
        return handleAdminGetCategories();
    }

    // ────────── Utilitaires ──────────
    private String safe(String s) {
        return s == null ? "" : s.replace(";", ",").replace("|", "/").replace(":", "-");
    }

    // ────────── Authentification Admin ──────────
    private String handleAdminLoginRequest(String request) {
        try {
            String[] parts = request.split(":");
            if (parts.length != 2) return "ERROR:ADMIN_LOGIN_FORMAT";
            String email = parts[1].trim().toLowerCase();

            User user = userDAO.findByEmail(email);
            if (user == null || !"admin".equalsIgnoreCase(user.getRole())) return "ERROR:NOT_ADMIN";
            if (!"active".equalsIgnoreCase(user.getStatus())) return "ERROR:ADMIN_ACCOUNT_NOT_ACTIVE";

            String challenge = ChallengeGenerator.generateChallenge();
            synchronized (activeChallenges) {
                activeChallenges.put(email, challenge);
            }
            secLogger.debug("Challenge généré pour admin {}", email);
            return "ADMIN_CHALLENGE:" + challenge;
        } catch (Exception e) {
        	logger.error("Erreur ADMIN_LOGIN_REQUEST :", e);
            return "ERROR:ADMIN_LOGIN_REQUEST_EXCEPTION";
        }
    }

    private String handleAdminLoginVerify(String request) {
        try {
            String[] parts = request.split(":", 4);
            if (parts.length != 4) return "ERROR:ADMIN_VERIFY_FORMAT";
            String email = parts[1].trim().toLowerCase();
            String signatureBase64 = parts[2];
            String certBase64 = parts[3];

            String challenge;
            synchronized (activeChallenges) {
                challenge = activeChallenges.remove(email);
            }
            if (challenge == null) {
            	secLogger.warn("⚠️ Aucun challenge trouvé pour {}", email);
                return "ERROR:NO_CHALLENGE";
            }

            byte[] certBytes = Base64.getDecoder().decode(certBase64);
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            X509Certificate cert = (X509Certificate) cf.generateCertificate(new ByteArrayInputStream(certBytes));

            //  Charger le certificat de confiance depuis le truststore
            char[] truststorePassword = ServerConfig.getTruststorePassword();
            X509Certificate trustedCert = (X509Certificate) KeystoreManager.loadCertificate(
                    ServerConfig.getTruststorePath(),
                    truststorePassword.clone(),
                    "admin"
            );

            if (trustedCert == null) {
                secLogger.warn("⚠️ Certificat admin introuvable dans le truststore");
                return "ERROR:CERT_NOT_TRUSTED";
            }

            //  Comparer le certificat reçu avec le certificat de confiance
            if (!cert.equals(trustedCert)) {
                secLogger.warn("🚨 Certificat reçu différent du certificat de confiance pour {}", email);
                return "ERROR:CERT_NOT_TRUSTED";
            }

            //  Extraire la clé publique du certificat validé
            PublicKey publicKey = KeystoreManager.loadPublicKeyFromCertificate(cert);
            if (publicKey == null) {
                secLogger.warn("⚠️ Clé publique invalide pour {}", email);
                return "ERROR:INVALID_CERT";
            }

            byte[] signatureBytes = Base64.getDecoder().decode(signatureBase64);
            boolean valid = Verifier.verify(challenge, signatureBytes, publicKey);
            if (!valid) {
            	secLogger.warn("🚨 Signature invalide pour {}", email);
            	return "ERROR:INVALID_SIGNATURE";
            }

            User adminUser = userDAO.findByEmail(email);
            
            // ⭐ Création d'une session admin
            String token = sessionManager.createSession(adminUser.getId(), clientIP);
            logger.info("AUTHENTIFICATION ADMIN RÉUSSIE pour {}", email);
            return "ADMIN_LOGIN_SUCCESS:" + adminUser.getId() + ":" + adminUser.getRole() + ":" + token;
        } catch (Exception e) {
        	logger.error("Erreur ADMIN_LOGIN_VERIFY :", e);
            return "ERROR:ADMIN_LOGIN_VERIFY_EXCEPTION";
        }
    }

    private void closeResources() {
        try {
            if (secureChannel != null) secureChannel.close();
        } catch (IOException e) {
        	logger.error("Erreur fermeture canal : {}", e.getMessage());
        }
        Server.decrementIP(clientIP);
    }
}