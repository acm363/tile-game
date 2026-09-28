package boardgame.engine;

import boardgame.player.Player;

public record Passed(Player player, int goldEarned) implements GameEvent {
}
