package com.zlatanstajic.einsteinintelligencetest;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

public final class LanguageTest {
    public static void main(String[] args) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            Language language = new Language();
            JPanel panel = new JPanel();
            JLabel label = new JLabel("Start");
            panel.add(label);
            JComboBox<String> answers = new JComboBox<>(new String[] {"", "Yellow", "Blue"});
            answers.setSelectedItem("Yellow");
            answers.setRenderer(language.answerRenderer());
            GameSessionTest.require(language.text("Start").equals("Start"), "English default");
            language.setSerbian(true);
            language.apply(panel);
            GameSessionTest.require(label.getText().equals("Počni"), "translated component");
            GameSessionTest.require(language.text("Yellow").equals("Žuta"), "Serbian answer");
            for (String key : new String[] {"Rank List", "Rank", "Name", "Time", "Date", "Close",
                    "No winners yet. Solve the puzzle to add your name.", "The rank list could not be loaded.",
                    "Your time: ", "Enter your name for the rank list:", "Save", "Cancel", "Invalid name",
                    "Enter a name of 1 to 40 characters.", "Your result could not be saved to the rank list."}) {
                GameSessionTest.require(!language.text(key).equals(key), "Serbian text: " + key);
            }
            JLabel rendered = (JLabel) language.answerRenderer().getListCellRendererComponent(
                    new JList<>(), "Yellow", 1, false, false);
            GameSessionTest.require(rendered.getText().equals("Žuta"), "Serbian renderer");
            GameSessionTest.require(answers.getSelectedItem().equals("Yellow")
                    && answers.getItemAt(1).equals("Yellow"), "canonical values preserved");
            language.setSerbian(false);
            language.apply(panel);
            GameSessionTest.require(label.getText().equals("Start"), "English restored");
            rendered = (JLabel) language.answerRenderer().getListCellRendererComponent(
                    new JList<>(), "Yellow", 1, false, false);
            GameSessionTest.require(rendered.getText().equals("Yellow"), "English renderer");
            GameSessionTest.require(language.text("Unmapped").equals("Unmapped"), "fallback");
        });
        System.out.println("LanguageTest passed");
    }
}
