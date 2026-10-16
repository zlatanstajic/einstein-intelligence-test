package com.zlatanstajic.einsteinintelligencetest;

import java.util.concurrent.atomic.AtomicLong;

public final class GameSessionTest {
    static String[][] solution() {
        return new String[][] {
            {"Yellow", "Blue", "Red", "Green", "White"},
            {"Norwegian", "Dane", "Brit", "German", "Swede"},
            {"Water", "Tea", "Milk", "Coffee", "Beer"},
            {"Volleyball", "Basketball", "Cycling", "Golf", "Swimming"},
            {"Cat", "Horse", "Bird", "Fish", "Dog"}
        };
    }
    static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
    private static void pauseAndResumeExcludeInactiveTime() {
        AtomicLong time = new AtomicLong();
        GameSession game = new GameSession(time::get);
        require(game.state() == GameSession.State.IDLE, "new session is idle");
        require(game.check(solution()) == GameSession.Result.UNAVAILABLE, "idle check is unavailable");
        game.pause();
        game.resume();
        game.restart();
        require(game.state() == GameSession.State.IDLE, "idle transitions are ignored");
        game.start();
        time.addAndGet(2_500_000_000L);
        game.start();
        game.resume();
        require(game.elapsedSeconds() == 2, "start/resume while running cannot reset clock");
        game.pause();
        time.addAndGet(9_000_000_000L);
        game.pause();
        game.start();
        game.restart();
        require(game.state() == GameSession.State.PAUSED && game.elapsedSeconds() == 2,
                "repeated pause, start and restart leave paused clock unchanged");
        require(game.remainingAttempts() == 3, "ignored transitions preserve attempts");
        require(game.check(solution()) == GameSession.Result.UNAVAILABLE, "paused check is unavailable");
        game.resume();
        time.addAndGet(500_000_000L);
        game.pause();
        time.addAndGet(9_000_000_000L);
        game.resume();
        require(game.elapsedSeconds() == 3, "repeated resume excludes pauses and retains fractions");
    }

    private static void incompleteAnswersNeverConsumeAttempts() {
        GameSession game = new GameSession(() -> 0L);
        game.start();
        String[][] answers = solution();
        answers[0][0] = "";
        require(game.check(answers) == GameSession.Result.INCOMPLETE, "empty answer is incomplete");
        answers[0][0] = null;
        require(game.check(answers) == GameSession.Result.INCOMPLETE, "null answer is incomplete");
        answers[0] = null;
        require(game.check(answers) == GameSession.Result.INCOMPLETE, "null row is incomplete");
        require(game.check(null) == GameSession.Result.INCOMPLETE, "null grid is incomplete");
        require(game.check(new String[1][]) == GameSession.Result.INCOMPLETE, "wrong row count is incomplete");
        answers = solution();
        answers[0] = new String[] {"Yellow"};
        require(game.check(answers) == GameSession.Result.INCOMPLETE, "wrong column count is incomplete");
        require(game.remainingAttempts() == 3, "all incomplete checks preserve attempts");
    }

    private static void restartResetsClockAndAttempts() {
        AtomicLong time = new AtomicLong();
        GameSession game = new GameSession(time::get);
        game.start();
        String[][] answers = solution();
        answers[0][0] = "Red";
        require(game.check(answers) == GameSession.Result.INCORRECT, "wrong answer consumes attempt");
        time.addAndGet(6_000_000_000L);
        game.restart();
        require(game.remainingAttempts() == 3 && game.elapsedSeconds() == 0, "restart resets attempts and clock");
        require(game.state() == GameSession.State.RUNNING, "restart remains running");
    }

    private static void winningFreezesLongElapsedTime() {
        AtomicLong time = new AtomicLong();
        GameSession game = new GameSession(time::get);
        game.start();
        time.addAndGet(3_661_000_000_000L);
        require(game.elapsedSeconds() == 3661, "clock supports more than an hour");
        require(game.check(solution()) == GameSession.Result.SOLVED, "canonical answer wins");
        time.addAndGet(4_000_000_000L);
        game.pause();
        game.resume();
        game.restart();
        require(game.elapsedSeconds() == 3661 && game.state() == GameSession.State.FINISHED,
                "winning freezes terminal clock and ignores pause/resume/restart");
        require(game.remainingAttempts() == 3, "correct answer consumes no attempt");
        require(game.check(solution()) == GameSession.Result.UNAVAILABLE, "terminal check is unavailable");
        game.start();
        require(game.elapsedSeconds() == 0 && game.remainingAttempts() == 3, "new game resets after win");
    }

    private static void thirdWrongAnswerFinishesGame() {
        AtomicLong time = new AtomicLong();
        GameSession game = new GameSession(time::get);
        game.start();
        String[][] answers = solution();
        answers[0][0] = "Red";
        require(game.check(answers) == GameSession.Result.INCORRECT && game.remainingAttempts() == 2,
                "first wrong answer leaves two attempts");
        require(game.check(answers) == GameSession.Result.INCORRECT && game.remainingAttempts() == 1,
                "second wrong answer leaves one attempt");
        time.addAndGet(1_000_000_000L);
        require(game.check(answers) == GameSession.Result.EXHAUSTED, "third wrong answer ends game");
        time.addAndGet(5_000_000_000L);
        require(game.remainingAttempts() == 0 && game.elapsedSeconds() == 1, "exhaustion freezes clock");
        require(game.check(answers) == GameSession.Result.UNAVAILABLE, "exhausted game rejects further checks");
    }

    public static void main(String[] args) {
        pauseAndResumeExcludeInactiveTime();
        incompleteAnswersNeverConsumeAttempts();
        restartResetsClockAndAttempts();
        winningFreezesLongElapsedTime();
        thirdWrongAnswerFinishesGame();
        System.out.println("GameSessionTest passed");
    }
}
