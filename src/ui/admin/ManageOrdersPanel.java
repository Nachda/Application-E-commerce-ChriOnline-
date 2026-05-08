package ui.admin;

import client.ClientSocketService;
import utils.UITheme;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

public class ManageOrdersPanel extends JPanel {

    private final ClientSocketService clientService;
    private final DefaultTableModel model = new DefaultTableModel(
            new Object[]{"ID", "UUID", "Client", "Email", "Total", "Statut", "Date"}, 0) {
        @Override public boolean isCellEditable(int row, int col) { return false; }
    };
    private final JTable table = new JTable(model);

    public ManageOrdersPanel(ClientSocketService clientService) {
        this.clientService = clientService;
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
        String resp = clientService.adminGetOrders();
        if (resp == null || resp.startsWith("ERROR") || resp.equals("NO_ORDERS")) return;
        for (String row : resp.split("\\|")) {
            String[] f = row.split(";");
            if (f.length >= 7) model.addRow(new Object[]{f[0], f[1], f[2], f[3], f[4], f[5], f[6]});
        }
    }
}