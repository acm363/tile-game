package boardgame.engine;

import boardgame.player.Player;

public record TurnStarted(Player player) implements GameEvent {
}
