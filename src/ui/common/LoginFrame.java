package ui.common;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagLayout;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;

import client.AppSession;
import client.ClientSocketService;
import ui.admin.AdminLoginFrame;   
import ui.client.ShopFrame;
import utils.LanguageManager;
import utils.UITheme;

public class LoginFrame extends JFrame {

    private final ClientSocketService clientService;
    private JTextField emailField;
    private JPanel passwordPanel;
    private JLabel statusLabel;

    public LoginFrame(ClientSocketService clientService) {
        this.clientService = clientService;
        initUI();
    }

    private void initUI() {
        setTitle(LanguageManager.getInstance().getText("login.title"));
        setSize(800, 750);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setResizable(false);

        JPanel root = UITheme.darkPanel();
        root.setLayout(new GridBagLayout());

        JPanel card = UITheme.cardPanel();
        card.setPreferredSize(new Dimension(480, 520));
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UITheme.BORDER, 1, true),
                new EmptyBorder(32, 42, 32, 42)
        ));

        JLabel icon = new JLabel("🛒");
        icon.setAlignmentX(Component.CENTER_ALIGNMENT);
        icon.setFont(new Font("SansSerif", Font.PLAIN, 58));
        icon.setForeground(UITheme.GOLD);

        JLabel title = new JLabel(LanguageManager.getInstance().getText("login.title"));
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        title.setForeground(Color.WHITE);
        title.setFont(new Font("SansSerif", Font.BOLD, 32));

        JLabel subtitle = new JLabel(LanguageManager.getInstance().getText("login.subtitle"));
        subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        subtitle.setForeground(UITheme.MUTED);
        subtitle.setFont(new Font("SansSerif", Font.PLAIN, 13));

        emailField = UITheme.textField();
        emailField.setMaximumSize(new Dimension(340, 52));
        emailField.setBorder(UITheme.titledBorder(LanguageManager.getInstance().getText("login.email")));

        passwordPanel = UITheme.createPasswordFieldWithEye(LanguageManager.getInstance().getText("login.password"));
        passwordPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        passwordPanel.setMaximumSize(new Dimension(340, 60));

        statusLabel = new JLabel(" ");
        statusLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        statusLabel.setForeground(UITheme.RED);
        statusLabel.setFont(UITheme.smallFont());

        JButton loginBtn = UITheme.primaryButton("🔐 " + LanguageManager.getInstance().getText("login.button"));
        loginBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        loginBtn.setMaximumSize(new Dimension(340, 48));

        JButton registerBtn = UITheme.blueButton("📝 " + LanguageManager.getInstance().getText("login.register"));
        registerBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        registerBtn.setMaximumSize(new Dimension(340, 48));

        JButton adminBtn = UITheme.goldButton("🔐 CONNEXION ADMIN (RSA)");
        adminBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        adminBtn.setMaximumSize(new Dimension(340, 48));

        card.add(icon);
        card.add(Box.createVerticalStrut(10));
        card.add(title);
        card.add(Box.createVerticalStrut(6));
        card.add(subtitle);
        card.add(Box.createVerticalStrut(28));
        card.add(emailField);
        card.add(Box.createVerticalStrut(14));
        card.add(passwordPanel);
        card.add(Box.createVerticalStrut(10));
        card.add(statusLabel);
        card.add(Box.createVerticalStrut(18));
        card.add(loginBtn);
        card.add(Box.createVerticalStrut(12));
        card.add(registerBtn);
        card.add(Box.createVerticalStrut(12));
        card.add(adminBtn);

        JScrollPane scrollPane = new JScrollPane(card);
        scrollPane.setBorder(null);
        root.add(scrollPane);
        setContentPane(root);

        loginBtn.addActionListener(e -> doLogin());
        registerBtn.addActionListener(e -> {
            setVisible(false);
            new RegisterFrame(clientService, this).setVisible(true);
        });
        adminBtn.addActionListener(e -> {
            setVisible(false);
            new AdminLoginFrame(clientService, this).setVisible(true);
        });
    }

    private void doLogin() {
        String email = emailField.getText().trim();
        JPasswordField passField = UITheme.getPasswordFieldFromPanel(passwordPanel);
        String password = new String(passField.getPassword()).trim();

        if (email.isEmpty() || password.isEmpty()) {
            statusLabel.setText(LanguageManager.getInstance().getText("login.error.empty"));
            return;
        }

        if (!clientService.connect()) {
            statusLabel.setText(LanguageManager.getInstance().getText("login.error.server"));
            return;
        }

        String response = clientService.login(email, password);

        if (response != null && response.startsWith("LOGIN_SUCCESS")) {
            String[] parts = response.split(":");
            AppSession session = new AppSession();
            session.setClientId(Integer.parseInt(parts[1]));
            session.setRole(parts[2]);

            // Récupération du token de session
            if (parts.length >= 4) {
                String token = parts[3];
                session.setSessionToken(token);
                clientService.setSessionToken(token);
            }

            // Récupération du profil pour le nom complet
            String prof = clientService.getProfile(session.getClientId());
            if (prof != null && prof.startsWith("PROFILE_DATA:")) {
                String data = prof.substring("PROFILE_DATA:".length());
                String[] fields = data.split(";");
                if (fields.length > 0) session.setFullName(fields[0]);
            }

            dispose();
            new ShopFrame(clientService, session).setVisible(true);
        } else if ("ERROR:ACCOUNT_NOT_ACTIVE".equals(response)) {
            setVisible(false);
            new OtpFrame(clientService, email, this).setVisible(true);
        } else if ("ERROR:BRUTE_FORCE_BLOCKED".equals(response)) {
            statusLabel.setText("⛔ Compte temporairement bloqué. Réessayez dans 5 minutes.");
            JOptionPane.showMessageDialog(this,
                    "Trop de tentatives échouées.\nVotre compte est verrouillé pour 5 minutes.",
                    "Sécurité", JOptionPane.WARNING_MESSAGE);
        } else {
            statusLabel.setText(LanguageManager.getInstance().getText("login.error.invalid"));
        }
    }
}