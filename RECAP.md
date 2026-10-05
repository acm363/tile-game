# Recap — tactical war game (to resume on another machine)

Hand-over only: the design lives in `docs/design/`. Delete this file once work has resumed.

## Where things are

- Branch `feat/tactical-war-game`, 8 commits ahead of `main`, **not pushed** (only exists locally).
- Working tree clean at `3288402`.
- Design docs: `docs/design/war-game.md` (rules, decisions, open questions), `docs/design/board.md` (board generation).
- README documents the rules as played, the window controls and `--simulate`.

## Done on the branch

| Commit    | Change                                                                                      |
|-----------|---------------------------------------------------------------------------------------------|
| `1b71864` | Rule and board decisions settled (all recommended options picked)                           |
| `d9436c7` | First player rotates each round (`Game.currentPlayer`), all games — farming too             |
| `037510f` | New war rules: deploy / attack / pass, power = soldiers + level, range by terrain, 1 pt/tile |
| `9ef10b8` | Swing: select an army outlined in red, click a red target; level shown as gold pips          |
| `0ff71b1` | Session notes replaced by `docs/design/` docs                                                |
| `3288402` | Greedy `WarBot` + `--simulate <n>` (seat-swapped seeds, balance report)                      |

Rule interpretations chosen where the draft was silent: no size cap per terrain, equal power cannot attack, armies
never move.

## What the simulation showed (`guerre A B --simulate 1000`, 2,000 games, ~4 s)

- **99.7% ties.** Every action is worth exactly one tile of lead (deploy +1 for me, kill −1 for them), so equal turns
  give equal scores. First-player win rate cannot be measured until scoring changes; komi would just make seat 2 win.
- **79% of kills happen on a double turn** (rotation gives A B | B A…): deploy a threat, attack before the victim
  plays. A threatened army has no answer — it can't move or be reinforced.
- Kills by attacker terrain: desert 53%, mountain 26%, forest 10%, plain 10%. Board still uses the old generator
  (24.6 land tiles on average), so these shares are not meaningful yet.

## Decision pending (blocks everything else)

What the score counts:

| Option | Score                                               | Effect                                                   |
|--------|-----------------------------------------------------|----------------------------------------------------------|
| **A** (recommended) | Total power of armies on board (soldiers + levels) | Killing big beats deploying small; levels count; reuses "power" |
| B      | Tile value by terrain (mountain 3, desert 2, others 1) | Rewards position, ignores levels                       |
| C      | Each kill worth 2                                   | Pushes attacks, arbitrary weight                         |

## Next steps, in order

1. Pick the scoring (A/B/C), implement it test-first, rerun `--simulate 1000`.
2. From the double-turn numbers, keep rotation or switch to plain alternation (+ komi if seat 1 wins > 55%).
   Proposed balance targets: seat 1 wins 45–55%, mountains ≤ ~40% of kills, most games reach round 6+.
3. Board generator to the target distribution in `docs/design/board.md` (≈40% land, 1–2 islands, mountain 10–15%),
   then rerun the simulation — only then are terrain shares meaningful.
4. Fairness levers if needed: komi (+0.5 for seat 2, removes ties), 9 rounds (seat 2 moves last), pie rule.

## Practical notes

- Build: needs a full JDK 21+. On the current laptop the system `java-25-openjdk` has no `javac`; used
  `JAVA_HOME=~/.jdks/temurin-26.0.2.1 ./gradlew test`.
- Run the simulation: `./gradlew installDist` then `build/install/tile-game/bin/tile-game guerre A B --simulate 1000`.
- GUI checks were done with a throwaway Java script painting the realized window to PNG (Wayland blocks screen grabs).
- Conventions: tests use `// Given.` / `// When.` / `// Then.` only, no other comments; concise commits, no AI
  trailer; never push without approval.
