package dao;

import database.DatabaseConnection;
import model.Category;
import model.Product;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Gère l'accès à la table 'products' et la relation avec 'categories'.
 */
public class ProductDAO {

    private final Connection conn;

    public ProductDAO() {
        this.conn = DatabaseConnection.getConnection();
    }

    /**
     * Récupère tous les produits avec leur catégorie.
     */
    public List<Product> findAll() {
        List<Product> products = new ArrayList<>();
        String sql = "SELECT p.*, c.id AS c_id, c.name AS c_name, c.description AS c_description " +
                     "FROM products p LEFT JOIN categories c ON p.category_id = c.id " +
                     "ORDER BY p.id_product DESC";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                products.add(mapProduct(rs));
            }
        } catch (SQLException e) {
            System.err.println("Erreur findAll products : " + e.getMessage());
        }
        return products;
    }

    public Product findById(int id) {
        String sql = "SELECT p.*, c.id AS c_id, c.name AS c_name, c.description AS c_description " +
                     "FROM products p LEFT JOIN categories c ON p.category_id = c.id " +
                     "WHERE p.id_product = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapProduct(rs);
        } catch (SQLException e) {
            System.err.println("Erreur findById product : " + e.getMessage());
        }
        return null;
    }

    public Product findByName(String name) {
        String sql = "SELECT p.*, c.id AS c_id, c.name AS c_name, c.description AS c_description " +
                     "FROM products p LEFT JOIN categories c ON p.category_id = c.id " +
                     "WHERE p.name = ? LIMIT 1";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapProduct(rs);
        } catch (SQLException e) {
            System.err.println("Erreur findByName : " + e.getMessage());
        }
        return null;
    }

    /**
     * Ajoute un produit. L'id généré est mis à jour dans l'objet.
     */
    public boolean save(Product product) {
        String sql = "INSERT INTO products (name, description, price, stock, image, category_id, created_at) " +
                     "VALUES (?, ?, ?, ?, ?, ?, NOW())";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, product.getName());
            ps.setString(2, product.getDescription());
            ps.setDouble(3, product.getPrice());
            ps.setInt(4, product.getStock());
            ps.setString(5, product.getImage());
            if (product.getCategory() != null) {
                ps.setInt(6, product.getCategory().getId());
            } else {
                ps.setNull(6, Types.INTEGER);
            }
            int rows = ps.executeUpdate();
            if (rows > 0) {
                ResultSet rs = ps.getGeneratedKeys();
                if (rs.next()) product.setIdProduct(rs.getInt(1));
            }
            return rows > 0;
        } catch (SQLException e) {
            System.err.println("Erreur save product : " + e.getMessage());
            return false;
        }
    }

    public boolean update(Product product) {
        String sql = "UPDATE products SET name=?, description=?, price=?, stock=?, image=?, category_id=? " +
                     "WHERE id_product=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, product.getName());
            ps.setString(2, product.getDescription());
            ps.setDouble(3, product.getPrice());
            ps.setInt(4, product.getStock());
            ps.setString(5, product.getImage());
            if (product.getCategory() != null) {
                ps.setInt(6, product.getCategory().getId());
            } else {
                ps.setNull(6, Types.INTEGER);
            }
            ps.setInt(7, product.getIdProduct());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Erreur update product : " + e.getMessage());
            return false;
        }
    }
    
    public boolean updateStock(int productId, int newStock) {
        String sql = "UPDATE products SET stock = ? WHERE id_product = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, newStock);
            ps.setInt(2, productId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Erreur ProductDAO updateStock : " + e.getMessage());
            return false;
        }
    }

    public boolean delete(int id) {
        String sql = "DELETE FROM products WHERE id_product=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Erreur delete product : " + e.getMessage());
            return false;
        }
    }

    private Product mapProduct(ResultSet rs) throws SQLException {
        Product product = new Product(
                rs.getInt("id_product"),
                rs.getString("name"),
                rs.getString("description"),
                rs.getString("image"),
                rs.getDouble("price"),
                rs.getInt("stock")
        );
        Timestamp created = rs.getTimestamp("created_at");
        if (created != null) product.setCreatedAt(created.toLocalDateTime());

        int catId = rs.getInt("c_id");
        if (!rs.wasNull()) {
            Category category = new Category(catId, rs.getString("c_name"), rs.getString("c_description"));
            product.setCategory(category);
        }
        return product;
    }
}