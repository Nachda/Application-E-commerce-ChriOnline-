package ui.admin;

import client.ClientSocketService;
import utils.UITheme;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

public class ManageUsersPanel extends JPanel {

    private final ClientSocketService clientService;
    private final DefaultTableModel model = new DefaultTableModel(
            new Object[]{"ID", "Nom", "Prénom", "Email", "Rôle", "Statut"}, 0) {
        @Override public boolean isCellEditable(int row, int col) { return false; }
    };
    private final JTable table = new JTable(model);

    public ManageUsersPanel(ClientSocketService clientService) {
        this.clientService = clientService;
        setLayout(new BorderLayout());
        setBackground(UITheme.BG);
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        table.setBackground(UITheme.CARD_2);
        table.setForeground(UITheme.TEXT);
        table.setRowHeight(28);
        add(new JScrollPane(table), BorderLayout.CENTER);
        refreshData();
    }

    public void refreshData() {
        model.setRowCount(0);
        String resp = clientService.adminGetUsers();
        if (resp == null || resp.startsWith("ERROR") || resp.equals("NO_USERS")) return;
        for (String row : resp.split("\\|")) {
            String[] f = row.split(";");
            if (f.length >= 5) model.addRow(new Object[]{f[0], f[1], f[2], f[3], f[4]});
        }
    }
}