package ui.client;

import client.AppSession;
import client.ClientSocketService;
import utils.LanguageManager;
import utils.UITheme;

import javax.swing.*;
import java.awt.*;

/**
 * Fenêtre de modification du profil client.
 */
public class EditProfileFrame extends JFrame {

    private final ClientSocketService clientService;
    private final AppSession session;
    private final ProfileFrame parent;

    private JTextField nameField;
    private JTextField emailField;
    private JTextField phoneField;
    private JTextField addressField;
    private JTextField cityField;

    public EditProfileFrame(ClientSocketService clientService, AppSession session, ProfileFrame parent) {
        this.clientService = clientService;
        this.session = session;
        this.parent = parent;
        initUI();
        loadCurrentData();
    }

    private void initUI() {
        setTitle("✏️ " + LanguageManager.getInstance().getText("profile.edit.title"));
        setSize(500, 400);
        setLocationRelativeTo(parent);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        JPanel root = UITheme.darkPanel();
        root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
        root.setBorder(BorderFactory.createEmptyBorder(20, 30, 20, 30));

        nameField = UITheme.textField();
        nameField.setBorder(UITheme.titledBorder(LanguageManager.getInstance().getText("profile.name")));

        emailField = UITheme.textField();
        emailField.setBorder(UITheme.titledBorder(LanguageManager.getInstance().getText("profile.email")));

        phoneField = UITheme.textField();
        phoneField.setBorder(UITheme.titledBorder(LanguageManager.getInstance().getText("profile.phone")));

        addressField = UITheme.textField();
        addressField.setBorder(UITheme.titledBorder(LanguageManager.getInstance().getText("profile.address")));

        cityField = UITheme.textField();
        cityField.setBorder(UITheme.titledBorder(LanguageManager.getInstance().getText("profile.city")));

        JButton saveBtn = UITheme.primaryButton(LanguageManager.getInstance().getText("profile.save"));
        JButton cancelBtn = UITheme.dangerButton(LanguageManager.getInstance().getText("profile.cancel"));

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
        btnPanel.setOpaque(false);
        btnPanel.add(saveBtn);
        btnPanel.add(cancelBtn);

        root.add(nameField);
        root.add(Box.createVerticalStrut(10));
        root.add(emailField);
        root.add(Box.createVerticalStrut(10));
        root.add(phoneField);
        root.add(Box.createVerticalStrut(10));
        root.add(addressField);
        root.add(Box.createVerticalStrut(10));
        root.add(cityField);
        root.add(Box.createVerticalStrut(20));
        root.add(btnPanel);

        setContentPane(root);

        saveBtn.addActionListener(e -> saveProfile());
        cancelBtn.addActionListener(e -> dispose());
    }

    private void loadCurrentData() {
        String response = clientService.getProfile(session.getClientId());
        if (response != null && response.startsWith("PROFILE_DATA:")) {
            String data = response.substring("PROFILE_DATA:".length());
            String[] parts = data.split(";");
            if (parts.length >= 5) {
                nameField.setText(parts[0]);
                emailField.setText(parts[1]);
                phoneField.setText(parts[2]);
                addressField.setText(parts[3]);
                cityField.setText(parts[4]);
            }
        }
    }

    private void saveProfile() {
        String fullName = nameField.getText().trim();
        String email = emailField.getText().trim();
        String phone = phoneField.getText().trim();
        String address = addressField.getText().trim();
        String city = cityField.getText().trim();

        if (fullName.isEmpty() || email.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Le nom et l'email sont obligatoires.", "Erreur", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String response = clientService.updateProfile(session.getClientId(), fullName, email, phone, address, city);

        if ("UPDATE_PROFILE_SUCCESS".equals(response)) {
            session.setFullName(fullName);
            JOptionPane.showMessageDialog(this, LanguageManager.getInstance().getText("profile.update.success"));
            parent.refreshProfile();
            dispose();
        } else {
            JOptionPane.showMessageDialog(this, LanguageManager.getInstance().getText("profile.update.error") + " : " + response, "Erreur", JOptionPane.ERROR_MESSAGE);
        }
    }
}