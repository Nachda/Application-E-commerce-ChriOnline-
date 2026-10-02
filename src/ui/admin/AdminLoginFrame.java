package ui.admin;

import client.AppSession;
import client.ClientSocketService;
import security.KeystoreManager;
import security.Signer;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.File;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.util.Arrays;
import java.util.Base64;

public class AdminLoginFrame extends JFrame {

    private final ClientSocketService clientService;
    private final JFrame backFrame;

    private JTextField emailField;
    private JPasswordField keystorePasswordField;
    private JButton requestChallengeButton;
    private JButton signAndSendButton;
    private JLabel statusLabel;
    private JTextField keystorePathField;

    private PrivateKey adminPrivateKey;
    private Certificate adminCertificate;
    private String currentChallenge;
    private String adminEmail;

    private String keystorePath = "admin.p12"; // valeur par défaut

    private int failedAttempts = 0;
    private static final int MAX_ATTEMPTS = 3;
    private Timer securityTimer;

    public AdminLoginFrame(ClientSocketService clientService, JFrame backFrame) {
        this.clientService = clientService;
        this.backFrame = backFrame;
        initUI();
        attachListeners();
    }

    private void initUI() {
        setTitle("🔐 ChriOnline - Connexion Admin");
        setSize(600, 520);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        JPanel root = new JPanel(new GridBagLayout());
        root.setBackground(new Color(30, 30, 30));

        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(new Color(45, 45, 45));
        card.setBorder(BorderFactory.createEmptyBorder(30, 40, 30, 40));

        JLabel title = new JLabel("🔐 Connexion Admin RSA");
        title.setForeground(Color.WHITE);
        title.setFont(new Font("SansSerif", Font.BOLD, 20));
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel subtitle = new JLabel("Authentification PKI sécurisée");
        subtitle.setForeground(Color.LIGHT_GRAY);
        subtitle.setFont(new Font("SansSerif", Font.PLAIN, 12));
        subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Email
        emailField = new JTextField();
        emailField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        emailField.setBorder(BorderFactory.createTitledBorder("Email Admin"));

        // Sélecteur de fichier keystore
        JPanel keystoreFilePanel = new JPanel(new BorderLayout(5, 0));
        keystoreFilePanel.setOpaque(false);
        keystoreFilePanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        keystoreFilePanel.setBorder(BorderFactory.createTitledBorder("Fichier Keystore"));

        keystorePathField = new JTextField("admin.p12");
        keystorePathField.setEditable(false);
        keystorePathField.setBackground(new Color(60, 60, 60));
        keystorePathField.setForeground(Color.WHITE);
        keystorePathField.setCaretColor(Color.WHITE);

        JButton browseBtn = new JButton("📁");
        browseBtn.setFocusPainted(false);
        browseBtn.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser(new File("."));
            chooser.setFileFilter(new FileNameExtensionFilter("Keystore PKCS12 (*.p12)", "p12"));
            if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                keystorePath = chooser.getSelectedFile().getPath();
                keystorePathField.setText(chooser.getSelectedFile().getName());
                adminPrivateKey = null;
                adminCertificate = null;
                requestChallengeButton.setEnabled(false);
                signAndSendButton.setEnabled(false);
                statusLabel.setText("Nouveau fichier sélectionné. Entrez le mot de passe.");
                statusLabel.setForeground(Color.YELLOW);
            }
        });

        keystoreFilePanel.add(keystorePathField, BorderLayout.CENTER);
        keystoreFilePanel.add(browseBtn, BorderLayout.EAST);

        // Mot de passe
        keystorePasswordField = new JPasswordField();
        keystorePasswordField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        keystorePasswordField.setBorder(BorderFactory.createTitledBorder("Mot de passe Keystore"));

        // Boutons
        requestChallengeButton = new JButton("Demander Challenge");
        requestChallengeButton.setEnabled(false);

        signAndSendButton = new JButton("Signer et Envoyer");
        signAndSendButton.setEnabled(false);

        statusLabel = new JLabel(" ");
        statusLabel.setForeground(Color.RED);
        statusLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Assemblage
        card.add(title);
        card.add(Box.createVerticalStrut(5));
        card.add(subtitle);
        card.add(Box.createVerticalStrut(20));
        card.add(emailField);
        card.add(Box.createVerticalStrut(12));
        card.add(keystoreFilePanel);
        card.add(Box.createVerticalStrut(12));
        card.add(keystorePasswordField);
        card.add(Box.createVerticalStrut(20));
        card.add(requestChallengeButton);
        card.add(Box.createVerticalStrut(10));
        card.add(signAndSendButton);
        card.add(Box.createVerticalStrut(15));
        card.add(statusLabel);

        root.add(card);
        add(root);
    }

    private void attachListeners() {
        keystorePasswordField.addActionListener(e -> tryUnlock());
        requestChallengeButton.addActionListener(e -> requestChallenge());
        signAndSendButton.addActionListener(e -> signAndSend());
    }

    private void tryUnlock() {
        if (failedAttempts >= MAX_ATTEMPTS) {
            statusLabel.setText("❌ Trop de tentatives - Application bloquée");
            keystorePasswordField.setEnabled(false);
            return;
        }

        char[] password = keystorePasswordField.getPassword();
        String email = emailField.getText().trim();

        if (password.length == 0 || email.isEmpty())
            return;

        // Générer l'alias à partir de l'email (partie avant le @)
        String alias = email.contains("@") ? email.substring(0, email.indexOf("@")) : email;
        // Nettoyer : garder seulement lettres, chiffres, tirets, points, underscores
        alias = alias.replaceAll("[^a-zA-Z0-9.\\-_]", "_");

        adminPrivateKey = KeystoreManager.loadPrivateKeyFromKeystore(
                keystorePath, password.clone(), alias, password.clone());

        adminCertificate = KeystoreManager.loadCertificate(
                keystorePath, password.clone(), alias);

        Arrays.fill(password, '\0');

        if (adminPrivateKey != null && adminCertificate != null) {
            statusLabel.setText("✅ Keystore déverrouillé");
            statusLabel.setForeground(Color.GREEN);
            requestChallengeButton.setEnabled(true);
            startSecurityTimeout();
        } else {
            failedAttempts++;
            statusLabel.setText("❌ Mot de passe incorrect (" 
                    + failedAttempts + "/" + MAX_ATTEMPTS + ")");
            statusLabel.setForeground(Color.RED);
        }
    }

    private void startSecurityTimeout() {
        if (securityTimer != null)
            securityTimer.stop();

        securityTimer = new Timer(300000, e -> {
            adminPrivateKey = null;
            adminCertificate = null;
            requestChallengeButton.setEnabled(false);
            signAndSendButton.setEnabled(false);
            statusLabel.setText("⏰ Session expirée");
            statusLabel.setForeground(Color.ORANGE);
        });

        securityTimer.setRepeats(false);
        securityTimer.start();
    }

    private void requestChallenge() {
        if (!clientService.connect()) {
            statusLabel.setText("❌ Serveur inaccessible");
            return;
        }

        String email = emailField.getText().trim();
        String response = clientService.sendRequest("ADMIN_LOGIN_REQUEST:" + email);

        System.out.println("✅ Réponse serveur : " + response);

        if (response.startsWith("ADMIN_CHALLENGE:")) {
            currentChallenge = response.substring("ADMIN_CHALLENGE:".length());
            adminEmail = email;
            signAndSendButton.setEnabled(true);
            statusLabel.setText("✅ Challenge reçu");
        } else {
            statusLabel.setText("❌ " + response);
        }
    }

    private void signAndSend() {
        try {
            byte[] signatureBytes = Signer.sign(currentChallenge, adminPrivateKey);
            String signatureBase64 = Base64.getEncoder().encodeToString(signatureBytes);

            String certBase64 = Base64.getEncoder().encodeToString(adminCertificate.getEncoded());

            String request = "ADMIN_LOGIN_VERIFY:" + adminEmail + ":" 
                    + signatureBase64 + ":" + certBase64;

            String response = clientService.sendRequest(request);

            System.out.println("✅ Réponse serveur : " + response);

            if (response.startsWith("ADMIN_LOGIN_SUCCESS:")) {
                String[] parts = response.split(":");

                int adminId = Integer.parseInt(parts[1]);
                String role = parts[2];

                AppSession adminSession = new AppSession();
                adminSession.setClientId(adminId);
                adminSession.setRole(role);
                adminSession.setFullName(adminEmail);

                if (parts.length >= 4) {
                    String token = parts[3];
                    adminSession.setSessionToken(token);
                    clientService.setSessionToken(token);
                }

                JOptionPane.showMessageDialog(this,
                        "✅ Connexion réussie",
                        "Succès",
                        JOptionPane.INFORMATION_MESSAGE);

                dispose();
                new AdminMainFrame(clientService, adminSession).setVisible(true);

            } else {
                statusLabel.setText("❌ Signature invalide");
            }

        } catch (Exception e) {
            statusLabel.setText("❌ Erreur signature");
            e.printStackTrace();
        }
    }
}