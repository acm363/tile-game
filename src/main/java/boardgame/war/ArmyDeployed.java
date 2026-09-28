package boardgame.war;

import boardgame.board.Position;
import boardgame.board.Terrain;
import boardgame.engine.GameEvent;
import boardgame.player.Player;

public record ArmyDeployed(Player player, Position position, Terrain terrain, int size) implements GameEvent {
}
