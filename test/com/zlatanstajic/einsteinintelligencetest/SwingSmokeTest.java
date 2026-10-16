package com.zlatanstajic.einsteinintelligencetest;

import java.awt.Component;
import java.awt.Container;
import java.awt.GraphicsEnvironment;
import java.awt.KeyboardFocusManager;
import java.awt.Window;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import javax.swing.AbstractButton;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JScrollBar;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.UIManager;

/** Explicit desktop integration checks; reflection keeps test hooks out of production. */
public final class SwingSmokeTest {
    private static MainWindow window;
    private static String pausedTime;
    /** Temporary user.home, so the rank list database never touches the real home directory. */
    private static Path home;

    private interface DialogCheck {
        void accept(JDialog dialog) throws Exception;
    }

    /** Runs an action that opens modal dialogs; checks, then disposes, each new visible dialog it opens. */
    private static List<JDialog> whileOpen(Runnable action, DialogCheck check) throws Exception {
        List<JDialog> observed = new ArrayList<>();
        List<Window> alreadyVisible = new ArrayList<>();
        for (Window owned : window.getOwnedWindows()) {
            if (owned.isVisible()) {
                alreadyVisible.add(owned);
            }
        }
        Throwable[] failure = {null};
        Timer inspector = new Timer(40, event -> {
            for (Window owned : window.getOwnedWindows()) {
                if (owned.isVisible() && owned instanceof JDialog && !alreadyVisible.contains(owned)) {
                    JDialog dialog = (JDialog) owned;
                    observed.add(dialog);
                    try {
                        check.accept(dialog);
                    } catch (Exception | AssertionError problem) {
                        if (failure[0] == null) {
                            failure[0] = problem;
                        }
                    }
                    if (dialog.isVisible()) {
                        dialog.dispose();
                    }
                }
            }
        });
        inspector.start();
        try {
            action.run();
        } finally {
            inspector.stop();
        }
        if (failure[0] instanceof Exception) {
            throw (Exception) failure[0];
        }
        if (failure[0] != null) {
            throw (Error) failure[0];
        }
        return observed;
    }

    private static <T extends Component> List<T> components(Container root, Class<T> type) {
        List<T> found = new ArrayList<>();
        for (Component component : root.getComponents()) {
            if (type.isInstance(component)) {
                found.add(type.cast(component));
            }
            if (component instanceof Container) {
                found.addAll(components((Container) component, type));
            }
        }
        return found;
    }

    private static boolean showsText(Container root, String text) {
        return components(root, JLabel.class).stream().anyMatch(label -> text.equals(label.getText()));
    }

    private static void click(JDialog dialog, String text) {
        for (AbstractButton candidate : dialogButtons(dialog)) {
            if (text.equals(candidate.getText())) {
                candidate.doClick();
                return;
            }
        }
        throw new AssertionError("dialog button: " + text);
    }

    /** Answers the visible Solved dialog: types the name, then presses the button with the translated text. */
    private static void answerSolved(JDialog dialog, String name, String buttonText) {
        List<JTextField> fields = components(dialog, JTextField.class);
        GameSessionTest.require(fields.size() == 1, "Solved dialog has one name field");
        fields.get(0).setText(name);
        click(dialog, buttonText);
    }

    /** Dialog buttons, excluding the arrow buttons inside scroll bars. */
    private static List<AbstractButton> dialogButtons(JDialog dialog) {
        List<AbstractButton> buttons = components(dialog, AbstractButton.class);
        buttons.removeIf(button -> SwingUtilities.getAncestorOfClass(JScrollBar.class, button) != null);
        return buttons;
    }

    private static <T> T field(Object owner, String name, Class<T> type) throws Exception {
        Field field = owner.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return type.cast(field.get(owner));
    }

    private static JButton button(String name) throws Exception {
        return field(window, name + "Button", JButton.class);
    }

    private static void language(int index) throws Exception {
        field(window, "languageMenu", JMenu.class).getItem(index).doClick();
    }

