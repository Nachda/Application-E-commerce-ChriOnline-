package ui.client;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractCellEditor;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;

import client.AppSession;
import client.ClientSocketService;
import ui.common.LanguageAwareFrame;
import utils.LanguageManager;
import utils.UITheme;

public class CartFrame extends LanguageAwareFrame {

    private final ClientSocketService clientService;
    private final AppSession session;
    private final JFrame backFrame;

    private JTable table;
    private DefaultTableModel model;
    private JLabel totalLabel;
    private JButton checkoutBtn;
    private JButton clearAllBtn;
    private JButton backBtn;
    private JLabel titleLabel;

    private final List<CartProductInfo> cartProducts = new ArrayList<>();

    public CartFrame(ClientSocketService clientService, AppSession session, JFrame backFrame) {
        this.clientService = clientService;
        this.session = session;
        this.backFrame = backFrame;
        initUI();
        loadCart();
    }

    private void initUI() {
        setTitle(LanguageManager.getInstance().getText("cart.title"));
        setSize(1100, 600);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        JPanel root = UITheme.darkPanel();
        root.setLayout(new BorderLayout(10, 10));
        root.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JPanel top = UITheme.cardPanel();
        top.setLayout(new BorderLayout());

        titleLabel = new JLabel("🛒 " + LanguageManager.getInstance().getText("cart.title"));
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 24));
        top.add(titleLabel, BorderLayout.WEST);

        model = new DefaultTableModel(new Object[]{
                LanguageManager.getInstance().getText("cart.product"),
                LanguageManager.getInstance().getText("cart.quantity"),
                LanguageManager.getInstance().getText("cart.unit.price"),
                LanguageManager.getInstance().getText("cart.subtotal"),
                LanguageManager.getInstance().getText("cart.remove")
        }, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 4;
            }
        };

        table = new JTable(model);
        table.setBackground(UITheme.CARD_2);
        table.setForeground(Color.WHITE);
        table.setRowHeight(50);
        table.setFont(new Font("SansSerif", Font.PLAIN, 13));
        table.getTableHeader().setBackground(UITheme.CARD);
        table.getTableHeader().setForeground(Color.WHITE);
        table.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 13));
        table.setSelectionBackground(new Color(67, 139, 208));

        table.getColumn(LanguageManager.getInstance().getText("cart.remove")).setCellRenderer(new ButtonRenderer());
        table.getColumn(LanguageManager.getInstance().getText("cart.remove")).setCellEditor(new ButtonEditor());

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createLineBorder(UITheme.BORDER));

        JPanel bottom = UITheme.cardPanel();
        bottom.setLayout(new BorderLayout(10, 10));

        totalLabel = new JLabel("💰 " + LanguageManager.getInstance().getText("cart.total") + ": 0.00 DH");
        totalLabel.setForeground(UITheme.GOLD);
        totalLabel.setFont(new Font("SansSerif", Font.BOLD, 26));

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        buttons.setOpaque(false);

        checkoutBtn = UITheme.primaryButton("💳 " + LanguageManager.getInstance().getText("cart.checkout"));
        clearAllBtn = UITheme.dangerButton("🗑️ " + LanguageManager.getInstance().getText("cart.clear"));
        backBtn = UITheme.blueButton("← " + LanguageManager.getInstance().getText("cart.back"));

        buttons.add(backBtn);
        buttons.add(clearAllBtn);
        buttons.add(checkoutBtn);

        bottom.add(totalLabel, BorderLayout.WEST);
        bottom.add(buttons, BorderLayout.EAST);

        root.add(top, BorderLayout.NORTH);
        root.add(scroll, BorderLayout.CENTER);
        root.add(bottom, BorderLayout.SOUTH);

        setContentPane(root);

        clearAllBtn.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(this,
                    LanguageManager.getInstance().getText("cart.clear.confirm"),
                    "Confirmation", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                String res = clientService.clearCart(session.getClientId());
                if ("CART_CLEAR_SUCCESS".equals(res)) {
                    loadCart();
                    JOptionPane.showMessageDialog(this, LanguageManager.getInstance().getText("cart.clear.success"));
                    refreshParentCartCount();
                } else {
                    JOptionPane.showMessageDialog(this, "Erreur: " + res);
                }
            }
        });

        backBtn.addActionListener(e -> {
            backFrame.setVisible(true);
            dispose();
        });

        checkoutBtn.addActionListener(e -> checkout());
    }

    private void loadCart() {
        SwingUtilities.invokeLater(() -> {
            model.setRowCount(0);
            cartProducts.clear();

            String response = clientService.getCart(session.getClientId());

            if ("CART_EMPTY".equals(response)) {
                totalLabel.setText("💰 " + LanguageManager.getInstance().getText("cart.total") + ": 0.00 DH");
                model.addRow(new Object[]{LanguageManager.getInstance().getText("cart.empty"), "", "", "", ""});
                return;
            }

            if (response == null || response.startsWith("ERROR")) {
                JOptionPane.showMessageDialog(this, "Erreur: " + response);
                return;
            }

            String total = "0.00";
            String[] parts = response.split("\\|");

            for (String part : parts) {
                if (part.startsWith("Total=")) {
                    total = part.substring("Total=".length());
                } else if (part.startsWith("ProductId=")) {
                    int productId = Integer.parseInt(extract(part, "ProductId=", ",Product="));
                    String product = extract(part, ",Product=", ",Qty=");
                    String qty = extract(part, ",Qty=", ",Subtotal=");
                    String subtotal = part.substring(part.indexOf(",Subtotal=") + 10);

                    double sub = Double.parseDouble(subtotal);
                    int qt = Integer.parseInt(qty);
                    double unitPrice = qt > 0 ? sub / qt : 0;

                    cartProducts.add(new CartProductInfo(productId, product, qt, unitPrice, sub));

                    model.addRow(new Object[]{
                            product,
                            qt,
                            String.format("%.2f DH", unitPrice),
                            String.format("%.2f DH", sub),
                            "🗑️ " + LanguageManager.getInstance().getText("cart.remove")
                    });
                }
            }

            totalLabel.setText(String.format("💰 " + LanguageManager.getInstance().getText("cart.total") + ": %s DH", total));

            if (model.getRowCount() == 0) {
                model.addRow(new Object[]{LanguageManager.getInstance().getText("cart.empty"), "", "", "", ""});
            }
        });
    }

    private void removeItemFromCart(int row) {
        if (row >= 0 && row < cartProducts.size()) {
            CartProductInfo product = cartProducts.get(row);

            int confirm = JOptionPane.showConfirmDialog(this,
                    LanguageManager.getInstance().getText("cart.remove.confirm") + " \"" + product.name + "\" ?",
                    "Confirmation", JOptionPane.YES_NO_OPTION);

            if (confirm == JOptionPane.YES_OPTION) {
                String response = clientService.removeFromCart(session.getClientId(), product.productId);

                if ("CART_REMOVE_SUCCESS".equals(response)) {
                    JOptionPane.showMessageDialog(this, product.name + " supprimé.");
                    loadCart();
                    refreshParentCartCount();
                } else {
                    JOptionPane.showMessageDialog(this, "Erreur: " + response);
                }
            }
        }
    }

    private void refreshParentCartCount() {
        if (backFrame instanceof ShopFrame) {
            ((ShopFrame) backFrame).refreshCartCount();
        }
    }

    private String extract(String source, String start, String end) {
        int s = source.indexOf(start);
        int e = source.indexOf(end);
        if (s == -1 || e == -1) return "";
        return source.substring(s + start.length(), e);
    }

    private void checkout() {
        if (cartProducts.isEmpty()) {
            JOptionPane.showMessageDialog(this, LanguageManager.getInstance().getText("cart.empty"));
            return;
        }

        String response = clientService.checkout(session.getClientId());

        if (response == null || response.startsWith("ERROR")) {
            JOptionPane.showMessageDialog(this, "Erreur checkout: " + response);
            return;
        }

        String[] parts = response.split(";");
        if (parts.length == 3 && "ORDER_CREATED".equals(parts[0])) {
            session.setOrderUUID(parts[1]);
            session.setLastOrderTotal(Double.parseDouble(parts[2]));

            dispose();
            new PaymentFrame(clientService, session, backFrame).setVisible(true);
        } else {
            JOptionPane.showMessageDialog(this, "Erreur lors de la validation de la commande.");
        }
    }

    @Override
    public void refreshTexts() {
        setTitle(LanguageManager.getInstance().getText("cart.title"));
        titleLabel.setText("🛒 " + LanguageManager.getInstance().getText("cart.title"));

        model.setColumnIdentifiers(new Object[]{
                LanguageManager.getInstance().getText("cart.product"),
                LanguageManager.getInstance().getText("cart.quantity"),
                LanguageManager.getInstance().getText("cart.unit.price"),
                LanguageManager.getInstance().getText("cart.subtotal"),
                LanguageManager.getInstance().getText("cart.remove")
        });

        checkoutBtn.setText("💳 " + LanguageManager.getInstance().getText("cart.checkout"));
        clearAllBtn.setText("🗑️ " + LanguageManager.getInstance().getText("cart.clear"));
        backBtn.setText("← " + LanguageManager.getInstance().getText("cart.back"));

        loadCart();
        revalidate();
        repaint();
    }

    static class CartProductInfo {
        int productId;
        String name;
        int quantity;
        double unitPrice;
        double subtotal;

        CartProductInfo(int productId, String name, int quantity, double unitPrice, double subtotal) {
            this.productId = productId;
            this.name = name;
            this.quantity = quantity;
            this.unitPrice = unitPrice;
            this.subtotal = subtotal;
        }
    }

    class ButtonRenderer extends JButton implements javax.swing.table.TableCellRenderer {
        public ButtonRenderer() {
            setOpaque(true);
            setBackground(UITheme.RED);
            setForeground(Color.WHITE);
            setFont(new Font("SansSerif", Font.BOLD, 12));
            setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus, int row, int column) {
            setText((value == null) ? LanguageManager.getInstance().getText("cart.remove") : value.toString());
            return this;
        }
    }

    class ButtonEditor extends AbstractCellEditor implements javax.swing.table.TableCellEditor {
        private final JButton button;
        private int selectedRow;

        public ButtonEditor() {
            button = new JButton();
            button.setOpaque(true);
            button.setBackground(UITheme.RED);
            button.setForeground(Color.WHITE);
            button.setFont(new Font("SansSerif", Font.BOLD, 12));
            button.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
            button.addActionListener(e -> fireEditingStopped());
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value,
                                                     boolean isSelected, int row, int column) {
            selectedRow = row;
            button.setText((value == null) ? LanguageManager.getInstance().getText("cart.remove") : value.toString());
            return button;
        }

        @Override
        public Object getCellEditorValue() {
            SwingUtilities.invokeLater(() -> removeItemFromCart(selectedRow));
            return LanguageManager.getInstance().getText("cart.remove");
        }
    }
}