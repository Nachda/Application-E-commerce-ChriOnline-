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

public class ManageProductsFrame extends JFrame {

    private final ClientSocketService clientService;
    private final ShopFrame shopFrame;
    private JTable table;
    private DefaultTableModel model;

    public ManageProductsFrame(ClientSocketService clientService, ShopFrame shopFrame) {
        this.clientService = clientService;
        this.shopFrame = shopFrame;
        initUI();
        loadProducts();
    }

    private void initUI() {
        setTitle("Manage Products");
        setSize(900, 550);
        setLocationRelativeTo(null);

        JPanel root = UITheme.darkPanel();
        root.setLayout(new BorderLayout(10, 10));
        root.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        model = new DefaultTableModel(new Object[]{"ID", "Name", "Price", "Image", "Category", "Stock"}, 0) {
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

        addBtn.addActionListener(e -> new AddProductFrame(clientService, this, shopFrame).setVisible(true));

        editBtn.addActionListener(e -> editSelectedProduct());

        deleteBtn.addActionListener(e -> deleteSelectedProduct());

        refreshBtn.addActionListener(e -> loadProducts());

        closeBtn.addActionListener(e -> dispose());
    }

    public void loadProducts() {
        model.setRowCount(0);

        String response = clientService.getProducts();

        if (response == null || response.startsWith("ERROR") || "NO_PRODUCTS".equals(response)) {
            return;
        }

        String[] products = response.split("\\|");
        for (String product : products) {
            String[] fields = product.split(";");
            if (fields.length >= 6) {
                model.addRow(new Object[]{
                        fields[0], fields[1], fields[2], fields[3], fields[4], fields[5]
                });
            }
        }
    }

    private void editSelectedProduct() {
        int row = table.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Sélectionne un produit.");
            return;
        }

        int productId = Integer.parseInt(model.getValueAt(row, 0).toString());
        new EditProductFrame(clientService, this, shopFrame, productId).setVisible(true);
    }

    private void deleteSelectedProduct() {
        int row = table.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Sélectionne un produit.");
            return;
        }

        int productId = Integer.parseInt(model.getValueAt(row, 0).toString());

        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Supprimer ce produit ?",
                "Confirmation",
                JOptionPane.YES_NO_OPTION
        );

        if (confirm == JOptionPane.YES_OPTION) {
            String response = clientService.adminDeleteProduct(productId);

            if ("ADMIN_DELETE_PRODUCT_SUCCESS".equals(response)) {
                JOptionPane.showMessageDialog(this, "Produit supprimé.");
                loadProducts();
                if (shopFrame != null) shopFrame.reloadProducts();
            } else {
                JOptionPane.showMessageDialog(this, "Erreur : " + response);
            }
        }
    }
}