    private static void fill(boolean correct) throws Exception {
        JComboBox<?>[][] boxes = field(window, "answerComboBoxes", JComboBox[][].class);
        for (JComboBox<?>[] row : boxes) {
            for (JComboBox<?> box : row) {
                box.setSelectedIndex(0);
            }
        }
        String[][] solution = GameSessionTest.solution();
        for (int row = 0; row < boxes.length; row++) {
            for (int house = 0; house < boxes[row].length; house++) {
                boxes[row][house].setSelectedItem(solution[row][house]);
            }
        }
        if (!correct) {
            Object first = boxes[0][0].getSelectedItem();
            Object third = boxes[0][2].getSelectedItem();
            boxes[0][0].setSelectedIndex(0);
            boxes[0][2].setSelectedIndex(0);
            boxes[0][0].setSelectedItem(third);
            boxes[0][2].setSelectedItem(first);
        }
    }

    private static void submit(String title) throws Exception {
        String[] observed = {null};
        Timer closer = new Timer(40, event -> {
            for (Window dialog : window.getOwnedWindows()) {
                if (dialog.isVisible() && dialog instanceof javax.swing.JDialog) {
                    observed[0] = ((javax.swing.JDialog) dialog).getTitle();
                    dialog.dispose();
                }
            }
        });
        closer.start();
        try {
            button("check").doClick();
        } finally {
            closer.stop();
        }
        GameSessionTest.require(title.equals(observed[0]), "submission dialog: " + observed[0]);
    }

    private static void startupAndPause() throws Exception {
        window = new MainWindow();
        window.setVisible(true);
        assertModernInterface();
        GameSessionTest.require(button("start").isEnabled() && !button("pause").isEnabled(), "initial controls");
        GameSessionTest.require(window.getTitle().equals("Einstein's Intelligence Test"), "English startup");
        GameSessionTest.require(field(window, "languageMenu", JMenu.class).getIcon() != null, "language icon");
        JLabel elapsed = field(window, "elapsedTimeLabel", JLabel.class);
        GameSessionTest.require(elapsed.getParent() == window.getJMenuBar() && elapsed.getIcon() != null, "top-bar clock");
        GameSessionTest.require(button("exit").getX() > button("check").getX(), "Exit on right");
        assertClueCounter("0/15");
        invokeShortcut("start");
        GameSessionTest.require(field(window, "statusLabel", JLabel.class).getText().equals("Game in progress"),
                "running status");
        answerChoicesAreUnique();
        fill(false);
        clueCounterTracksEveryMarking();
        field(window, "clue1CheckBox", JCheckBox.class).doClick();
        assertClueCounter("1/15");
        submit("Incorrect solution");
        language(1);
        GameSessionTest.require(button("start").getText().equals("Počni"), "Serbian running");
        assertClueCounter("1/15");
        invokeShortcut("pause");
        pausedTime = elapsed.getText();
        assertPaused();
        invokeShortcut("start");
        invokeShortcut("restart");
        invokeShortcut("check");
        assertPaused();
        language(0);
        assertPaused();
        language(1);
        assertPaused();
        GameSessionTest.require(button("pause").getText().equals("Nastavi"), "translated resume");
        assertClueCounter("1/15");
        GameSessionTest.require(field(window, "statusLabel", JLabel.class).getText()
                .equals("Pauza — nastavite kada budete spremni"), "translated pause status");
        GameSessionTest.require(field(window, "house1Label", JLabel.class).getText().equals("Kuća 1"),
                "translated house label");
        GameSessionTest.require(field(window, "clue1CheckBox", JCheckBox.class).isSelected(), "clue retained");
        GameSessionTest.require(field(window, "houseColorComboBox1", JComboBox.class).getSelectedItem().equals("Red"), "answer retained");
        GameSessionTest.require(button("check").getText().endsWith("(2)"), "attempt retained");
    }

    private static void assertClueCounter(String expected) throws Exception {
        assertClueOrder();
        JLabel counter = field(window, "clueCounterLabel", JLabel.class);
        GameSessionTest.require(counter.getText().equals(expected), "clue counter: " + expected);
        GameSessionTest.require(counter.getAccessibleContext().getAccessibleName().endsWith(": " + expected),
                "accessible clue counter");
        JLabel heading = field(window, "cluesLabel", JLabel.class);
        if (counter.isShowing()) {
            GameSessionTest.require(Math.abs(counter.getY() * 2 + counter.getHeight()
                    - heading.getY() * 2 - heading.getHeight()) <= 2,
                    "counter shares the Clues heading line");
        }
    }

