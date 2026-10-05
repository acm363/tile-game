package boardgame.war;

import boardgame.board.Position;
import boardgame.engine.GameEvent;
import boardgame.player.Player;

import java.util.List;

public record DeploymentsClashed(Position position, List<Player> players) implements GameEvent {
}
