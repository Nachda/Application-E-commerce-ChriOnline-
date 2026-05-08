package ui.client;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.function.Consumer;

/**
 * Fenêtre de sélection d'une catégorie.
 * Affiche une liste de boutons correspondant aux catégories.
 */
public class CategoriesFrame extends JDialog {

    private final List<String> categories;
    private final Consumer<String> onCategorySelected;

    public CategoriesFrame(List<String> categories, Consumer<String> onCategorySelected) {
        this.categories = categories;
        this.onCategorySelected = onCategorySelected;
        initUI();
    }

    private void initUI() {
        setTitle("Choisir une catégorie");
        setModal(true);
        setSize(300, 400);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(new Color(45, 49, 60));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JLabel label = new JLabel("Catégories disponibles");
        label.setForeground(Color.WHITE);
        label.setFont(new Font("SansSerif", Font.BOLD, 16));
        label.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(label);
        panel.add(Box.createVerticalStrut(10));

        // Ajouter "Tous" en premier
        JButton allBtn = createCategoryButton("Tous", e -> {
            onCategorySelected.accept("Tous");
            dispose();
        });
        panel.add(allBtn);

        // Ajouter les autres catégories
        for (String cat : categories) {
            JButton btn = createCategoryButton(cat, e -> {
                onCategorySelected.accept(cat);
                dispose();
            });
            panel.add(btn);
            panel.add(Box.createVerticalStrut(5));
        }

        JScrollPane scrollPane = new JScrollPane(panel);
        scrollPane.setBorder(null);
        getContentPane().add(scrollPane, BorderLayout.CENTER);
    }

    private JButton createCategoryButton(String text, ActionListener action) {
        JButton btn = new JButton(text);
        btn.setBackground(new Color(53, 58, 71));
        btn.setForeground(Color.WHITE);
        btn.setFont(new Font("SansSerif", Font.PLAIN, 14));
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setAlignmentX(Component.CENTER_ALIGNMENT);
        btn.setMaximumSize(new Dimension(250, 40));
        btn.addActionListener(action);
        return btn;
    }
}