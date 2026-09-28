# tile-game

A turn-based board game engine in Java. Players place characters on a rectangular grid of tiles; every tile can hold one
character and produces a resource. Each turn a player takes an action, harvests the resources of the tiles they occupy,
then pays upkeep for their characters.

The goal is a model that makes adding a new game cheap: the board, players, turn loop and events are shared, and a game
only defines its own rules. Two games are included — a war game (deploy, feed and capture armies) and a farming game
(deploy workers, sell harvests, pay wages) — playable in the console or in a Swing window.
