package model;

/**
 * Représente un administrateur.
 * Hérite de User avec le rôle 'admin'.
 * Stocke également la clé publique RSA pour l'authentification challenge-response.
 */
public class Admin extends User {

    //private String publicKey; // clé publique encodée en Base64

    public Admin(String nom, String prenom, String email, String password) {
        super(nom, prenom, email, password, "admin");
    }

    /*public String getPublicKey() { return publicKey; }
    public void setPublicKey(String publicKey) { this.publicKey = publicKey; }*/

    @Override
    public boolean login(String email, String password) {
        // L'authentification réelle passera par challenge-response RSA, pas ici.
        return false;
    }

    @Override
    public void logout() {
        System.out.println("Admin " + getNom() + " déconnecté.");
    }
}