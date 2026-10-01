# Board generation

**Status:** designed, not implemented — `BoardGenerator` still produces the original distribution.

## Today

Land count random in [2, ⅓ of the board] — 2 to 33 tiles on 10×10 — placed as scattered pairs of adjacent tiles. Each
land tile is plain, forest, desert or mountain with 25% each, with no clustering.

## Target

| Aspect   | Design                                                                          |
|----------|---------------------------------------------------------------------------------|
| Land     | Fixed share ≈ 40% of the board (≈ 40 tiles on 10×10), never 2                   |
| Shape    | 1–2 connected islands (noise + island mask, see the Vagabond map article)       |
| Mountain | Drawn per game from the seed, uniformly between 10% and 15% of land             |
| Others   | Remaining land split plain : forest : desert = 40 : 25 : 20                     |
| Grouping | Mountains in ridges, forests in patches, plains fill the rest                   |

Resulting share of land tiles at the two extremes:

| Mountain drawn | Plain | Forest | Desert | On 40 land tiles (M / P / F / D) |
|----------------|-------|--------|--------|----------------------------------|
| 10%            | 42.4% | 26.5%  | 21.2%  | 4 / 17 / 11 / 8                  |
| 15%            | 40.0% | 25.0%  | 20.0%  | 6 / 16 / 10 / 8                  |

## Decisions

| # | Question           | Decision                         | Rejected        | Why                                                |
|---|--------------------|----------------------------------|-----------------|----------------------------------------------------|
| 1 | Land share         | Fixed ≈ 40%                      | A range         | the mountain share already varies per game         |
| 2 | Non-mountain ratio | Plain : forest : desert 40:25:20 | —               | kept as drafted                                    |
| 3 | Fairness           | First player rotates each round  | Mirrored boards | players have no sides, turn order is the real bias |

Decision 3 is implemented in `Game`, for every game.

## Open questions

1. One or two islands: fixed, or drawn per game?
2. How ridges and patches grow: noise thresholds, or seeded growth from a few starting tiles.
