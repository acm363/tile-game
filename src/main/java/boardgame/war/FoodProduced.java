package boardgame.war;

import boardgame.engine.GameEvent;
import boardgame.player.Player;

public record FoodProduced(Player player, int food) implements GameEvent {
}
