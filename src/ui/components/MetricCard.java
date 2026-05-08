package ui.components;

import utils.UITheme;

import javax.swing.*;
import java.awt.*;

/**
 * Carte de métrique simple (utilisée dans le tableau de bord).
 */
public class MetricCard extends JPanel {

    private JLabel titleLabel, valueLabel, subtitleLabel;

    public MetricCard(String title, String value, String subtitle) {
        setBackground(UITheme.CARD);
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));

        titleLabel = new JLabel(title);
        titleLabel.setForeground(UITheme.MUTED);
        titleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));

        valueLabel = new JLabel(value);
        valueLabel.setForeground(UITheme.TEXT);
        valueLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));

        subtitleLabel = new JLabel(subtitle);
        subtitleLabel.setForeground(UITheme.MUTED);
        subtitleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));

        add(titleLabel);
        add(Box.createVerticalStrut(4));
        add(valueLabel);
        add(Box.createVerticalStrut(4));
        add(subtitleLabel);
    }

    public void setValue(String value) {
        valueLabel.setText(value);
    }

    public void setTitle(String title) {
        titleLabel.setText(title);
    }

    public void setSubtitle(String subtitle) {
        subtitleLabel.setText(subtitle);
    }
}