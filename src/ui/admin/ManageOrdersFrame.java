package ui.admin;

import client.ClientSocketService;
import utils.UITheme;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

public class ManageOrdersFrame extends JFrame {

    private final ClientSocketService clientService;
    private JTable table;
    private DefaultTableModel model;

    public ManageOrdersFrame(ClientSocketService clientService) {
        this.clientService = clientService;
        initUI();
        loadOrders();
    }

    private void initUI() {
        setTitle("Manage Orders");
        setSize(900, 500);
        setLocationRelativeTo(null);

        JPanel root = UITheme.darkPanel();
        root.setLayout(new BorderLayout(10, 10));
        root.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        model = new DefaultTableModel(new Object[]{"ID", "UUID", "Total", "Status", "Created At"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        table = new JTable(model);
        JScrollPane scrollPane = new JScrollPane(table);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        actions.setOpaque(false);

        JButton updateStatusBtn = UITheme.primaryButton("Update Status");
        JButton refreshBtn = UITheme.blueButton("Refresh");
        JButton closeBtn = UITheme.blueButton("Close");

        actions.add(updateStatusBtn);
        actions.add(refreshBtn);
        actions.add(closeBtn);

        root.add(scrollPane, BorderLayout.CENTER);
        root.add(actions, BorderLayout.SOUTH);

        setContentPane(root);

        updateStatusBtn.addActionListener(e -> updateSelectedOrderStatus());
        refreshBtn.addActionListener(e -> loadOrders());
        closeBtn.addActionListener(e -> dispose());
    }

    private void loadOrders() {
        model.setRowCount(0);

        String response = clientService.adminGetOrders();

        if (response == null || response.startsWith("ERROR") || "NO_ORDERS".equals(response)) {
            return;
        }

        String[] orders = response.split("\\|");
        for (String order : orders) {
            String[] fields = order.split(";");
            if (fields.length >= 5) {
                model.addRow(new Object[]{
                        fields[0], fields[1], fields[2], fields[3], fields[4]
                });
            }
        }
    }

    private void updateSelectedOrderStatus() {
        int row = table.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Sélectionne une commande.");
            return;
        }

        int orderId = Integer.parseInt(model.getValueAt(row, 0).toString());

        String[] statuses = {"pending", "paid", "shipped", "delivered", "cancelled"};
        String selectedStatus = (String) JOptionPane.showInputDialog(
                this,
                "Choisir le nouveau statut :",
                "Update Order Status",
                JOptionPane.PLAIN_MESSAGE,
                null,
                statuses,
                model.getValueAt(row, 3).toString()
        );

        if (selectedStatus == null || selectedStatus.trim().isEmpty()) {
            return;
        }

        String response = clientService.adminUpdateOrderStatus(orderId, selectedStatus);

        if ("ADMIN_UPDATE_ORDER_STATUS_SUCCESS".equals(response)) {
            JOptionPane.showMessageDialog(this, "Statut mis à jour.");
            loadOrders();
        } else {
            JOptionPane.showMessageDialog(this, "Erreur : " + response);
        }
    }
}