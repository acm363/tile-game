# tile-game

A turn-based board game engine in Java. Players place characters on a rectangular grid of tiles; every tile can hold one
character and produces a resource. Each turn a player takes an action, harvests the resources of the tiles they occupy,
then pays upkeep for their characters.

The goal is a model that makes adding a new game cheap: the board, players, turn loop and events are shared, and a game
only defines its own rules. Two games are included — a war game and a farming game — playable in the console or in a
Swing window. Player decisions are random for now; the engine is ready for human players.

## Requirements

JDK 21 or later. Gradle comes with the wrapper, nothing else to install.

## Build and test

```sh
./gradlew build
```

Compiles, runs the test suite and produces `build/libs/tile-game.jar`.

## Play

```sh
java -jar build/libs/tile-game.jar guerre Alice Bob
java -jar build/libs/tile-game.jar agricole Alice Bob Carol --gui
./gradlew run --args="guerre Alice Bob --gui --seed 42"
```

The first argument picks the game (`guerre` for war, `agricole` for farming), the other plain arguments are player names
— as many as you like.

| Option         | Effect                                                         |
|----------------|----------------------------------------------------------------|
| `--gui`        | shows the board in a Swing window instead of the console       |
| `--rows <n>`   | board rows (default 10)                                        |
| `--cols <n>`   | board columns (default 10)                                     |
| `--rounds <n>` | number of rounds (default 10 for war, 6 for farming)           |
| `--seed <n>`   | random seed, to replay the exact same game                     |

In the Swing window, **Tour suivant** / **Next turn** plays one turn and **Lecture auto** / **Auto play** plays the game
at a steady pace. Hovering a tile shows its terrain, owner, size and gold. A selector next to the buttons switches the
whole window between French and English, log included; the console stays in French. Texts live in
`src/main/resources/boardgame/ui/messages_<lang>.properties`.

## The board

Tiles are ocean, mountain, plain, desert or forest; land tiles produce rock, wheat, sand and wood respectively. The
generator guarantees that at least two thirds of the board is ocean and that every land tile touches another land tile.
Characters only stand on land, one per tile.

A game ends after its last round, or immediately — mid-turn — once no free land is left.

## War game

Each player starts with 35 warriors, 10 food and no gold, and each turn either deploys an army or does nothing.

- An army holds 1 to 5 warriors, at most 3 on mountains and deserts.
- On deployment, each neighbouring army (north, south, east, west) is compared with the new one:
  - a **weaker enemy** is halved; if it drops below one warrior it rallies to the deploying player, who earns 2 gold on
    the deployed army;
  - a **weaker ally** gains one warrior (within its tile's limit) and the deployed army earns 1 gold;
  - an army at least as strong is left alone.
- Against enemies, an army on a mountain counts two extra warriors.
- After harvesting, wheat turns into 5 food and wood into 1; rock and sand are worth nothing. Each army then eats its
  size in food, twice that in the desert. An army that cannot be fed is destroyed, its tile freed, and its owner gets
  1 gold.
- **Score:** player gold + army gold + a bonus per army by terrain (plain 1, forest 2, mountain and desert 4), plus 5 for
  holding at least 10 territories.

## Farming game

Each player starts with 15 gold. Each turn they deploy a worker, sell resources, or do nothing.

| Terrain  | Resource | Sale price | Wage | Income when doing nothing |
|----------|----------|-----------:|-----:|--------------------------:|
| Mountain | rock     | 8          | 5    | 0                         |
| Desert   | sand     | 5          | 3    | 2                         |
| Forest   | wood     | 2          | 1    | 1                         |
| Plain    | wheat    | 2          | 1    | 1                         |

- After the action, every worker harvests one unit of its tile's resource, sellable from the next turn on.
- Workers are then paid their wage from the player's gold; a worker who cannot be paid leaves the game and frees the
  tile.
- **Score:** the total gold paid to the workers still on the board.

## Architecture

| Package                 | Responsibility                                                                     |
|-------------------------|------------------------------------------------------------------------------------|
| `boardgame.board`       | terrain grid and occupancy — the single source of truth for who stands where        |
| `boardgame.unit`        | characters: `Army`, `Worker`                                                        |
| `boardgame.player`      | a player's gold and resources                                                       |
| `boardgame.engine`      | turn loop, actions, events, deciders                                                |
| `boardgame.war`         | war game rules and events                                                           |
| `boardgame.agriculture` | farming game rules and events                                                       |
| `boardgame.ui`          | console log and Swing view, both driven by engine events                            |
| `boardgame.app`         | command-line launcher                                                               |

A turn runs as: the `Decider` picks one of the actions the rules allow → the rules apply it → the game stops if no free
land is left → harvest → upkeep. Every change is published as a `GameEvent`; the console and the Swing window only
listen to events and never drive the rules.

Territories are never stored separately: they are derived from the board, so a capture or a removal can't leave a
player, a unit and a tile disagreeing.

### Adding a game

Implement `GameRules`: starting stock, legal actions, how an action applies, upkeep and scoring. Harvesting one resource
per territory comes for free and can be overridden. Emit your own `GameEvent` records for what happens, then add a
`GameKind` entry in `boardgame.app` to launch it.

### Adding a human player

Implement `Decider`: it receives the legal actions and returns the chosen one — from the keyboard, or from clicks in
the Swing window. `RandomDecider` is the current implementation.

## Rule interpretations

Where the rules are ambiguous, the engine chooses:

- a rallied enemy army keeps its size;
- the mountain bonus applies to both armies in an enemy confrontation, never between allies;
- harvested wheat and wood are always converted to food before feeding armies;
- a farming player's score ignores the gold they hold themselves.
