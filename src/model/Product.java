package model;

import java.time.LocalDateTime;

/**
 * Représente un produit.
 * Mappe la table 'products'.
 */
public class Product {
    private int idProduct;
    private String name;
    private String description;
    private String image; // chemin vers l'image
    private double price;
    private int stock;
    private LocalDateTime createdAt;
    private Category category;

    public Product(int idProduct, String name, String description, String image,
                   double price, int stock) {
        this.idProduct = idProduct;
        this.name = name;
        this.description = description;
        this.image = image;
        this.price = price;
        this.stock = stock;
        this.createdAt = LocalDateTime.now();
    }

    // Getters & setters
    public int getIdProduct() { return idProduct; }
    public void setIdProduct(int idProduct) { this.idProduct = idProduct; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getImage() { return image; }
    public void setImage(String image) { this.image = image; }

    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }

    public int getStock() { return stock; }
    public void setStock(int stock) { this.stock = stock; }

    public Category getCategory() { return category; }
    public void setCategory(Category category) { this.category = category; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}