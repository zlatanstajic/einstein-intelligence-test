package com.zlatanstajic.einsteinintelligencetest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

public final class WinnerStoreTest {
    private interface Action {
        void run() throws Exception;
    }

    private static void requireThrows(Class<? extends Exception> type, Action action, String message) {
        try {
            action.run();
        } catch (Exception exception) {
            GameSessionTest.require(type.isInstance(exception), message + ": " + exception);
            return;
        }
        throw new AssertionError(message);
    }

    private static void requireNames(List<WinnerStore.Winner> winners, String... names) {
        GameSessionTest.require(winners.size() == names.length, "record count " + names.length);
        for (int index = 0; index < names.length; index++) {
            GameSessionTest.require(winners.get(index).name.equals(names[index]), "rank " + (index + 1) + ": " + names[index]);
        }
    }

    private static void missingDirectoryIsCreatedEmpty(Path root) throws Exception {
        Path file = root.resolve("missing").resolve("winners.db");
        GameSessionTest.require(new WinnerStore(file).ranked().isEmpty(), "new database is empty");
        GameSessionTest.require(Files.isRegularFile(file), "database file created with its directory");
    }

    private static void rankingOrdersByTimeThenSolveMomentThenRecord(Path root) throws Exception {
        Path file = root.resolve("ranking.db");
        WinnerStore store = new WinnerStore(file);
        store.add("Ana", 120, 3000);
        store.add("Bob", 90, 5000);
        store.add("Cid", 120, 1000);
        store.add("Dan", 120, 1000);
        List<WinnerStore.Winner> winners = store.ranked();
        requireNames(winners, "Bob", "Cid", "Dan", "Ana");
        GameSessionTest.require(winners.get(0).solveSeconds == 90 && winners.get(0).solvedAt == 5000,
                "time and solve moment stored");
        requireNames(new WinnerStore(file).ranked(), "Bob", "Cid", "Dan", "Ana");
    }

    private static void namesRoundTripUnchanged(Path root) throws Exception {
        WinnerStore store = new WinnerStore(root.resolve("names.db"));
        String quoted = "O'Brien\"; DROP TABLE winners; --";
        store.add(quoted, 10, 1);
        store.add("Zlatan Stajić", 20, 2);
        requireNames(store.ranked(), quoted, "Zlatan Stajić");
    }

    private static void nameRule() {
        String smile = new String(Character.toChars(0x1F600));
        GameSessionTest.require(WinnerStore.normalizeName(null) == null, "null name rejected");
        GameSessionTest.require(WinnerStore.normalizeName("") == null, "empty name rejected");
        GameSessionTest.require(WinnerStore.normalizeName("   ") == null, "spaces rejected");
        GameSessionTest.require(WinnerStore.normalizeName("\t") == null, "tab rejected");
        GameSessionTest.require("Ana".equals(WinnerStore.normalizeName("  Ana  ")), "surrounding spaces removed");
        GameSessionTest.require("č".repeat(40).equals(WinnerStore.normalizeName("č".repeat(40))), "40 letters accepted");
        GameSessionTest.require(WinnerStore.normalizeName("č".repeat(41)) == null, "41 letters rejected");
        GameSessionTest.require(smile.repeat(40).equals(WinnerStore.normalizeName(smile.repeat(40))),
                "length counts code points");
        GameSessionTest.require(WinnerStore.normalizeName("A\u0007B") == null, "control character rejected");
    }

    private static void invalidRecordsAreRejected(Path root) throws Exception {
        WinnerStore store = new WinnerStore(root.resolve("invalid.db"));
        store.add("Ana", 30, 1);
        requireThrows(IllegalArgumentException.class, () -> store.add("   ", 30, 2), "invalid name");
        requireThrows(IllegalArgumentException.class, () -> store.add("Bob", -1, 2), "negative seconds");
        GameSessionTest.require(store.ranked().size() == 1, "rejected records are not stored");
    }

    private static void unusableDirectoryFails(Path root) throws Exception {
        Path blocker = Files.createFile(root.resolve("blocker"));
        WinnerStore store = new WinnerStore(blocker.resolve("winners.db"));
        requireThrows(IOException.class, () -> store.add("Ana", 30, 1), "add with file parent");
        requireThrows(IOException.class, store::ranked, "ranked with file parent");
    }

    private static void defaultDatabaseFollowsUserHome(Path root) {
        String home = System.getProperty("user.home");
        try {
            System.setProperty("user.home", root.toString());
            GameSessionTest.require(WinnerStore.defaultDatabase().equals(
                    root.resolve(".einstein-intelligence-test").resolve("winners.db")), "default database location");
        } finally {
            System.setProperty("user.home", home);
        }
    }

    private static void delete(Path root) throws IOException {
        try (Stream<Path> paths = Files.walk(root)) {
            for (Path path : (Iterable<Path>) paths.sorted(Comparator.reverseOrder())::iterator) {
                Files.delete(path);
            }
        }
    }

    public static void main(String[] args) throws Exception {
        Path root = Files.createTempDirectory("winner-store-test");
        try {
            missingDirectoryIsCreatedEmpty(root);
            rankingOrdersByTimeThenSolveMomentThenRecord(root);
            namesRoundTripUnchanged(root);
            nameRule();
            invalidRecordsAreRejected(root);
            unusableDirectoryFails(root);
            defaultDatabaseFollowsUserHome(root);
        } finally {
            delete(root);
        }
        System.out.println("WinnerStoreTest passed");
    }
}
