package ui.admin;

import java.awt.BorderLayout;
import java.awt.FlowLayout;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

import client.ClientSocketService;
import ui.client.ShopFrame;
import utils.UITheme;

public class ManageCategoriesFrame extends JFrame {

    private final ClientSocketService clientService;
    private final ShopFrame shopFrame;
    private JTable table;
    private DefaultTableModel model;

    public ManageCategoriesFrame(ClientSocketService clientService, ShopFrame shopFrame) {
        this.clientService = clientService;
        this.shopFrame = shopFrame;
        initUI();
        loadCategories();
    }

    private void initUI() {
        setTitle("Manage Categories");
        setSize(700, 500);
        setLocationRelativeTo(null);

        JPanel root = UITheme.darkPanel();
        root.setLayout(new BorderLayout(10, 10));
        root.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        model = new DefaultTableModel(new Object[]{"ID", "Name", "Description"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        table = new JTable(model);
        JScrollPane scrollPane = new JScrollPane(table);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        actions.setOpaque(false);

        JButton addBtn = UITheme.primaryButton("Add");
        JButton editBtn = UITheme.blueButton("Edit");
        JButton deleteBtn = UITheme.blueButton("Delete");
        JButton refreshBtn = UITheme.blueButton("Refresh");
        JButton closeBtn = UITheme.blueButton("Close");

        actions.add(addBtn);
        actions.add(editBtn);
        actions.add(deleteBtn);
        actions.add(refreshBtn);
        actions.add(closeBtn);

        root.add(scrollPane, BorderLayout.CENTER);
        root.add(actions, BorderLayout.SOUTH);

        setContentPane(root);

        addBtn.addActionListener(e -> new AddCategoryFrame(clientService, this, shopFrame).setVisible(true));
        editBtn.addActionListener(e -> editSelectedCategory());
        deleteBtn.addActionListener(e -> deleteSelectedCategory());
        refreshBtn.addActionListener(e -> {
            loadCategories();
            if (shopFrame != null) shopFrame.reloadCategoriesAndProducts();
        });
        closeBtn.addActionListener(e -> dispose());
    }

    public void loadCategories() {
        model.setRowCount(0);

        String response = clientService.adminGetCategories();

        if (response == null || response.startsWith("ERROR") || "NO_CATEGORIES".equals(response)) {
            return;
        }

        String[] categories = response.split("\\|");
        for (String category : categories) {
            String[] fields = category.split(";");
            if (fields.length >= 3) {
                model.addRow(new Object[]{fields[0], fields[1], fields[2]});
            }
        }
    }

    private void editSelectedCategory() {
        int row = table.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Sélectionne une catégorie.");
            return;
        }

        int id = Integer.parseInt(model.getValueAt(row, 0).toString());
        String name = model.getValueAt(row, 1).toString();
        String description = model.getValueAt(row, 2).toString();

        new EditCategoryFrame(clientService, this, shopFrame, id, name, description).setVisible(true);
    }

    private void deleteSelectedCategory() {
        int row = table.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Sélectionne une catégorie.");
            return;
        }

        int id = Integer.parseInt(model.getValueAt(row, 0).toString());

        int confirm = JOptionPane.showConfirmDialog(this, "Supprimer cette catégorie ?", "Confirmation", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            String response = clientService.adminDeleteCategory(id);

            if ("ADMIN_DELETE_CATEGORY_SUCCESS".equals(response)) {
                JOptionPane.showMessageDialog(this, "Catégorie supprimée.");
                loadCategories();
                if (shopFrame != null) shopFrame.reloadCategoriesAndProducts();
            } else {
                JOptionPane.showMessageDialog(this, "Erreur : " + response);
            }
        }
    }
}