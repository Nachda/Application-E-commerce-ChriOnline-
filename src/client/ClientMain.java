package client;

import ui.common.LoginFrame;

/**
 * Point d'entrée de l'application cliente (interface graphique).
 */
public class ClientMain {
    public static void main(String[] args) {
        // Créer le service de communication (sans encore se connecter)
        ClientSocketService clientService = new ClientSocketService();

        // Lancer la fenêtre de login (elle appellera clientService.connect() au moment du login)
        LoginFrame loginFrame = new LoginFrame(clientService);
        loginFrame.setVisible(true);
    }
}