package ui.admin;

import client.ClientSocketService;
import utils.UITheme;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

public class ManageCategoriesPanel extends JPanel {

    private final ClientSocketService clientService;
    private final DefaultTableModel model = new DefaultTableModel(
            new Object[]{"ID", "Nom", "Description"}, 0) {
        @Override public boolean isCellEditable(int row, int col) { return false; }
    };
    private final JTable table = new JTable(model);

    public ManageCategoriesPanel(ClientSocketService clientService) {
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
        String resp = clientService.adminGetCategories();
        if (resp == null || resp.startsWith("ERROR") || resp.equals("NO_CATEGORIES")) return;
        for (String row : resp.split("\\|")) {
            String[] f = row.split(";");
            if (f.length >= 3) model.addRow(new Object[]{f[0], f[1], f[2]});
        }
    }
}