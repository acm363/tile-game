package boardgame.agriculture;

import boardgame.board.Resource;
import boardgame.engine.GameEvent;
import boardgame.player.Player;

public record ResourceSold(Player player, Resource resource, int quantity, int gold) implements GameEvent {
}
