package com.zlatanstajic.einsteinintelligencetest;

import java.awt.Component;
import java.awt.Container;
import java.util.HashMap;
import java.util.Map;
import javax.swing.AbstractButton;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;

/** Per-window translations; answer models retain their language-independent values. */
final class Language {
    private final Map<String, String> serbian = new HashMap<>();
    private boolean serbianSelected;

    Language() {
        serbian.put("Use the clues to match each person to a house.", "Pomoću tragova povežite svaku osobu sa kućom.");
        serbian.put("Attribute", "Osobina");
        serbian.put("Your houses", "Vaše kuće");
        serbian.put("Clues", "Tragovi");
        serbian.put("Checked", "Označeno");
        serbian.put("Choose one answer in every field, then check your solution.", "Popunite sva polja, a zatim proverite rešenje.");
        serbian.put("Ready to play", "Spremni za igru");
        serbian.put("Attempts left", "Preostalo pokušaja");
        serbian.put("Paused — resume when you are ready", "Pauza — nastavite kada budete spremni");
        serbian.put("Puzzle solved — well done!", "Zagonetka je rešena — bravo!");
        serbian.put("Game over — try again", "Kraj igre — pokušajte ponovo");
        serbian.put("All houses filled — ready to check", "Sve kuće su popunjene — proverite rešenje");
        serbian.put("Game in progress", "Igra je u toku");
        serbian.put("House 1", "Kuća 1");
        serbian.put("House 2", "Kuća 2");
        serbian.put("House 3", "Kuća 3");
        serbian.put("House 4", "Kuća 4");
        serbian.put("House 5", "Kuća 5");
        serbian.put("Einstein's Intelligence Test", "Ajnštajnov test inteligencije");
        serbian.put("Language", "Jezik");
        serbian.put("English", "Engleski");
        serbian.put("Serbian (Latin)", "Srpski (latinica)");
        serbian.put("Exit", "Izlaz");
        serbian.put("Start", "Počni");
        serbian.put("Pause", "Pauza");
        serbian.put("Resume", "Nastavi");
        serbian.put("Restart", "Ponovi");
        serbian.put("Check", "Proveri");
        serbian.put("Website", "Sajt");
        serbian.put("About", "O igri");
        serbian.put("Clues:", "Tragovi:");
        serbian.put("Solution:", "Rešenje:");
        serbian.put("House color:", "Boja kuće:");
        serbian.put("Nationality: ", "Nacionalnost:");
        serbian.put("Favorite drink:", "Omiljeno piće:");
        serbian.put("Sport:", "Sport:");
        serbian.put("Pet:", "Ljubimac:");
        serbian.put("White", "Bela");
        serbian.put("Green", "Zelena");
        serbian.put("Yellow", "Žuta");
        serbian.put("Blue", "Plava");
        serbian.put("Red", "Crvena");
        serbian.put("German", "Nemac");
        serbian.put("Dane", "Danac");
        serbian.put("Brit", "Britanac");
        serbian.put("Norwegian", "Norvežanin");
        serbian.put("Swede", "Šveđanin");
        serbian.put("Water", "Voda");
        serbian.put("Tea", "Čaj");
        serbian.put("Coffee", "Kafa");
        serbian.put("Milk", "Mleko");
        serbian.put("Beer", "Pivo");
        serbian.put("Cycling", "Biciklizam");
        serbian.put("Golf", "Golf");
        serbian.put("Basketball", "Košarka");
        serbian.put("Swimming", "Plivanje");
        serbian.put("Volleyball", "Odbojka");
        serbian.put("Horse", "Konj");
        serbian.put("Cat", "Mačka");
        serbian.put("Dog", "Pas");
        serbian.put("Bird", "Ptica");
        serbian.put("Fish", "Riba");
        serbian.put("How to solve:", "Kako rešiti:");
        serbian.put("About:", "O igri:");
        serbian.put("Author:", "Autor:");
        serbian.put("There are five houses, each painted a different color.", "Postoji pet kuća, svaka je obojena drugom bojom.");
        serbian.put("The owners differ in nationality and have nothing in common.", "Vlasnici su različitih nacionalnosti i osobina.");
        serbian.put("Your task is to find out their traits.", "Vaš zadatak je da otkrijete njihove osobine.");
        serbian.put("You have three attempts to check your solution.", "Imate tri pokušaja da proverite svoje rešenje.");
        serbian.put("This puzzle was devised by the scientist Albert Einstein.", "Ovu zagonetku je osmislio naučnik Albert Ajnštajn.");
        serbian.put("He said only about 2% of people on Earth can solve it.", "Rekao je da samo oko 2% ljudi na Zemlji može da je reši.");
        serbian.put("Are you one of those 2%?", "Da li ste među tih 2%?");
        serbian.put("OK", "U redu");
        serbian.put("Yes", "Da");
        serbian.put("No", "Ne");
        serbian.put("Do you really want to exit?", "Da li zaista želite da izađete?");
        serbian.put("Congratulations! You have solved Einstein's Intelligence Test! Press Start to play again.", "Čestitamo! Rešili ste Ajnštajnov test inteligencije! Pritisnite Počni za novu igru.");
        serbian.put("Solved", "Rešeno");
        serbian.put("Your solution is not correct, and you have no attempts left. Press Start to play again.", "Rešenje nije tačno i nemate više pokušaja. Pritisnite Počni za novu igru.");
        serbian.put("Game over", "Kraj igre");
        serbian.put("Your solution is not correct. Attempts left: ", "Rešenje nije tačno. Preostalo pokušaja: ");
        serbian.put("Incorrect solution", "Netačno rešenje");
        serbian.put("Could not open the website. Visit ", "Otvaranje sajta nije uspelo. Posetite ");
        serbian.put(" in your browser.", " u svom pregledaču.");
        serbian.put("Could not open your email application. Email contact@zlatanstajic.com.", "Otvaranje programa za e-poštu nije uspelo. Pišite na contact@zlatanstajic.com.");
        serbian.put("Email", "E-pošta");
        serbian.put("Elapsed time", "Proteklo vreme");
        serbian.put("Rank List", "Rang lista");
        serbian.put("Rank", "Mesto");
        serbian.put("Name", "Ime");
        serbian.put("Time", "Vreme");
        serbian.put("Date", "Datum");
        serbian.put("Close", "Zatvori");
        serbian.put("No winners yet. Solve the puzzle to add your name.", "Još nema pobednika. Rešite zagonetku da biste dodali svoje ime.");
        serbian.put("The rank list could not be loaded.", "Rang listu nije moguće učitati.");
        serbian.put("Your time: ", "Vaše vreme: ");
        serbian.put("Enter your name for the rank list:", "Unesite ime za rang listu:");
        serbian.put("Save", "Sačuvaj");
        serbian.put("Cancel", "Otkaži");
        serbian.put("Invalid name", "Neispravno ime");
        serbian.put("Enter a name of 1 to 40 characters.", "Unesite ime dužine od 1 do 40 znakova.");
        serbian.put("Your result could not be saved to the rank list.", "Vaš rezultat nije moguće sačuvati na rang listi.");
        serbian.put("15. Basketball player's neighbor drinks water", "15. Sused košarkaša pije vodu");
        serbian.put("1. Brit lives in the red house", "1. Britanac živi u crvenoj kući");
        serbian.put("2. Swede keeps a dog as a pet", "2. Šveđanin ima psa");
        serbian.put("3. Dane's favorite drink is tea", "3. Danac najradije pije čaj");
        serbian.put("4. Green house is left of the white one", "4. Zelena kuća je levo od bele");
        serbian.put("5. Green house owner drinks coffee", "5. Vlasnik zelene kuće pije kafu");
        serbian.put("6. Cyclist keeps birds", "6. Biciklista čuva ptice");
        serbian.put("7. Yellow house owner plays volleyball", "7. Vlasnik žute kuće igra odbojku");
        serbian.put("8. Middle house owner drinks milk", "8. Vlasnik srednje kuće pije mleko");
        serbian.put("9. Norwegian lives in the first house", "9. Norvežanin živi u prvoj kući");
        serbian.put("10. Basketball player's neighbor has a cat", "10. Sused košarkaša ima mačku");
        serbian.put("11. Volleyball player's neighbor has a horse", "11. Sused odbojkaša ima konja");
        serbian.put("12. Swimmer drinks beer", "12. Plivač pije pivo");
        serbian.put("13. German likes to play golf", "13. Nemac voli da igra golf");
        serbian.put("14. Norwegian lives by the blue house", "14. Norvežanin živi pored plave kuće");
    }

