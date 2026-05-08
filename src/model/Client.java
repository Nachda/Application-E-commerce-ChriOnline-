package model;

/**
 * Représente un client.
 * Hérite de User avec le rôle 'client'.
 * Contient les informations spécifiques au client (adresse, téléphone, ville).
 */
public class Client extends User {

    private String address;
    private String phone;
    private String ville;

    public Client(String nom, String prenom, String email, String password,
                  String address, String phone, String ville) {
        super(nom, prenom, email, password, "client");
        this.address = address;
        this.phone = phone;
        this.ville = ville;
    }

    @Override
    public boolean login(String email, String password) {
        // L'authentification réelle utilise AuthService et ne passe pas par cette méthode.
        return false;
    }

    @Override
    public void logout() {
        System.out.println("Client " + getNom() + " déconnecté.");
    }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getVille() { return ville; }
    public void setVille(String ville) { this.ville = ville; }
}