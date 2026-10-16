# Contributing

Use a Linux desktop with JDK 11 or newer and `make`. See [README.md](README.md)
for build and run commands. The only third-party Java library is the SQLite JDBC
driver committed as `lib/sqlite-jdbc-3.50.3.0.jar`. To replace it, download the new
jar and its `.sha1` from Maven Central, confirm that `sha1sum` of the jar matches
the `.sha1` value, update the jar path in `Makefile`, `nbproject/project.properties`
and `build.xml`, and record the new SHA-256 in `README.md`.

## Making changes

- Keep changes focused and describe the behavior they change.
- Preserve Java 11 compatibility. Keep the Makefile and NetBeans compiler
  settings aligned if the target changes.
- When editing generated GUI sections in `MainWindow.java` or `AboutDialog.java`,
  update the matching `.form` file too; NetBeans regenerates those sections.
- Keep `src` and `lib/sqlite-jdbc-3.50.3.0.jar` on the runtime classpath so image
  resources load correctly and the rank list can be stored.
- Follow the [Code of Conduct](CODE_OF_CONDUCT.md). Report vulnerabilities
  privately using [SECURITY.md](SECURITY.md).

## Verification

Run `make check` to compile for Java 11, execute the headless gameplay, language and
rank-list storage harnesses, and inspect the packaged JAR, its image resources and
the bundled SQLite driver. CI runs that same
command with Temurin 11. No test framework or dependency download is required.
Run `make test-ui` on an accessible Linux desktop for real Swing checks on the
event dispatch thread; a missing display is an error, not a skipped success.

For gameplay or GUI changes, run `make run` and verify:

1. The initial interface is English, with Language and About on the top bar, a
   globe beside Language, a clock and timer at the right, and Exit at bottom right.
2. Start clears all 25 answers and 15 clues, displays three attempts and 00:00,
   enables the grid and Pause/Restart, and disables Start and Check.
3. Mark a clue; it gains a teal highlight without changing attempts. Fill only part of the grid;
   Check stays disabled. Fill every answer incorrectly; Check consumes one attempt.
   Three incorrect submissions end the game, lock inputs, and enable Start.
4. Start again and enter the correct grid below. Check shows the solved dialog
   with your time and a focused name field, locks inputs, stops elapsed time, and
   enables Start. Type a name and press Enter to save it.
5. During another game, select answers and mark clues, then Pause. Inputs, Check,
   Start and Restart are disabled; Resume remains available and the timer freezes.
   Wait several seconds, Resume, then repeat. Answers, clue marks and attempts stay
   intact, and resumed time excludes paused intervals.
6. Switch English/Serbian while running and while paused. Labels and rendered
   answers translate; selections, clues, attempts, elapsed time and pause state stay
   unchanged. Switch back to restore English.
7. Restart while running: all answers clear, clues become unmarked with the normal text color,
   three attempts return, and the clock resets to 00:00.
8. Open About in both languages. Check instructions, author name, email below the
   name and website below email, with compact spacing and no built/refreshed year lines. No website icon
   should appear. Close using OK. Exit and the window close control both show an
   exit confirmation; No keeps the app open, Yes closes it.
9. Run `make jar`, change to a directory outside the checkout, and launch
   `java -jar /absolute/path/to/dist/einstein-intelligence-test.jar`. Confirm images,
   English startup, Serbian switching and About without any source classpath. Solve,
   save a name and open the Rank List; `~/.einstein-intelligence-test/winners.db`
   exists and no database file appears in the current directory.
10. Resize the main window to narrow and wide sizes: clues stack below the houses
    or sit beside them, and all fields/clues remain reachable using scroll panes.
    Check both languages and larger fonts. About is also resizable and scrollable.
11. Verify the footer state and attempts after Start, Pause, Resume, win and loss.
    Test Alt+S, Alt+P, Alt+R, Alt+C/Enter, F1 and Escape. Disabled actions must stay
    unavailable through shortcuts; About closes with Escape. Tab/focus outlines
    and each answer's translated house/attribute accessible name must remain usable.
12. After a solve, a whitespace-only name and a 41-character name show Invalid name
    and reopen the prompt with the text kept; Cancel and Escape save nothing. Open
    the Rank List with its button and with Alt+L in English and Serbian: results are
    listed fastest first, cells cannot be edited, Tab moves from the table to Close,
    and Escape closes the dialog. Restart the app; the results remain.

Correct answers, houses 1–5 from left to right:

| Attribute | 1 | 2 | 3 | 4 | 5 |
|---|---|---|---|---|---|
| Color | Yellow | Blue | Red | Green | White |
| Nationality | Norwegian | Dane | Brit | German | Swede |
| Drink | Water | Tea | Milk | Coffee | Beer |
| Sport | Volleyball | Basketball | Cycling | Golf | Swimming |
| Pet | Cat | Horse | Bird | Fish | Dog |

The permanent harnesses are under `test/com/zlatanstajic/einsteinintelligencetest/`.
`GameSessionTest` uses an injected clock without sleeps; `LanguageTest` checks
translations without a display; `WinnerStoreTest` checks rank-list storage,
ordering and the name rule in a temporary directory; `SwingSmokeTest` exercises real
controls; `JarVerificationTest` uses an isolated JAR loader so source resources cannot
hide missing packaged images or a missing SQLite driver. Keep test logic outside production sources.

Include commands, results, and any checks you could not run with your contribution.
Do not include generated `build/` or `dist/` output. Run `git diff --check` before
submitting changes.
