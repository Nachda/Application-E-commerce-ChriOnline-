package dao;

import database.DatabaseConnection;
import model.Cart;
import model.CartItem;
import model.Product;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Gère la persistance du panier (tables 'carts' et 'cart_items').
 */
public class CartDAO {

    private final Connection conn;

    public CartDAO() {
        this.conn = DatabaseConnection.getConnection();
    }

    /**
     * Récupère le panier d'un client, ou le crée s'il n'existe pas.
     */
    public Cart getCartByClient(int clientId) {
        // Recherche du panier
        String sqlCart = "SELECT * FROM carts WHERE client_id = ?";
        try (PreparedStatement psCart = conn.prepareStatement(sqlCart)) {
            psCart.setInt(1, clientId);
            ResultSet rsCart = psCart.executeQuery();
            Cart cart = null;

            if (rsCart.next()) {
                cart = new Cart();
                cart.setId(rsCart.getInt("id"));
                cart.setClientId(rsCart.getInt("client_id"));
                Timestamp createdAt = rsCart.getTimestamp("created_at");
                if (createdAt != null) cart.setCreatedAt(createdAt.toString());
            } else {
                // Création d'un nouveau panier
                int newId = createCart(clientId);
                if (newId != -1) {
                    cart = new Cart();
                    cart.setId(newId);
                    cart.setClientId(clientId);
                }
            }

            if (cart != null) {
                // Chargement des articles
                List<CartItem> items = new ArrayList<>();
                String sqlItems = "SELECT ci.id, ci.quantity, p.* FROM cart_items ci " +
                                  "LEFT JOIN products p ON ci.product_id = p.id_product " +
                                  "WHERE ci.cart_id = ?";
                try (PreparedStatement psItems = conn.prepareStatement(sqlItems)) {
                    psItems.setInt(1, cart.getId());
                    ResultSet rsItems = psItems.executeQuery();
                    while (rsItems.next()) {
                        CartItem item = new CartItem();
                        item.setId(rsItems.getInt("id"));
                        item.setQuantity(rsItems.getInt("quantity"));
                        int productId = rsItems.getInt("id_product");
                        if (!rsItems.wasNull()) {
                            Product product = new Product(
                                    productId,
                                    rsItems.getString("name"),
                                    rsItems.getString("description"),
                                    rsItems.getString("image"),
                                    rsItems.getDouble("price"),
                                    rsItems.getInt("stock")
                            );
                            item.setProduct(product);
                        }
                        items.add(item);
                    }
                }
                cart.setItems(items);
            }
            return cart;

        } catch (SQLException e) {
            System.err.println("Erreur getCartByClient : " + e.getMessage());
            return null;
        }
    }

    private int createCart(int clientId) {
        String sql = "INSERT INTO carts (client_id) VALUES (?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, clientId);
            ps.executeUpdate();
            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            System.err.println("Erreur createCart : " + e.getMessage());
        }
        return -1;
    }

    /**
     * Ajoute un produit au panier (ou augmente la quantité).
     */
    public boolean addItem(int cartId, CartItem item) {
        if (item == null || item.getProduct() == null) return false;
        String checkSql = "SELECT * FROM cart_items WHERE cart_id = ? AND product_id = ?";
        String updateSql = "UPDATE cart_items SET quantity = quantity + ? WHERE cart_id = ? AND product_id = ?";
        String insertSql = "INSERT INTO cart_items (cart_id, product_id, quantity) VALUES (?, ?, ?)";

        try (PreparedStatement psCheck = conn.prepareStatement(checkSql)) {
            psCheck.setInt(1, cartId);
            psCheck.setInt(2, item.getProduct().getIdProduct());
            ResultSet rs = psCheck.executeQuery();
            if (rs.next()) {
                try (PreparedStatement psUpdate = conn.prepareStatement(updateSql)) {
                    psUpdate.setInt(1, item.getQuantity());
                    psUpdate.setInt(2, cartId);
                    psUpdate.setInt(3, item.getProduct().getIdProduct());
                    psUpdate.executeUpdate();
                }
            } else {
                try (PreparedStatement psInsert = conn.prepareStatement(insertSql)) {
                    psInsert.setInt(1, cartId);
                    psInsert.setInt(2, item.getProduct().getIdProduct());
                    psInsert.setInt(3, item.getQuantity());
                    psInsert.executeUpdate();
                }
            }
            return true;
        } catch (SQLException e) {
            System.err.println("Erreur addItem : " + e.getMessage());
            return false;
        }
    }

    public boolean removeItem(int cartId, int productId) {
        String sql = "DELETE FROM cart_items WHERE cart_id = ? AND product_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, cartId);
            ps.setInt(2, productId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Erreur removeItem : " + e.getMessage());
            return false;
        }
    }

    public boolean clear(int cartId) {
        String sql = "DELETE FROM cart_items WHERE cart_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, cartId);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.err.println("Erreur clear cart : " + e.getMessage());
            return false;
        }
    }
}