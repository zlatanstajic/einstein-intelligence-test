# Layout assessment

The UI now uses layout managers and a flat-styled bundled Nimbus theme. Both
windows resize; narrow main windows stack the clue card below the house grid.
Scroll panes preserve access when content exceeds the available space. No
third-party UI dependency or source resource fallback is needed by the runnable JAR.

## Layout contract

- Main sections use BorderLayout; house/attribute cells and cards use GridBagLayout.
  Five house headings identify every answer column. Grid cells grow with the window.
- Cards sit side by side when the viewport can accommodate the preferred answer
  width and clue card. Otherwise they stack; the main viewport can scroll.
- Clues wrap inside their own scroll pane. Only vertical clue scrolling is needed
  at the tested settled narrow/wide sizes.
- About uses BorderLayout/GridBagLayout and a scroll pane; the close action uses
  FlowLayout. Name, email and website retain their order; the modal packs to compact content.
- Nimbus painters provide flat controls, hover/focus outlines and teal used clues.
  Fonts follow Swing logical sizing. Styling and responsive logic sit outside the
  generated Java regions; matching NetBeans forms record the layout hierarchy.

## Observed verification

Checks ran on 16 October 2026 on Linux, JDK 18 targeting Java 11, using the X11
1920 × 1080 desktop. No user's desktop settings were changed.

| Setting | Result |
|---|---|
| Normal fonts, scale 1 | Gameplay/dialog/language checks pass; the redesigned main and About windows were visually inspected. |
| Explicit 900 × 760 main window | Clues stack below the houses; answer fields and clue content remain reachable. |
| Explicit 1500 × 900 main window | Cards sit beside each other when their preferred widths fit; clues wrap without horizontal scrolling. |
| Swing UI fonts multiplied by 1.5 | Gameplay, language and resize checks pass; the layout uses preferred sizes and scrolls where needed. |
| Process-local `-Dsun.java2d.uiScale=2` | Gameplay and resize checks pass; initial sizing is bounded by the logical screen, and scroll panes keep content reachable. |

The desktop harness checks actual controls on the event dispatch thread, translated
house headings/status/accessibility names, shortcut availability, responsive card
constraints, vertical answer text space and settled clue scrolling. Recursive
label/button width diagnostics supplement those assertions; they are not a complete
accessibility audit. Screenshots were inspected for spacing, readable controls and
About contact placement and compact spacing.

## Reproducing

```bash
make test-ui
java -cp build:build/test:src com.zlatanstajic.einsteinintelligencetest.SwingSmokeTest 1.5
java -Dsun.java2d.uiScale=2 -cp build:build/test:src com.zlatanstajic.einsteinintelligencetest.SwingSmokeTest
```

The font multiplier and Java scaling property apply only to the test process.
Large content may require scrolling instead of fitting on one screen. Native Java
11 desktop rendering, other operating systems, fractional scaling, mixed-DPI
monitors and full NetBeans IDE regeneration remain unverified locally.
