package boardgame.agriculture;

import boardgame.board.Position;
import boardgame.board.Terrain;
import boardgame.engine.GameEvent;
import boardgame.player.Player;

public record WorkerDeployed(Player player, Position position, Terrain terrain) implements GameEvent {
}
