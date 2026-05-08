package ui.admin;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import client.AppSession;
import client.ClientSocketService;
import utils.UITheme;

public class AdminMainFrame extends JFrame {

    private final ClientSocketService clientService;
    private final AppSession session;

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel contentPanel = new JPanel(cardLayout);

    private final AdminDashboardPanel dashboardPanel;
    private final ManageProductsPanel productsPanel;
    private final ManageCategoriesPanel categoriesPanel;
    private final ManageOrdersPanel ordersPanel;
    private final ManageUsersPanel usersPanel;
    private final NotificationsPanel notificationsPanel;
    private final StockHistoryPanel stockHistoryPanel;
    private final AdminStatisticsPanel statisticsPanel;

    private final Map<String, String[]> pageMeta = new LinkedHashMap<>();

    public AdminMainFrame(ClientSocketService clientService, AppSession session) {
        this.clientService = clientService;
        this.session = session;

        setTitle("ChriOnline - Administration");
        setSize(1380, 860);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        pageMeta.put("dashboard", new String[]{"Tableau de bord", "Vue d'ensemble du système"});
        pageMeta.put("products", new String[]{"Produits", "Gestion du catalogue"});
        pageMeta.put("categories", new String[]{"Catégories", "Gestion des catégories"});
        pageMeta.put("orders", new String[]{"Commandes", "Suivi des commandes"});
        pageMeta.put("users", new String[]{"Utilisateurs", "Clients et administrateurs"});
        pageMeta.put("notifications", new String[]{"Notifications", "Alertes système"});
        pageMeta.put("stockHistory", new String[]{"Historique Stock", "Mouvements de stock"});
        pageMeta.put("statistics", new String[]{"Statistiques", "Indicateurs clés"});

        dashboardPanel = new AdminDashboardPanel(clientService);
        productsPanel = new ManageProductsPanel(clientService, session);
        categoriesPanel = new ManageCategoriesPanel(clientService);
        ordersPanel = new ManageOrdersPanel(clientService);
        usersPanel = new ManageUsersPanel(clientService);
        notificationsPanel = new NotificationsPanel(clientService);
        stockHistoryPanel = new StockHistoryPanel(clientService, session);
        statisticsPanel = new AdminStatisticsPanel(clientService);

        initUI();
        registerPages();
        navigateTo("dashboard");
    }

    private void initUI() {
        JPanel sidebar = createSidebar();
        JPanel mainArea = new JPanel(new BorderLayout());
        mainArea.setBackground(UITheme.BG);

        JLabel header = new JLabel("Administration");
        header.setForeground(UITheme.TEXT);
        header.setFont(new Font("Segoe UI", Font.BOLD, 22));
        header.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));
        header.setBackground(UITheme.CARD);
        header.setOpaque(true);
        mainArea.add(header, BorderLayout.NORTH);
        mainArea.add(contentPanel, BorderLayout.CENTER);

        JPanel root = new JPanel(new BorderLayout());
        root.add(sidebar, BorderLayout.WEST);
        root.add(mainArea, BorderLayout.CENTER);
        setContentPane(root);
    }

    private JPanel createSidebar() {
        JPanel sidebar = new JPanel();
        sidebar.setBackground(new Color(20, 26, 42));
        sidebar.setPreferredSize(new Dimension(240, 0));
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBorder(BorderFactory.createEmptyBorder(20, 16, 20, 16));

        JLabel logo = new JLabel("ChriOnline");
        logo.setForeground(UITheme.GOLD);
        logo.setFont(new Font("Segoe UI", Font.BOLD, 18));
        logo.setAlignmentX(Component.LEFT_ALIGNMENT);
        sidebar.add(logo);
        sidebar.add(Box.createVerticalStrut(24));

        for (String pageId : pageMeta.keySet()) {
            String[] meta = pageMeta.get(pageId);
            JButton btn = new JButton(meta[0]);
            btn.setBackground(new Color(20, 26, 42));
            btn.setForeground(UITheme.MUTED);
            btn.setFont(new Font("Segoe UI", Font.PLAIN, 14));
            btn.setFocusPainted(false);
            btn.setBorderPainted(false);
            btn.setHorizontalAlignment(SwingConstants.LEFT);
            btn.setMaximumSize(new Dimension(200, 40));
            btn.setAlignmentX(Component.LEFT_ALIGNMENT);
            btn.addActionListener(e -> navigateTo(pageId));
            sidebar.add(btn);
            sidebar.add(Box.createVerticalStrut(4));
        }

        sidebar.add(Box.createVerticalGlue());
        JButton logoutBtn = new JButton("Déconnexion");
        logoutBtn.setBackground(UITheme.RED);
        logoutBtn.setForeground(Color.WHITE);
        logoutBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        logoutBtn.setFocusPainted(false);
        logoutBtn.setBorderPainted(false);
        logoutBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        logoutBtn.setMaximumSize(new Dimension(200, 36));
        logoutBtn.addActionListener(e -> dispose());
        sidebar.add(logoutBtn);

        return sidebar;
    }

    private void registerPages() {
        contentPanel.add(dashboardPanel, "dashboard");
        contentPanel.add(productsPanel, "products");
        contentPanel.add(categoriesPanel, "categories");
        contentPanel.add(ordersPanel, "orders");
        contentPanel.add(usersPanel, "users");
        contentPanel.add(notificationsPanel, "notifications");
        contentPanel.add(stockHistoryPanel, "stockHistory");
        contentPanel.add(statisticsPanel, "statistics");
    }

    private void navigateTo(String pageId) {
        cardLayout.show(contentPanel, pageId);
        if (pageId.equals("dashboard")) dashboardPanel.refreshData();
        if (pageId.equals("products")) productsPanel.refreshData();
        if (pageId.equals("categories")) categoriesPanel.refreshData();
        if (pageId.equals("orders")) ordersPanel.refreshData();
        if (pageId.equals("users")) usersPanel.refreshData();
        if (pageId.equals("notifications")) notificationsPanel.refreshData();
        if (pageId.equals("stockHistory")) stockHistoryPanel.refreshData();
        if (pageId.equals("statistics")) statisticsPanel.refreshData();
    }
}