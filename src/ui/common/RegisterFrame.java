package ui.common;

import client.ClientSocketService;
import utils.PasswordValidator;
import utils.PasswordValidator.ValidationResult;
import utils.PasswordValidator.PasswordStrength;

import ui.common.OtpFrame; 

import utils.UITheme; 
import utils.LanguageManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.util.List;

public class RegisterFrame extends JFrame {

    private final ClientSocketService clientService;
    private final JFrame backFrame;

    private JTextField nomField;
    private JTextField prenomField;
    private JTextField emailField;
    private JPanel passwordPanel;
    private JPanel confirmPasswordPanel;
    private JTextField addressField;
    private JTextField phoneField;
    private JTextField villeField;
    private JLabel statusLabel;

    private JLabel strengthLabel;
    private JPanel requirementsPanel;
    
    
    public RegisterFrame(ClientSocketService clientService, JFrame backFrame) {
        this.clientService = clientService;
        this.backFrame = backFrame;
        initUI();
    }

    private void initUI() {
        setTitle("📝 ChriOnline - Inscription");
        setSize(800, 900);  // ⭐ HAUTEUR RÉDUITE (scrollable)
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setResizable(true);

        JPanel root = UITheme.darkPanel();
        root.setLayout(new GridBagLayout());

        JPanel card = UITheme.cardPanel();
        card.setPreferredSize(new Dimension(500, 1050));  // ⭐ GRANDE HAUTEUR INTERNE
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UITheme.BORDER, 1, true),
                new EmptyBorder(22, 35, 22, 35)
        ));

        JLabel title = new JLabel("📝 Créer un compte");
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        title.setForeground(Color.WHITE);
        title.setFont(new Font("SansSerif", Font.BOLD, 26));

        JLabel subtitle = new JLabel("Inscription client");
        subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        subtitle.setForeground(UITheme.MUTED);
        subtitle.setFont(UITheme.normalFont());

        nomField = createStyledTextField("👤 Nom");
        prenomField = createStyledTextField("👤 Prénom");
        emailField = createStyledTextField("📧 Email");

        passwordPanel = UITheme.createPasswordFieldWithEye("🔒 Mot de passe");
        passwordPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        passwordPanel.setMaximumSize(new Dimension(340, 60));
        passwordPanel.setPreferredSize(new Dimension(340, 60));

        strengthLabel = new JLabel(" ");
        strengthLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        strengthLabel.setFont(new Font("SansSerif", Font.BOLD, 13));

        requirementsPanel = createRequirementsPanel();
        
        
        confirmPasswordPanel = UITheme.createPasswordFieldWithEye("🔒 Confirmer le mot de passe");
        confirmPasswordPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        confirmPasswordPanel.setMaximumSize(new Dimension(340, 60));
        confirmPasswordPanel.setPreferredSize(new Dimension(340, 60));

        addressField = createStyledTextField("🏠 Adresse");
        phoneField = createStyledTextField("📱 Téléphone");
        villeField = createStyledTextField("🏙️ Ville");

        statusLabel = new JLabel(" ");
        statusLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        statusLabel.setForeground(UITheme.RED);
        statusLabel.setFont(UITheme.smallFont());

        JButton registerBtn = UITheme.primaryButton("✅ S'INSCRIRE");
        registerBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        registerBtn.setMaximumSize(new Dimension(340, 45));
        registerBtn.setPreferredSize(new Dimension(340, 45));

        JButton backBtn = UITheme.blueButton("← RETOUR");
        backBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        backBtn.setMaximumSize(new Dimension(340, 45));
        backBtn.setPreferredSize(new Dimension(340, 45));

        // ⭐⭐⭐ ASSEMBLAGE DU CONTENU ⭐⭐⭐
        card.add(title);
        card.add(Box.createVerticalStrut(6));
        card.add(subtitle);
        card.add(Box.createVerticalStrut(18));
        card.add(nomField);
        card.add(Box.createVerticalStrut(10));
        card.add(prenomField);
        card.add(Box.createVerticalStrut(10));
        card.add(emailField);
        card.add(Box.createVerticalStrut(10));
        card.add(passwordPanel);
        card.add(Box.createVerticalStrut(6));
        card.add(strengthLabel); 
        card.add(Box.createVerticalStrut(8));
        card.add(requirementsPanel); 
        card.add(Box.createVerticalStrut(10));
        card.add(confirmPasswordPanel);
        card.add(Box.createVerticalStrut(10));
        card.add(addressField);
        card.add(Box.createVerticalStrut(10));
        card.add(phoneField);
        card.add(Box.createVerticalStrut(10));
        card.add(villeField);
        card.add(Box.createVerticalStrut(12));
        card.add(statusLabel);
        card.add(Box.createVerticalStrut(16));
        card.add(registerBtn);
        card.add(Box.createVerticalStrut(12));
        card.add(backBtn);

        // ⭐⭐⭐ NOUVEAU : SCROLL PANE ⭐⭐⭐
        JScrollPane scrollPane = new JScrollPane(card);
        scrollPane.setBackground(UITheme.BG);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;

        root.add(scrollPane, gbc);   
        setContentPane(root);
        
        // ⭐ Listener pour validation en temps réel
        JPasswordField passwordField = UITheme.getPasswordFieldFromPanel(passwordPanel);
        passwordField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) { updatePasswordStrength(); }
            @Override
            public void removeUpdate(DocumentEvent e) { updatePasswordStrength(); }
            @Override
            public void changedUpdate(DocumentEvent e) { updatePasswordStrength(); }
        });

        registerBtn.addActionListener(e -> register());
        backBtn.addActionListener(e -> {
            backFrame.setVisible(true);
            dispose();
        });
    }

    private JTextField createStyledTextField(String title) {
        JTextField field = UITheme.textField();
        field.setMaximumSize(new Dimension(340, 48));
        field.setPreferredSize(new Dimension(340, 48));
        field.setBorder(UITheme.titledBorder(title));
        return field;
    }

    // ⭐ NOUVEAU : Panel des exigences du mot de passe
    private JPanel createRequirementsPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(UITheme.CARD);
        panel.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.setMaximumSize(new Dimension(340, 140));
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UITheme.BORDER, 1, true),
                new EmptyBorder(8, 12, 8, 12)
        ));

        JLabel header = new JLabel("📋 Exigences du mot de passe :");
        header.setForeground(Color.WHITE);
        header.setFont(new Font("SansSerif", Font.BOLD, 11));
        panel.add(header);
        panel.add(Box.createVerticalStrut(6));

        List<String> requirements = PasswordValidator.getRequirementsList();
        for (String req : requirements) {
            JLabel label = new JLabel("❌ " + req);
            label.setForeground(UITheme.MUTED);
            label.setFont(new Font("SansSerif", Font.PLAIN, 10));
            label.setName(req); // Pour mise à jour dynamique
            panel.add(label);
            panel.add(Box.createVerticalStrut(3));
        }

        return panel;
    }

    // ⭐ NOUVEAU : Mise à jour en temps réel
    private void updatePasswordStrength() {
        JPasswordField passwordField = UITheme.getPasswordFieldFromPanel(passwordPanel);
        String password = new String(passwordField.getPassword());

        ValidationResult result = PasswordValidator.validate(password);
        PasswordStrength strength = result.getStrength();

        // Mettre à jour l'indicateur de force
        strengthLabel.setText(strength.getIcon() + " " + strength.getLabel());
        switch (strength) {
            case WEAK:
                strengthLabel.setForeground(UITheme.RED);
                break;
            case MEDIUM:
                strengthLabel.setForeground(Color.ORANGE);
                break;
            case STRONG:
                strengthLabel.setForeground(Color.YELLOW);
                break;
            case VERY_STRONG:
                strengthLabel.setForeground(Color.GREEN);
                break;
        }

        // Mettre à jour les checkmarks
        updateRequirementsChecks(password);
    }

    // ⭐ NOUVEAU : Met à jour les ✅/❌ pour chaque exigence
    private void updateRequirementsChecks(String password) {
        Component[] components = requirementsPanel.getComponents();
        for (Component comp : components) {
            if (comp instanceof JLabel) {
                JLabel label = (JLabel) comp;
                String name = label.getName();
                if (name != null) {
                    boolean satisfied = false;
                    
                    if (name.contains("8 caractères")) {
                        satisfied = PasswordValidator.hasMinLength(password);
                    } else if (name.contains("majuscule")) {
                        satisfied = PasswordValidator.hasUpperCase(password);
                    } else if (name.contains("minuscule")) {
                        satisfied = PasswordValidator.hasLowerCase(password);
                    } else if (name.contains("chiffre")) {
                        satisfied = PasswordValidator.hasDigit(password);
                    } else if (name.contains("spécial")) {
                        satisfied = PasswordValidator.hasSpecialChar(password);
                    }

                    String icon = satisfied ? "✅" : "❌";
                    String text = name;
                    label.setText(icon + " " + text);
                    label.setForeground(satisfied ? Color.GREEN : UITheme.MUTED);
                }
            }
        }
    }
    
    
    
    private void register() {
        String nom = nomField.getText().trim();
        String prenom = prenomField.getText().trim();
        String email = emailField.getText().trim();

        JPasswordField passwordField = UITheme.getPasswordFieldFromPanel(passwordPanel);
        JPasswordField confirmPasswordField = UITheme.getPasswordFieldFromPanel(confirmPasswordPanel);

        String password = new String(passwordField.getPassword()).trim();
        String confirmPassword = new String(confirmPasswordField.getPassword()).trim();

        String address = addressField.getText().trim();
        String phone = phoneField.getText().trim();
        String ville = villeField.getText().trim();

        // Vérification : tous les champs remplis
        if (nom.isEmpty() || prenom.isEmpty() || email.isEmpty() || password.isEmpty()
                || address.isEmpty() || phone.isEmpty() || ville.isEmpty()) {
            statusLabel.setText("Veuillez remplir tous les champs.");
            return;
        }

        // Vérification : email valide
        if (!email.contains("@") || !email.contains(".")) {
            statusLabel.setText("Email invalide.");
            return;
        }

        // ⭐ VALIDATION DU MOT DE PASSE
        ValidationResult validation = PasswordValidator.validate(password);
        if (!validation.isValid()) {
            String errors = String.join("\n", validation.getErrors());
            JOptionPane.showMessageDialog(this,
                    "❌ Mot de passe non conforme :\n\n" + errors,
                    "Mot de passe invalide",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Vérification : confirmation du mot de passe
        if (!password.equals(confirmPassword)) {
            statusLabel.setText("Les mots de passe ne correspondent pas.");
            passwordField.setText("");
            confirmPasswordField.setText("");
            return;
        }

        // Vérification : connexion au serveur
        if (!clientService.connect()) {
            statusLabel.setText("Serveur inaccessible.");
            return;
        }

        // ⭐ Le hashage se fait maintenant dans ClientSocketService.register()
        String response = clientService.register(
                nom, prenom, email, password, address, phone, ville
        );

        if ("REGISTER_SUCCESS_OTP_SENT".equals(response)) {
            JOptionPane.showMessageDialog(this,
                    "✅ Compte créé.\nUn code OTP a été envoyé à votre email.",
                    "Vérification requise", JOptionPane.INFORMATION_MESSAGE);

            setVisible(false);
            new OtpFrame(clientService, email, backFrame).setVisible(true);
            dispose();

        } else if ("ERROR:EMAIL_ALREADY_EXISTS".equals(response)) {
            statusLabel.setText("Cet email est déjà utilisé.");
        } else if ("REGISTER_SUCCESS_BUT_OTP_FAILED".equals(response)) {
            JOptionPane.showMessageDialog(this,
                    "Compte créé, mais l'envoi du code a échoué.\nEssayez de vous reconnecter puis renvoyez le code.",
                    "Attention", JOptionPane.WARNING_MESSAGE);

            setVisible(false);
            new OtpFrame(clientService, email, backFrame).setVisible(true);
            dispose();
        } else {
            statusLabel.setText("Erreur : " + response);
        }
    }
}