    private static void clueCounterTracksEveryMarking() throws Exception {
        JCheckBox[] clues = field(window, "clueCheckBoxes", JCheckBox[].class);
        for (JCheckBox clue : clues) {
            clue.doClick(0);
            assertClueOrder();
        }
        assertClueCounter("15/15");
        clues[0].doClick(0);
        assertClueCounter("14/15");
        for (JCheckBox clue : clues) {
            clue.setSelected(false);
        }
        assertClueCounter("0/15");
    }

    private static void answerChoicesAreUnique() throws Exception {
        JComboBox<?>[][] rows = field(window, "answerComboBoxes", JComboBox[][].class);
        for (JComboBox<?>[] row : rows) {
            Object value = row[0].getItemAt(1);
            row[0].setSelectedItem(value);
            for (int house = 1; house < row.length; house++) {
                row[house].setSelectedItem(value);
                GameSessionTest.require(row[house].getSelectedIndex() == 0,
                        "used value cannot be selected in another house");
                row[house].setSelectedIndex(1);
                GameSessionTest.require(row[house].getSelectedIndex() == 0,
                        "index selection also rejects used values");
            }
            assertChoiceEnabled(row[1], value, false);
            row[0].setSelectedIndex(2);
            assertChoiceEnabled(row[1], value, true);
            row[1].setSelectedItem(value);
            GameSessionTest.require(value.equals(row[1].getSelectedItem()), "changed value is released");
            row[1].setSelectedIndex(0);
            row[0].setSelectedIndex(0);
            row[1].setSelectedIndex(2);
            GameSessionTest.require(row[1].getSelectedIndex() == 2, "cleared value is released");
            row[1].setSelectedIndex(0);
        }
    }

    private static <T> void assertChoiceEnabled(JComboBox<T> combo, Object value, boolean enabled) {
        @SuppressWarnings("unchecked")
        T item = (T) value;
        Component cell = combo.getRenderer().getListCellRendererComponent(
                new javax.swing.JList<T>(), item, 1, false, false);
        GameSessionTest.require(cell.isEnabled() == enabled, "used choice is visually disabled");
    }

    private static void assertClueOrder() throws Exception {
        javax.swing.JPanel panel = field(window, "clueListPanel", javax.swing.JPanel.class);
        JCheckBox[] clues = field(window, "clueCheckBoxes", JCheckBox[].class);
        java.awt.GridBagLayout layout = (java.awt.GridBagLayout) panel.getLayout();
        int position = 0;
        for (boolean checked : new boolean[] {false, true}) {
            for (JCheckBox clue : clues) {
                if (clue.isSelected() == checked) {
                    GameSessionTest.require(panel.getComponent(position) == clue
                            && layout.getConstraints(clue).gridy == position,
                            "unchecked clues precede checked clues in original order");
                    position++;
                }
            }
        }
    }

    private static void invokeShortcut(String action) {
        javax.swing.Action shortcut = window.getRootPane().getActionMap().get(action);
        GameSessionTest.require(shortcut != null, "shortcut exists: " + action);
        shortcut.actionPerformed(new java.awt.event.ActionEvent(window, 0, action));
    }

    private static void assertModernInterface() throws Exception {
        GameSessionTest.require(window.isResizable(), "main window is resizable");
        GameSessionTest.require(UIManager.getLookAndFeel().getName().equals("Nimbus"), "bundled modern theme");
        GameSessionTest.require(field(window, "answerGridPanel", javax.swing.JPanel.class).getLayout()
                instanceof java.awt.GridBagLayout, "answer grid uses layout manager");
        GameSessionTest.require(field(window, "contentScrollPane", javax.swing.JScrollPane.class)
                .getViewport().getView() != null, "small-screen scrolling available");
        for (int house = 1; house <= 5; house++) {
            GameSessionTest.require(field(window, "house" + house + "Label", JLabel.class).getText()
                    .equals("House " + house), "numbered house: " + house);
        }
        for (JComboBox<?>[] row : field(window, "answerComboBoxes", JComboBox[][].class)) {
            for (JComboBox<?> box : row) {
                GameSessionTest.require(box.getAccessibleContext().getAccessibleName().contains("House"),
                        "answer has house and attribute accessible name");
            }
        }
        GameSessionTest.require(field(window, "statusLabel", JLabel.class).getText().equals("Ready to play"),
                "idle status");
        GameSessionTest.require(window.getRootPane().getDefaultButton() == button("check"), "Enter checks answers");
        GameSessionTest.require(window.getRootPane().getInputMap(javax.swing.JComponent.WHEN_IN_FOCUSED_WINDOW)
                .get(javax.swing.KeyStroke.getKeyStroke("alt P")).equals("pause"), "pause key bound");
    }

