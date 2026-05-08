package ui.admin;

import client.AppSession;
import client.ClientSocketService;
import utils.UITheme;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

public class ManageProductsPanel extends JPanel {

    private final ClientSocketService clientService;
    private final AppSession session;
    private final DefaultTableModel model = new DefaultTableModel(
            new Object[]{"ID", "Nom", "Prix", "Stock", "Catégorie"}, 0) {
        @Override public boolean isCellEditable(int row, int col) { return false; }
    };
    private final JTable table = new JTable(model);

    public ManageProductsPanel(ClientSocketService clientService, AppSession session) {
        this.clientService = clientService;
        this.session = session;
        setLayout(new BorderLayout());
        setBackground(UITheme.BG);
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        table.setBackground(UITheme.CARD_2);
        table.setForeground(UITheme.TEXT);
        table.setRowHeight(28);
        add(new JScrollPane(table), BorderLayout.CENTER);
        JButton refreshBtn = UITheme.blueButton("Actualiser");
        refreshBtn.addActionListener(e -> refreshData());
        add(refreshBtn, BorderLayout.SOUTH);
        refreshData();
    }

    public void refreshData() {
        model.setRowCount(0);
        String resp = clientService.getProducts();
        if (resp == null || resp.startsWith("ERROR") || resp.equals("NO_PRODUCTS")) return;
        for (String row : resp.split("\\|")) {
            String[] f = row.split(";");
            if (f.length >= 6) model.addRow(new Object[]{f[0], f[1], f[2], f[5], f[4]});
        }
    }
}