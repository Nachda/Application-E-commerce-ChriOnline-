package client;

import ui.admin.AdminLoginFrame;
import javax.swing.SwingUtilities;

/**
 * Lanceur de l'application admin (autonome).
 */
public class AdminMain {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            ClientSocketService clientService = new ClientSocketService();
            // backFrame = null car c'est la première fenêtre
            AdminLoginFrame loginFrame = new AdminLoginFrame(clientService, null);
            loginFrame.setVisible(true);
        });
    }
}