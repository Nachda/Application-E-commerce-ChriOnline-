package service;

import dao.CartDAO;
import model.Cart;
import model.CartItem;

public class CartService {

    private final CartDAO cartDAO;

    public CartService() {
        this.cartDAO = new CartDAO();
    }

    public Cart getCartByClient(int clientId) {
        return cartDAO.getCartByClient(clientId);
    }

    public boolean addItemToCart(int clientId, CartItem item) {
        Cart cart = cartDAO.getCartByClient(clientId);
        if (cart == null) return false;
        if (item == null || item.getProduct() == null) return false;
        if (item.getQuantity() <= 0) return false;
        return cartDAO.addItem(cart.getId(), item);
    }

    public boolean removeItemFromCart(int clientId, int productId) {
        Cart cart = cartDAO.getCartByClient(clientId);
        if (cart == null) return false;
        return cartDAO.removeItem(cart.getId(), productId);
    }

    public boolean clearCart(int clientId) {
        Cart cart = cartDAO.getCartByClient(clientId);
        if (cart == null) return false;
        return cartDAO.clear(cart.getId());
    }

    public double calculateCartTotal(int clientId) {
        Cart cart = cartDAO.getCartByClient(clientId);
        if (cart == null || cart.getItems().isEmpty()) return 0.0;
        return cart.calculateTotal();
    }

    public boolean isCartEmpty(int clientId) {
        Cart cart = cartDAO.getCartByClient(clientId);
        return cart == null || cart.isEmpty();
    }
}