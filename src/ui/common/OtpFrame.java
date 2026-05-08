package ui.common;

import client.ClientSocketService;
import utils.UITheme;
import utils.LanguageManager;

import javax.swing.*;
import java.awt.*;

public class OtpFrame extends JFrame {

    private final ClientSocketService clientService;
    private final String email;
    private final JFrame backFrame;  // LoginFrame

    private JTextField codeField;
    private JLabel statusLabel;
    private JButton verifyBtn;
    private JButton resendBtn;
    private JButton backBtn;
    private boolean waitingForResponse = false;  // verrou anti-double clic

    public OtpFrame(ClientSocketService clientService, String email, JFrame backFrame) {
        this.clientService = clientService;
        this.email = email;
        this.backFrame = backFrame;
        initUI();
    }

    private void initUI() {
        setTitle("Code OTP");
        setSize(400, 280);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);

        JPanel root = UITheme.darkPanel();
        root.setLayout(new GridBagLayout());

        JPanel card = UITheme.cardPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createEmptyBorder(20, 25, 20, 25));

        JLabel title = new JLabel("📧 Vérification OTP");
        title.setForeground(Color.WHITE);
        title.setFont(new Font("SansSerif", Font.BOLD, 20));
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(title);
        card.add(Box.createVerticalStrut(10));

        JLabel instr = new JLabel("Code reçu par email (" + email + ")");
        instr.setForeground(UITheme.MUTED);
        instr.setFont(new Font("SansSerif", Font.PLAIN, 12));
        instr.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(instr);
        card.add(Box.createVerticalStrut(18));

        codeField = UITheme.textField();
        codeField.setMaximumSize(new Dimension(200, 40));
        codeField.setBorder(UITheme.titledBorder("Code OTP"));
        codeField.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(codeField);
        card.add(Box.createVerticalStrut(12));

        statusLabel = new JLabel(" ");
        statusLabel.setForeground(UITheme.RED);
        statusLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        statusLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(statusLabel);
        card.add(Box.createVerticalStrut(18));

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        btnPanel.setOpaque(false);

        verifyBtn = UITheme.primaryButton("✔ Vérifier");
        resendBtn = UITheme.blueButton("↻ Renvoyer");
        backBtn = UITheme.dangerButton("← Retour");

        btnPanel.add(verifyBtn);
        btnPanel.add(resendBtn);
        btnPanel.add(backBtn);
        card.add(btnPanel);

        root.add(card);
        setContentPane(root);

        // Actions
        verifyBtn.addActionListener(e -> verifyCode());
        resendBtn.addActionListener(e -> resendCode());
        backBtn.addActionListener(e -> {
            backFrame.setVisible(true);
            dispose();
        });
    }

    private void verifyCode() {
        if (waitingForResponse) return; // empêche double clic

        String code = codeField.getText().trim();
        if (code.isEmpty()) {
            statusLabel.setText("Veuillez entrer le code.");
            return;
        }

        waitingForResponse = true;
        verifyBtn.setEnabled(false);
        resendBtn.setEnabled(false);
        statusLabel.setText("Vérification...");

        // Lancer la vérification dans un thread séparé pour ne pas bloquer l'UI
        new Thread(() -> {
            try {
                String response = clientService.verifyOtp(email, code);
                SwingUtilities.invokeLater(() -> {
                    waitingForResponse = false;
                    verifyBtn.setEnabled(true);
                    resendBtn.setEnabled(true);

                    if ("OTP_VERIFIED".equals(response)) {
                        JOptionPane.showMessageDialog(OtpFrame.this,
                                "✅ Compte activé ! Vous pouvez maintenant vous connecter.",
                                "Succès", JOptionPane.INFORMATION_MESSAGE);
                        dispose();
                        backFrame.setVisible(true);
                    } else if ("ERROR:OTP_INVALID".equals(response)) {
                        statusLabel.setText("Code incorrect ou expiré.");
                    } else {
                        statusLabel.setText("Erreur : " + response);
                    }
                });
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> {
                    waitingForResponse = false;
                    verifyBtn.setEnabled(true);
                    resendBtn.setEnabled(true);
                    statusLabel.setText("Erreur de communication.");
                });
            }
        }).start();
    }

    private void resendCode() {
        if (waitingForResponse) return;
        waitingForResponse = true;
        resendBtn.setEnabled(false);
        statusLabel.setText("Envoi...");

        new Thread(() -> {
            try {
                String response = clientService.sendOtp(email);
                SwingUtilities.invokeLater(() -> {
                    waitingForResponse = false;
                    resendBtn.setEnabled(true);
                    if ("OTP_SENT".equals(response)) {
                        statusLabel.setText("Nouveau code envoyé.");
                    } else {
                        statusLabel.setText("Échec de l'envoi.");
                    }
                });
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> {
                    waitingForResponse = false;
                    resendBtn.setEnabled(true);
                    statusLabel.setText("Erreur.");
                });
            }
        }).start();
    }
}