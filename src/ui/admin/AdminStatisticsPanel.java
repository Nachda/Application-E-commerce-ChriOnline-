package ui.admin;

import client.ClientSocketService;
import utils.UITheme;
import ui.components.MetricCard;

import javax.swing.*;
import java.awt.*;

public class AdminStatisticsPanel extends JPanel {

    private final ClientSocketService clientService;
    private final MetricCard totalProductsCard = new MetricCard("Produits", "--", "");
    private final MetricCard totalOrdersCard = new MetricCard("Commandes", "--", "");
    private final MetricCard pendingCard = new MetricCard("En attente", "--", "");
    private final MetricCard paidCard = new MetricCard("Payées", "--", "");
    private final MetricCard todayCard = new MetricCard("Revenus jour", "--", "");
    private final MetricCard monthCard = new MetricCard("Revenus mois", "--", "");

    public AdminStatisticsPanel(ClientSocketService clientService) {
        this.clientService = clientService;
        setLayout(new GridLayout(2, 3, 10, 10));
        setBackground(UITheme.BG);
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        add(totalProductsCard);
        add(totalOrdersCard);
        add(pendingCard);
        add(paidCard);
        add(todayCard);
        add(monthCard);
        refreshData();
    }

    public void refreshData() {
        String resp = clientService.adminGetDashboardSummary();
        if (resp == null || !resp.startsWith("DASHBOARD_SUMMARY:")) return;
        String[] f = resp.substring("DASHBOARD_SUMMARY:".length()).split(";");
        if (f.length < 10) return;
        totalProductsCard.setValue(f[0]);
        totalOrdersCard.setValue(f[4]);
        pendingCard.setValue(f[5]);
        paidCard.setValue(f[6]);
        todayCard.setValue(f[7] + " DH");
        monthCard.setValue(f[8] + " DH");
    }
}