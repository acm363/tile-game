# War game

**Status:** implemented in `boardgame.war`. The rules as played are in the README; this page keeps the intent, the
decisions behind them and what is still open.

## Intent

A tactical positioning and levelling game: players choose where to place soldiers depending on terrain, distance and
army level — later also weapon type and weapon level. The rules must stay easy for a new player.

## Model

- **Turn:** one action — deploy 1–5 soldiers from a reserve of 35 on a free land tile, attack with one army, or pass.
  No economy: tiles produce nothing.
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
- **End:** board full or 10 rounds. **Score:** 1 point per tile held.

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

## Extension points

- Power and range are each computed in one place (`WarRules.power`, `WarRules.range`): weapon type and level plug in
  there — a rifle could set the range instead of the terrain.
- `GameRules.preview` returns an attack's consequences from the same code that applies it; the window's preview relies
  on it, and a greedy bot can too.

## Open questions

1. **Balance** — the biggest risk: a mountain's range 3 covers up to 24 tiles of a 10×10 board. To be measured over
   many seeded games played by a greedy bot, not the random one. Levers: mountain range, desert exposure, max level,
   number of rounds.
2. **End condition** — attacks free tiles, so "board full" may rarely end a game; the balance runs will tell whether
   10 rounds is the real end.
3. **Weapons** — type and level, not designed yet; line of sight (decision 5) is revisited with them.
4. **Deploy hint** — show, before deploying, which enemies the new army could reach and beat.
