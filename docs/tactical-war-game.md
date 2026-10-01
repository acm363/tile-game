# Tactical war game — design notes

Working notes to continue the design discussion. Work happens on `feat/tactical-war-game`, branched from `main` at
`a159840`. **Implemented on the branch:** the rules below, attacks from the Swing window, first player rotating each
round. **Not yet:** the board distribution, the greedy bot and the balance check.

## Already on `main`

| Commit    | Change                                                                                |
|-----------|---------------------------------------------------------------------------------------|
| `f1b9d59` | Tests structured as `// Given.` / `// When.` / `// Then.`                             |
| `ed953eb` | FR/EN selector in the Swing window (`messages_<lang>.properties`), console stays FR   |
| `d7cc7b9` | Closing the window stops auto-play and lets the JVM exit                              |
| `b615c1e` | Human players: `--human <name>` (needs `--gui`), click highlighted tiles, Pass / Sell |
| `11ffa46` | Hover preview: ghost army, red/gold/green marks from `GameRules.preview`              |
| `a159840` | Flash on touched tiles, falling-sand crumble when an army starves or a worker leaves  |

## Goal

Make the war game a **tactical positioning and levelling game**: players choose where to place soldiers
depending on terrain, distance and army level — later also weapon type and weapon level. Rules must stay easy
for a new player. The farming game is out of scope for now.

## Previous war rules (replaced)

Deploy 1–5 of 35 warriors; mountains/deserts hold at most 3. A deployment confronts its 4 neighbours: weaker
enemies are halved or rally (+2 gold), smaller allies gain +1 (+1 gold); mountains give +2 strength. Upkeep turns
wheat/wood into food, armies eat their size (double in desert), starving armies die (+1 gold). Score: gold + army
gold + terrain bonus + 5 for 10 tiles. Considered too complex for new players.

## Proposed rules (draft)

1. **Start**: 35 soldiers in reserve per player. No food, no gold.
2. **Turn** — one action: **deploy** 1–5 soldiers on a free land tile, **attack** with one of your armies, or
   **pass**.
3. **Power** = soldiers + level. A new army is level 0.
4. **Attack**: target an enemy within your army's range that you out-power. It is destroyed, its tile freed, and
   your army gains +1 level (max 3).
5. **Terrain**, distance counted in orthogonal steps:

   | Terrain  | Range | Trait                                         |
   |----------|-------|-----------------------------------------------|
   | Plain    | 1     | open ground, no modifier                      |
   | Forest   | 1     | cover: can only be attacked from a neighbour  |
   | Desert   | 2     | exposed: −1 power when defending              |
   | Mountain | 3     | none — the range is its strength              |

6. **End**: board full or 10 rounds. **Score**: 1 point per tile held.

Example: 5 soldiers on a mountain destroy a 3-soldier army on a plain 3 tiles away, but not one in a forest.

### Rule decisions (settled 2026-10-01)

| # | Question                     | Decision                                      | Rejected                    |
|---|------------------------------|-----------------------------------------------|-----------------------------|
| 1 | When fights happen           | Attack is a separate action                   | Auto-fire on deploy         |
| 2 | Effect of a won attack       | Enemy destroyed                               | Enemy captured (as today)   |
| 3 | Which attacks are legal      | Only winnable ones (highlighted = beatable)   | Failed attacks with a cost  |
| 4 | Levelling                    | +1 level per kill, max 3                      | No levels yet               |
| 5 | Line of sight                | Ignored for now (shoot over sea and armies)   | Blocked by terrain/armies   |

Read from the draft: no terrain caps an army's size any more (5 soldiers on a mountain is legal, as in the example),
and an equal power is not enough to attack.

### Structural notes

- One place computes **power** and one computes **range**: weapon type/level plug in there later (a rifle could
  set range instead of terrain).
- Engine gains a second action, *Attack(from, target)*; the farming game is untouched.
- UI: select own army → beatable targets light up → click; preview and animations are reused.
- Tests first: which attacks are legal (range, cover, power comparison) — everything else depends on it.
- Biggest risk: **balance** (range 3 covers up to 24 tiles of a 10×10 board). Check with many seeded games
  played by a greedy bot, not the random one.
- No one-way door: the old rules stay in git history.

## Board tile distribution (draft)

**Today** (`BoardGenerator`): land count random in [2, ⅓ of the board] — 2 to 33 tiles on 10×10 — placed as
scattered pairs of adjacent tiles; each land tile is plain/forest/desert/mountain with 25% each, no clustering.

**Proposal**:

| Aspect   | Proposal                                                                       |
|----------|--------------------------------------------------------------------------------|
| Land     | Fixed share ≈ 40% of the board (≈ 40 tiles on 10×10), never 2                  |
| Shape    | 1–2 connected islands (noise + island mask, see the Vagabond map article)      |
| Mountain | **Drawn per game, uniformly between 10% and 15% of land** (from the seed)      |
| Others   | Remaining land split plain : forest : desert = 40 : 25 : 20                    |
| Grouping | Mountains in ridges, forests in patches, plains fill the rest                  |
| Fairness | First player rotates each round (no mirrored board)                            |

Resulting share of land tiles:

| Mountain drawn | Plain | Forest | Desert | On 40 land tiles (M / P / F / D) |
|----------------|-------|--------|--------|----------------------------------|
| 10%            | 42.4% | 26.5%  | 21.2%  | 4 / 17 / 11 / 8                  |
| 15%            | 40.0% | 25.0%  | 20.0%  | 6 / 16 / 10 / 8                  |

### Board decisions (settled 2026-10-01)

1. Land share: fixed ≈ 40% — the mountain share already varies per game; a land range on top blurs balance checks.
2. Plain : forest : desert ratio 40 : 25 : 20 — kept.
3. Mirrored boards: dropped. Players have no sides — anyone deploys on any free tile — so there is nothing to mirror.
   The real bias is turn order (`Game` always opened rounds with the first player): the first player now rotates.

## Backlog after the rules

1. Greedy bot using `GameRules.preview`, then the balance check over many seeded games — the biggest open risk.
2. Board generator following the distribution above.
3. Deploy preview showing which enemies the new army could reach and beat (tactical hint, not an event).
4. Always generate and show the seed; record actions to replay any game.
5. UX: announce the turn before the click, end-of-game dialog, 1–5 keyboard shortcuts for size; hide gold and
   resources in the war game's player panel, where they are always empty.
6. Network play (later; turn-based → TCP, authoritative server, exchange seed + actions).

## Conventions

- Commits: concise, never an AI co-author trailer; never push without approval.
- Tests: JUnit 5, `// Given.` / `// When.` / `// Then.` markers only — no other comments in code.
- Verify GUI changes on the real display (screenshots via a throwaway script in the scratchpad).
- Build with a full JDK: the system `java-25-openjdk` has no `javac`; use `JAVA_HOME=~/.jdks/temurin-26.0.2.1`.
