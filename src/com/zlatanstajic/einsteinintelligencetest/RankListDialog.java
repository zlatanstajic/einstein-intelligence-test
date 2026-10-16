package com.zlatanstajic.einsteinintelligencetest;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.FontMetrics;
import java.awt.Frame;
import java.awt.KeyboardFocusManager;
import java.awt.Rectangle;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

/**
 * Modal, read-only rank list of stored winners, fastest first. It offers no way to edit or
 * delete records. Written by hand; there is no NetBeans form for this dialog.
 */
final class RankListDialog extends javax.swing.JDialog {

    private static final long serialVersionUID = 1L;

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", Locale.ROOT);

    RankListDialog(Frame owner, Language language, List<WinnerStore.Winner> winners) {
        super(owner, language.text("Rank List"), true);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        JTable table = null;
        JComponent content;
        if (winners.isEmpty()) {
            JLabel empty = new JLabel(language.text("No winners yet. Solve the puzzle to add your name."));
            empty.setOpaque(true);
            empty.setBackground(Color.WHITE);
            empty.setBorder(BorderFactory.createEmptyBorder(24, 20, 24, 20));
            content = empty;
        } else {
            table = table(language, winners);
            JScrollPane scrollPane = new JScrollPane(table);
            scrollPane.setBorder(null);
            scrollPane.getViewport().setBackground(Color.WHITE);
            scrollPane.getVerticalScrollBar().setUnitIncrement(20);
            content = scrollPane;
        }
        JButton closeButton = new JButton(language.text("Close"));
        closeButton.setBackground(ModernTheme.ACCENT);
        closeButton.addActionListener(event -> dispose());
        JPanel actionsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 0));
        actionsPanel.setBackground(ModernTheme.BACKGROUND);
        actionsPanel.setBorder(BorderFactory.createEmptyBorder(8, 20, 12, 20));
        actionsPanel.add(closeButton);
        getContentPane().setLayout(new BorderLayout());
        getContentPane().setBackground(ModernTheme.BACKGROUND);
        getContentPane().add(content, BorderLayout.CENTER);
        getContentPane().add(actionsPanel, BorderLayout.SOUTH);
        ModernTheme.fonts(getContentPane());
        // Like About's OK button; set after fonts(), which resets button text colors.
        closeButton.setForeground(Color.WHITE);
        if (table != null) {
            size(table, winners.size());
        }
        getRootPane().setDefaultButton(closeButton);
        getRootPane().registerKeyboardAction(event -> dispose(),
                KeyStroke.getKeyStroke("ESCAPE"), JComponent.WHEN_IN_FOCUSED_WINDOW);
        pack();
        Rectangle screen = getGraphicsConfiguration().getBounds();
        setMinimumSize(new Dimension(360, 200));
        setSize(Math.min(getWidth(), screen.width - 64), Math.min(getHeight(), screen.height - 80));
        setLocationRelativeTo(owner);
    }

    private static JTable table(Language language, List<WinnerStore.Winner> winners) {
        String[] columns = {language.text("Rank"), language.text("Name"), language.text("Time"), language.text("Date")};
        Object[][] rows = new Object[winners.size()][];
        for (int index = 0; index < rows.length; index++) {
            WinnerStore.Winner winner = winners.get(index);
            rows[index] = new Object[] {index + 1, winner.name, MainWindow.formatElapsed(winner.solveSeconds),
                DATE.format(Instant.ofEpochMilli(winner.solvedAt).atZone(ZoneId.systemDefault()))};
        }
        DefaultTableModel model = new DefaultTableModel(rows, columns) {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        JTable table = new JTable(model);
        table.getTableHeader().setReorderingAllowed(false);
        // Names are player input: render them as literal text, never as Swing HTML.
        DefaultTableCellRenderer renderer = new DefaultTableCellRenderer();
        renderer.putClientProperty("html.disable", Boolean.TRUE);
        table.setDefaultRenderer(Object.class, renderer);
        table.getAccessibleContext().setAccessibleName(language.text("Rank List"));
        table.setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS,
                Set.of(KeyStroke.getKeyStroke("TAB")));
        table.setFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS,
                Set.of(KeyStroke.getKeyStroke("shift TAB")));
        return table;
    }

    /** Sizes rows and columns from the themed font, so larger fonts keep their text readable. */
    private static void size(JTable table, int rowCount) {
        FontMetrics metrics = table.getFontMetrics(table.getFont());
        table.setRowHeight(metrics.getHeight() + 10);
        int unit = metrics.stringWidth("00000");
        int[] widths = {unit * 2, unit * 7, unit * 2, unit * 4};
        for (int column = 0; column < widths.length; column++) {
            table.getColumnModel().getColumn(column).setPreferredWidth(widths[column]);
        }
        table.setPreferredScrollableViewportSize(new Dimension(unit * 15,
                table.getRowHeight() * Math.min(rowCount, 12)));
    }
}
