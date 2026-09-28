package boardgame.engine;

import boardgame.board.Resource;
import boardgame.player.Player;

import java.util.Map;

public record Harvested(Player player, Map<Resource, Integer> resources) implements GameEvent {
}
