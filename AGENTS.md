# AGENTS.md

This file is the single source of shared project guidance for all coding agents.
`CLAUDE.md` imports this file.

## Project

Java Swing desktop game from 2013: Einstein's Intelligence Test, a version of Einstein's riddle. The player uses 15 clues to work out which owner lives in which of 5 houses. The app was written in Serbian and later translated to English.

## Build and run

The only third-party library is the vendored SQLite JDBC driver
`lib/sqlite-jdbc-3.50.3.0.jar` (`org.xerial:sqlite-jdbc:3.50.3.0`, SHA-256 in
`README.md`). Production code compiles against `java.sql` only; `run`, `test` and
`test-ui` add the jar to their runtime classpaths, and `make jar` merges the driver
into the runnable JAR. CI runs `make check` with Temurin JDK 11 on Linux for pushes
and pull requests. A second CI job on `windows-latest` packages that verified JAR
into a per-user MSI with jpackage from Temurin 21, bundling a runtime of
`java.base`, `java.desktop` and `java.sql`. Keep its upgrade UUID fixed. Tagged MSI
versions match the tag; branch builds use `1.0.<run number>`. Optional release signing
uses SignPath after enrollment; see `docs/windows-signing.md`. Named artifacts use
the default ZIP archive so upload/download names match. Available commands:

```bash
make build    # compile production classes targeting Java 11
make run      # development launch using build:src for image resources
make test     # deterministic headless gameplay and translation tests
make test-ui  # real Swing integration checks; requires a graphical display
make jar      # self-contained dist/einstein-intelligence-test.jar
make check    # headless tests and JAR verification, matching CI
make clean    # delete generated build/ and dist/
make install-hooks  # install the repository pre-commit hook locally
```

- `hooks/pre-commit` is the maintained hook source. Run `make install-hooks` after
  cloning or updating it to copy it, executable, into Git's active hooks directory;
  this replaces the local pre-commit hook. It checks staged whitespace and runs
  `make --always-make check` against the working tree, blocking commits on failure.
  Desktop checks remain separate. The hook must never stage files or commit.
- UTF-8 compilation preserves Serbian text and the author's name.
- `make run` needs `src` on its classpath because javac does not copy images, and
  the driver jar for the rank list.
- The runnable JAR includes production classes, package images and the extracted
  driver without its manifest, signature files, `META-INF/versions/` and
  `META-INF/native-image/` (GraalVM-only). It has no `Class-Path`, excludes tests,
  sources and forms, and launches with `java -jar dist/einstein-intelligence-test.jar`.
- `build.xml` and `nbproject/` retain the NetBeans Ant setup. Ant is not installed
  locally; Make is the supported verified path. Both builds use the same JAR name.
  `nbproject/project.properties` puts `file.reference.sqlite-jdbc-3.50.3.0.jar` on
  `javac.classpath` (which feeds the run and test classpaths) and sets
  `mkdist.disabled=true`, so NetBeans creates no `dist/lib` or `Class-Path`; the
  `build.xml` `-post-jar` target merges the driver into `dist.jar` with the same
  exclusions as Make. Ant has not verified this NetBeans setup locally.
- Java 11 is the target in Make and NetBeans. Local verification used JDK 18 with
  `--release 11`; CI is configured for Temurin 11.
- `build/` and `dist/` are gitignored.

## Architecture

8 classes in package `com.zlatanstajic.einsteinintelligencetest`:

- `ModernTheme` initializes bundled Nimbus with flat painters, shared fonts, colors,
  focus outlines, and screen-aware sizing. It adds no third-party dependencies.
- `MainWindow` is the main `JFrame`; it owns Swing controls, translated rendering,
  clue presentation, enablement, and dialogs.
- `GameSession` owns idle/running/paused/finished states, three attempts, canonical
  answer validation and accumulated active elapsed time. Its injected `LongSupplier`
  clock supports deterministic tests; production uses `System.nanoTime`.
- `Language` provides English/Serbian (Latin) display text and answer renderers. Answer models and `SOLUTION` retain English values so switching languages preserves game state. The Language menu and translations are applied outside NetBeans-generated blocks.
- `AboutDialog` is the modal "About" dialog with solving instructions, owned by `MainWindow`.
- `WinnerStore` persists solved games in SQLite at `.einstein-intelligence-test/winners.db`
  under `user.home`, opening one connection per call. `ranked()` orders by solve
  seconds, then earlier `solved_at`, then `id`. `normalizeName` strips a name and
  accepts 1 to 40 code points without control characters. It has no update or
  delete SQL and no update or delete method.
- `RankListDialog` is the modal, read-only rank list table, written by hand without
  a `.form` file. Cells are not editable, names render as literal text, and `Close`
  is its only button.
- `AuthorWebsite.open()` and `openEmail()` dispatch fixed contact URLs through Windows `rundll32.exe url.dll,FileProtocolHandler`, macOS `open`, or Linux `xdg-open`. They avoid `java.awt.Desktop`, which can report browsing as unsupported when GTK fails to load in snap terminals. `AuthorWebsiteTest` checks both URL commands for each platform without launching external applications.

Images live in `src/com/zlatanstajic/einsteinintelligencetest/images/` with lowercase, hyphenated names. Component fields use camelCase with a type suffix, such as `checkButton`, `clue1CheckBox` and `houseColorComboBox1`.

### NetBeans GUI Builder forms

