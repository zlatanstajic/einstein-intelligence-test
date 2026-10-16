package com.zlatanstajic.einsteinintelligencetest;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Component;
import java.awt.Container;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.Graphics2D;
import java.awt.BasicStroke;
import java.awt.RenderingHints;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.Window;
import java.util.Collections;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.plaf.FontUIResource;

/** Shared JDK-only appearance; fonts and layout sizes follow Swing's logical scale. */
final class ModernTheme {
    static final Color BACKGROUND = new Color(0xf3f5fa);
    static final Color TEXT = new Color(0x24324b);
    static final Color MUTED = new Color(0x64748b);
    static final Color ACCENT = new Color(0x4f46e5);
    static final Color LINE = new Color(0xe2e8f0);
    static final Color USED = new Color(0x0f766e);
    static final Color USED_BACKGROUND = new Color(0xecfdf5);
    private static boolean installed;
    private static final javax.swing.UIDefaults FLAT_DEFAULTS = new javax.swing.UIDefaults();

    private ModernTheme() {
    }

    static String windowTitle() {
        install();
        return "Einstein's Intelligence Test";
    }

    static void install() {
        if (installed) {
            return;
        }
        try {
            UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException
                | UnsupportedLookAndFeelException exception) {
            throw new IllegalStateException("The bundled Nimbus theme is unavailable", exception);
        }
        for (Object key : Collections.list(UIManager.getDefaults().keys())) {
            if (UIManager.get(key) instanceof FontUIResource) {
                UIManager.put(key, new FontUIResource(Font.SANS_SERIF, Font.PLAIN, 14));
            }
        }
        UIManager.put("defaultFont", new FontUIResource(Font.SANS_SERIF, Font.PLAIN, 14));
        UIManager.put("control", BACKGROUND);
        UIManager.put("text", TEXT);
        UIManager.put("textText", TEXT);
        UIManager.put("nimbusBase", new Color(0x526580));
        UIManager.put("nimbusBlueGrey", LINE);
        UIManager.put("nimbusLightBackground", Color.WHITE);
        UIManager.put("nimbusSelectionBackground", ACCENT);
        UIManager.put("nimbusSelectedText", Color.WHITE);
        UIManager.put("nimbusFocus", ACCENT);
        UIManager.put("ComboBox.contentMargins", new Insets(7, 8, 7, 8));
        UIManager.put("Button.contentMargins", new Insets(8, 14, 8, 14));
        UIManager.put("Button.background", Color.WHITE);
        UIManager.put("Button.foreground", TEXT);
        UIManager.put("Label.foreground", TEXT);
        UIManager.put("CheckBox.foreground", TEXT);
        installFlatPainters();
        for (Object key : Collections.list(UIManager.getDefaults().keys())) {
            String name = key.toString();
            if (name.endsWith("Painter") && (name.startsWith("Button[") || name.startsWith("ComboBox[")
                    || name.startsWith("CheckBox[") || name.startsWith("ScrollBar:"))) {
                FLAT_DEFAULTS.put(key, UIManager.get(key));
            }
        }
        installed = true;
    }