    void setSerbian(boolean selected) {
        serbianSelected = selected;
    }

    String text(String english) {
        return serbianSelected ? serbian.getOrDefault(english, english) : english;
    }

    /** Translates form labels without editing NetBeans-generated initialization. */
    void apply(Container container) {
        for (Component component : container.getComponents()) {
            if (component instanceof JLabel || component instanceof AbstractButton) {
                JComponent control = (JComponent) component;
                String original = (String) control.getClientProperty("englishText");
                if (original == null) {
                    original = component instanceof JLabel
                            ? ((JLabel) component).getText() : ((AbstractButton) component).getText();
                    control.putClientProperty("englishText", original);
                }
                if (original != null) {
                    if (component instanceof JLabel) {
                        ((JLabel) component).setText(text(original));
                    } else {
                        ((AbstractButton) component).setText(text(original));
                    }
                }
            }
            if (component instanceof Container) {
                apply((Container) component);
            }
        }
    }

    DefaultListCellRenderer answerRenderer() {
        return new DefaultListCellRenderer() {
            private static final long serialVersionUID = 1L;

            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value,
                    int index, boolean selected, boolean focused) {
                return super.getListCellRendererComponent(list,
                        value == null ? "" : text(value.toString()), index, selected, focused);
            }
        };
    }
}
