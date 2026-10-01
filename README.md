# tile-game

A turn-based board game engine in Java. Players place characters on a rectangular grid of tiles; every tile can hold one
character and produces a resource. Each turn a player takes an action, harvests the resources of the tiles they occupy,
then pays upkeep for their characters.

The goal is a model that makes adding a new game cheap: the board, players, turn loop and events are shared, and a game
only defines its own rules. Two games are included — a war game and a farming game — playable in the console or in a
Swing window. Players are bots — greedy in the war game, random in the farming game — unless named with `--human` to be played
with the mouse.

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
./gradlew run --args="guerre Alice Bob --gui --human Alice"
```

The first argument picks the game (`guerre` for war, `agricole` for farming), the other plain arguments are player names
— as many as you like.

| Option         | Effect                                                         |
|----------------|----------------------------------------------------------------|
| `--gui`        | shows the board in a Swing window instead of the console       |
| `--human <n>`  | player `n` plays with the mouse (repeatable, needs `--gui`)    |
| `--rows <n>`   | board rows (default 10)                                        |
| `--cols <n>`   | board columns (default 10)                                     |
| `--rounds <n>` | number of rounds (default 10 for war, 6 for farming)           |
| `--seed <n>`   | random seed, to replay the exact same game                     |
| `--simulate <n>` | plays seeds n times from every seat between bots, prints the balance report |

In the Swing window, **Tour suivant** / **Next turn** plays one turn and **Lecture auto** / **Auto play** plays the game
at a steady pace. Hovering a tile shows its terrain, owner, size and gold. A selector next to the buttons switches the
whole window between French and English, log included; the console stays in French. Texts live in
`src/main/resources/boardgame/ui/messages_<lang>.properties`.

## The board

Tiles are ocean, mountain, plain, desert or forest; land tiles produce rock, wheat, sand and wood respectively. The
generator guarantees that at least two thirds of the board is ocean and that every land tile touches another land tile.
Characters only stand on land, one per tile.

Players take turns in the order given; the first player rotates each round so nobody always moves first. A game ends after its last round, or immediately — mid-turn — once no free land is left.

## War game

Each player starts with 35 soldiers in reserve, and each turn deploys an army, attacks with one, or does nothing.

- An army holds 1 to 5 soldiers, on any land tile. Its **power** is its soldiers plus its level; a new army is level 0.
- An army can attack an enemy within its range that it out-powers: the enemy is destroyed, its tile freed, and the
  attacker gains a level (at most 3). Only attacks that win are allowed.
- Range is counted in orthogonal steps and ignores ocean and armies in between:

  | Terrain  | Range | Trait                                        |
  |----------|------:|----------------------------------------------|
  | Plain    | 1     | —                                            |
  | Forest   | 1     | cover: can only be attacked from a neighbour |
  | Desert   | 2     | exposed: defends with 1 power less           |
  | Mountain | 3     | —                                            |

- There is no food and no gold: tiles produce nothing in this game.
- **Score:** 1 point per tile held.

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

Territories are never stored separately: they are derived from the board, so a removal can't leave a player, a unit
and a tile disagreeing.

### Adding a game

Implement `GameRules`: starting stock, legal actions, how an action applies, upkeep and scoring. Harvesting one resource
per territory comes for free and can be overridden. Emit your own `GameEvent` records for what happens, then add a
`GameKind` entry in `boardgame.app` to launch it.

### Human players

`HumanDecider` returns the action submitted by the window. Everything runs on the Swing thread: on a human's turn the
window asks `Game` for the upcoming player's legal actions and only calls `playTurn()` once a move is clicked. Bots keep
playing through **Next turn** and **Auto play**, which wait whenever a human is to move.

- **Deploy:** tiles where the selected size can go are outlined in white; hovering one shows a ghost of the army.
- **Attack:** armies with a winnable attack are outlined in red. Clicking one selects it (gold) and outlines its targets
  in red; clicking a target attacks, clicking anywhere else cancels. Hovering a target marks it with a × and the
  attacker with its next level.
- **Pass** and **Sell** are buttons.

The marks come from `GameRules.preview`, which the war rules compute with the same code that applies an attack. After
each move the touched tiles flash, a promoted army flashes gold, and a destroyed army or dismissed worker crumbles into
a pile of sand (a small falling-sand automaton) before fading out. An army's level shows as gold pips under its size.

## Rule interpretations

Where the rules are ambiguous, the engine chooses:

- an equal power is not enough to attack;
- an army never moves: it attacks from the tile it was deployed on;
- a farming player's score ignores the gold they hold themselves.
