package model;

import java.util.Date;

/**
 * Classe abstraite représentant un utilisateur (client ou admin).
 * Mappe la table 'users'.
 */
public abstract class User {
    private int id;
    private String nom;
    private String prenom;
    private String email;
    private String password;  // stocké haché (PBKDF2 côté serveur)
    private String role;      // "admin" ou "client"
    private String status;    // "active", "pending", etc.
    private Date createdAt;

    public User(String nom, String prenom, String email, String password, String role) {
        this.nom = nom;
        this.prenom = prenom;
        this.email = email;
        this.password = password;
        this.role = role;
        this.status = "active";
        this.createdAt = new Date();
    }

    // Méthodes abstraites (seront implémentées par les sous-classes si nécessaire)
    public abstract boolean login(String email, String password);
    public abstract void logout();

    // Getters et Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public String getPrenom() { return prenom; }
    public void setPrenom(String prenom) { this.prenom = prenom; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }

    @Override
    public String toString() {
        return "User{id=" + id + ", nom='" + nom + "', prenom='" + prenom +
                "', email='" + email + "', role='" + role + "', status='" + status + "'}";
    }
}