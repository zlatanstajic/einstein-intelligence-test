package com.zlatanstajic.einsteinintelligencetest;

import java.util.function.LongSupplier;

/** Display-independent rules and monotonic active-play clock. */
final class GameSession {
    enum State { IDLE, RUNNING, PAUSED, FINISHED }

    enum Result { INCOMPLETE, UNAVAILABLE, INCORRECT, EXHAUSTED, SOLVED }

    static final int MAX_ATTEMPTS = 3;

    private static final String[][] SOLUTION = {
        {"Yellow", "Blue", "Red", "Green", "White"},
        {"Norwegian", "Dane", "Brit", "German", "Swede"},
        {"Water", "Tea", "Milk", "Coffee", "Beer"},
        {"Volleyball", "Basketball", "Cycling", "Golf", "Swimming"},
        {"Cat", "Horse", "Bird", "Fish", "Dog"}
    };
    private final LongSupplier clock;
    private State state = State.IDLE;
    private int attempts = MAX_ATTEMPTS;
    private long accumulatedNanos;
    private long startedNanos;

    GameSession() {
        this(System::nanoTime);
    }

    GameSession(LongSupplier clock) {
        this.clock = clock;
    }

    State state() {
        return state;
    }

    int remainingAttempts() {
        return attempts;
    }

    void start() {
        if (state == State.RUNNING || state == State.PAUSED) {
            return;
        }
        begin();
    }

    void restart() {
        if (state == State.RUNNING) {
            begin();
        }
    }

    private void begin() {
        attempts = MAX_ATTEMPTS;
        accumulatedNanos = 0;
        startedNanos = clock.getAsLong();
        state = State.RUNNING;
    }

    void pause() {
        if (state == State.RUNNING) {
            accumulatedNanos += clock.getAsLong() - startedNanos;
            state = State.PAUSED;
        }
    }

    void resume() {
        if (state == State.PAUSED) {
            startedNanos = clock.getAsLong();
            state = State.RUNNING;
        }
    }

    long elapsedSeconds() {
        long active = state == State.RUNNING ? clock.getAsLong() - startedNanos : 0;
        return (accumulatedNanos + active) / 1_000_000_000L;
    }

    boolean isComplete(String[][] answers) {
        if (answers == null || answers.length != SOLUTION.length) {
            return false;
        }
        for (String[] row : answers) {
            if (row == null || row.length != 5) {
                return false;
            }
            for (String answer : row) {
                if (answer == null || answer.isEmpty()) {
                    return false;
                }
            }
        }
        return true;
    }

    Result check(String[][] answers) {
        if (state != State.RUNNING) {
            return Result.UNAVAILABLE;
        }
        if (!isComplete(answers)) {
            return Result.INCOMPLETE;
        }
        boolean solved = java.util.Arrays.deepEquals(SOLUTION, answers);
        if (!solved) {
            attempts--;
        }
        if (solved || attempts == 0) {
            accumulatedNanos += clock.getAsLong() - startedNanos;
            state = State.FINISHED;
        }
        return solved ? Result.SOLVED : attempts == 0 ? Result.EXHAUSTED : Result.INCORRECT;
    }
}