    private static void assertPaused() throws Exception {
        GameSessionTest.require(!button("start").isEnabled() && !button("restart").isEnabled()
                && !button("check").isEnabled() && button("pause").isEnabled(), "paused controls");
        for (JComboBox<?>[] row : field(window, "answerComboBoxes", JComboBox[][].class)) {
            for (JComboBox<?> box : row) {
                GameSessionTest.require(!box.isEnabled(), "paused answers locked");
            }
        }
        for (JCheckBox clue : field(window, "clueCheckBoxes", JCheckBox[].class)) {
            GameSessionTest.require(!clue.isEnabled(), "paused clues locked");
        }
    }

    private static void resumeAndFinish() throws Exception {
        GameSessionTest.require(field(window, "elapsedTimeLabel", JLabel.class).getText().equals(pausedTime), "paused display frozen");
        button("pause").doClick();
        GameSessionTest.require(button("restart").isEnabled() && button("check").isEnabled(), "resume controls");
        button("pause").doClick();
        button("pause").doClick();
        button("restart").doClick();
        assertClueCounter("0/15");
        GameSessionTest.require(button("check").getText().endsWith("(3)"), "restart attempts");
        GameSessionTest.require(field(window, "elapsedTimeLabel", JLabel.class).getText().equals("00:00"), "restart time");
        for (JComboBox<?>[] row : field(window, "answerComboBoxes", JComboBox[][].class)) {
            for (JComboBox<?> box : row) {
                GameSessionTest.require(box.getSelectedIndex() == 0, "restart clears answers");
            }
        }
        for (JCheckBox clue : field(window, "clueCheckBoxes", JCheckBox[].class)) {
            GameSessionTest.require(!clue.isSelected() && clue.getForeground().equals(ModernTheme.TEXT), "restart clears clues");
        }
        language(0);
        assertClueCounter("0/15");
        fill(true);
        field(window, "clue1CheckBox", JCheckBox.class).doClick(0);
        assertClueCounter("1/15");
        submit("Solved");
        GameSessionTest.require(new WinnerStore(WinnerStore.defaultDatabase()).ranked().isEmpty(),
                "closing the Solved dialog stores nothing");
        GameSessionTest.require(button("start").isEnabled() && !button("pause").isEnabled(), "win controls");
        button("start").doClick();
        assertClueCounter("0/15");
        fill(false);
        submit("Incorrect solution");
        submit("Incorrect solution");
        submit("Game over");
        GameSessionTest.require(button("start").isEnabled() && !button("check").isEnabled(), "exhausted controls");
        AboutDialog about = field(window, "aboutDialog", AboutDialog.class);
        String contact = field(about, "authorContactLabel", JEditorPane.class).getText();
        GameSessionTest.require(contact.indexOf("Zlatan") < contact.indexOf("mailto:")
                && contact.indexOf("mailto:") < contact.indexOf("https://zlatanstajic.com"), "contact order");
        about.setModal(false);
        about.setVisible(true);
        JButton close = field(about, "okButton", JButton.class);
        Container actions = close.getParent();
        GameSessionTest.require(Math.abs(close.getX() * 2 + close.getWidth() - actions.getWidth()) <= 2,
                "About OK button is horizontally centered");
        JEditorPane author = field(about, "authorContactLabel", JEditorPane.class);
        GameSessionTest.require(author.getInsets().left == 0 && author.getMargin().left == 0,
                "author details have no left inset or margin");
        GameSessionTest.require(author.getX() == field(about, "authorHeadingLabel", JLabel.class).getX(),
                "author details align with heading");
        int nameOffset = author.getDocument().getText(0, author.getDocument().getLength()).indexOf("Zlatan");
        GameSessionTest.require(nameOffset >= 0 && author.modelToView2D(nameOffset).getX() <= 1,
                "author text starts at the component's left edge");
        GameSessionTest.require(!containsHistoricalDates(about.getContentPane()), "removed dates are absent");
        GameSessionTest.require(field(about, "howToSolveHeadingLabel", JLabel.class).getY() <= 24,
                "About content is top aligned with compact padding");
        JLabel firstLine = field(about, "instructionsLine1Label", JLabel.class);
        JLabel secondLine = field(about, "instructionsLine2Label", JLabel.class);
        GameSessionTest.require(secondLine.getY() - firstLine.getY() - firstLine.getHeight() <= 4,
                "About instructions use compact line spacing");
        GameSessionTest.require(about.getHeight() <= about.getPreferredSize().height + 12,
                "About packs to content rather than forcing a tall modal");
        assess(window.getContentPane(), "main");
        assess(about.getContentPane(), "about");
        language(1);
        assess(window.getContentPane(), "main Serbian");
        assess(about.getContentPane(), "about Serbian");
        GameSessionTest.require(!containsHistoricalDates(about.getContentPane()), "removed dates absent in Serbian");
    }

