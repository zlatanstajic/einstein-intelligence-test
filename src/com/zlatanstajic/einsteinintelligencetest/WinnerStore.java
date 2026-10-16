package com.zlatanstajic.einsteinintelligencetest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Append-only rank list of solved games in a per-user SQLite database.
 * Records can be added and read, never updated or deleted.
 */
final class WinnerStore {
    static final int MAX_NAME_LENGTH = 40;

    private final Path database;

    /** One solved game: the winner's name, the game clock's seconds and the solve moment in epoch milliseconds. */
    static final class Winner {
        final String name;
        final long solveSeconds;
        final long solvedAt;

        Winner(String name, long solveSeconds, long solvedAt) {
            this.name = name;
            this.solveSeconds = solveSeconds;
            this.solvedAt = solvedAt;
        }
    }

    /** Stores the database location; the file and its directory are created on first use. */
    WinnerStore(Path database) {
        this.database = database;
    }

    static Path defaultDatabase() {
        return Path.of(System.getProperty("user.home"), ".einstein-intelligence-test", "winners.db");
    }

    /** Returns the name without surrounding whitespace, or null when it breaks the 1 to 40 code point rule. */
    static String normalizeName(String raw) {
        if (raw == null) {
            return null;
        }
        String name = raw.strip();
        if (name.isEmpty() || name.codePointCount(0, name.length()) > MAX_NAME_LENGTH
                || name.codePoints().anyMatch(Character::isISOControl)) {
            return null;
        }
        return name;
    }

    void add(String name, long solveSeconds, long solvedAt) throws IOException, SQLException {
        String normalized = normalizeName(name);
        if (normalized == null || solveSeconds < 0) {
            throw new IllegalArgumentException("Invalid winner record");
        }
        try (Connection connection = open();
                PreparedStatement insert = connection.prepareStatement(
                        "INSERT INTO winners (name, solve_seconds, solved_at) VALUES (?, ?, ?)")) {
            insert.setString(1, normalized);
            insert.setLong(2, solveSeconds);
            insert.setLong(3, solvedAt);
            insert.executeUpdate();
        }
    }

    /** Every record, fastest first; equal times rank the earlier solve, then the earlier record. */
    List<Winner> ranked() throws IOException, SQLException {
        List<Winner> winners = new ArrayList<>();
        try (Connection connection = open();
                Statement query = connection.createStatement();
                ResultSet rows = query.executeQuery("SELECT name, solve_seconds, solved_at FROM winners"
                        + " ORDER BY solve_seconds ASC, solved_at ASC, id ASC")) {
            while (rows.next()) {
                winners.add(new Winner(rows.getString(1), rows.getLong(2), rows.getLong(3)));
            }
        }
        return winners;
    }

    private Connection open() throws IOException, SQLException {
        Path file = database.toAbsolutePath();
        Files.createDirectories(file.getParent());
        Connection connection;
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:" + file);
        } catch (LinkageError error) {
            throw new SQLException("The SQLite driver could not be loaded", error);
        }
        try (Statement schema = connection.createStatement()) {
            schema.execute("CREATE TABLE IF NOT EXISTS winners (id INTEGER PRIMARY KEY, name TEXT NOT NULL,"
                    + " solve_seconds INTEGER NOT NULL, solved_at INTEGER NOT NULL)");
            return connection;
        } catch (SQLException | RuntimeException exception) {
            try {
                connection.close();
            } catch (SQLException closing) {
                exception.addSuppressed(closing);
            }
            throw exception;
        }
    }
}
