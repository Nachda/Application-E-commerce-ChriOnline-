package ui.components;

import utils.UITheme;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

/**
 * Table simple avec style sombre.
 */
public class AppTable extends JTable {

    public AppTable(DefaultTableModel model) {
        super(model);
        setBackground(UITheme.CARD_2);
        setForeground(UITheme.TEXT);
        setRowHeight(30);
        setFont(new Font("Segoe UI", Font.PLAIN, 13));
        getTableHeader().setBackground(UITheme.CARD);
        getTableHeader().setForeground(UITheme.TEXT);
        getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));
        setSelectionBackground(new Color(67, 139, 208));
        setGridColor(UITheme.BORDER);
    }
}