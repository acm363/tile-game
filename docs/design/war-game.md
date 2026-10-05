# War game

**Status:** implemented in `boardgame.war`. The rules as played are in the README; this page keeps the intent, the
decisions behind them and what is still open.

## Intent

A tactical positioning and levelling game: players choose where to place soldiers depending on terrain, distance and
army level — later also weapon type and weapon level. The rules must stay easy for a new player.

## Model

- **Turn:** one action — deploy 1–5 soldiers from a reserve of 35 on a free land tile, attack with one army, or pass.
  No economy: tiles produce nothing.
- **Orders are simultaneous:** everyone chooses on the board as the round began; all orders resolve together at the
  end of the round. Attacks fire together; armies deployed on the same tile fight, the bigger keeps the difference.
- **Power** = soldiers + level (0 to 3). Defending in the desert costs 1 power.
- **Range** depends on the attacker's tile, counted in orthogonal steps:

  | Terrain  | Range | Trait                                        |
  |----------|------:|----------------------------------------------|
  | Plain    | 1     | —                                            |
  | Forest   | 1     | cover: can only be attacked from a neighbour |
  | Desert   | 2     | exposed: −1 power when defending             |
  | Mountain | 3     | none — the range is its strength             |

- **Attack:** legal only against an enemy in range that the attacker out-powers. The target is destroyed, its tile
  freed, and the attacker gains a level.
- **End:** board full or 10 rounds. **Score:** total power of the armies on the board (soldiers + levels).

## Decisions

| # | Question                 | Decision                          | Rejected                     | Why                                              |
|---|--------------------------|-----------------------------------|------------------------------|--------------------------------------------------|
| 1 | When fights happen       | Attack is a separate action       | Auto-fire on deploy          | one cause per effect, readable and previewable   |
| 2 | Effect of a won attack   | Target destroyed, tile freed      | Target captured              | a freed tile reopens the board                   |
| 3 | Which attacks are legal  | Only winnable ones                | Failed attacks with a cost   | highlighted means beatable, no hidden penalty    |
| 4 | Levelling                | +1 level per kill, max 3          | No levels                    | rewards veterans, power capped at 8              |
| 5 | Line of sight            | Ignored: range crosses sea/armies | Blocked by terrain or armies | one rule for range until weapons exist           |
| 6 | Size cap per terrain     | None                              | 3 on mountains and deserts   | the mountain's strength is its range             |
| 7 | Equal power              | Not enough to attack              | Ties go to the attacker      | "out-power" is strict                            |
| 8 | Movement                 | Armies never move                 | Moving armies                | positioning is the deployment choice             |
| 9 | Score                    | Power held (soldiers + levels)    | Tiles held; tiles + 2 a kill | tiles tie 99.7%; power counts levels and size    |
| 10 | Turn order              | Simultaneous orders               | Rotation, alternation, komi  | seat no longer matters, for any pair of players  |
| 11 | Same-tile deployments   | Fight: bigger keeps the excess    | Both bounce; bigger only     | only rule where unlike styles are even (50/50)   |

## Extension points

- Power and range are each computed in one place (`WarRules.power`, `WarRules.range`): weapon type and level plug in
  there — a rifle could set the range instead of the terrain.
- `GameRules.preview` returns an attack's consequences from the same code that applies it; the window's preview relies
  on it, and a greedy bot can too.

## Open questions

1. **Draws between identical bots** — two greedy bots draw 17–19% of games, two "always deploy 5 on the longest
   range" bots 71%: same strategy, same board, same choices. Unlike pairings draw 0–7%. Seat-neutral tie-breakers
   (kills, tiles, terrain value) split at most a fifth of the mirror draws; only chance or seat order could split the
   rest. Balance is therefore measured with a draw counting as half a win.
2. **Balance** — the biggest risk: a mountain's range 3 covers up to 24 tiles of a 10×10 board. Between greedy bots
   on the current generator, attackers on deserts make 69% of kills and mountains 22% — to measure again on the target
   board. Levers: mountain range, desert exposure, max level, number of rounds.
3. **End condition** — every simulated game reaches round 10: attacks and clashes free tiles faster than the board
   fills.
4. **Weapons** — type and level, not designed yet; line of sight (decision 5) is revisited with them.
5. **Deploy hint** — show, before deploying, which enemies the new army could reach and beat.

## Seat balance (2026-10-05)

Each seed played from both seats, 10 rounds, 500 seeds for rotation and 1,000 for simultaneous orders; "seat 1 / seat 2"
are win rates in %, draws are the rest. "Aggressive" attacks whenever it can, else deploys 5 on the longest range.

| Turn order                    | greedy × greedy  | greedy × random | greedy × aggressive | random × random | aggressive × aggressive |
|-------------------------------|------------------|-----------------|---------------------|-----------------|-------------------------|
| Rotating first player         | 40 / 60          | 50 / 50         | 48 / 49             | 47 / 48         | 49 / 44                 |
| Rotation, seat 1 +11.5 points | 52 / 49          | 51 / 49         | 77 / 23             | 93 / 8          | 64 / 37                 |
| Simultaneous (decision 10)    | 41 / 41, 19 draw | 50 / 50         | 47 / 47             | 47 / 47         | 15 / 15, 71 draw        |
| Simultaneous, +11.5 points    | 83 / 17          | 51 / 49         | 89 / 11             | 93 / 7          | 91 / 9                  |

A points handicap is tuned to one pairing and breaks the others; simultaneous orders give both seats exactly the same
results in every pairing, which `SeatFairnessTest` checks by swapping seats. Plain alternation was worse than rotation
(seat 2: 73%). With fighting clashes, greedy beats the aggressive style 50% of the time (12% when clashes bounced).
