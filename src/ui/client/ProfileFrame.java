package ui.client;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;

import client.AppSession;
import client.ClientSocketService;
import ui.common.LanguageAwareFrame;
import utils.LanguageManager;
import utils.UITheme;

public class ProfileFrame extends LanguageAwareFrame {

    private final ClientSocketService clientService;
    private final AppSession session;
    private final JFrame backFrame;

    private JLabel welcomeLabel;
    private JLabel nameLabel;
    private JLabel emailLabel;
    private JLabel phoneLabel;
    private JLabel addressLabel;
    private JLabel cityLabel;
    private JButton editBtn;
    private JButton ordersBtn;
    private JButton backBtn;
    private JLabel titleLabel;

    public ProfileFrame(ClientSocketService clientService, AppSession session, JFrame backFrame) {
        this.clientService = clientService;
        this.session = session;
        this.backFrame = backFrame;
        initUI();
        refreshProfile();
    }

    private void initUI() {
        setTitle(LanguageManager.getInstance().getText("profile.title"));
        setSize(600, 550);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        JPanel root = UITheme.darkPanel();
        root.setLayout(new BorderLayout(10, 10));
        root.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JPanel header = UITheme.cardPanel();
        header.setLayout(new BorderLayout());

        titleLabel = new JLabel("👤 " + LanguageManager.getInstance().getText("profile.title"));
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 24));
        header.add(titleLabel, BorderLayout.WEST);

        root.add(header, BorderLayout.NORTH);

        JPanel content = UITheme.cardPanel();
        content.setLayout(new BorderLayout(15, 15));
        content.setBorder(BorderFactory.createEmptyBorder(20, 25, 20, 25));

        JPanel welcomePanel = new JPanel();
        welcomePanel.setOpaque(false);
        welcomePanel.setLayout(new BoxLayout(welcomePanel, BoxLayout.Y_AXIS));

        welcomeLabel = new JLabel(LanguageManager.getInstance().getText("profile.welcome") + " !");
        welcomeLabel.setForeground(UITheme.GOLD);
        welcomeLabel.setFont(new Font("SansSerif", Font.BOLD, 20));
        welcomeLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        welcomePanel.add(welcomeLabel);
        welcomePanel.add(Box.createVerticalStrut(15));

        JSeparator sep = new JSeparator();
        sep.setForeground(UITheme.BORDER);
        welcomePanel.add(sep);

        content.add(welcomePanel, BorderLayout.NORTH);

        JPanel infoPanel = new JPanel();
        infoPanel.setOpaque(false);
        infoPanel.setLayout(new GridLayout(5, 2, 15, 12));

        infoPanel.add(createLabel("👤 " + LanguageManager.getInstance().getText("profile.name") + " :"));
        nameLabel = createValueLabel("---");
        infoPanel.add(nameLabel);

        infoPanel.add(createLabel("📧 " + LanguageManager.getInstance().getText("profile.email") + " :"));
        emailLabel = createValueLabel("---");
        infoPanel.add(emailLabel);

        infoPanel.add(createLabel("📱 " + LanguageManager.getInstance().getText("profile.phone") + " :"));
        phoneLabel = createValueLabel("---");
        infoPanel.add(phoneLabel);

        infoPanel.add(createLabel("🏠 " + LanguageManager.getInstance().getText("profile.address") + " :"));
        addressLabel = createValueLabel("---");
        infoPanel.add(addressLabel);

        infoPanel.add(createLabel("🏙️ " + LanguageManager.getInstance().getText("profile.city") + " :"));
        cityLabel = createValueLabel("---");
        infoPanel.add(cityLabel);

        content.add(infoPanel, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 0));
        buttonPanel.setOpaque(false);

        editBtn = UITheme.blueButton("✏️ " + LanguageManager.getInstance().getText("profile.edit"));
        ordersBtn = UITheme.primaryButton("📦 " + LanguageManager.getInstance().getText("profile.orders"));
        backBtn = UITheme.dangerButton("← " + LanguageManager.getInstance().getText("cart.back"));

        editBtn.addActionListener(e -> new EditProfileFrame(clientService, session, this).setVisible(true));
        ordersBtn.addActionListener(e -> new OrderHistoryFrame(clientService, session).setVisible(true));
        backBtn.addActionListener(e -> {
            dispose();
            backFrame.setVisible(true);
        });

        buttonPanel.add(editBtn);
        buttonPanel.add(ordersBtn);
        buttonPanel.add(backBtn);

        content.add(buttonPanel, BorderLayout.SOUTH);

        root.add(content, BorderLayout.CENTER);
        setContentPane(root);
    }

    private JLabel createLabel(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(UITheme.TEXT);
        label.setFont(new Font("SansSerif", Font.BOLD, 14));
        return label;
    }

    private JLabel createValueLabel(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(UITheme.MUTED);
        label.setFont(new Font("SansSerif", Font.PLAIN, 14));
        return label;
    }

    public void refreshProfile() {
        String response = clientService.getProfile(session.getClientId());

        if (response == null || response.startsWith("ERROR")) {
            welcomeLabel.setText(LanguageManager.getInstance().getText("profile.welcome") + " Client #" + session.getClientId());
            nameLabel.setText("Client #" + session.getClientId());
            emailLabel.setText("...");
            phoneLabel.setText("...");
            addressLabel.setText("...");
            cityLabel.setText("...");
            return;
        }

        if (!response.startsWith("PROFILE_DATA:")) {
            return;
        }

        String data = response.substring("PROFILE_DATA:".length());
        String[] parts = data.split(";");

        String fullName = parts.length > 0 ? parts[0] : "";
        String email = parts.length > 1 ? parts[1] : "";
        String phone = parts.length > 2 ? parts[2] : "";
        String address = parts.length > 3 ? parts[3] : "";
        String city = parts.length > 4 ? parts[4] : "";

        if (fullName == null || fullName.isBlank()) {
            fullName = "Client #" + session.getClientId();
        }

        welcomeLabel.setText(LanguageManager.getInstance().getText("profile.welcome") + " " + fullName);
        nameLabel.setText(fullName);
        emailLabel.setText(email == null || email.isBlank() ? "..." : email);
        phoneLabel.setText(phone == null || phone.isBlank() ? "..." : phone);
        addressLabel.setText(address == null || address.isBlank() ? "..." : address);
        cityLabel.setText(city == null || city.isBlank() ? "..." : city);

        revalidate();
        repaint();
    }

    @Override
    public void refreshTexts() {
        setTitle(LanguageManager.getInstance().getText("profile.title"));
        titleLabel.setText("👤 " + LanguageManager.getInstance().getText("profile.title"));
        welcomeLabel.setText(LanguageManager.getInstance().getText("profile.welcome") + " " + nameLabel.getText());
        editBtn.setText("✏️ " + LanguageManager.getInstance().getText("profile.edit"));
        ordersBtn.setText("📦 " + LanguageManager.getInstance().getText("profile.orders"));
        backBtn.setText("← " + LanguageManager.getInstance().getText("cart.back"));
        revalidate();
        repaint();
    }
}