    private static void rankList() throws Exception {
        language(0);
        JMenuBar menuBar = window.getJMenuBar();
        JButton rankListButton = button("rankList");
        int position = menuBar.getComponentIndex(rankListButton);
        GameSessionTest.require(position > 0 && menuBar.getComponent(position - 1) == field(window, "languageMenu", JMenu.class)
                && menuBar.getComponent(position + 1) == button("about"), "Rank List between Language and About");
        GameSessionTest.require(rankListButton.getIcon() != null, "rank list icon");
        GameSessionTest.require("rankList".equals(window.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .get(KeyStroke.getKeyStroke("alt L"))), "Alt+L opens the rank list");
        List<JDialog> opened = whileOpen(() -> invokeShortcut("rankList"), dialog -> {
            GameSessionTest.require(dialog instanceof RankListDialog && dialog.getTitle().equals("Rank List"), "rank list title");
            GameSessionTest.require(components(dialog, JTable.class).isEmpty()
                    && showsText(dialog, "No winners yet. Solve the puzzle to add your name."), "empty rank list message");
        });
        GameSessionTest.require(opened.size() == 1, "empty rank list opened once");
        WinnerStore store = new WinnerStore(WinnerStore.defaultDatabase());
        String html = "<html><b>Bold</b>";
        store.add(html, 75, 2_000_000L);
        store.add("O'Brien", 30, 3_000_000L);
        store.add("Zed", 75, 1_000_000L);
        DateTimeFormatter date = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        String[][] expected = {
            {"1", "O'Brien", "00:30", date.format(Instant.ofEpochMilli(3_000_000L).atZone(ZoneId.systemDefault()))},
            {"2", "Zed", "01:15", date.format(Instant.ofEpochMilli(1_000_000L).atZone(ZoneId.systemDefault()))},
            {"3", html, "01:15", date.format(Instant.ofEpochMilli(2_000_000L).atZone(ZoneId.systemDefault()))},
        };
        opened = whileOpen(() -> invokeShortcut("rankList"), dialog -> {
            JTable table = components(dialog, JTable.class).get(0);
            GameSessionTest.require(table.getRowCount() == expected.length, "every record listed");
            for (int row = 0; row < expected.length; row++) {
                for (int column = 0; column < expected[row].length; column++) {
                    GameSessionTest.require(expected[row][column].equals(String.valueOf(table.getValueAt(row, column))),
                            "rank list cell " + row + "," + column + ": " + table.getValueAt(row, column));
                    GameSessionTest.require(!table.isCellEditable(row, column), "rank list is read-only");
                }
            }
            GameSessionTest.require(table.getRowSorter() == null, "rank list order cannot be re-sorted");
            List<AbstractButton> buttons = dialogButtons(dialog);
            GameSessionTest.require(buttons.size() == 1 && buttons.get(0).getText().equals("Close"), "Close is the only button");
            GameSessionTest.require(dialog.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                    .get(KeyStroke.getKeyStroke("ESCAPE")) != null, "Escape closes the rank list");
            GameSessionTest.require(table.getFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS)
                    .contains(KeyStroke.getKeyStroke("TAB")), "Tab leaves the table");
            GameSessionTest.require("Rank List".equals(table.getAccessibleContext().getAccessibleName()),
                    "table accessible name");
            JComponent cell = (JComponent) table.prepareRenderer(table.getCellRenderer(2, 1), 2, 1);
            GameSessionTest.require(cell.getClientProperty("html") == null && html.equals(((JLabel) cell).getText()),
                    "names render as literal text");
        });
        GameSessionTest.require(opened.size() == 1, "rank list opened once");
        language(1);
        opened = whileOpen(() -> invokeShortcut("rankList"), dialog -> {
            JTable table = components(dialog, JTable.class).get(0);
            GameSessionTest.require(dialog.getTitle().equals("Rang lista")
                    && "Rang lista".equals(table.getAccessibleContext().getAccessibleName()), "Serbian rank list title");
            String[] headers = {"Mesto", "Ime", "Vreme", "Datum"};
            for (int column = 0; column < headers.length; column++) {
                GameSessionTest.require(headers[column].equals(table.getColumnName(column)), "Serbian header " + headers[column]);
            }
            GameSessionTest.require(dialogButtons(dialog).get(0).getText().equals("Zatvori"), "Serbian Close button");
        });
        GameSessionTest.require(opened.size() == 1, "Serbian rank list opened once");
        Path directory = WinnerStore.defaultDatabase().getParent();
        Path saved = directory.resolveSibling("saved-rank-list");
        Files.move(directory, saved);
        Files.createFile(directory);
        try {
            opened = whileOpen(() -> invokeShortcut("rankList"), dialog -> GameSessionTest.require(
                    !(dialog instanceof RankListDialog) && dialog.getTitle().equals("Rang lista")
                    && showsText(dialog, "Rang listu nije moguće učitati."), "rank list load error"));
            GameSessionTest.require(opened.size() == 1, "load failure opens only the error message");
        } finally {
            Files.delete(directory);
            Files.move(saved, directory);
        }
        language(0);
    }

    private static void nameEntry() throws Exception {
        language(0);
        WinnerStore store = new WinnerStore(WinnerStore.defaultDatabase());
        int stored = store.ranked().size();
        JButton check = button("check");
        JLabel elapsed = field(window, "elapsedTimeLabel", JLabel.class);
        button("start").doClick();
        fill(true);
        String[] names = {"   ", "x".repeat(41), " Ada Lovelace "};
        int[] answered = {0};
        List<JDialog> opened = whileOpen(check::doClick, dialog -> {
            if (dialog.getTitle().equals("Invalid name")) {
                GameSessionTest.require(showsText(dialog, "Enter a name of 1 to 40 characters."), "invalid name message");
                return;
            }
            GameSessionTest.require(dialog.getTitle().equals("Solved"), "name dialog title: " + dialog.getTitle());
            JTextField name = components(dialog, JTextField.class).get(0);
            GameSessionTest.require("Enter your name for the rank list:".equals(name.getAccessibleContext().getAccessibleName()),
                    "name field accessible name");
            GameSessionTest.require(answered[0] == 0 || name.getText().equals(names[answered[0] - 1]), "typed name kept");
            answerSolved(dialog, names[answered[0]++], "Save");
        });
        GameSessionTest.require(opened.stream().filter(dialog -> dialog.getTitle().equals("Invalid name")).count() == 2,
                "two invalid names rejected");
        List<WinnerStore.Winner> winners = store.ranked();
        GameSessionTest.require(winners.size() == stored + 1, "one record saved");
        GameSessionTest.require(winners.stream().filter(winner -> winner.name.equals("Ada Lovelace")
                && MainWindow.formatElapsed(winner.solveSeconds).equals(elapsed.getText())).count() == 1,
                "saved name is stripped and time matches the clock");
        stored++;
        button("start").doClick();
        fill(true);
        whileOpen(check::doClick, dialog -> answerSolved(dialog, "Nobody", "Cancel"));
        GameSessionTest.require(store.ranked().size() == stored, "Cancel stores nothing");
        button("start").doClick();
        fill(false);
        submit("Incorrect solution");
        submit("Incorrect solution");
        opened = whileOpen(check::doClick, dialog -> GameSessionTest.require(dialog.getTitle().equals("Game over")
                && components(dialog, JTextField.class).isEmpty(), "Game over has no name field"));
        GameSessionTest.require(opened.size() == 1 && store.ranked().size() == stored, "a lost game stores nothing");
        Path directory = WinnerStore.defaultDatabase().getParent();
        Path saved = directory.resolveSibling("saved-rank-list");
        Files.move(directory, saved);
        Files.createFile(directory);
        String[] clock = {null};
        try {
            button("start").doClick();
            fill(true);
            opened = whileOpen(check::doClick, dialog -> {
                if (dialog.getTitle().equals("Solved")) {
                    clock[0] = elapsed.getText();
                    answerSolved(dialog, "Grace", "Save");
                } else {
                    GameSessionTest.require(dialog.getTitle().equals("Rank List")
                            && showsText(dialog, "Your result could not be saved to the rank list."), "save error message");
                }
            });
            GameSessionTest.require(opened.size() == 2, "save failure shows one error message");
        } finally {
            Files.delete(directory);
            Files.move(saved, directory);
        }
        GameSessionTest.require(field(window, "statusLabel", JLabel.class).getText().equals("Puzzle solved — well done!")
                && button("start").isEnabled() && !check.isEnabled(), "game stays solved after a save failure");
        for (JComboBox<?>[] row : field(window, "answerComboBoxes", JComboBox[][].class)) {
            for (JComboBox<?> box : row) {
                GameSessionTest.require(!box.isEnabled(), "answers stay locked after a save failure");
            }
        }
        GameSessionTest.require(elapsed.getText().equals(clock[0]) && !field(window, "gameTimer", Timer.class).isRunning(),
                "clock stays frozen after a save failure");
        GameSessionTest.require(store.ranked().size() == stored, "failed save stores nothing");
        language(1);
        button("start").doClick();
        fill(true);
        whileOpen(check::doClick, dialog -> {
            GameSessionTest.require(dialog.getTitle().equals("Rešeno"), "Serbian name dialog title");
            GameSessionTest.require("Unesite ime za rang listu:".equals(components(dialog, JTextField.class).get(0)
                    .getAccessibleContext().getAccessibleName()), "Serbian name field accessible name");
            GameSessionTest.require(dialogButtons(dialog).stream().anyMatch(button -> button.getText().equals("Sačuvaj")),
                    "Serbian Save button");
            answerSolved(dialog, "Nobody", "Otkaži");
        });
        GameSessionTest.require(store.ranked().size() == stored, "Serbian Cancel stores nothing");
        language(0);
    }

    private static boolean containsHistoricalDates(Container root) {
        for (Component component : root.getComponents()) {
            if (component instanceof JLabel) {
                String text = ((JLabel) component).getText();
                if ("Built in 2013".equals(text) || "Refreshed in 2026".equals(text)
                        || "Napravljeno 2013".equals(text) || "Osveženo 2026".equals(text)) {
                    return true;
                }
            }
            if (component instanceof Container && containsHistoricalDates((Container) component)) {
                return true;
            }
        }
        return false;
    }

    private static void assess(Container root, String name) {
        int clipped = 0;
        for (Component component : root.getComponents()) {
            if (component instanceof javax.swing.JPanel || component instanceof javax.swing.JViewport
                    || component instanceof javax.swing.JScrollPane) {
                assess((Container) component, name + "/" + component.getClass().getSimpleName());
            }
            if (component instanceof JLabel) {
                JLabel label = (JLabel) component;
                if (label.getText() != null && !label.getText().isEmpty()
                        && label.getPreferredSize().width > label.getWidth()) { clipped++; }
            } else if (component instanceof AbstractButton) {
                AbstractButton button = (AbstractButton) component;
                if (button.getPreferredSize().width > button.getWidth()) {
                    clipped++;
                }
            }
        }
        if (clipped > 0) {
            System.out.println("Layout " + name + ": " + clipped + " labels/buttons wider than bounds");
        }
    }

    private static void checkResponsiveLayout(int width, int height, boolean expectStacked) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            window.setSize(width, height);
            window.validate();
        });
        // Deliver resize events before asserting layout constraints and viewport sizes.
        for (int eventPass = 0; eventPass < 5; eventPass++) {
            SwingUtilities.invokeAndWait(() -> { });
        }
        SwingUtilities.invokeAndWait(() -> {
            try {
                window.validate();
                javax.swing.JPanel body = field(window, "bodyPanel", javax.swing.JPanel.class);
                javax.swing.JPanel solution = field(window, "solutionPanel", javax.swing.JPanel.class);
                javax.swing.JPanel clues = field(window, "cluesPanel", javax.swing.JPanel.class);
                java.awt.GridBagLayout layout = (java.awt.GridBagLayout) body.getLayout();
                java.awt.GridBagConstraints first = layout.getConstraints(solution);
                java.awt.GridBagConstraints second = layout.getConstraints(clues);
                if (expectStacked) {
                    GameSessionTest.require(first.gridx == second.gridx && second.gridy > first.gridy,
                            "narrow window stacks clues below answers");
                } else if (Math.max(1200, solution.getPreferredSize().width + 468)
                        <= field(window, "contentScrollPane", javax.swing.JScrollPane.class).getViewport().getWidth()) {
                    GameSessionTest.require(second.gridx > first.gridx, "wide window places cards side by side");
                }
                javax.swing.JScrollPane scroll = field(window, "cluesScrollPane", javax.swing.JScrollPane.class);
                GameSessionTest.require(scroll.getViewport().getWidth() > 0, "clue viewport remains reachable");
                GameSessionTest.require(!scroll.getHorizontalScrollBar().isVisible(), "clue text wraps without horizontal scrolling");
                for (JComboBox<?>[] row : field(window, "answerComboBoxes", JComboBox[][].class)) {
                    for (JComboBox<?> answer : row) {
                        GameSessionTest.require(answer.getHeight() >= answer.getFontMetrics(answer.getFont()).getHeight(),
                                "answer text fits vertically after resizing");
                    }
                }
            } catch (Exception exception) {
                throw new RuntimeException(exception);
            }
        });
    }

    public static void main(String[] args) throws Exception {
        if (GraphicsEnvironment.isHeadless()) {
            throw new IllegalStateException("make test-ui requires an accessible graphical display (DISPLAY).");
        }
        home = Files.createTempDirectory("swing-smoke-home");
        System.setProperty("user.home", home.toString());
        ModernTheme.install();
        if (args.length > 0) {
            float factor = Float.parseFloat(args[0]);
            for (Object key : java.util.Collections.list(UIManager.getDefaults().keys())) {
                Object value = UIManager.get(key);
                if (value instanceof javax.swing.plaf.FontUIResource) {
                    java.awt.Font font = (java.awt.Font) value;
                    UIManager.put(key, new javax.swing.plaf.FontUIResource(font.deriveFont(font.getSize2D() * factor)));
                }
            }
        }
        try {
            SwingUtilities.invokeAndWait(() -> {
                try {
                    startupAndPause();
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            });
            // Let several display-refresh intervals pass, outside the event dispatch thread.
            Thread.sleep(1200);
            SwingUtilities.invokeAndWait(() -> {
                try {
                    resumeAndFinish();
                    rankList();
                    nameEntry();
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            });
            checkResponsiveLayout(900, 760, true);
            checkResponsiveLayout(1500, 900, false);
            System.out.println("SwingSmokeTest passed");
        } finally {
            try {
                SwingUtilities.invokeAndWait(() -> {
                    for (Window owned : Window.getWindows()) {
                        owned.dispose();
                    }
                    if (window != null) {
                        try {
                            field(window, "gameTimer", Timer.class).stop();
                        } catch (Exception ex) {
                            throw new RuntimeException(ex);
                        }
                    }
                });
            } finally {
                try (java.util.stream.Stream<Path> paths = Files.walk(home)) {
                    for (Path path : (Iterable<Path>) paths.sorted(Comparator.reverseOrder())::iterator) {
                        Files.delete(path);
                    }
                }
            }
        }
    }
}
