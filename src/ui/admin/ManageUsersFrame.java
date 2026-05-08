package ui.admin;

import client.ClientSocketService;
import utils.UITheme;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

public class ManageUsersFrame extends JFrame {

    private final ClientSocketService clientService;
    private JTable table;
    private DefaultTableModel model;

    public ManageUsersFrame(ClientSocketService clientService) {
        this.clientService = clientService;
        initUI();
        loadUsers();
    }

    private void initUI() {
        setTitle("Manage Users");
        setSize(800, 500);
        setLocationRelativeTo(null);

        JPanel root = UITheme.darkPanel();
        root.setLayout(new BorderLayout(10, 10));
        root.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        model = new DefaultTableModel(new Object[]{"ID", "Nom", "Prénom", "Email", "Role"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        table = new JTable(model);
        JScrollPane scrollPane = new JScrollPane(table);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        actions.setOpaque(false);

        JButton refreshBtn = UITheme.primaryButton("Refresh");
        JButton closeBtn = UITheme.blueButton("Close");

        actions.add(refreshBtn);
        actions.add(closeBtn);

        root.add(scrollPane, BorderLayout.CENTER);
        root.add(actions, BorderLayout.SOUTH);

        setContentPane(root);

        refreshBtn.addActionListener(e -> loadUsers());
        closeBtn.addActionListener(e -> dispose());
    }

    private void loadUsers() {
        model.setRowCount(0);

        String response = clientService.adminGetUsers();

        if (response == null || response.startsWith("ERROR") || "NO_USERS".equals(response)) {
            return;
        }

        String[] users = response.split("\\|");
        for (String user : users) {
            String[] fields = user.split(";");
            if (fields.length >= 5) {
                model.addRow(new Object[]{
                        fields[0], fields[1], fields[2], fields[3], fields[4]
                });
            }
        }
    }
}