`MainWindow` and `AboutDialog` each have a `.form` XML file that NetBeans uses to regenerate the Java code. Code between `GEN-BEGIN` and `GEN-END` markers, such as `initComponents()` and the field declarations, is generated from the `.form` file. If you change those blocks by hand, make the same change in the `.form` file, or NetBeans will overwrite it. Event handler bodies between `GEN-FIRST` and `GEN-LAST` are safe to edit. Component names and handler names in the `.form` file must match the Java fields and methods. The combo boxes are `JComboBox<String>` through the `JavaCodeGenerator_TypeParameters` aux value in `MainWindow.form`.

Both windows use layout managers rather than absolute coordinates: BorderLayout
for sections, GridBagLayout for answers/clues/details, and FlowLayout for About's
close action. The Java and matching `.form` hierarchies must remain consistent.
Theme, accessibility, toolbar setup, and responsive rearrangement are outside the
generated blocks. Numbered house labels and every new status string translate.

The main window stacks clues below answers when there is insufficient room for
both cards. Scroll panes preserve access at small sizes and enlarged fonts. Clue
HTML is formatted after translation using cached canonical English text; never
replace combo-box model values. Used clues have teal text/backgrounds; clearing
returns the theme text color. See `docs/layout-assessment.md` for observed settings.
Never restore null layouts, absolute bounds, or fixed-width text positioning.

If you rename the NetBeans project, `nbproject/genfiles.properties` stores CRC32 checksums of `project.xml`, `build.xml` and `build-impl.xml`. NetBeans computes them after converting line endings to `\n`.

### Gameplay and regression checks

- `MainWindow` groups 25 answer dropdowns as `[attribute][house]` and 15 clue
  checkboxes. Start/Restart clear both groups; empty answers are at model index 0.
- `GameSession` contains the canonical English solution. Translation changes only
  display text/renderers; never replace canonical combo-box values.
- `refreshGameControls()` derives Swing enablement from the model. Check requires
  a complete grid while running. Paused games disable inputs, Start/Restart/Check,
  preserving selections and clues; Resume excludes paused intervals.
- The Swing timer refreshes display only. `GameSession` owns monotonic timing and
  freezes it after solving or exhausting three attempts. Start and Restart reset it.
- The window close button and Exit both use `confirmExit()`.
- `GameSessionTest` tests fake-clock transitions, attempts and answer validation.
  `LanguageTest` verifies translations and canonical values without a display.
  `WinnerStoreTest` checks rank-list ordering, persistence, the name rule, literal
  storage of quotes, SQL text and Serbian letters, I/O failures and the default location.
  `SwingSmokeTest` checks actual controls on the event dispatch thread and disposes
  all windows. It uses reflection to avoid production-only test hooks and points
  `user.home` at a temporary directory for the rank list database.
  `JarVerificationTest` checks manifest/classes and scans production resource
  references, resolving images through an artifact classloader whose parent is the
  platform loader. It also requires the driver classes, `java.sql.Driver` service and
  Linux x86_64 native library; no `Class-Path`, signatures, root `module-info.class`,
  `META-INF/versions/` or `META-INF/native-image/`; class files of major version 55
  or lower; and a real `select sqlite_version()` through the packaged driver.
- See `CONTRIBUTING.md` for manual checks and `docs/layout-assessment.md` for
  observed font/scaling limits. Retain NetBeans form/source consistency when
  touching generated sections.

## Keyboard and accessibility

Alt+S starts, Alt+P pauses/resumes, Alt+R restarts, Alt+C/Enter checks, F1 opens
About, Alt+L opens the Rank List, and Escape requests exit. Bindings invoke enabled
buttons only. Escape closes About and the Rank List; Tab moves from the rank list
table to `Close`. Answer accessibility names include the translated house and attribute;
the rank list table and the name field have translated accessible names.
The footer reports ready/running/paused/solved/exhausted state and remaining attempts.

## graphify

This project has a knowledge graph at graphify-out/ with god nodes, community structure, and cross-file relationships.

When the user types `/graphify`, use the installed graphify skill or instructions before doing anything else.

Rules:
- For codebase questions, first run `graphify query "<question>"` when graphify-out/graph.json exists. Use `graphify path "<A>" "<B>"` for relationships and `graphify explain "<concept>"` for focused concepts. These return a scoped subgraph, usually much smaller than GRAPH_REPORT.md or raw grep output.
- Dirty graphify-out/ files are expected after hooks or incremental updates; dirty graph files are not a reason to skip graphify. Only skip graphify if the task is about stale or incorrect graph output, or the user explicitly says not to use it.
- If graphify-out/wiki/index.md exists, use it for broad navigation instead of raw source browsing.
- Read graphify-out/GRAPH_REPORT.md only for broad architecture review or when query/path/explain do not surface enough context.
- After modifying code, run `graphify update .` to keep the graph current (AST-only, no API cost).

## Landing page

`site/` is a buildless static landing page deployed to GitHub Pages by
`.github/workflows/pages.yml` from `master`. Preview with
`python3 -m http.server 8000 --directory site`. Keep asset URLs relative for the
repository subpath. `site/downloads.js` resolves the latest published JAR and MSI
and retains release-page fallbacks when no asset is available. Do not hard-code
unpublished downloads. The Build workflow publishes both verified packages for
`v*` tags only. Keep website requirements and limitations consistent with the app.
The landing page does not depend on or modify `brag-output/`.
