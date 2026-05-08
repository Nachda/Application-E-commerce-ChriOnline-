package ui.admin;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridLayout;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import client.ClientSocketService;
import ui.client.ShopFrame;
import utils.UITheme;

public class EditCategoryFrame extends JFrame {

    private final ClientSocketService clientService;
    private final ManageCategoriesFrame parent;
    private final ShopFrame shopFrame;
    private final int categoryId;

    private JTextField nameField;
    private JTextArea descriptionArea;

    public EditCategoryFrame(ClientSocketService clientService, ManageCategoriesFrame parent, ShopFrame shopFrame,
                             int categoryId, String currentName, String currentDescription) {
        this.clientService = clientService;
        this.parent = parent;
        this.shopFrame = shopFrame;
        this.categoryId = categoryId;
        initUI(currentName, currentDescription);
    }

    private void initUI(String currentName, String currentDescription) {
        setTitle("Edit Category");
        setSize(450, 320);
        setLocationRelativeTo(null);

        JPanel root = UITheme.darkPanel();
        root.setLayout(new BorderLayout(10, 10));
        root.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JPanel form = new JPanel(new GridLayout(0, 1, 10, 10));
        form.setOpaque(false);

        nameField = UITheme.textField();
        nameField.setText(currentName);

        descriptionArea = new JTextArea(5, 20);
        descriptionArea.setText(currentDescription);
        descriptionArea.setBackground(UITheme.INPUT_BG);
        descriptionArea.setForeground(UITheme.TEXT);
        descriptionArea.setCaretColor(UITheme.TEXT);
        descriptionArea.setFont(UITheme.normalFont());
        descriptionArea.setBorder(BorderFactory.createLineBorder(UITheme.BORDER));
        descriptionArea.setLineWrap(true);
        descriptionArea.setWrapStyleWord(true);

        JScrollPane descriptionScroll = new JScrollPane(descriptionArea);
        descriptionScroll.setBorder(BorderFactory.createLineBorder(UITheme.BORDER));

        form.add(createFieldPanel("Nom", nameField));
        form.add(createFieldPanel("Description", descriptionScroll));

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        actions.setOpaque(false);

        JButton saveBtn = UITheme.primaryButton("Update");
        JButton closeBtn = UITheme.blueButton("Close");

        actions.add(saveBtn);
        actions.add(closeBtn);

        root.add(form, BorderLayout.CENTER);
        root.add(actions, BorderLayout.SOUTH);

        setContentPane(root);

        saveBtn.addActionListener(e -> updateCategory());
        closeBtn.addActionListener(e -> dispose());
    }

    private JPanel createFieldPanel(String label, Component field) {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setOpaque(false);

        JLabel jLabel = new JLabel(label);
        jLabel.setForeground(Color.WHITE);

        panel.add(jLabel, BorderLayout.NORTH);
        panel.add(field, BorderLayout.CENTER);
        return panel;
    }

    private void updateCategory() {
        String name = nameField.getText().trim();
        String description = descriptionArea.getText().trim();

        if (name.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Nom obligatoire.");
            return;
        }

        String response = clientService.adminUpdateCategory(categoryId, name, description);

        if ("ADMIN_UPDATE_CATEGORY_SUCCESS".equals(response)) {
            JOptionPane.showMessageDialog(this, "Catégorie modifiée.");

            if (parent != null) {
                parent.loadCategories();
            }

            if (shopFrame != null) {
                shopFrame.reloadCategoriesAndProducts();
            }

            dispose();
        } else {
            JOptionPane.showMessageDialog(this, "Erreur : " + response);
        }
    }
}