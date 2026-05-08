package ui.admin;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.filechooser.FileNameExtensionFilter;

import client.ClientSocketService;
import ui.client.ShopFrame;
import utils.UITheme;

public class AddProductFrame extends JFrame {

    private final ClientSocketService clientService;
    private final ManageProductsFrame parent;
    private final ShopFrame shopFrame;

    private JTextField nameField;
    private JTextArea descriptionArea;
    private JTextField priceField;
    private JTextField stockField;
    private JComboBox<String> categoryBox;
    private JLabel imageLabel;

    private final Map<String, Integer> categoryMap = new LinkedHashMap<>();
    private String imagePath = "image/default.jpg";

    public AddProductFrame(ClientSocketService clientService, ManageProductsFrame parent, ShopFrame shopFrame) {
        this.clientService = clientService;
        this.parent = parent;
        this.shopFrame = shopFrame;
        initUI();
        loadCategories();
    }

    private void initUI() {
        setTitle("Add Product");
        setSize(540, 580);
        setLocationRelativeTo(null);

        JPanel root = UITheme.darkPanel();
        root.setLayout(new BorderLayout(10, 10));
        root.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JPanel form = new JPanel(new GridLayout(0, 1, 10, 10));
        form.setOpaque(false);

        nameField = UITheme.textField();

        descriptionArea = new JTextArea(4, 20);
        descriptionArea.setBackground(UITheme.INPUT_BG);
        descriptionArea.setForeground(UITheme.TEXT);
        descriptionArea.setCaretColor(UITheme.TEXT);
        descriptionArea.setFont(UITheme.normalFont());
        descriptionArea.setLineWrap(true);
        descriptionArea.setWrapStyleWord(true);

        priceField = UITheme.textField();
        stockField = UITheme.textField();
        categoryBox = new JComboBox<>();
        imageLabel = new JLabel(imagePath);
        imageLabel.setForeground(Color.WHITE);

        form.add(createFieldPanel("Nom", nameField));
        form.add(createFieldPanel("Description", new JScrollPane(descriptionArea)));
        form.add(createFieldPanel("Prix", priceField));
        form.add(createFieldPanel("Stock", stockField));
        form.add(createFieldPanel("Catégorie", categoryBox));
        form.add(createFieldPanel("Image", imageLabel));

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        actions.setOpaque(false);

        JButton importBtn = UITheme.blueButton("Import Image");
        JButton saveBtn = UITheme.primaryButton("Save");
        JButton closeBtn = UITheme.blueButton("Close");

        actions.add(importBtn);
        actions.add(saveBtn);
        actions.add(closeBtn);

        root.add(form, BorderLayout.CENTER);
        root.add(actions, BorderLayout.SOUTH);

        setContentPane(root);

        importBtn.addActionListener(e -> importImage());
        saveBtn.addActionListener(e -> saveProduct());
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

    private void loadCategories() {
        categoryMap.clear();
        categoryBox.removeAllItems();

        String response = clientService.adminGetCategories();

        if (response == null || response.startsWith("ERROR") || "NO_CATEGORIES".equals(response)) {
            JOptionPane.showMessageDialog(this, "Impossible de charger les catégories.");
            return;
        }

        String[] categories = response.split("\\|");
        for (String c : categories) {
            String[] parts = c.split(";");
            if (parts.length >= 2) {
                int id = Integer.parseInt(parts[0]);
                String name = parts[1];
                categoryMap.put(name, id);
                categoryBox.addItem(name);
            }
        }
    }

    private void importImage() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Choisir une image");
        chooser.setFileFilter(new FileNameExtensionFilter(
                "Images", "jpg", "jpeg", "png", "webp", "gif"
        ));

        int result = chooser.showOpenDialog(this);

        if (result == JFileChooser.APPROVE_OPTION) {
            try {
                File selectedFile = chooser.getSelectedFile();

                if (selectedFile == null || !selectedFile.exists()) {
                    JOptionPane.showMessageDialog(this, "Fichier image introuvable.");
                    return;
                }

                File imageDir = new File("image");
                if (!imageDir.exists()) {
                    imageDir.mkdirs();
                }

                String originalName = selectedFile.getName();
                String extension = "";

                int dotIndex = originalName.lastIndexOf('.');
                if (dotIndex >= 0) {
                    extension = originalName.substring(dotIndex);
                }

                String newFileName = "prod_" + System.currentTimeMillis() + extension;
                File destination = new File(imageDir, newFileName);

                Files.copy(selectedFile.toPath(), destination.toPath(), StandardCopyOption.REPLACE_EXISTING);

                imagePath = "image/" + newFileName;
                imageLabel.setText(imagePath);

                JOptionPane.showMessageDialog(this, "Image importée avec succès.");

            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this, "Erreur lors de l'import de l'image : " + ex.getMessage());
            }
        }
    }

    private void saveProduct() {
        try {
            String name = nameField.getText().trim();
            String description = descriptionArea.getText().trim();
            String priceText = priceField.getText().trim();
            String stockText = stockField.getText().trim();

            if (name.isEmpty() || description.isEmpty() || priceText.isEmpty() || stockText.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Remplis tous les champs.");
                return;
            }

            double price = Double.parseDouble(priceText);
            int stock = Integer.parseInt(stockText);

            if (price < 0 || stock < 0) {
                JOptionPane.showMessageDialog(this, "Prix ou stock invalide.");
                return;
            }

            String categoryName = (String) categoryBox.getSelectedItem();
            if (categoryName == null || !categoryMap.containsKey(categoryName)) {
                JOptionPane.showMessageDialog(this, "Choisis une catégorie.");
                return;
            }

            int categoryId = categoryMap.get(categoryName);

            String response = clientService.adminAddProduct(name, description, price, stock, imagePath, categoryId);

            if ("ADMIN_ADD_PRODUCT_SUCCESS".equals(response)) {
                JOptionPane.showMessageDialog(this, "Produit ajouté avec succès.");
                parent.loadProducts();
                if (shopFrame != null) {
                    shopFrame.reloadProducts();
                }
                dispose();
            } else {
                JOptionPane.showMessageDialog(this, "Erreur serveur : " + response);
            }

        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Prix ou stock invalide.");
        }
    }
}