    /** Replace Nimbus gradients with flat surfaces while retaining its keyboard/focus behavior. */
    private static void installFlatPainters() {
        for (Object key : Collections.list(UIManager.getLookAndFeelDefaults().keys())) {
            String name = key.toString();
            if ((name.startsWith("Button[") || name.startsWith("ComboBox["))
                    && name.endsWith(".backgroundPainter")) {
                boolean disabled = name.contains("Disabled");
                boolean focused = name.contains("Focused");
                boolean hovered = name.contains("MouseOver");
                boolean button = name.startsWith("Button[");
                UIManager.put(key, (javax.swing.Painter<JComponent>) (graphics, component, width, height) -> {
                    Graphics2D canvas = drawing(graphics);
                    try {
                        Color fill = disabled ? new Color(0xf8fafc)
                                : button ? component.getBackground() : Color.WHITE;
                        canvas.setColor(fill);
                        canvas.fillRoundRect(1, 1, width - 3, height - 3, 10, 10);
                        canvas.setColor(focused ? ACCENT : hovered ? new Color(0xa5b4fc) : LINE);
                        canvas.setStroke(new BasicStroke(focused ? 2f : 1f));
                        canvas.drawRoundRect(1, 1, width - 3, height - 3, 10, 10);
                    } finally {
                        canvas.dispose();
                    }
                });
            } else if (name.startsWith("CheckBox[") && name.endsWith(".iconPainter")) {
                boolean selected = name.contains("Selected");
                boolean focused = name.contains("Focused");
                boolean disabled = name.contains("Disabled");
                UIManager.put(key, (javax.swing.Painter<JComponent>) (graphics, component, width, height) -> {
                    Graphics2D canvas = drawing(graphics);
                    try {
                        canvas.setColor(selected ? USED : disabled ? BACKGROUND : Color.WHITE);
                        canvas.fillRoundRect(2, 2, width - 5, height - 5, 4, 4);
                        canvas.setColor(focused ? ACCENT : selected ? USED : new Color(0xcbd5e1));
                        canvas.setStroke(new BasicStroke(focused ? 2f : 1f));
                        canvas.drawRoundRect(2, 2, width - 5, height - 5, 4, 4);
                        if (selected) {
                            canvas.setColor(Color.WHITE);
                            canvas.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                            canvas.drawLine(5, height / 2, width / 2 - 1, height - 6);
                            canvas.drawLine(width / 2 - 1, height - 6, width - 5, 5);
                        }
                    } finally {
                        canvas.dispose();
                    }
                });
            } else if (name.startsWith("ScrollBar:ScrollBarThumb[") && name.endsWith(".backgroundPainter")) {
                UIManager.put(key, (javax.swing.Painter<JComponent>) (graphics, component, width, height) -> {
                    Graphics2D canvas = drawing(graphics);
                    try {
                        canvas.setColor(new Color(0xcbd5e1));
                        canvas.fillRoundRect(3, 3, Math.max(2, width - 6), Math.max(2, height - 6), 8, 8);
                    } finally {
                        canvas.dispose();
                    }
                });
            } else if (name.startsWith("ScrollBar:ScrollBarTrack[") && name.endsWith(".backgroundPainter")) {
                UIManager.put(key, (javax.swing.Painter<JComponent>) (graphics, component, width, height) -> {
                    graphics.setColor(new Color(0xf8fafc));
                    graphics.fillRect(0, 0, width, height);
                });
            }
        }
    }

    private static Graphics2D drawing(Graphics2D graphics) {
        Graphics2D canvas = (Graphics2D) graphics.create();
        canvas.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        return canvas;
    }

    static void fonts(Container root) {
        Font font = UIManager.getFont("Label.font");
        for (Component component : root.getComponents()) {
            if (component instanceof JComponent) {
                ((JComponent) component).putClientProperty("Nimbus.Overrides", FLAT_DEFAULTS);
                ((JComponent) component).putClientProperty("Nimbus.Overrides.InheritDefaults", true);
            }
            component.setFont(font.deriveFont(font.getStyle(), font.getSize2D()));
            if (component instanceof JLabel || component instanceof javax.swing.AbstractButton
                    || component instanceof javax.swing.JComboBox) {
                component.setForeground(TEXT);
            }
            if (component instanceof Container) {
                fonts((Container) component);
            }
        }
    }

    static void card(JPanel panel) {
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(LINE, 1, true),
                BorderFactory.createEmptyBorder(20, 20, 20, 20)));
    }

    static void heading(JLabel label, float multiplier) {
        Font font = UIManager.getFont("Label.font");
        label.setFont(font.deriveFont(Font.BOLD, font.getSize2D() * multiplier));
        label.setForeground(TEXT);
    }

    static void muted(JComponent component) {
        component.setForeground(MUTED);
    }

    static void fitOnScreen(Window window, int preferredWidth, int preferredHeight) {
        Rectangle screen = GraphicsEnvironment.getLocalGraphicsEnvironment()
                .getDefaultScreenDevice().getDefaultConfiguration().getBounds();
        int width = Math.min(preferredWidth, Math.max(360, screen.width - 64));
        int height = Math.min(preferredHeight, Math.max(320, screen.height - 80));
        window.setMinimumSize(new Dimension(Math.min(720, width), Math.min(520, height)));
        window.setSize(width, height);
    }
}
